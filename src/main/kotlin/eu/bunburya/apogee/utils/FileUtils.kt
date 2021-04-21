package eu.bunburya.apogee.utils

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