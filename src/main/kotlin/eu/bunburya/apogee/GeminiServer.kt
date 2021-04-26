package eu.bunburya.apogee

import eu.bunburya.apogee.handlers.StaticFileHandler
import eu.bunburya.apogee.handlers.RequestDecoder
import eu.bunburya.apogee.handlers.ResponseEncoder
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContextBuilder
import java.security.cert.X509Certificate
import java.util.logging.Logger
import javax.net.ssl.X509TrustManager
import kotlin.jvm.Throws

private class GeminiChannelInitializer(private val config: Config): ChannelInitializer<SocketChannel>() {

    private val logger = Logger.getLogger(javaClass.name)

    @Throws(Exception::class)
    override fun initChannel(ch: SocketChannel) {

        logger.fine("Initialising channel.")

        val pipeline = ch.pipeline()

        // Build SSL context
        val sslCtx = SslContextBuilder.forServer(config.CERT_FILE, config.KEY_FILE)
            .trustManager(object: X509TrustManager {
                // "Dummy" trust manager to accept all client certs (including self-signed certs).
                // We can then decide what to do with the certs (ie, accept or reject) later on.
                override fun getAcceptedIssuers(): Array<X509Certificate>? = arrayOf<X509Certificate>()
                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            })
            .clientAuth(ClientAuth.OPTIONAL)
            .build()

        // Add the SSL handler first, with a name so it can be accessed later on.
        pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()))

        pipeline.addLast(
            // Outbound handlers go first so inbound handlers can send Response directly back to clients if necessary
            ResponseEncoder(getAccessLogger(config)),

            // Inbound handlers
            RequestDecoder(),

            // The main request handler with our business logic should always be last in the pipeline
            StaticFileHandler(config),
        )

        logger.fine("All handlers added.")
    }

}

class GeminiServer(private val config: Config) {

    private val logger = Logger.getLogger(javaClass.name)

    @Throws(Exception::class)
    fun run() {
        logger.info("Initialising server.")
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        try {
            val bootstrap = ServerBootstrap().apply {
                group(bossGroup, workerGroup)
                channel(NioServerSocketChannel::class.java)
                childHandler(GeminiChannelInitializer(config))
                option(ChannelOption.SO_BACKLOG, 128)
                childOption(ChannelOption.SO_KEEPALIVE, true)
            }

            logger.fine("Server bootstrapped; beginning bind.")

            // Bind and start to accepting incoming connections.
            val future = bootstrap.bind(config.HOSTNAME, config.PORT).sync()
            future.channel().closeFuture().sync()
            logger.fine("Bind successful.")

        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

}