package eu.bunburya.apogee.static

import eu.bunburya.apogee.*
import eu.bunburya.apogee.models.*
import eu.bunburya.apogee.utils.compileKeys
import eu.bunburya.apogee.utils.getFilePath
import eu.bunburya.apogee.utils.splitExt
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger

class FileServer(val config: Config) {

    private val logger = Logger.getLogger(javaClass.name)
    private val rootPath = Paths.get(config.DOCUMENT_ROOT).toAbsolutePath()
    private val dlf = DirectoryListingFactory(config)
    private val mimeOverrides = compileKeys(config.MIME_OVERRIDES)

    /**
     * Get appropriate MIME type for a file, first by checking the name against the overrides specified in the config,
     * then by checking if the file has a .gmi extension, and finally by using Java's standard library to probe the
     * filename.
     */
    private fun getMimeType(filePath: String): String {
        for ((pattern, mimeType) in mimeOverrides)
            if (pattern.matcher(filePath).find()) return mimeType
        val (_, ext) = splitExt(filePath)
        return if (ext == config.GEMINI_EXT) "text/gemini" else Files.probeContentType(Paths.get(filePath))
    }
    private fun getMimeType(filePath: Path): String {
        val str = filePath.toString()
        for ((pattern, mimeType) in mimeOverrides)
            if (pattern.matcher(str).find()) return mimeType
        val (_, ext) = splitExt(str)
        return if (ext == config.GEMINI_EXT) "text/gemini" else Files.probeContentType(filePath)
    }

    /**
     * Perform some basic checks that a path is safe.
     */
    private fun pathIsSafe(path: Path): Boolean {
        for (part in path.iterator()) {
            // Reject if ".." appears anywhere in path
            if (part.toString() == "..") return false
        }

        // Check whether target path is within root directory.
        return path.startsWith(rootPath)

    }

    private fun serveStaticFile(file: File, request: Request): Response {
        val path = file.path
        val mimetype = getMimeType(path)
        return SuccessResponse(request, mimetype, file.readBytes())
    }

    private fun directoryIndex(file: File): File? {
        val indexPath = Paths.get(file.path, config.INDEX_FILE).toString()
        logger.fine("Checking index file at $indexPath")
        val indexFile = File(indexPath)
        return if (indexFile.isFile) indexFile else null
    }


    private fun serveDirectory(file: File, request: Request): Response {
        if (!file.canExecute()) return NotFoundResponse(request)
        if (! request.content.endsWith('/'))
            return RedirectionResponse(request, "${request.content}/", true)
        val index = directoryIndex(file)
        return if (index != null) serveStaticFile(index, request) else dlf.directoryListing(file, request)

    }

    private fun serveStaticResource(targetPath: Path, request: Request): Response {
        // Check that path is safe
        if (! pathIsSafe(targetPath))
            return BadRequestResponse(request)

        // Check that resource exists and is readable
        val file = targetPath.toFile()
        if ((! (file.exists() && file.canRead())) || file.isHidden)
            return NotFoundResponse(request)

        // Check if resource is a directory
        if (file.isDirectory) return serveDirectory(file, request)

        return serveStaticFile(file, request)
    }

    fun serveResource(request: Request): Response {
        // Generate the path to work with
        if (request.uri == null) return BadRequestResponse(request)
        val targetPath = getFilePath(request, config)
        logger.fine("Got request for path: $targetPath")

        return serveStaticResource(targetPath, request)
    }

}