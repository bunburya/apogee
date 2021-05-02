package eu.bunburya.apogee

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.Delimiters
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.CharsetUtil
import java.io.File
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.math.pow


const val CR = 13.toByte()
const val LF = 10.toByte()

data class TestResponse(
    val statusCode: Int,
    val meta: String,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestResponse

        if (statusCode != other.statusCode) return false
        if (meta != other.meta) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + meta.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

class TestResponseDecoder: DelimiterBasedFrameDecoder(
    2.0.pow(32).toInt(),
    true,
    true,
    *Delimiters.nulDelimiter()
) {

    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf): Any {
        val numBytes = buffer.readableBytes()
        val bytes = ByteArray(numBytes)
        buffer.readBytes(bytes)
        println("Got response: ${bytes.decodeToString()}")
        val cr = bytes.indexOf(CR)
        val lf = bytes.indexOf(LF)
        assert(lf == cr + 1)
        val header = bytes.slice(0 until cr).toByteArray().decodeToString()
        val statusCode = header.slice(0..1).toByteArray().decodeToString().toInt()
        val meta = header.slice(3 until header.length).toByteArray().decodeToString()
        val body = bytes.slice(lf + 1 until numBytes).toByteArray()
        return TestResponse(statusCode, meta, body)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // Close the connection when an exception is raised.
        cause.printStackTrace()
        ctx.close()
    }
}

class TestResponseHandler(private val testFunc: (TestResponse) -> Any): ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val response = msg as TestResponse
        testFunc(response)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // Close the connection when an exception is raised.
        cause.printStackTrace()
        ctx.close()
    }
}

class TestClientHandler(private val request: ByteArray): ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        val str = request.decodeToString()
            .replace('\r', 'R')
            .replace('\n', 'N')
        println("Writing bytes: $str")
        val f = ctx.writeAndFlush(Unpooled.copiedBuffer(request))
        f.addListener { if (!f.isSuccess) println("Send failed: ${f.cause()}") }
    }
}

class TestClientChannelInitializer(
    private val request: ByteArray,
    private val testFunc: (TestResponse) -> Any,
    certFile: File? = null,
    keyFile: File? = null,
): ChannelInitializer<SocketChannel>() {

    private val sslCtx = SslContextBuilder.forClient()
        .keyManager(certFile, keyFile)
        .trustManager(object: X509TrustManager {
            // "Dummy" trust manager to accept all server certs (including self-signed certs).
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
        })
        .build()

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()))
        pipeline.addLast(TestResponseDecoder())
        pipeline.addLast(TestResponseHandler(testFunc))
        pipeline.addLast(TestClientHandler(request))
    }

}

class TestClient(private val host: String = "localhost", private val port: Int = 1965) {

    /**
     * Make a single request of the Gemini server and call testFunc on the result.
     *
     * @param request The request to send.
     * @param certFile The file containing the client cert to send, if any.
     * @param keyFile The file containing the associated key, if any.
     * @param testFunc A function that takes a TestResponse object and handles it in some way. In a test context, this
     * function should perform various assertions on the TestResponse. It can return anything, and its return value is
     * not used.
     */
    fun testRequest(
        request: ByteArray,
        certFile: File? = null,
        keyFile: File? = null,
        testFunc: (TestResponse) -> Any
    ) {
        val workerGroup = NioEventLoopGroup()
        try {
            val bootstrap = Bootstrap().apply {
                group(workerGroup)
                channel(NioSocketChannel::class.java)
                handler(TestClientChannelInitializer(request, testFunc, certFile, keyFile))
            }
            val future = bootstrap.connect(host, port).sync()
            future.channel().closeFuture().sync()
        } catch (e: Exception) {
            throw e
        } finally {
            workerGroup.shutdownGracefully()
        }
    }

    fun testRequest(request: String, certFile: File? = null, keyFile: File? = null, testFunc: (TestResponse) -> Any) =
        testRequest(request.toByteArray(CharsetUtil.UTF_8), certFile, keyFile, testFunc)

}