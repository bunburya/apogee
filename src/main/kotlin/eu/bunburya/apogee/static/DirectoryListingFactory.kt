package eu.bunburya.apogee.static

import eu.bunburya.apogee.*
import eu.bunburya.apogee.models.Request
import eu.bunburya.apogee.models.Response
import eu.bunburya.apogee.models.SuccessResponse
import eu.bunburya.apogee.utils.formatTime
import eu.bunburya.apogee.utils.splitExt
import io.netty.util.CharsetUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

const val SEPARATOR = "::"

class DirectoryListingFactory(private val config: Config) {

    private val rootDir = File(config.DOCUMENT_ROOT)

    private fun getGeminiLink(file: File): String {
        val fullName = file.name
        var relativeUrl = fullName.replace(" ", "%20")
        if (file.isDirectory) relativeUrl += "/"
        val name = splitExt(fullName).first
        val size = Files.size(file.toPath())
        val modified = formatTime(file.lastModified())
        return "=> $relativeUrl $name $SEPARATOR $size $SEPARATOR $modified\n"
    }

    fun directoryListing(directory: File, request: Request): Response {
        val parentPath = directory.parentFile.relativeTo(rootDir)
        val files = directory.listFiles()!!.sortedBy{ it.name }
        val lines = mutableListOf(
            "# Directory listing\n",
            "\n",
            "=> /$parentPath ..\n",
            "\n",
            "name $SEPARATOR size $SEPARATOR last modified\n"
        )
        lines.addAll(files.map { getGeminiLink(it) })
        val bytes = ByteArrayOutputStream()
        for (line in lines) {
            bytes.write(line.toByteArray(CharsetUtil.UTF_8))
        }
        bytes.flush()
        println(bytes.toString())
        return SuccessResponse("text/gemini", bytes.toByteArray(), request)

    }


}