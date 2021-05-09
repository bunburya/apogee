package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.BadRequestResponse
import eu.bunburya.apogee.models.ProxyRequestRefusedResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.RequestValidity
import eu.bunburya.apogee.utils.writeResponse
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class RequestValidator(private val config: Config): ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

        val request = msg as Request
        if (request.isValid) {
            // We know the request *looks* valid; now check to make sure it is valid having regard to the server
            // configuration
            val host = request.uri!!.host
            val port = request.uri.port
            if (host != config.HOSTNAME) writeResponse(ctx, ProxyRequestRefusedResponse(request))
            else if (port != -1 && port != config.PORT) writeResponse(ctx, ProxyRequestRefusedResponse(request))
            else ctx.fireChannelRead(msg)
        } else {
            if (request.validity == RequestValidity.NOT_GEMINI_URI)
                writeResponse(ctx, ProxyRequestRefusedResponse(request))
            else writeResponse(ctx, BadRequestResponse(request))
        }
    }

}