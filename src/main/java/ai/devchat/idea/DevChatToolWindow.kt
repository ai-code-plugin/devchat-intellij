package ai.devchat.idea

import ai.devchat.common.Log.error
import ai.devchat.common.Log.info
import ai.devchat.common.Log.setLevelInfo
import ai.devchat.devchat.DevChatActionHandler.Companion.instance
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class DevChatToolWindow : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(
            DevChatToolWindowContent(project).content,
            "",
            false
        )
        contentManager.addContent(content)
        val devChatThread = DevChatSetupThread(project)
        devChatThread.start()
    }
}

internal class DevChatToolWindowContent(project: Project) {
    val content: JPanel
    private val project: Project

    init {
        setLevelInfo()
        this.project = project
        content = JPanel(BorderLayout())
        // Check if JCEF is supported
        if (!JBCefApp.isSupported()) {
            error("JCEF is not supported.")
            content.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
            return
        }
        val jbCefBrowser = JBCefBrowser()
        content.add(jbCefBrowser.component, BorderLayout.CENTER)

        // Read static files
        var htmlContent = readStaticFile("/static/main.html")
        if (htmlContent!!.isEmpty()) {
            error("main.html is missing.")
            htmlContent = "<html><body><h1>Error: main.html is missing.</h1></body></html>"
        }
        var jsContent = readStaticFile("/static/main.js")
        if (jsContent!!.isEmpty()) {
            error("main.js is missing.")
            jsContent = "console.log('Error: main.js not found')"
        }
        val HtmlWithJsContent = insertJStoHTML(htmlContent, jsContent)
        info("main.html and main.js are loaded.")

        // enable dev tools
        val myDevTools = jbCefBrowser.cefBrowser.devTools
        JBCefBrowser.createBuilder()
            .setCefBrowser(myDevTools)
            .setClient(jbCefBrowser.jbCefClient)
            .build()

        // initialize DevChatActionHandler
        val cefBrowser = jbCefBrowser.cefBrowser
        val handler = instance
        handler!!.initialize(cefBrowser, project)

        // initialize JSJavaBridge
        val jsJavaBridge = JSJavaBridge(jbCefBrowser)
        jsJavaBridge.registerToBrowser()
        jbCefBrowser.loadHTML(HtmlWithJsContent!!)
    }

    private fun readStaticFile(fileName: String): String? {
        val contentBuilder = StringBuilder()
        try {
            val url = javaClass.getResource(fileName)
            if (url == null) {
                println("File not found: $fileName")
                return null
            }
            val reader = BufferedReader(InputStreamReader(url.openStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                contentBuilder.append(line)
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return contentBuilder.toString()
    }

    private fun insertJStoHTML(html: String?, js: String?): String? {
        var html = html
        val index = html!!.lastIndexOf("<script>")
        val endIndex = html.lastIndexOf("</script>")
        if (index != -1 && endIndex != -1) {
            html = """
                ${html.substring(0, index + "<script>".length)}
                $js${html.substring(endIndex)}
                """.trimIndent()
        }
        return html
    }
}
