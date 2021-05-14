package eu.bunburya.apogee

import com.moandjiezana.toml.Toml
import eu.bunburya.apogee.utils.compileKeys
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.jvm.Throws

class ConfigError(msg: String): Exception(msg)


/**
 * The main configuration class.  Most values are stored as simple primitive types (such as strings instead of
 * filepaths, etc), in order to make it easier to read config values from a file.
 */
data class Config (
    val PORT: Int = defaults.PORT,
    val HOSTNAME: String = defaults.HOSTNAME,
    val CERT_FILE: String = defaults.CERT_FILE,
    val KEY_FILE: String = defaults.KEY_FILE,
    val DOCUMENT_ROOT: String = defaults.DOCUMENT_ROOT,

    val ACCESS_FILE: String? = defaults.ACCESS_FILE,
    val LOG_FILE: String? = defaults.LOG_FILE,
    val LOG_LEVEL: String = defaults.LOG_LEVEL,

    val GEMINI_EXT: String = defaults.GEMINI_EXT,
    val INDEX_FILE: String = defaults.INDEX_FILE,
    val DIR_SORT_METHOD: String = defaults.DIR_SORT_METHOD,

    val CGI_TIMEOUT: Long = defaults.CGI_TIMEOUT,
    val CGI_PATHS: List<String> = defaults.CGI_PATHS,
    val SCGI_PATHS: Map<String, String> = defaults.SCGI_PATHS,

    val MIME_OVERRIDES: Map<String, String> = defaults.MIME_OVERRIDES,

    val TEMP_REDIRECTS: Map<String, String> = defaults.TEMP_REDIRECTS,
    val PERM_REDIRECTS: Map<String, String> = defaults.PERM_REDIRECTS,

    val CLIENT_CERT_ZONES: Map<String, Collection<String>> = defaults.CLIENT_CERT_ZONES,
) {
    companion object {

        /**
         * Convert a map created from a TOML table (which is a Map<String, Any>) to a Map<String, valueType>.
         * Will throw a ConfigError if values cannot be cast to valueType.
         *
         * Also strip surrounding quotes from the key.
         */
        @Throws(ClassCastException::class)
        private fun <valueType> tableToMap(table: Toml?, default: Map<String, valueType>): Map<String, valueType> {
            if (table == null) return default
            val inMap = table.toMap()
            val outMap: MutableMap<String, valueType> = mutableMapOf()
            try {
                for ((k, v) in inMap) outMap[k.removeSurrounding("\"")] = v as valueType
            } catch (e: ClassCastException) {
                throw ConfigError("Error trying to parse map from config file: ${e.message}")
            }
            return outMap.toMap()
        }

        val defaults = Config(
            PORT = 1965,
            HOSTNAME = "localhost",
            CERT_FILE = "cert.pem",
            KEY_FILE = "key.pem",
            DOCUMENT_ROOT = "/var/gemini/",
            ACCESS_FILE = null,
            LOG_FILE = null,
            LOG_LEVEL = "WARNING",
            GEMINI_EXT = "gmi",
            INDEX_FILE = "index.gmi",
            DIR_SORT_METHOD = "NAME",
            CGI_TIMEOUT = 10L,
            CGI_PATHS = listOf(),
            SCGI_PATHS = mapOf(),
            MIME_OVERRIDES = mapOf(),
            TEMP_REDIRECTS = mapOf(),
            PERM_REDIRECTS = mapOf(),
            CLIENT_CERT_ZONES = mapOf(),
        )

        /**
         * Read a configuration from the given TOML file. Keys not specified in the TOML file will get the default
         * values.
         */
        fun fromTomlFile(file: File): Config {
            val toml = Toml().read(file)
            return Config(
                PORT = toml.getLong("PORT", null)?.toInt() ?: defaults.PORT,
                HOSTNAME = toml.getString("HOSTNAME", defaults.HOSTNAME),
                CERT_FILE = toml.getString("CERT_FILE", defaults.CERT_FILE),
                KEY_FILE = toml.getString("KEY_FILE", defaults.KEY_FILE),
                DOCUMENT_ROOT = toml.getString("DOCUMENT_ROOT", defaults.DOCUMENT_ROOT),
                ACCESS_FILE = toml.getString("ACCESS_FILE", defaults.ACCESS_FILE),
                LOG_FILE = toml.getString("LOG_FILE", defaults.LOG_FILE),
                LOG_LEVEL = toml.getString("LOG_LEVEL", defaults.LOG_LEVEL),
                GEMINI_EXT = toml.getString("GEMINI_EXT", defaults.GEMINI_EXT),
                INDEX_FILE = toml.getString("INDEX_FILE", defaults.INDEX_FILE),
                DIR_SORT_METHOD = toml.getString("DIR_SORT_METHOD", defaults.DIR_SORT_METHOD),
                CGI_TIMEOUT = toml.getLong("CGI_TIMEOUT", defaults.CGI_TIMEOUT),
                CGI_PATHS = toml.getList("CGI_PATHS", defaults.CGI_PATHS),
                SCGI_PATHS = tableToMap(toml.getTable("SCGI_PATHS"), defaults.SCGI_PATHS),
                MIME_OVERRIDES = tableToMap(toml.getTable("MIME_OVERRIDES"), defaults.MIME_OVERRIDES),
                TEMP_REDIRECTS = tableToMap(toml.getTable("TEMP_REDIRECTS"), defaults.TEMP_REDIRECTS),
                PERM_REDIRECTS = tableToMap(toml.getTable("PERM_REDIRECTS"), defaults.PERM_REDIRECTS),
                CLIENT_CERT_ZONES = tableToMap(toml.getTable("CLIENT_CERT_ZONES"), defaults.CLIENT_CERT_ZONES),
            )
        }
    }

    val errorLogOutputStream: OutputStream get() =
        if (LOG_FILE != null) FileOutputStream(File(LOG_FILE)) else System.err

}