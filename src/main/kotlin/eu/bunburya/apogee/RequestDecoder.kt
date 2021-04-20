package eu.bunburya.apogee

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.Delimiters
import io.netty.util.CharsetUtil

/**
 * Decodes inbound requests, converting them to Request objects.
 */
class RequestDecoder: DelimiterBasedFrameDecoder(
    1026, // 1024 (max URL length per spec) + "\r\n"
    true,
    true,
    *Delimiters.lineDelimiter()
) {
    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf): Any? {
        val byteBuf = super.decode(ctx, buffer) as ByteBuf?
        return if (byteBuf != null) {
            Request(
                byteBuf.toString(CharsetUtil.UTF_8),
                ctx.channel().remoteAddress()
            )
        } else null
    }
}