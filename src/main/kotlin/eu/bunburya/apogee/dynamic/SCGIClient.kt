package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.CGIErrorResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import eu.bunburya.apogee.models.ResponseParseError
import eu.bunburya.apogee.utils.toByteArray
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import io.netty.channel.unix.UnixChannel
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.Delimiters
import io.netty.util.Timeout
import io.netty.util.Timer
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import java.nio.charset.StandardCharsets

private fun MutableList<Byte>.addAll(bytes: ByteArray) {
    for (b in bytes) this.add(b)
}

class SCGIRequestEncoder(): MessageToByteEncoder<SCGIRequest>() {

    private val logger = Logger.getLogger(javaClass.name)

    /**
     * Encode a list of bytes as a netstring per the SCGI specification.
     */
    private fun toNetstring(bytes: List<Byte>): ByteArray {
        val len = bytes.size
        val lenStr = len.toString()
        val lenStrLen = lenStr.length // The length of the string denoting the length of the content
        val byteArray = ByteArray(len + lenStrLen + 2)
        for (i in lenStr.indices) byteArray[i] = lenStr[i].toByte()
        byteArray[lenStrLen] = ':'.toByte()
        for (i in bytes.indices) byteArray[lenStrLen + 1 + i] = bytes[i]
        byteArray[byteArray.lastIndex] = ','.toByte()
        return byteArray
    }

    override fun encode(ctx: ChannelHandlerContext, msg: SCGIRequest, out: ByteBuf) {
        val bytes = mutableListOf<Byte>()
        // CONTENT_LENGTH always has to be first (and, in our case, will always be 0)
        bytes.addAll("CONTENT_LENGTH".encodeToByteArray())
        bytes.add(0)
        bytes.add('0'.toByte())
        bytes.add(0)
        for ((key, value) in msg.env) {
            bytes.addAll(key.encodeToByteArray())
            bytes.add(0)
            bytes.addAll(value.encodeToByteArray())
            bytes.add(0)
        }
        out.writeBytes(toNetstring(bytes))
    }
}

class SCGIResponseDecoder(): ByteToMessageDecoder() {
    
    override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
        if (! ctx.channel().isActive()) {
            out.add(byteBuf.readBytes(byteBuf.readableBytes()))
        }
    }

}

/**
 * Handle both requests to, and responses from, a SCGI script, storing the context associated with a request
 * and using that context to build a response that can be sent back to the client.
 */
class SCGIRequestContextHandler(private val config: Config, private val timer: Timer): ChannelDuplexHandler() {

    private val logger = Logger.getLogger(javaClass.name)
    private lateinit var request: Request
    private lateinit var serverCtx: ChannelHandlerContext
    private lateinit var timeout: Timeout

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        val scgiRequest = msg as SCGIRequest
        request = scgiRequest.request
        serverCtx = scgiRequest.serverCtx
        // Set a timeout for reading a response, and send a CGI error and close the connection if the timeout expires.
        // Netty has a builtin ReadTimeoutHandler, but it doesn't seem to work in all cases.
        timeout = timer.newTimeout({
            logger.severe("SCGI script timed out after ${config.CGI_TIMEOUT} seconds.")
            ctx.close()
            serverCtx.writeAndClose(CGIErrorResponse(request), logger)
        }, config.CGI_TIMEOUT, TimeUnit.SECONDS)

        ctx.write(scgiRequest)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        timeout.cancel()
        val bytes = (msg as ByteBuf).toByteArray()
        try {
            val response = Response.fromBytes(bytes, request)
            serverCtx.writeAndClose(response, logger)
        } catch (e: ResponseParseError) {
            logger.severe("Invalid response received from SCGI script: ${bytes.decodeToString()}.")
            serverCtx.writeAndClose(CGIErrorResponse(request), logger)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        timeout.cancel()
        logger.severe("Encountered error in communicating with SCGI script: ${cause.message}")
        cause.printStackTrace()
        serverCtx.writeAndClose(CGIErrorResponse(request), logger)
    }
}

class SCGIChannelInitializer(private val config: Config, private val timer: Timer): ChannelInitializer<UnixChannel>() {
    private val logger = Logger.getLogger(javaClass.name)
    override fun initChannel(ch: UnixChannel) {
        val pipeline = ch.pipeline()
        pipeline.addLast(SCGIRequestEncoder())
        pipeline.addLast(SCGIResponseDecoder())
        pipeline.addLast(SCGIRequestContextHandler(config, timer))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.severe("Error initialising connection to SCGI app: ${cause.message}")
        cause.printStackTrace()
    }
}


/**
 * A class that handles SCGI requests. One of these should be established per SCGI socket.
 *
 * From Netty's perspective, this is a client that makes requests over a socket and sends the results back to the
 * SCGIHandler.
 */

class SCGIClient(config: Config, socket: File, private val timer: Timer) {

    private val logger = Logger.getLogger(javaClass.name)
    private val socketAddr = DomainSocketAddress(socket)
    private val workerGroup = EpollEventLoopGroup()
    private val bootstrap: Bootstrap
    init {
        try {
            logger.fine("Bootstrapping SCGI client")
            bootstrap = Bootstrap().apply {
                group(workerGroup)
                channel(EpollDomainSocketChannel::class.java)
                handler(SCGIChannelInitializer(config, timer))
            }
        } catch (e: Exception) {
            logger.severe("Error establishing connection with SCGI script: ${e.message}")
            throw e
        } /*finally {
            logger.fine("Shutting down SCGI client workerGroup")
            workerGroup.shutdownGracefully()
        }*/
    }

    fun write(scgiRequest: SCGIRequest) {
        logger.fine("Writing to SCGI script.")
        val future = bootstrap.connect(socketAddr).sync()
        logger.fine("Connected")
        future.addListener(object: ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                logger.fine("Connecting to SCGI socket complete.")
                future.channel().writeAndClose(scgiRequest, logger)
            }

        })
    }

}
