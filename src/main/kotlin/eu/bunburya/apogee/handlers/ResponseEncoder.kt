package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.access
import eu.bunburya.apogee.models.Response
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import java.util.logging.Logger

/**
 * An outbound handler to encode Response objects to bytes for sending to the client.
 *
 * This class also handles access logging (because every response must pass through this class).
 */
@ChannelHandler.Sharable
class ResponseEncoder(private val accessLogger: Logger): MessageToByteEncoder<Response>() {

    private val logger = Logger.getLogger(javaClass.name)

    override fun encode(ctx: ChannelHandlerContext, msg: Response, out: ByteBuf) {
        val byteBuf = msg.toByteBuf()
        logger.fine("Sending response of type ${msg.statusCode}.")
        accessLogger.access(msg)
        out.writeBytes(byteBuf)
    }
}