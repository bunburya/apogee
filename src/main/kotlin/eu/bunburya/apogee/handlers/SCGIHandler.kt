package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.dynamic.SCGIManager
import eu.bunburya.apogee.dynamic.SCGIRequest
import eu.bunburya.apogee.models.CGIErrorResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

@ChannelHandler.Sharable
class SCGIHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)
    private val scgiManager = SCGIManager(config)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.fine("SCGI handler reached.")
        val request = msg as Request
        val scriptInfo = scgiManager.getPathInfo(request)
        if (scriptInfo == null) ctx.fireChannelRead(msg)
        else {
            val (scriptPath, pathInfo) = scriptInfo
            try {
                scgiManager.handleRequest(SCGIRequest(request, scriptPath, pathInfo, ctx))
            } catch (e: Exception) {
                logger.severe("Encountered SCGI error: ${e.message}")
                ctx.writeAndClose(CGIErrorResponse(request, "SCGI error"), logger)
            }
        }
    }
}