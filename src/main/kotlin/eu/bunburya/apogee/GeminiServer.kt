package eu.bunburya.apogee

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import kotlin.jvm.Throws

private class GeminiChannelInitializer(private val config: Config): ChannelInitializer<SocketChannel>() {

    private val logger = Logger.fromConfig(javaClass.name, config)

    @Throws(Exception::class)
    override fun initChannel(ch: SocketChannel) {

        logger.debug("Initialising channel.")

        val pipeline = ch.pipeline()

        // Add TLS handler
        val keyStoreFile = config.KEY_STORE
        val keyPass = config.KEY_PASS?.toCharArray()
        val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(FileInputStream(keyStoreFile), keyPass)
        keyManagerFactory.init(keyStore, keyPass)
        val sslCtx = SslContextBuilder.forServer(keyManagerFactory).build()
        pipeline.addLast(
            // SSH handler
            sslCtx.newHandler(ch.alloc()),

            // Inbound handlers
            RequestDecoder(),

            // Outbound handlers
            ResponseEncoder(),

            // The main request handler with our business logic should always be last in the pipeline
            MainRequestHandler(config),
        )

        logger.debug("All handlers added.")
    }

}

class GeminiServer(private val config: Config) {

    private val logger = Logger(javaClass.name)
    init {
        logger.addLogHandler(config.LOG_LEVEL, LogHandler(config.LOG_FILE))
    }

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

            logger.debug("Server bootstrapped; beginning bind.")

            // Bind and start to accepting incoming connections.
            val future = bootstrap.bind(config.HOSTNAME, config.PORT).sync()
            future.channel().closeFuture().sync()
            logger.debug("Bind successful.")

        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

}