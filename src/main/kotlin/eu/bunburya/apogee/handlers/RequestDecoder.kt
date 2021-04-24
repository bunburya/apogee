package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.models.Request
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.Delimiters
import io.netty.handler.ssl.SslHandler
import io.netty.util.CharsetUtil
import java.util.logging.Logger
import javax.net.ssl.SSLPeerUnverifiedException
import java.security.cert.Certificate

/**
 * Decodes inbound requests, converting them to Request objects.
 */
class RequestDecoder: DelimiterBasedFrameDecoder(
    1026, // 1024 (max URL length per spec) + "\r\n"
    true,
    true,
    *Delimiters.lineDelimiter()
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
        val byteBuf = super.decode(ctx, buffer) as ByteBuf?

        return if (byteBuf != null) {
            Request(
                byteBuf.toString(CharsetUtil.UTF_8),
                ctx.channel().remoteAddress(),
                clientCerts
            )
        } else null
    }

}