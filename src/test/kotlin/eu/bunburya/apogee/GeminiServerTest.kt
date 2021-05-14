package eu.bunburya.apogee

import io.netty.util.TimerTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File


const val URL_BASE = "gemini://localhost/"

/**
 * Test the server by making various requests and asserting that the responses are as expected. Should be run while
 * the server is already running.
 *
 * NOTE: For some reason, assertion failures (or other exceptions) encountered during testRequest don't trigger a test
 * failure, so we need to check the output manually for errors (it seems that all tests will always pass).
 */
internal class GeminiServerTest {

    private val client = TestClient()

    private val mimeTypes = mapOf(
        "atom.xml" to "application/atom+xml",
        "gemtext_file.gmi" to "text/gemini",
        "jpg_file.jpg" to "image/jpeg",
        "text_file.txt" to "text/plain",
        "xml_file.xml" to "application/xml",
        "rss.xml" to "application/rss+xml"
    )

    private fun addCRLF(bytes: ByteArray) {
        val end = bytes.lastIndex
        bytes[end] = LF
        bytes[end-1] = CR
    }

    private fun getLongRequest(urlSize: Int, fillerChar: Char = 't'): ByteArray {
        val filler = fillerChar.toByte()
        val request = ByteArray(urlSize + 2)
        for (i in URL_BASE.indices) {
            request[i] = URL_BASE[i].toByte()
        }
        for (i in URL_BASE.length until urlSize) {
            request[i] = filler
        }
        addCRLF(request)
        return request
    }

    private fun getRightCredentials(i: Int): Pair<File, File> = CERTS[i]
    private fun getWrongCredentials(i: Int): Pair<File, File> = CERTS[if (i < 4) i + 1 else 0]

    @Test
    fun `test sending garbage data with CRLF`() {
        val byteRange = (1..9)
        val request = ByteArray(40)
        repeat(38) { request[it] = byteRange.random().toByte() }
        addCRLF(request)
        client.testRequest(request) {
            assertEquals(59, it.statusCode)
        }
    }

    @Test
    fun `test successful static gemtext request`() {
        client.testRequest(URL_BASE + "hello.gmi\r\n") {
            assertEquals(20, it.statusCode)
            assertEquals("Hello world!\n", it.body.decodeToString())
        }
    }

    @Test
    fun `test not found request`() {
        client.testRequest(URL_BASE + "non_existent_file\r\n") {
            assertEquals(51, it.statusCode)
        }
    }

    @Test
    fun `test sending URL that is too long`() {
        val request = getLongRequest(1025)
        client.testRequest(request) {
            assertEquals(59, it.statusCode)
        }
    }

    @Test
    fun `test sending URL that is just short enough`() {
        val request = getLongRequest(1024)
        client.testRequest(request) {
            assertEquals(51, it.statusCode)
        }
    }

    @Test
    fun `test temporary redirect`() {
        client.testRequest(URL_BASE + "redirect-test/temp-redirect-from\r\n") {
            assertEquals(30, it.statusCode)
            assertEquals("/redirect-test/temp-redirect-to", it.meta)
        }
    }

    @Test
    fun `test permanent redirect`() {
        client.testRequest(URL_BASE + "redirect-test/perm-redirect-from\r\n") {
            assertEquals(31, it.statusCode)
            assertEquals("/redirect-test/perm-redirect-to", it.meta)
        }
    }

    @Test
    fun `test auth zones with no certificate`() {
        for (i in 0 until 5) {
            // Cert required: Returns directory listing
            val request1 = URL_BASE + "auth_zone_$i/\r\n"
            // Cert required: Returns file
            val request2 = URL_BASE + "auth_zone_$i/subdir/test_file.txt\r\n"
            // Cert required: Returns not found
            val request3 = URL_BASE + "auth_zone_$i/non_existent_file\r\n"
            // Cert not required: Returns not found
            val request4 = URL_BASE + "auth_zone_${i}_but_not_really/non_existent_file\r\n"

            for (r in listOf(request1, request2, request3)) {
                client.testRequest(r) {
                    assertEquals(60, it.statusCode)
                }
            }
            client.testRequest(request4) {
                assertEquals(51, it.statusCode)
            }

        }
    }

    @Test
    fun `test auth zones with correct certificate`() {
        for (i in 0 until 5) {
            val (certFile, keyFile) = getRightCredentials(i)

            // Cert required: Returns directory listing
            val request1 = URL_BASE + "auth_zone_$i/\r\n"
            // Cert required: Returns file
            val request2 = URL_BASE + "auth_zone_$i/subdir/test_file.txt\r\n"
            // Cert required: Returns not found
            val request3 = URL_BASE + "auth_zone_$i/non_existent_file\r\n"
            // Cert not required: Returns not found
            val request4 = URL_BASE + "auth_zone_${i}_but_not_really/non_existent_file\r\n"

            client.testRequest(request1, certFile, keyFile) {
                assertEquals(20, it.statusCode)
            }
            client.testRequest(request2, certFile, keyFile) {
                assertEquals(20, it.statusCode)
            }
            client.testRequest(request3, certFile, keyFile) {
                assertEquals(51, it.statusCode)
            }
            client.testRequest(request4, certFile, keyFile) {
                assertEquals(51, it.statusCode)
            }

        }
    }

    @Test
    fun `test client auth with wrong certificate`() {
        for (i in 0 until 5) {
            val (certFile, keyFile) = getWrongCredentials(i)

            // Cert required: Returns directory listing
            val request1 = URL_BASE + "auth_zone_$i/\r\n"
            // Cert required: Returns file
            val request2 = URL_BASE + "auth_zone_$i/subdir/test_file.txt\r\n"
            // Cert required: Returns not found
            val request3 = URL_BASE + "auth_zone_$i/non_existent_file\r\n"
            // Cert not required: Returns not found
            val request4 = URL_BASE + "auth_zone_${i}_but_not_really/non_existent_file\r\n"

            for (r in listOf(request1, request2, request3)) {
                client.testRequest(r, certFile, keyFile) {
                    assertEquals(61, it.statusCode)
                }
            }
            client.testRequest(request4, certFile, keyFile) {
                assertEquals(51, it.statusCode)
            }
        }
    }

    @Test
    fun `test client auth where multiple certs accepted`() {
        val url = URL_BASE + "auth_zone_0_2_4/subdir/test_file.txt\r\n"

        // No cert
        client.testRequest(url) {
            assertEquals(60, it.statusCode)
        }

        // Right cert
        for (i in arrayOf(0, 2, 4)) {
            val (rightCertFile, rightKeyFile) = getRightCredentials(i)
            client.testRequest(url, rightCertFile, rightKeyFile) {
                assertEquals(20, it.statusCode)
            }
        }

        // Wrong cert
        for (i in arrayOf(1, 3)) {
            val (wrongCertFile, wrongKeyFile) = getRightCredentials(i)
            client.testRequest(url, wrongCertFile, wrongKeyFile) {
                assertEquals(61, it.statusCode)
            }
        }
    }

    @Test
    fun `test MIME type recognition`() {
        for ((fileName, mimeType) in mimeTypes) {
            client.testRequest(URL_BASE + "mime_tests/$fileName\r\n") {
                assertEquals(mimeType, it.meta)
            }
        }
    }

    @Test
    fun `test basic CGI script`() {
        client.testRequest(URL_BASE + "cgi-bin/sh_print_env\r\n") {
            assertEquals(20, it.statusCode)
            assertEquals("text/plain", it.meta)
        }
        client.testRequest(URL_BASE + "cgi-bin/sh_sleep_5\r\n") {
            assertEquals(20, it.statusCode)
            assertEquals("text/plain", it.meta)
        }
    }

    @Test
    fun `test CGI script with timeout`() {
        client.testRequest(URL_BASE + "cgi-bin/sh_sleep_15\r\n") {
            assertEquals(42, it.statusCode)
        }
    }

    @Test
    fun `test SCGI`() {
        client.testRequest(URL_BASE + "scgi-path-1\r\n") {
            assertEquals(20, it.statusCode)
            assertEquals("text/plain", it.meta)
            assert(it.body.isNotEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_redirect\r\n") {
            assertEquals(31, it.statusCode)
            assertEquals("/redirect/to", it.meta)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_cgi_error\r\n") {
            assertEquals(42, it.statusCode)
            assertEquals("Testing SCGI error", it.meta)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_need_cert\r\n") {
            assertEquals(60, it.statusCode)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_bad_cert\r\n") {
            assertEquals(61, it.statusCode)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_server_error\r\n") {
            assertEquals(51, it.statusCode)
            assertEquals("SCGI says error", it.meta)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_actual_scgi_error_1\r\n") {
            assertEquals(42, it.statusCode)
            assertNotEquals("SCGI says error", it.meta)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_actual_scgi_error_2\r\n") {
            assertEquals(42, it.statusCode)
            assertNotEquals("SCGI says error", it.meta)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_actual_scgi_error_3\r\n") {
            assertEquals(42, it.statusCode)
            assertNotEquals("SCGI says error", it.meta)
            assert(it.body.isEmpty())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_sleep_5\r\n") {
            assertEquals(20, it.statusCode)
            assertEquals("slept 5\n", it.body.decodeToString())
        }
        client.testRequest(URL_BASE + "scgi-path-1/test_sleep_15\r\n") {
            assertEquals(42, it.statusCode)
            assertNotEquals("SCGI says error", it.meta)
        }
        client.testRequest(URL_BASE + "scgi-path-1/some_other_path\r\n") {
            assertEquals(20, it.statusCode)
            assertEquals("some other path received\n", it.body.decodeToString())
        }
    }

    @Test
    fun `test SCGI with client cert`() {
        var fingerprint: String
        for (i in 0 until 5) {
            fingerprint = FINGERPRINTS[i]
            val (certFile, keyFile) = getRightCredentials(i)
            client.testRequest(URL_BASE + "scgi-path-1/client_auth\r\n", certFile, keyFile) {
                assertEquals(20, it.statusCode)
                assertEquals("$fingerprint\n", it.body.decodeToString())
            }
        }
    }


}