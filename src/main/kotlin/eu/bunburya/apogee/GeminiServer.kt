package eu.bunburya.apogee

import eu.bunburya.apogee.handlers.*
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import java.io.File
import java.security.cert.X509Certificate
import java.util.logging.Logger
import javax.net.ssl.X509TrustManager
import kotlin.jvm.Throws

/**
 * A class to create and store instances of all handlers, so that new instances do not need to be generated on every
 * connection. Important to ensure that handlers to be reused in this way are stateless.
 */
private class HandlerStore(config: Config) {

    private val logger = Logger.getLogger(javaClass.name)

    // Initialise all sharable handlers once, and pass these instances to channel initialisers
    val responseEncoder = ResponseEncoder(getAccessLogger(config))
    val requestValidator = RequestValidator(config)
    val staticFileHandler = StaticFileHandler(config)

    // These handlers may be null, depending on our configuration
    val clientAuthHandler = if (config.CLIENT_CERT_ZONES.isNotEmpty()) ClientAuthHandler(config) else null
    val redirectHandler =
        if (config.TEMP_REDIRECTS.isNotEmpty() || config.PERM_REDIRECTS.isNotEmpty()) RedirectHandler(config)
        else null
    val cgiHandler = if (config.CGI_PATHS.isNotEmpty()) CGIHandler(config) else null
    val scgiHandler = if (config.SCGI_PATHS.isNotEmpty()) SCGIHandler(config) else null

    // Some handlers are not sharable because they maintain internal state, so provide functions to generate new ones
    fun newRequestDecoder() = RequestDecoder()

    // Build SSL context
    private val sslCtx = SslContextBuilder.forServer(File(config.CERT_FILE), File(config.KEY_FILE))
        .protocols("TLSv1.3", "TLSv1.2")
        .trustManager(object: X509TrustManager {
            // "Dummy" trust manager to accept all client certs (including self-signed certs).
            // We can then decide what to do with the certs (ie, accept or reject) later on.
            // TODO: Consider whether this is safe enough, or whether we should add these certs to a TrustStore.
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
        })
        .clientAuth(ClientAuth.OPTIONAL)
        .build()

    fun newSslHandler(ch: SocketChannel): SslHandler {
        logger.fine("Getting new SSL handler")
        val handler = sslCtx.newHandler(ch.alloc())
        logger.fine("Engine: ${handler.engine()}")
        return handler
    }
}

private class GeminiChannelInitializer(private val config: Config): ChannelInitializer<SocketChannel>() {

    private val logger = Logger.getLogger(javaClass.name)
    private val handlerStore = HandlerStore(config)

    @Throws(Exception::class)
    override fun initChannel(ch: SocketChannel) {

        val pipeline = ch.pipeline()

        // Add the SSL handler first, with a name so it can be accessed later on.
        pipeline.addLast("ssl", handlerStore.newSslHandler(ch))
        pipeline.addLast(
            // Outbound handlers go first so inbound handlers can send Response directly back to clients if necessary
            handlerStore.responseEncoder,
            handlerStore.newRequestDecoder(),
            handlerStore.requestValidator
        )
        if (handlerStore.clientAuthHandler != null) pipeline.addLast(handlerStore.clientAuthHandler)
        if (handlerStore.cgiHandler != null) pipeline.addLast(handlerStore.cgiHandler)
        if (handlerStore.scgiHandler != null) pipeline.addLast(handlerStore.scgiHandler)
        if (handlerStore.redirectHandler != null) pipeline.addLast(handlerStore.redirectHandler)
        pipeline.addLast(handlerStore.staticFileHandler)
        logger.fine("All handlers added; channel initialised.")
    }
}

class GeminiServer(private val config: Config) {

    private val logger = Logger.getLogger(javaClass.name)

    @Throws(Exception::class)
    fun run() {
        logger.info("Initialising server.")
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup(1024)
        try {
            val bootstrap = ServerBootstrap().apply {
                group(bossGroup, workerGroup)
                channel(NioServerSocketChannel::class.java)
                childHandler(GeminiChannelInitializer(config))
                option(ChannelOption.SO_BACKLOG, 128)
                // TODO: Consider whether we actually want KEEPALIVE
                childOption(ChannelOption.SO_KEEPALIVE, true)
            }

            logger.fine("Server bootstrapped; binding to port ${config.PORT} on ${config.HOSTNAME}.")

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