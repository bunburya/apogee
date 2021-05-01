package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.utils.compileKeys
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class RedirectHandler(
    tempRedirects: Map<String, String>,
    permRedirects: Map<String, String>
): ChannelInboundHandlerAdapter() {

    private val tempRedirects = compileKeys(tempRedirects)
    private val permRedirects = compileKeys(permRedirects)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
        for ((pattern, path) in tempRedirects) {

        }

    }

}