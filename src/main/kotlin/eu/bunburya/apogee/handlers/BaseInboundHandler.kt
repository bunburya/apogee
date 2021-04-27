package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.access
import eu.bunburya.apogee.models.Response
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger
import java.util.regex.Pattern


/**
 * A base class for inbound handlers that may send outbound requests and close the connection
 * (effectively "short-circuiting" the pipeline, so that subsequent inbound handlers are not reached).
 */
abstract class BaseInboundHandler: ChannelInboundHandlerAdapter() {

    protected val logger: Logger = Logger.getLogger(javaClass.name)

    /**
     * Take a map where the keys are strings representing regex patters, and return a map where the keys are compiled
     * Pattern objects.
     */
    protected fun <valType> compileKeys(inMap: Map<String, valType>): Map<Pattern, valType> =
        inMap.map { (k, v) -> Pattern.compile(k) to v }.toMap()

    /**
     * Write a response back to the client and close the connection.
     */
    protected fun writeResponse(ctx: ChannelHandlerContext, response: Response) {
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