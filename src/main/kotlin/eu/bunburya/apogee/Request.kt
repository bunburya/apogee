package eu.bunburya.apogee

import java.net.SocketAddress
import java.net.URI

/**
 * A basic class representing the validity of a request.
 *
 * @param isValid Whether the request is valid.
 * @param defaultMsg A default human-readable message that can be returned to the client, explaining the status.
 */
enum class RequestValidity(val isValid: Boolean, val defaultMsg: String) {
    NOT_GEMINI_URI(false, "Not a Gemini URI"),
    URI_TOO_LARGE(false, "URI too large"),
    NO_HOST(false, "No host specified"),
    USERINFO(false, "Request may not specify a userinfo portion"),
    FRAGMENT(false, "Request may not include a fragment"),
    OK(true, "OK")
}

/**
 * A class representing an inbound request, which may or may not be valid.
 *
 * @param content The content of the request.
 * @param ipAddr The IP address from which the request originated.
 */
data class Request (
    val content: String,
    val ipAddr: SocketAddress
) {
    /**
     * java.net.URI object representing the requested URI. This will throw a java.net.URISyntaxException if content
     * cannot be parsed as a URI.
     */
    val uri = URI(content)

    /**
     * The IP address from which the request originated, as a String.
     */
    val ipString = ipAddr.toString()

    /**
     * Perform some basic checks to determine if a request looks like a valid Gemini request.
     *
     * Does not check that the resource sought is appropriate, only that the request is validly formed.
     */
    val validity: RequestValidity by lazy {
        when {
            uri.scheme != "gemini" -> RequestValidity.NOT_GEMINI_URI
            content.toByteArray().size > 1024 -> RequestValidity.URI_TOO_LARGE
            uri.host == null -> RequestValidity.NO_HOST
            uri.userInfo != null -> RequestValidity.USERINFO
            uri.fragment != null -> RequestValidity.FRAGMENT
            else -> RequestValidity.OK
        }
    }

    val isValid: Boolean get() = validity.isValid
}