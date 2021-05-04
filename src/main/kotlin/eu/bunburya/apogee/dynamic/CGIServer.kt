package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.CGIErrorResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class CGIServer(private val config: Config) {

    private val logger = Logger.getLogger(javaClass.name)

    fun launchProcess(filePath: String, request: Request): Response {
        logger.fine("Launching script at $filePath")
        var logged = false
        val pb = ProcessBuilder(filePath)
        val env = pb.environment()
        env["TEST_ENV"] = "test_val"
        try {
            val proc = pb.start()
            val completed = proc.waitFor(10, TimeUnit.SECONDS)
            val error = proc.errorStream.readBytes()
            if (error.isNotEmpty()) {
                logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
                logger.warning("Message printed to stderr: ${error.decodeToString()}")
                logged = true
            }
            if (!completed) {
                if (!logged) {
                    logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
                    logged = true
                }
                logger.severe("CGI script timed out.")
                return CGIErrorResponse(request)
            } else {
                val exitCode = proc.exitValue()
                if (exitCode != 0) {
                    if (!logged) {
                        logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
                        logged = true
                    }
                    logger.severe("CGI script returned with non-zero exit code $exitCode.")
                    return CGIErrorResponse(request)
                } else {
                    val output = proc.inputStream.readBytes()
                    val statusCode = output.slice(0..1).toByteArray().decodeToString().toInt()
                    val cr = output.indexOf('\r'.toByte())
                    val lf = output.indexOf('\n'.toByte())
                    if ((cr == -1) || (lf == -1) || (lf != cr + 1)) {
                        if (!logged) {
                            logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
                            logged = true
                        }
                        logger.severe("CGI script output has no CRLF.")
                        return CGIErrorResponse(request)
                    } else {
                        val mimeType = output.slice(3 until cr).toByteArray().decodeToString()
                        val body = output.slice(lf + 1..output.lastIndex).toByteArray()
                        return Response(statusCode, mimeType, request, body)
                    }
                }
            }
        } catch (e: Exception) {
            if (!logged) {
                logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
                logged = true
            }
            logger.severe("Got exception when calling script: ${e.message}")
            return CGIErrorResponse(request)
        }
    }
}