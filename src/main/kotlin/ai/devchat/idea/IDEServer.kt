package ai.devchat.idea

import ai.devchat.common.ProjectUtils
import ai.devchat.idea.balloon.DevChatNotifier
import ai.devchat.idea.settings.DevChatSettingsState
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.awt.Point
import java.io.File
import java.net.ServerSocket


const val START_PORT: Int = 31800

@Serializable
data class Position(val line: Int, val character: Int)
@Serializable
data class Range(val start: Position, val end: Position)
@Serializable
data class Location(val abspath: String, val range: Range)
@Serializable
data class LocationWithText(val abspath: String, val range: Range, val text: String)
@Serializable
data class SymbolNode(val name: String?, val kind: String, val range: Range, val children: List<SymbolNode>)
class IDEServer(private var project: Project) {
    private var server: ApplicationEngine? = null

    fun start() {
        val port = getAvailablePort(START_PORT)
        ProjectUtils.ideServerPort = port
        server = embeddedServer(Netty, port=port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/definitions") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val definitions = withContext(Dispatchers.IO)  { project.findDefinitions(path, line, column) }
                    call.respond(definitions)
                }


                get("/references") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val references = withContext(Dispatchers.IO)  { project.findReferences(path, line, column) }
                    call.respond(references)
                }

                get("/get_document_symbols") {
                    val path = call.parameters["abspath"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val symbols = withContext(Dispatchers.IO)  { project.findSymbols(path) }
                    call.respond(symbols!!)
                }

                get("/find_type_def_locations") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val typeDef = withContext(Dispatchers.IO)  {
                        val psiFile = project.getPsiFile(path)
                        val editor = project.getEditorForFile(psiFile)
                        val offset = project.computeOffset(psiFile, line, column)
                        findTypeDefinition(editor, offset)
                    }
                    call.respond(typeDef)
                }

                post("/ide_language") {
                    call.respond(mapOf("result" to DevChatSettingsState.instance.language))
                }

                post("/ide_name") {
                    call.respond(mapOf("result" to "intellij"))
                }

                post("/get_selected_range") {
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    editor?.let {
                        call.respond(mapOf("result" to it.selection()))
                    } ?: call.respond(HttpStatusCode.NoContent)
                }
                post("/get_selected_range") {
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    editor?.let {
                        call.respond(mapOf("result" to it.selection()))
                    } ?: call.respond(HttpStatusCode.NoContent)
                }
                post("/get_visible_range") {
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    editor?.let {
                        call.respond(mapOf("result" to it.visibleRange()))
                    } ?: call.respond(HttpStatusCode.NoContent)
                }
                post("/diff_apply") {
                    val body = call.receive<Map<String, String>>()
                    val filePath: String? = body["filepath"]
                    var content: String? = body["content"]
                    if (content.isNullOrEmpty() && !filePath.isNullOrEmpty()) {
                        content = File(filePath).readText()
                    }
                    if (content.isNullOrEmpty()) {
                        content = ""
                    }
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    editor?.diffWith(content)
                    call.respond(mapOf("result" to true))
                }
            }
        }

        // Register listener to stop the server when project closed
        ProjectManager.getInstance().addProjectManagerListener(
            project, object: ProjectManagerListener {
                override fun projectClosed(project: Project) {
                    super.projectClosed(project)
                    DevChatNotifier.info("Stopping IDE server...")
                    server?.stop(1_000, 2_000)
                }
            }
        )

        server?.start(wait = false)
        DevChatNotifier.info("IDE server started at $port.")
    }
}

fun Editor.selection(): LocationWithText {
    val selectionModel = this.selectionModel
    var startPosition: LogicalPosition? = null
    var endPosition: LogicalPosition? = null
    var selectedText: String? = null
    ApplicationManager.getApplication().invokeAndWait {
        if (selectionModel.hasSelection()) {
            val startOffset = selectionModel.selectionStart
            val endOffset = selectionModel.selectionEnd
            startPosition = this.offsetToLogicalPosition(startOffset)
            endPosition = this.offsetToLogicalPosition(endOffset)
            selectedText = selectionModel.selectedText
        }
    }
    return LocationWithText(
        this.virtualFile.path, Range(
            start = Position(startPosition?.line ?: -1, startPosition?.column ?: -1),
            end = Position(endPosition?.line ?: -1, endPosition?.column ?: -1),
        ), selectedText ?: ""
    )
}

fun Editor.visibleRange(): LocationWithText {
    var firstVisibleLine = 0
    var lastVisibleLine = 0
    var lastVisibleColumn = 0
    var visibleText = ""
    ApplicationManager.getApplication().invokeAndWait {
        val visibleArea = scrollingModel.visibleArea
        firstVisibleLine = xyToLogicalPosition(Point(visibleArea.x, visibleArea.y)).line
        lastVisibleLine = xyToLogicalPosition(Point(visibleArea.x, visibleArea.y + visibleArea.height)).line
        val startOffset = document.getLineStartOffset(firstVisibleLine)
        val endOffset = document.getLineEndOffset(lastVisibleLine)
        visibleText = document.getText(TextRange.create(startOffset, endOffset))
        lastVisibleColumn = offsetToLogicalPosition(endOffset).column
    }

    return LocationWithText(
        this.virtualFile.path, Range(
            start = Position(firstVisibleLine, 0),
            end = Position(lastVisibleLine, lastVisibleColumn),
        ), visibleText
    )
}

fun Editor.diffWith(newText: String) {
    ApplicationManager.getApplication().invokeLater {
        val fileType = virtualFile.fileType
        val localContent = if (selectionModel.hasSelection()) selectionModel.selectedText else document.text
        val contentFactory = DiffContentFactory.getInstance()
        val diffRequest = SimpleDiffRequest(
            "Code Diff",
            contentFactory.create(localContent!!, fileType),
            contentFactory.create(newText, fileType),
            "Old code",
            "New code"
        )
        DiffManager.getInstance().showDiff(project, diffRequest)
    }

}

fun getLocParams(parameters: Parameters): Triple<String, Int, Int>? {
    val path = parameters["abspath"]
    val line = parameters["line"]?.toIntOrNull()
    val column = parameters["character"]?.toIntOrNull()

    if (path == null || line == null || column == null) {
        return null
    }

    return Triple(path, line, column)
}

fun getAvailablePort(startPort: Int): Int {
    var port = startPort
    while (true) {
        try {
            ServerSocket(port).use { return port }
        } catch (ex: Exception) {
            port++
        }
    }
}

fun Project.getPsiFile(filePath: String): PsiFile = ReadAction.compute<PsiFile, Throwable> {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    PsiManager.getInstance(this).findFile(virtualFile!!)
}

fun Project.computeOffset(
    psiFile: PsiFile,
    lineNumber: Int?,
    columnIndex: Int?,
): Int = ReadAction.compute<Int, Throwable> {
    if (lineNumber == null || columnIndex == null) return@compute -1
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile)
    document!!.getLineStartOffset(lineNumber) + columnIndex
}

fun Project.getEditorForFile(psiFile: PsiFile): Editor {
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile)
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
        editor = EditorFactory.getInstance().createEditor(document!!, this)
    }
    return editor!!
}

fun PsiElement.toSymbolNode(): List<SymbolNode> {
    return if (this is PsiNamedElement) {
        listOf(SymbolNode(
            this.name,
            this.javaClass.name,
            this.getRange(),
            children = this.children.flatMap { it.toSymbolNode() }
        ))
    } else {
        this.children.flatMap { it.toSymbolNode() }
    }
}

fun PsiElement.getRange(): Range {
    val document = PsiDocumentManager.getInstance(this.project).getDocument(this.containingFile)

    fun calculatePosition(offset: Int): Position {
        // line numbers are 0-based
        val line = document!!.getLineNumber(offset)
        // verifying the line number correctness
        if (line < 0 || line >= document.lineCount) {
            throw RuntimeException("Error locating element: Got an invalid line number $line")
        }
        val lineStartOffset = document.getLineStartOffset(line)
        val column = offset - lineStartOffset
        return Position(line, column)
    }

    return Range(calculatePosition(this.startOffset), calculatePosition(this.endOffset))
}

fun PsiElement.getLocation(): Location {
    return Location(this.containingFile.virtualFile.path, this.getRange())
}

fun Project.findReferences(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<Location> = ReadAction.compute<List<Location>, Throwable> {
    val psiFile = this.getPsiFile(filePath)
    val offset = this.computeOffset(psiFile, lineNumber, columnIndex)
    ProgressManager.getInstance().runProcess(Computable {
        PsiTreeUtil.findElementOfClassAtOffset(
            psiFile, offset,  PsiNamedElement::class.java, false
        )?.let {ele ->
            ReferencesSearch.search(ele).map {it.element.getLocation()}
        }.orEmpty()
    }, EmptyProgressIndicator())
}

fun Project.findDefinitions(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<Location> = ReadAction.compute<List<Location>, Throwable> {
    val psiFile = this.getPsiFile(filePath)
    val offset = this.computeOffset(psiFile, lineNumber, columnIndex)
    ProgressManager.getInstance().runProcess(Computable {
        listOfNotNull(psiFile.findReferenceAt(offset)?.resolve()?.let {
            it.getLocation()
        })
    }, EmptyProgressIndicator())
}

fun Project.findSymbols(
    filePath: String,
    lineNumber: Int? = null,
    columnIndex: Int? = null,
): List<SymbolNode>? = ReadAction.compute<List<SymbolNode>?, Throwable> {
    val psiFile = this.getPsiFile(filePath)
    val offset = this.computeOffset(psiFile, lineNumber, columnIndex)
    if (offset == -1) {
        psiFile.toSymbolNode()
    }

    PsiTreeUtil.findElementOfClassAtOffset(
        psiFile, offset,  PsiNamedElement::class.java, false
    )?.toSymbolNode()
}

fun findTypeDefinition(
    editor: Editor,
    offset: Int,
): List<Location> = ReadAction.compute<List<Location>, Throwable> {
    GotoTypeDeclarationAction.findSymbolType(editor, offset)?.let {
        listOf(it.getLocation())
    }.orEmpty()
}
