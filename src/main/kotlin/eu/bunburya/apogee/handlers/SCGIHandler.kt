package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.dynamic.SCGIManager
import eu.bunburya.apogee.dynamic.SCGIRequest
import eu.bunburya.apogee.models.Request
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter



class SCGIHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val scgiManager = SCGIManager(config)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
        val scriptInfo = scgiManager.getPathInfo(request)
        if (scriptInfo == null) ctx.fireChannelRead(msg)
        else {
            val (scriptPath, pathInfo) = scriptInfo
            scgiManager.handleRequest(SCGIRequest(request, scriptPath, pathInfo, ctx))
        }
    }
}