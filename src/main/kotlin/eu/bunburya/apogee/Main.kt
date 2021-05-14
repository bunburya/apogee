package eu.bunburya.apogee

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.File
import java.io.FileNotFoundException
import java.lang.IllegalStateException
import java.lang.RuntimeException

/**
 * Main function to run the server; currently, this initialises a test setup.
 */

fun main(args: Array<String>) {
    val parser = ArgParser("apogee")
    val configFile by parser.option(ArgType.String, fullName = "config", shortName = "c",
        description = "Path to configuration file").required()
    parser.parse(args)
    val config = try {
        Config.fromTomlFile(File(configFile))
    } catch (e: Exception) {
        when (e.cause) {
            is FileNotFoundException -> System.err.println("Config file not found.")
            is IllegalStateException -> System.err.println("Could not parse config file.")
            else -> {
                System.err.println("Encountered uncaught exception when initialising application.")
                e.printStackTrace(System.err)
            }
        }
        return
    }
    configureLogging(config)
    val server = GeminiServer(config)
    server.run()
}