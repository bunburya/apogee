package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.Request

/**
 * A base class for classes which handle gateway interfaces, such as CGI and SCGI.
 */

abstract class GatewayManager(private val config: Config) {

    protected fun prepareGatewayEnv(request: Request, processBuilder: ProcessBuilder,
                                    scriptPath: String, pathInfo: String): MutableMap<String, String> {
        val env = processBuilder.environment()
        env.clear()
        env["QUERY_STRING"] = request.uri!!.rawQuery ?: ""
        env["REQUEST_METHOD"] = ""
        env["SERVER_NAME"] = config.HOSTNAME
        env["SERVER_PORT"] = config.PORT.toString()
        env["SERVER_PROTOCOL"] = "GEMINI"
        env["SERVER_SOFTWARE"] = "APOGEE"
        env["REMOTE_ADDR"] = request.ipString
        env["SCRIPT_PATH"] = scriptPath
        env["PATH_INFO"] = pathInfo
        return env
    }

}