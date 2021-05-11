package eu.bunburya.apogee.utils

import eu.bunburya.apogee.models.Response
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelOutboundInvoker
import java.util.logging.Logger

val logger: Logger = Logger.getLogger("eu.bunburya.apogee.utils.HandlerUtils")

/*
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
*/

private class LogAndCloseListener(private val invoker: ChannelOutboundInvoker,
                                  private val relevantFuture: ChannelFuture,
                                  private val logger: Logger): ChannelFutureListener {
    override fun operationComplete(future: ChannelFuture) {
        logger.fine("writeAndFlush complete.")
        if (future == relevantFuture) {
            if (!future.isSuccess) logger.severe("Error writing to channel: ${future.cause().message}.")
            invoker.close()
            logger.fine("Closed connection.")
        }
    }
}

/**
 * Write a response back to the client and close the connection.
 */
fun ChannelOutboundInvoker.writeAndClose(msg: Any, logger: Logger) {
    val writtenFuture = this.writeAndFlush(msg)
    writtenFuture.addListener(LogAndCloseListener(this, writtenFuture, logger))
}