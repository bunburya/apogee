package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.dynamic.CGIServer
import eu.bunburya.apogee.models.CGIErrorResponse
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.utils.fileIsInDirectory
import eu.bunburya.apogee.utils.getFilePath
import eu.bunburya.apogee.utils.resolvePath
import eu.bunburya.apogee.utils.writeResponse
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import javafx.application.Application.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import java.util.logging.Logger

class CGIHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)
    private val cgiServer = CGIServer(config)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.fine("CGIHandler reached.")
        val request = msg as Request
        // If we can't find a CGI script, return a CGI error (note: this is different to how Molly Brown does it, which
        // is to let the file be handled as a static file).
        val (scriptPath, pathInfo) = cgiServer.getCGIScript(request)
            ?: return writeResponse(ctx, CGIErrorResponse(request))
        return writeResponse(ctx, cgiServer.launchProcess(scriptPath, pathInfo, request))

    }

}