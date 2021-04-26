package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.access
import eu.bunburya.apogee.models.Response
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

/**
 * A base class for inbound handlers that may send outbound requests and close the connection
 * (effectively "short-circuiting" the pipeline, so that subsequent inbound handlers are not reached).
 */
abstract class BaseInboundHandler: ChannelInboundHandlerAdapter() {

    protected val logger: Logger = Logger.getLogger(javaClass.name)

    fun writeResponse(ctx: ChannelHandlerContext, response: Response) {
        val writtenFuture = ctx.writeAndFlush(response)
        writtenFuture.addListener(object: ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                assert(writtenFuture == future)
                if (!future.isSuccess) {
                    logger.severe("Error writing to client.")
                    future.cause().printStackTrace()
                }
                ctx.close()
                logger.fine("Closed connection.")
            }
        })
    }

}