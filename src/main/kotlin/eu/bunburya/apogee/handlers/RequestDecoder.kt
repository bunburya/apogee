package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.utils.toByteBuf
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.ssl.SslHandler
import io.netty.util.CharsetUtil
import java.net.InetSocketAddress
import java.util.logging.Logger
import javax.net.ssl.SSLPeerUnverifiedException
import java.security.cert.Certificate

// TODO: consider splitting Request into BadRequest (with a null URI) and GoodRequest (with a non-null URI), the former
// being used only to write BadRequestResponse back to the client, so that the subsequent handlers in the pipeline will
// also be working with a non-null URI.

/**
 * Decodes inbound requests, converting them to Request objects.
 */
class RequestDecoder: DelimiterBasedFrameDecoder(
    1026, // 1024 (max URL length per spec) + "\r\n"
    true,
    true,
    "\r\n".toByteBuf()
) {

    private val logger = Logger.getLogger(javaClass.name)

    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf): Any? {

        logger.fine("Decoding request.")

        val clientCerts = mutableListOf<Certificate>()
        try {
            val sslHandler = ctx.channel().pipeline().get("ssl") as SslHandler
            clientCerts.addAll(sslHandler.engine().session.peerCertificates)
        } catch (e: SSLPeerUnverifiedException) {
            logger.fine("Peer not authenticated.")
        }
        val byteBuf = super.decode(ctx, buffer) as ByteBuf? ?: return null

        /*println("Bytes:")
        for (b in byteBuf.toByteArray()) {
            println(b)
        }*/

        return Request(
            byteBuf.toString(CharsetUtil.UTF_8),
            ctx.channel().remoteAddress() as InetSocketAddress,
            clientCerts
        )
    }

}