package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.*
import eu.bunburya.apogee.models.*
import eu.bunburya.apogee.static.FileServer
import eu.bunburya.apogee.utils.writeResponse
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.logging.Logger

/*
Application logic:
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
class StaticFileHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)
    private val fileServer = FileServer(config)

    /**
     * The main handler function which acts as a gateway to our business logic.
     */
    fun processRequest(request: Request): Response {

        val validity = request.validity
        if (! validity.isValid) return BadRequestResponse(request, validity.defaultMsg)
        return fileServer.serveResource(request)

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