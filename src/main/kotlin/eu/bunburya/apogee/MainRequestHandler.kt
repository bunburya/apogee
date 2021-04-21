package eu.bunburya.apogee

import eu.bunburya.apogee.static.FileServer
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

/*
Pipeline:
- Inbound request > SSL handled > decoded to Request > Request handled by application
- Response written by application > Response encoded to byte stream > SSL handled > sent to client

Application logic:
- TODO: dynamic content handlers
- File server
  - Check if request is valid; if not, write bad Response
  - Check if requested resource exists and is readable; if not, write bad Response
  - Check if requested resource is a directory; if so:
    - Check if directory is executable; if not, write bad response
    - Check if there is an index file:
      - If so, return the index file as OK response
      - If not, return the directory listing as OK response
  - If requested resource is not a directory, return the resource as OK response
 */

/**
 * The main handler object that handles successful inbound requests and determines the appropriate response.
 */
class MainRequestHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)
    private val accessLogger = getAccessLogger(config)
    private val fileServer = FileServer(config)

    /**
     * The main handler function which acts as a gateway to our business logic.
     */
    fun processRequest(request: Request): Response {

        val validity = request.validity
        if (! validity.isValid) return BadRequestResponse(validity.defaultMsg, request)
        return fileServer.serveResource(request)

    }

    fun writeResponse(ctx: ChannelHandlerContext, response: Response) {
        val writtenFuture = ctx.writeAndFlush(response)
        writtenFuture.addListener(object: ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                assert(writtenFuture == future)
                if (future.isSuccess) {
                    accessLogger.access(response)
                } else {
                    logger.severe("Error writing to client.")
                    future.cause().printStackTrace()
                }
                ctx.close()
                logger.fine("Closed connection.")
            }
        })
    }

    /**
     * Handle a single inbound request and write the appropriate response. Requests are received as Request objects, and
     * should be written as Response objects (which should be encoded to ByteBuf objects by an outbound handler).
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
        logger.fine("Got request ${request.content} from ${request.ipString}")

        writeResponse(ctx, processRequest(request))

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

}