package eu.bunburya.apogee

fun main() {
    val config = Config(
        "/home/alan/bin/apogee/src/test/resources/certs/ks.p12",
        "/home/alan/bin/apogee/src/test/resources/srv",
        "localhost",
        1965,
        "testpass",
        null,
        LogLevel.DEBUG
    )
    val server = GeminiServer(config)
    server.run()
}