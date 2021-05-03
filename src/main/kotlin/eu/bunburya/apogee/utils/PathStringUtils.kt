package eu.bunburya.apogee.utils

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.Request
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Split a file name into its "base" (without extension) and its extension.
 */
fun splitExt(fullName: String): Pair<String, String?> {
    val name: String
    val ext: String?
    val dotIndex = fullName.lastIndexOf('.')
    if (dotIndex <= 0) {
        name = fullName
        ext = null
    } else {
        name = fullName.substring(0 until dotIndex)
        ext = fullName.substring(dotIndex + 1)
    }
    return Pair(name, ext)
}

fun urlEncode(url: String): String {
    return url.replace(" ", "%20")
}

fun urlDecode(url: String): String {
    return url.replace("%20", " ")
}

fun fileIsInDirectory(filePath: Path, dirPath: Path): Boolean {
    return filePath.normalize().startsWith(dirPath.normalize())
}

fun fileIsInDirectory(filePath: String, dirPath: String): Boolean {
    return fileIsInDirectory(Paths.get(filePath), Paths.get(dirPath))
}

/**
 * Convert the content of a request to an absolute file path. The file
 * path will begin with the document root specified in the config, but
 * we don't check for ".." so the actual path may be outside the
 * document root.
 *
 * Assumes that the request has a validly formed path (so this should be
 * checked prior to calling this function).
 */
fun getFilePath(request: Request, config: Config): Path {
    require(request.uri != null)
    return Paths.get(config.DOCUMENT_ROOT, request.uri.path.replace('/', File.separatorChar))
        .toAbsolutePath()
}

/**
 * Convert the content to a request to an absolute file path which is
 * resolved to remove redundant parts, including "..". Therefore, the
 * resulting file path may not be within the document root.
 */
fun resolvePath(request: Request, config: Config): Path = getFilePath(request, config).normalize()
