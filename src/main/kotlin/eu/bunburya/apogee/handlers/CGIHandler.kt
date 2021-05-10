package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.dynamic.CGIManager
import eu.bunburya.apogee.dynamic.CGIRequest
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

class CGIHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)
    private val cgiManager = CGIManager(config)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.fine("CGIHandler reached.")
        val request = msg as Request
        val pathPair = cgiManager.getPathInfo(request)
        if (pathPair == null) ctx.fireChannelRead(msg)
        else {
            val (scriptPath, pathInfo) = pathPair
            return ctx.writeAndClose(cgiManager.handleRequest(CGIRequest(request, scriptPath, pathInfo, ctx)), logger)
        }
    }

}