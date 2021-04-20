package eu.bunburya.apogee

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.CharsetUtil
import java.util.logging.Logger

class ResponseEncoder: MessageToByteEncoder<Response>() {

    private val logger = Logger.getLogger(javaClass.name)

    override fun encode(ctx: ChannelHandlerContext, msg: Response, out: ByteBuf) {
        val byteBuf = msg.toByteBuf()
        logger.fine("Sending response of type ${msg.statusCode}.")
        out.writeBytes(byteBuf)
    }
}