package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import eu.bunburya.apogee.utils.toByteArray
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.unix.DomainSocketAddress
import io.netty.channel.unix.DomainSocketChannel
import io.netty.handler.codec.MessageToByteEncoder
import kotlinx.coroutines.*
import java.io.File
import java.util.logging.Logger

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
        val netstring = toNetstring(bytes)
        logger.fine("Writing: ${netstring.decodeToString()}")
        out.writeBytes(toNetstring(bytes))
    }
}

/**
 * Handle both requests to, and responses from, a SCGI script, storing the context associated with a request
 * and using that context to build a response that can be sent back to the client.
 */
class SCGIRequestContextHandler(): ChannelDuplexHandler() {

    private val logger = Logger.getLogger(javaClass.name)
    private lateinit var request: Request
    private lateinit var serverCtx: ChannelHandlerContext

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        logger.fine("Write to SCGIRequestContextHandler")
        val scgiRequest = msg as SCGIRequest
        request = scgiRequest.request
        serverCtx = scgiRequest.serverCtx
        ctx.write(scgiRequest)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.fine("Read from SCGIRequestContextHandler")
        val bytes = (msg as ByteBuf).toByteArray()
        serverCtx.writeAndClose(Response.fromBytes(bytes, request), logger)
    }
}

class SCGIChannelInitializer(): ChannelInitializer<DomainSocketChannel>() {
    private val logger = Logger.getLogger(javaClass.name)
    override fun initChannel(ch: DomainSocketChannel) {
        logger.fine("Initialising SCGI channel")
        val pipeline = ch.pipeline()
        pipeline.addLast(SCGIRequestEncoder())
        pipeline.addLast(SCGIRequestContextHandler())
        logger.fine("All handlers added")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        println("Exception: ${cause.message}")
    }
}


/**
 * A class that handles SCGI requests. One of these should be established per SCGI socket.
 *
 * From Netty's perspective, this is a client that makes requests over a socket and sends the results back to the
 * SCGIHandler.
 */

class SCGIClient(socket: File) {

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
                handler(SCGIChannelInitializer())
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
