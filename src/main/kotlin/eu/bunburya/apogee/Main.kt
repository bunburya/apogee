package eu.bunburya.apogee

import kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME
import java.io.File

/**
 * Main function to run the server; currently, this initialises a test setup.
 */

fun main(args: Array<String>) {
    val config = Config.fromTomlFile(File(args[0]))
    //System.setProperty(IO_PARALLELISM_PROPERTY_NAME, "1024")
    //println("MAX THREADS: ${System.getProperty(IO_PARALLELISM_PROPERTY_NAME)}")
    configureLogging(config)
    val server = GeminiServer(config)
    server.run()
}