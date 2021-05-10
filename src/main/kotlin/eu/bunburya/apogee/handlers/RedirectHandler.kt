package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.models.RedirectionResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.utils.compileKeys
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

class RedirectHandler(
    tempRedirects: Map<String, String>,
    permRedirects: Map<String, String>
): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)

    private val tempRedirects = compileKeys(tempRedirects)
    private val permRedirects = compileKeys(permRedirects)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
        var matched = false
        for ((pattern, path) in tempRedirects) {
            if (pattern.matcher(request.uri!!.path).find()) {
                matched = true
                ctx.writeAndClose(RedirectionResponse(request, path, false), logger)
            }
        }
        for ((pattern, path) in permRedirects) {
            if (pattern.matcher(request.uri!!.path).find()) {
                matched = true
                ctx.writeAndClose(RedirectionResponse(request, path, true), logger)
            }
        }
        if (! matched) ctx.fireChannelRead(msg)

    }

}