package eu.bunburya.apogee

import java.io.File
import java.util.logging.Level

fun main() {
    val config = Config(
        "/home/alan/bin/apogee/src/test/resources/srv/capsule",
        "localhost",
        1965,
        null,
        Level.FINE,
        null,
        KEY_FILE = File("/home/alan/bin/apogee/src/test/resources/server_certs/openssl_generated/test_key.pem"),
        CERT_FILE = File("/home/alan/bin/apogee/src/test/resources/server_certs/openssl_generated/test_cert.pem")
    )
    configureLogging(config)
    val server = GeminiServer(config)
    server.run()
}