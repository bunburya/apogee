package eu.bunburya.apogee

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.CharsetUtil
import java.nio.charset.Charset

/**
 * The main handler object that handles successful inbound requests and determines the appropriate response.
 */

class MainRequestHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger(javaClass.name)
    init {
        logger.addLogHandler(config.LOG_LEVEL, LogHandler(config.LOG_FILE))
    }

    fun writeResponse(ctx: ChannelHandlerContext, response: Response) {
        val writtenFuture = ctx.writeAndFlush(response)
        writtenFuture.addListener(object: ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                assert(writtenFuture == future)
                if (future.isSuccess) {
                    logger.access(response)
                } else {
                    logger.error("Error writing to client.")
                    future.cause().printStackTrace()
                }
                ctx.close()
                logger.debug("Closed connection.")
            }
        })
    }

    /**
     * Handle a single inbound request and write the appropriate response. Requests are received as Request objects, and
     * should be written as Response objects (which should be encoded to ByteBuf objects by an outbound handler).
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
        logger.info("Got request ${request.content} from ${request.ipString}")

        writeResponse(ctx, SuccessResponse("text/plain", "Hello world!\n", request))

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

}