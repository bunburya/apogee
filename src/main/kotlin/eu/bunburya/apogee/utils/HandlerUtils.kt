package eu.bunburya.apogee.utils

import eu.bunburya.apogee.models.Response
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import java.util.logging.Logger

val logger: Logger = Logger.getLogger("eu.bunburya.apogee.utils.HandlerUtils")

/**
 * Write a response back to the client and close the connection.
 */
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