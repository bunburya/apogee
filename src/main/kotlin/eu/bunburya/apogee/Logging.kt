package eu.bunburya.apogee

import eu.bunburya.apogee.models.Response
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.logging.*

/**
 * Custom formatter for logging access requests.
 */
class AccessFormatter: Formatter() {

    override fun format(logRecord: LogRecord): String {
        val dateTime = Instant.ofEpochMilli(logRecord.millis).atZone(ZoneId.systemDefault()).toOffsetDateTime()
        val formattedTime = ISO_OFFSET_DATE_TIME.format(dateTime)
        return "$formattedTime ${logRecord.message}\n"
    }

}

/**
 * Log access requests in the appropriate format. In general we'll only want to call this extension function on the
 * logger named "access". It's a bit hacky.
 */
fun Logger.access(response: Response) {
    finest("${response.request.ipString} ${response.request.content} ${response.statusCode}")
}


/**
 * Configure the loggers based on the given Config object.
 */
fun configureLogging(config: Config) {

    System.setProperty("java.util.logging.SimpleFormatter.format", "[%1\$s] %3\$s: %5\$s%n")
    val rootLogger = Logger.getLogger("eu.bunburya.apogee")  // Root logger for all "normal" logs

    // Remove default handler
    for (handler in rootLogger.handlers) rootLogger.removeHandler(handler)

    // Add file and/or console handlers as desired
    if (config.LOG_FILE != null) rootLogger.addHandler(FileHandler(config.LOG_FILE))
    else rootLogger.addHandler(ConsoleHandler())

    // Set desired log level
    rootLogger.level = Level.parse(config.LOG_LEVEL)
    for (handler in rootLogger.handlers) handler.level = Level.parse(config.LOG_LEVEL)


}

fun getAccessLogger(config: Config): Logger {
    val accessLogger = Logger.getLogger("access.eu.bunburya.apogee")
    for (handler in accessLogger.handlers) accessLogger.removeHandler(handler)
    if (config.ACCESS_FILE != null) accessLogger.addHandler(FileHandler(config.ACCESS_FILE))
    else accessLogger.addHandler(ConsoleHandler())
    accessLogger.level = Level.FINEST
    for (handler in accessLogger.handlers) {
        handler.level = Level.FINEST
        handler.formatter = AccessFormatter()
    }
    return accessLogger
}
