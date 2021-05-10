package eu.bunburya.apogee.dynamic

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import eu.bunburya.apogee.utils.toByteArray
import eu.bunburya.apogee.utils.writeAndClose
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.unix.DomainSocketAddress
import io.netty.channel.unix.DomainSocketChannel
import io.netty.handler.codec.MessageToByteEncoder
import kotlinx.coroutines.*
import java.io.File
import java.util.logging.Logger


class SCGIRequestEncoder(): MessageToByteEncoder<SCGIRequest>() {

    override fun encode(ctx: ChannelHandlerContext, msg: SCGIRequest, out: ByteBuf) {
        for ((key, value) in msg.env) {
            out.writeBytes(key.toByteArray())
            out.writeByte(0)
            out.writeBytes(value.toByteArray())
            out.writeByte(0)
        }
    }
}

/**
 * Handle both requests to, and responses from, to a SCGI script, storing the context associated with a request
 * and using that context to build a response that can be sent back to the client.
 */
class SCGIRequestContextHandler(): ChannelDuplexHandler() {

    private val logger = Logger.getLogger(javaClass.name)
    private lateinit var request: Request
    private lateinit var serverCtx: ChannelHandlerContext

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        val scgiRequest = msg as SCGIRequest
        request = scgiRequest.request
        serverCtx = scgiRequest.serverCtx
        ctx.fireChannelRead(scgiRequest)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val bytes = (msg as ByteBuf).toByteArray()
        serverCtx.writeAndClose(Response.fromBytes(bytes, request), logger)
    }
}

class SCGIChannelInitializer(): ChannelInitializer<DomainSocketChannel>() {
    override fun initChannel(ch: DomainSocketChannel) {
        val pipeline = ch.pipeline()
        pipeline.addLast(SCGIRequestEncoder())
        pipeline.addLast(SCGIRequestContextHandler())
    }
}


/**
 * A class that handles SCGI requests. One of these should be established per SCGI socket.
 *
 * (QUERY: Does this block?)
 *
 * From Netty's perspective, this is a client that makes requests over a socket and sends the results back to the
 * SCGIHandler.
 */

class SCGIClient(socket: File) {

    private val logger = Logger.getLogger(javaClass.name)
    private val socketAddr = DomainSocketAddress(socket)
    private val workerGroup = NioEventLoopGroup()
    private val bootstrap: Bootstrap
    init {
        try {
            bootstrap = Bootstrap().apply {
                group(workerGroup)
                channel(NioSocketChannel::class.java)
                handler(SCGIChannelInitializer())
            }
        } catch (e: Exception) {
            logger.severe("Error establishing connection with SCGI script: ${e.message}")
            throw e
        } finally {
            workerGroup.shutdownGracefully()
        }
    }

    fun write(scgiRequest: SCGIRequest) {
        val future = bootstrap.connect(socketAddr).sync()
        future.channel().writeAndClose(scgiRequest, logger)
    }

}
