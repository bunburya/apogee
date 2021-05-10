package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.models.Request
import io.netty.channel.ChannelHandlerContext

/**
 * A base POJO class representing a single request for dynamic content via a gateway protocol, eg, CGI or SCGI.
 *
 * @param request The Request object representing the request received from the client.
 * @param scriptPath The portion of the request path that corresponds to the matched SCGI prefix, as a String.
 * @param pathInfo The portion of the request path that follows scriptPath, as a String.
 * @param serverCtx The ChannelHandlerContext object representing the connection with the main server, which can be used
 * to write responses to the server.
 */
abstract class BaseGatewayRequest(
    val request: Request,
    val scriptPath: String,
    val pathInfo: String,
    val serverCtx: ChannelHandlerContext
)


class CGIRequest(
    request: Request,
    scriptPath: String,
    pathInfo: String,
    serverCtx: ChannelHandlerContext
): BaseGatewayRequest(request, scriptPath, pathInfo, serverCtx)


class SCGIRequest(
    request: Request,
    scriptPath: String,
    pathInfo: String,
    serverCtx: ChannelHandlerContext
): BaseGatewayRequest(request, scriptPath, pathInfo, serverCtx) {
    lateinit var env: Map<String, String>
}
