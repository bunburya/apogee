package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.CGIErrorResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import eu.bunburya.apogee.utils.resolvePath
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class CGIServer(private val config: Config): GatewayManager(config) {

    private val logger = Logger.getLogger(javaClass.name)

    private fun makeEnv(processBuilder: ProcessBuilder, request: Request, scriptPath: String, pathInfo: String) {
        val env = prepareGatewayEnv(request, processBuilder, scriptPath, pathInfo)
        env["GATEWAY_INTERFACE"] = "CGI/1.1"

    }

    /**
     * From a request, return a pair, the first element of which is a path to the CGI script and the second element of
     * which is the rest of the request path.
     *
     * If the request does not correspond to any CGI script, return null.
     */
    fun getCGIScript(request: Request): Pair<String, String>? {
        var fullCgiScriptPath : String
        var relativeCgiDirPath: String
        val uriPathString = request.uri!!.path
        val fullPath = resolvePath(request, config)
        var components: List<String>
        for (fullCgiDirPath in config.CGI_PATHS) {
            // TODO: Consider whether we can tidy this up.
            if (! fullPath.startsWith(fullCgiDirPath)) continue
            relativeCgiDirPath = "/" + fullCgiDirPath.removePrefix(config.DOCUMENT_ROOT)
            fullCgiScriptPath = fullCgiDirPath.removeSuffix("/")
            components = uriPathString.removePrefix(relativeCgiDirPath).split("/").filter { it.isNotBlank() }
            for (component in components) {
                fullCgiScriptPath = "$fullCgiScriptPath/$component"
                val file = File(fullCgiScriptPath)
                if (file.isFile && file.canExecute()) {
                    val pathInfo = uriPathString.toString().removePrefix(fullCgiScriptPath)
                    return Pair(fullCgiScriptPath, pathInfo)
                }
            }
        }
        return null
    }

    fun launchProcess(scriptPath: String, pathInfo: String, request: Request): Response {
        logger.fine("Launching script at $scriptPath")
        var logged = false
        val pb = ProcessBuilder(scriptPath)
        makeEnv(pb, request, scriptPath, pathInfo)
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
            }
            logger.severe("Got exception when calling script: ${e.message}")
            return CGIErrorResponse(request)
        }
    }
}