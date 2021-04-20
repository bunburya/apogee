package eu.bunburya.apogee

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.CharsetUtil

class ResponseEncoder: MessageToByteEncoder<Response>() {
    private val logger = Logger(javaClass.name)
    init {
        logger.addLogHandler(LogLevel.DEBUG, LogHandler())
    }
    override fun encode(ctx: ChannelHandlerContext, msg: Response, out: ByteBuf) {
        val byteBuf = msg.toByteBuf()
        logger.debug("Sending message: ${byteBuf.toString(CharsetUtil.UTF_8)}")
        out.writeBytes(byteBuf)
    }
}