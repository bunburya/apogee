package eu.bunburya.apogee

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ServerTest {

    private lateinit var config: Config
    private lateinit var server: BasicServer

    @BeforeAll
    fun setUp() {
        config = Config(
            "/home/alan/bin/apogee/src/test/resources/certs/ks.p12",
            "/home/alan/bin/apogee/src/test/resources/srv",
            "localhost",
            1965,
            "testpass"
        )
        server = BasicServer(config)
    }


    /**
     * Test that we can establish a basic SSL socket without error.
     */
    @Test
    fun testTlsConnection() {
        server.getTlsSocket()
    }

}