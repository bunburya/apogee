package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.models.Request
import io.netty.channel.ChannelHandlerContext

class RedirectHandler(tempRedirects: Map<String, String>, permRedirects: Map<String, String>): BaseInboundHandler() {

    private val tempRedirects = compileKeys(tempRedirects)
    private val permRedirects = compileKeys(permRedirects)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
    }

}