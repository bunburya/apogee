package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.BadRequestResponse
import eu.bunburya.apogee.models.ProxyRequestRefusedResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.RequestValidity
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

/**
 * Performs basic validation of requests and writes an appropriate error response if a request is not validly formed.
 */
@ChannelHandler.Sharable
class RequestValidator(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

        val request = msg as Request
        if (request.isValid) {
            // We know the request *looks* valid; now check to make sure it is valid having regard to the server
            // configuration
            val host = request.uri!!.host.toLowerCase()  // Lower case to perform case-insensitive matching
            val port = request.uri.port
            if (host != config.HOSTNAME_LOWERCASE) ctx.writeAndClose(ProxyRequestRefusedResponse(request), logger)
            else if (port != -1 && port != config.PORT) ctx.writeAndClose(ProxyRequestRefusedResponse(request), logger)
            else ctx.fireChannelRead(msg)
        } else {
            if (request.validity == RequestValidity.NOT_GEMINI_URI)
                ctx.writeAndClose(ProxyRequestRefusedResponse(request), logger)
            else ctx.writeAndClose(BadRequestResponse(request), logger)
        }
    }

}