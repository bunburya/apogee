package eu.bunburya.apogee

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MainTest {
    private lateinit var config: Config
    private lateinit var server: BasicServer

    @BeforeAll
    fun setUp() {
        config = Config(
            "/home/alan/bin/apogee/src/test/resources/certs/openssl_test/test.p12",
            "/home/alan/bin/apogee/src/test/resources/srv",
            "localhost",
            1965,
            "testpass"
        )
        server = BasicServer(config)
    }
    @Test
    fun runServer() {
        //server.run()

    }

}