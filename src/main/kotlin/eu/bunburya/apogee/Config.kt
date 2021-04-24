package eu.bunburya.apogee

import java.io.File
import java.util.logging.*

enum class DirectorySortMethod {
    NAME,
    TIME,
    SIZE
}

data class Config (
    val DOCUMENT_ROOT: String,
    val HOSTNAME: String = "localhost",
    val PORT: Int = 1065,
    val LOG_FILE: String? = null,
    val LOG_LEVEL: Level = Level.FINE,
    val ACCESS_FILE: String? = null,
    val GMI_EXT: String = "gmi",
    val INDEX_FILE: String = "index.gmi",
    val NOT_FOUND_MSG: String = "Resource not found or not accessible.",
    val DIR_SORT_METHOD: DirectorySortMethod = DirectorySortMethod.NAME,
    val KEY_FILE: File? = null,
    val CERT_FILE: File? = null,
)