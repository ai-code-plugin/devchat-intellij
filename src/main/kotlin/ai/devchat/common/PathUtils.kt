package ai.devchat.common

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

object PathUtils {
    val workPath: String = Paths.get(System.getProperty("user.home"), ".chat").toString()
    var pythonCommand: String = "python"
    var pythonForWorkflows: String = "python"
    val pythonPath: String = Paths.get(workPath, "site-packages").toString()

    fun copyResourceDirToPath(resourceDir: String, outputPath: String) {
        val uri = javaClass.getResource(resourceDir)!!.toURI()
        val path = if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            fileSystem.getPath("/$resourceDir")
        } else {
            Paths.get(uri)
        }

        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativeDir = dir.toString().substring(path.toString().length)
                val targetPath = Paths.get(outputPath, relativeDir)
                return if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath)
                    FileVisitResult.CONTINUE
                } else {
                    if (relativeDir == "") FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
                }
            }

            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = file.toString().substring(path.toString().length)
                val targetFilePath = Paths.get(outputPath, relativePath)
                if (!Files.exists(targetFilePath)) {
                    Files.copy(file, targetFilePath)
                }
                return FileVisitResult.CONTINUE
            }
        })
    }
}