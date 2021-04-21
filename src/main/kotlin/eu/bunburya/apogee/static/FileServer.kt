package eu.bunburya.apogee.static

import eu.bunburya.apogee.*
import io.netty.util.CharsetUtil
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger

class FileServer(val config: Config) {

    private val logger = Logger.getLogger(javaClass.name)
    private val rootPath = Paths.get(config.DOCUMENT_ROOT).toAbsolutePath()
    private val dlf = DirectoryListingFactory(config)


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
        val dotIndex = path.lastIndexOf('.')
        val mimetype = if (dotIndex > 0 && path.substring(dotIndex+1) == config.GMI_EXT) "text/gemini"
            else Files.probeContentType(file.toPath())
        return SuccessResponse(mimetype, file.readBytes(), request)
    }

    private fun directoryIndex(file: File): File? {
        val indexPath = Paths.get(file.path, config.INDEX_FILE).toString()
        logger.fine("Checking index file at $indexPath")
        val indexFile = File(indexPath)
        return if (indexFile.isFile) indexFile else null
    }


    private fun serveDirectory(file: File, request: Request): Response {
        if (!file.canExecute()) return NotFoundResponse(config.NOT_FOUND_MSG, request)
        if (! request.content.endsWith('/'))
            return RedirectionResponse("${request.content}/", true, request)
        val index = directoryIndex(file)
        return if (index != null) serveStaticFile(index, request) else dlf.directoryListing(file, request)

    }

    private fun serveStaticResource(targetPath: Path, request: Request): Response {
        // Check that path is safe
        if (! pathIsSafe(targetPath))
            return BadRequestResponse(config.NOT_FOUND_MSG, request)

        // Check that resource exists and is readable
        val file = targetPath.toFile()
        if ((! (file.exists() && file.canRead())) || file.isHidden)
            return NotFoundResponse(config.NOT_FOUND_MSG, request)

        // Check if resource is a directory
        if (file.isDirectory) return serveDirectory(file, request)

        return serveStaticFile(file, request)
    }

    fun serveResource(request: Request): Response {
        // Generate the path to work with
        val path = request.uri?.path ?: return BadRequestResponse("Badly formed path.", request)
        val targetPath = Paths.get(config.DOCUMENT_ROOT, path.replace('/', File.separatorChar))
            .normalize()
            .toAbsolutePath()
        logger.fine("Got request for path: ${targetPath.toString()}")

        return serveStaticResource(targetPath, request)
    }

}