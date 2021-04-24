package eu.bunburya.apogee.utils

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