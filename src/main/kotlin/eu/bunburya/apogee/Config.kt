package eu.bunburya.apogee

import java.util.logging.*

data class Config (
    val KEY_STORE: String,
    val DOCUMENT_ROOT: String,
    val HOSTNAME: String = "localhost",
    val PORT: Int = 1065,
    val KEY_PASS: String? = null,
    val LOG_FILE: String? = null,
    val LOG_LEVEL: Level = Level.FINE,
    val ACCESS_FILE: String? = null
)