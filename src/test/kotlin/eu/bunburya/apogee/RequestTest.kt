package eu.bunburya.apogee

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.SocketAddress

internal class RequestTest {

    private val veryLongUrl = mutableListOf<String>("gemini://").apply {
        repeat(251) {
            this.add("test")
        }
        add(".bunburya.eu")
    }.joinToString("")

    private val expected = mapOf<String, RequestValidity>(
        "gemini://gemini.bunburya.eu" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu:1965" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu/" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu:1965/" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu/test_path" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu/test_path/" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu:1965/test_path" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu/test_path?test_query" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu/test_path?test_query&param1=val1&param2=val2&param3" to RequestValidity.OK,
        "gemini://gemini.bunburya.eu:1965/test_path?test_query" to RequestValidity.OK,

        "gemini://0.0.0.0/" to RequestValidity.OK,
        "gemini://localhost" to RequestValidity.OK,

        "gemini.bunburya.eu" to RequestValidity.NOT_GEMINI_URI,
        "http://gemini.bunburya.eu" to RequestValidity.NOT_GEMINI_URI,
        veryLongUrl to RequestValidity.URI_TOO_LARGE,
        "gemini:///test_path" to RequestValidity.NO_HOST,
        "gemini://test_user@gemini.bunburya.eu/" to RequestValidity.USERINFO,
        "gemini://test_user@bunburya.eu/" to RequestValidity.USERINFO,
        "gemini://gemini.bunburya.eu/test_path#test_fragment" to RequestValidity.FRAGMENT
    )

    private val dummySocketAddress = InetSocketAddress("localhost", 1965)

    @Test
    fun testRequestValidation() {
        var request: Request
        for ((uri, validity) in expected) {
            request = Request(uri, dummySocketAddress)
            assertEquals(validity, request.validity)
        }
    }

}