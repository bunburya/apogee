package eu.bunburya.apogee

import java.io.File
import java.nio.file.Paths
import java.util.logging.Level
import java.util.regex.Pattern

/**
 * Main function to run the server; currently, this initialises a test setup.
 * TODO: Move config data out of this file.
 */

fun main(args: Array<String>) {
    val config = Config.fromTomlFile(File(args[1]))
    configureLogging(config)
    val server = GeminiServer(config)
    server.run()
}