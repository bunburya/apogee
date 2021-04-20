package eu.bunburya.apogee

import java.io.PrintStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.temporal.TemporalAccessor

/**
 * Basic logging.
 */


enum class LogLevel (val severity: Int) {
    NOTSET(0),
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40),
    CRITICAL(50);

    constructor(severity: String): this(
        when (severity.toLowerCase()) {
            "notset" -> 0
            "debug" -> 10
            "info" -> 20
            "warn" -> 30
            "error" -> 40
            "critical" -> 50
            else -> throw IllegalArgumentException("Invalid severity string: $severity")

        }
    )
}

/**
 * A class which can be used to create log handlers, including both "standard" log handlers (to log error messages etc)
 * and access log handlers (to log access requests).
 *
 * @param logFile The path to the file to log to. If not provided, messages will be logged to standard error.
 */
class LogHandler(logFile: String? = null) {

    var logger: Logger? = null
    private val stream = if (logFile != null) PrintStream(logFile) else System.err
    private fun write(text: String) {
        stream.println(text)
        stream.flush()
    }

    private fun formattedTime(time: TemporalAccessor? = null): String {
        val timeFormat = ISO_OFFSET_DATE_TIME
        return if (time == null) timeFormat.format(ZonedDateTime.now()) else timeFormat.format(time)
    }

    fun log(msg: String, logLevel: LogLevel) {
        val time = formattedTime()
        val loggerName = logger?.name ?: ""
        write("$time $loggerName ${logLevel.name} $msg")
    }

    fun log(response: Response) {
        val time = formattedTime()
        write("$time ${response.request.ipString} ${response.request.content} ${response.statusCode}")
    }

}

class Logger(val name: String) {

    companion object {
        fun fromConfig(name: String, config: Config): Logger {
            val logger = Logger(name)
            logger.addLogHandler(config.LOG_LEVEL, LogHandler(config.LOG_FILE))
            logger.addAccessLogHandler(LogHandler(config.ACCESS_FILE))
            return logger
        }
    }

    private val logHandlers = mutableListOf<Pair<LogLevel, LogHandler>>()
    private val accessLogHandlers = mutableListOf<LogHandler>()

    fun addLogHandler(logLevel: LogLevel, logHandler: LogHandler) {
        logHandler.logger = this
        logHandlers.add(Pair(logLevel, logHandler))
    }

    fun addAccessLogHandler(logHandler: LogHandler) {
        logHandler.logger = this
        accessLogHandlers.add(logHandler)
    }

    /**
     * Log a message (other than an access message). Sends the message to all
     * relevant handlers.
     *
     * @param logLevel The level/severity of the message.
     * @param msg The message to log.
     */
    fun log(msg: String, logLevel: LogLevel) {
        for ((handlerLevel, handler) in logHandlers) {
            if (logLevel.severity >= handlerLevel.severity) {
                handler.log(msg, logLevel)
            }
        }
    }

    fun debug(msg: String) = log(msg, LogLevel.DEBUG)
    fun info(msg: String) = log(msg, LogLevel.INFO)
    fun warn(msg: String) = log(msg, LogLevel.WARN)
    fun error(msg: String) = log(msg, LogLevel.ERROR)
    fun critical(msg: String) = log(msg, LogLevel.CRITICAL)

    /**
     * Log an access message. Sends the message to all relevant access log handlers.
     *
     * @param reponse The Response object representing the completed request.
     */
    fun access(response: Response) {
        for (handler in accessLogHandlers) {
            handler.log(response)
        }
    }

}