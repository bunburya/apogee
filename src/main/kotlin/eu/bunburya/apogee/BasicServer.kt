package eu.bunburya.apogee

import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext


class BasicServer (val config: Config) {

    val logger = Logger("Server")

    init {
        val logHandler = LogHandler()
        logger.addLogHandler(LogLevel.DEBUG, logHandler)
        logger.addAccessLogHandler(logHandler)
        logger.info("Server initialised.")
    }

    /**
     * Create and return an SSL socket using TLS with the key specified in the config.
     */
    fun getTlsSocket(): ServerSocket {

        val hostname = config.HOSTNAME
        val port = config.PORT
        val keyStoreFile = config.KEY_STORE
        val keyPass = config.KEY_PASS?.toCharArray()

        val sslContext = SSLContext.getInstance("TLSv1.2")

        val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        val keyStore = KeyStore.getInstance("PKCS12")

        keyStore.load(FileInputStream(keyStoreFile), keyPass)
        keyManagerFactory.init(keyStore, keyPass)
        sslContext.init(keyManagerFactory.keyManagers, null, null)
        logger.debug("Beginning supported application protocol logging.")
        for (p in sslContext.supportedSSLParameters.protocols) {
            logger.debug("Supported application protocol: $p")
        }

        val tlsSocketFactory = sslContext.serverSocketFactory

        val serverSocket = tlsSocketFactory.createServerSocket(port)
        logger.debug("Created SSL server socked on port $hostname:$port.")
        return serverSocket

    }

    private fun readSocket(socket: Socket): String {
        logger.debug("readSocket working in thread ${Thread.currentThread().name}")
        val ipAddr = socket.inetAddress
        val socketReader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val request = socketReader.readLine()
        logger.debug("Received request from ${ipAddr}: $request")
        return request
    }

    private fun writeSocket(socket: Socket, data: String) {
        logger.debug("writeSocket working in thread ${Thread.currentThread().name}")
        logger.debug("Sending: $data")

        val outputStream = socket.getOutputStream()
        val socketWriter = PrintWriter(
            BufferedWriter(
                OutputStreamWriter(
                    outputStream
                )
            )
        )
        socketWriter.print(data)
        logger.debug("Wrote data.")
        socketWriter.flush()
        logger.debug("Flushed stream.")
    }

    private fun writeSocket(socket: Socket, data: ByteArray) {
        val outputStream = socket.getOutputStream()
        outputStream.write(data)
        outputStream.flush()
    }

    /**
     * Handle a single connection and request.
     *
     * @param socket the socket with which we have a connection.
     *
     * TODO: Make this asynchronous using coroutines.
     */
    private suspend fun handleConnection(socket: Socket) {
        try {
            val request = withContext(Dispatchers.IO) {
                readSocket(socket)
            }
            logger.debug("Read request: $request")
            withContext(Dispatchers.IO) {
                //writeSocket(socket, "10 Enter text\r\n")
                //writeSocket(socket, "42 Test error\r\n")
                writeSocket(socket, "20 text/gemini; lang=en-IE\r\n")
                writeSocket(socket, "Hello world!\n")
                //writeSocket(socket, "")


                //val file = File("/home/alan/bin/apogee/src/test/resources/srv/hello.gmi")
                //val fileInput = FileInputStream(file)
                //val dataStream = DataInputStream(fileInput)
                //val bytes = ByteArray(file.length().toInt())
                //dataStream.readFully(bytes)
                //writeSocket(socket, bytes)
                socket.shutdownOutput()
                socket.close()
                logger.debug("Closed socket.")
            }
        } catch (e: Exception) {
            logger.critical("Error: ${e.message}")
        }

    }

    fun run() = runBlocking {
        val serverSocket = getTlsSocket()
        while (true) {
            try {
                logger.debug("Accepting socket on thread ${Thread.currentThread().name}")
                val socket = serverSocket.accept()
                GlobalScope.launch {
                    logger.debug("Accepted socket.")
                    logger.debug("Launching handler.")
                    handleConnection(socket)
                }
            } catch (e: IOException) {
                println("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

}