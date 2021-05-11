package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.CGIErrorResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import eu.bunburya.apogee.utils.resolvePath
import eu.bunburya.apogee.utils.writeAndClose
import java.io.File
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger


/**
 * Base class for classes which help manage dynamic content using gateway protocols, eg CGI and SCGI.
 */
abstract class GatewayManager<requestType: BaseGatewayRequest>(protected val config: Config) {

    protected fun prepareGatewayEnv(env: MutableMap<String, String>,
                                    gatewayRequest: requestType): MutableMap<String, String> {
        val request = gatewayRequest.request
        env.clear()
        env["QUERY_STRING"] = request.uri!!.rawQuery ?: ""
        env["REQUEST_METHOD"] = ""
        env["SERVER_NAME"] = config.HOSTNAME
        env["SERVER_PORT"] = config.PORT.toString()
        env["SERVER_PROTOCOL"] = "GEMINI"
        env["SERVER_SOFTWARE"] = "APOGEE"
        env["REMOTE_ADDR"] = request.ipString
        env["SCRIPT_PATH"] = gatewayRequest.scriptPath
        env["PATH_INFO"] = gatewayRequest.pathInfo
        return env
    }

    /**
     * From a request, return a Pair, the first element of which is a path to the script and the second element of
     * which is the rest of the request path.
     *
     * If the request does not correspond to any script, return null.
     */
    abstract fun getPathInfo(request: Request): Serializable?

    /**
     * Handle a request for dynamic content and return an appropriate Response object.
     */
    abstract fun handleRequest(gatewayRequest: requestType)

}

/**
 * Helper class for managing CGI requests.
 */
class CGIManager(config: Config): GatewayManager<CGIRequest>(config) {

    private val logger = Logger.getLogger(javaClass.name)

    private fun makeEnv(processBuilder: ProcessBuilder, cgiRequest: CGIRequest) {
        val env = prepareGatewayEnv(processBuilder.environment(), cgiRequest)
        env["GATEWAY_INTERFACE"] = "CGI/1.1"
    }


    override fun getPathInfo(request: Request): Pair<String, String>? {
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

    override fun handleRequest(gatewayRequest: CGIRequest) {
        val serverCtx = gatewayRequest.serverCtx
        val scriptPath = gatewayRequest.scriptPath
        val request = gatewayRequest.request
        logger.fine("Launching script at $scriptPath")
        var logged = false
        val pb = ProcessBuilder(scriptPath)
        makeEnv(pb, gatewayRequest)
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
                serverCtx.writeAndClose(CGIErrorResponse(request), logger)
            } else {
                val exitCode = proc.exitValue()
                if (exitCode != 0) {
                    if (!logged) {
                        logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
                        logged = true
                    }
                    logger.severe("CGI script returned with non-zero exit code $exitCode.")
                    serverCtx.writeAndClose(CGIErrorResponse(request), logger)
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
                        serverCtx.writeAndClose(CGIErrorResponse(request), logger)
                    } else {
                        val mimeType = output.slice(3 until cr).toByteArray().decodeToString()
                        val body = output.slice(lf + 1..output.lastIndex).toByteArray()
                        serverCtx.writeAndClose(Response(statusCode, mimeType, request, body), logger)
                    }
                }
            }
        } catch (e: Exception) {
            if (!logged) {
                logger.warning("Encountered errors in CGI request: ${request.uri!!.toASCIIString()}")
            }
            logger.severe("Got exception when calling script: ${e.message}")
            serverCtx.writeAndClose(CGIErrorResponse(request), logger)
        }
    }
}

/**
 * Helper class for managing SCGI requests.
 */
class SCGIManager(config: Config): GatewayManager<SCGIRequest>(config) {

    private val scgiClients = mutableMapOf<String, SCGIClient>().apply {
        for ((prefix, socketPath) in config.SCGI_PATHS) {
            this[prefix] = SCGIClient(File(socketPath))
        }
    }.toMap()

    /**
     * Assign a map of environment variables to a SCGIRequest object (in-place).
     */
    fun makeEnv(scgiRequest: SCGIRequest) {
        val env = prepareGatewayEnv(mutableMapOf(), scgiRequest)
        // Don't add CONTENT_LENGTH, as we write this separately when encoding the request
        env["SCGI"] = "1"
        scgiRequest.env = env
    }


    override fun getPathInfo(request: Request): Pair<String, String>? {
        val requestPath = request.uri!!.path
        for ((prefix, _) in config.SCGI_PATHS) {
            if (requestPath.startsWith(prefix)) {
                return Pair(prefix, requestPath.removePrefix(prefix))
            }
        }
        return null
    }

    override fun handleRequest(gatewayRequest: SCGIRequest) {
        makeEnv(gatewayRequest)
        val client = scgiClients[gatewayRequest.scriptPath]
            ?: throw IllegalArgumentException("No SCGI client corresponding to path: ${gatewayRequest.scriptPath}")
        client.write(gatewayRequest)
    }
}