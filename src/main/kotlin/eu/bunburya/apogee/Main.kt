package eu.bunburya.apogee

import java.util.logging.Level

fun main() {
    val config = Config(
        "/home/alan/bin/apogee/src/test/resources/certs/ks.p12",
        "/home/alan/bin/apogee/src/test/resources/srv",
        "localhost",
        1965,
        "testpass",
        null,
        Level.FINE,
        null
    )
    configureLogging(config)
    val server = GeminiServer(config)
    server.run()
}