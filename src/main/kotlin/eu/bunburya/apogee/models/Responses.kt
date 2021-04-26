package eu.bunburya.apogee.models

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import java.util.logging.Logger

const val UTF_8_SPACE = 32
val UTF_8_CRLF = byteArrayOf(13, 10)

/**
 * This file contains classes corresponding to specific Gemini responses. The RequestHandler class should send responses
 * as relevant Response subclasses, and ResponseEncoder should encode Response objects to ByteBuf objects for sending
 * to the client over a socket.
 */

/**
 * Base class for all responses to requests on the server.
 *
 * @param statusCode The (full) status code of the response.
 * @param meta The data that follows the status code (after the space).
 * @param request The Request object representing the inbound request that triggered the response.
 * @param body The body of the response.
 */
abstract class Response(
    val statusCode: Int,
    val meta: String?,
    val request: Request,
    val body: ByteArray? = null
) {
    private val logger = Logger.getLogger(javaClass.name)

    /**
     * Convert the Response object to a ByteBuf for writing to a socket.
     *
     * Assumes a charset of UTF-8.
     */
    fun toByteBuf(): ByteBuf {
        val buffer = Unpooled.buffer()
        buffer.writeBytes(statusCode.toString().toByteArray(CharsetUtil.UTF_8))
        if (meta != null) {
            buffer.writeByte(UTF_8_SPACE)
            buffer.writeBytes(meta.toByteArray(CharsetUtil.UTF_8))
        }
        buffer.writeBytes(UTF_8_CRLF)
        if (body != null) buffer.writeBytes(body)
        return buffer
    }
}

/**
 * A base class for all non-error responses (status codes 10-39). These responses must have a non-null meta value.
 */
abstract class NonErrorResponse(
    statusCode: Int,
    meta: String,
    request: Request,
    body: ByteArray? = null
): Response(statusCode, meta, request, body)

/**
 * Input expected.
 *
 * @param prompt The message the client should display to the user when requesting input.
 * @param sensitive Whether the input is sensitive (ie, not to be echoed to the screen).
 */
class InputResponse(
    request: Request,
    val prompt: String,
    val sensitive: Boolean = false
): NonErrorResponse(if (sensitive) 11 else 11, prompt, request)

/**
 * Request was successful.
 *
 * @param mimetype The MIME type of the content being returned.
 * @param body The main data to be returned.
 */
class SuccessResponse(
    request: Request,
    val mimetype: String,
    body: ByteArray
): NonErrorResponse(20, mimetype, request, body) {
    constructor(mimetype: String, body: String, request: Request):
            this(request, mimetype, body.toByteArray(CharsetUtil.UTF_8))
}

/**
 * Redirecting client to a new location.
 *
 * @param uri The new location the client should request.
 * @param permanent Whether the redirection is permanent as opposed to temporary.
 */
class RedirectionResponse(
    request: Request,
    val uri: String,
    val permanent: Boolean
): NonErrorResponse(if (permanent) 31 else 30, uri, request)

/**
 * Base class for all error responses (status codes 40-69). These responses may have a null meta value.
 *
 * @param errorMessage The optional error message that the client should display to the user.
 * @param baseStatusCode An integer corresponding to the first digit of the response status code.
 * @param subStatusCode An integer corresponding to the second digit of the response status code.
 */
abstract class ErrorResponse(
    request: Request,
    errorMessage: String? = null,
    baseStatusCode: Int,
    subStatusCode: Int
): Response(baseStatusCode + subStatusCode, errorMessage, request)


/**
 * An unspecified temporary failure occurred.
 *
 * This class also acts as a base class for all temporary failure response classes.
 */
open class TemporaryFailureResponse(
    request: Request,
    errorMessage: String? = null,
    subStatusCode: Int = 0,
): ErrorResponse(request, errorMessage, 40, subStatusCode)

/**
 * Server currently unavailable.
 */
class ServerUnavailableResponse(
    request: Request,
    errorMessage: String? = "Server unavailable"
): TemporaryFailureResponse(request, errorMessage, 1)

/**
 * A CGI process, or similar system for generating dynamic content, died unexpectedly or timed out.
 */
class CGIErrorResponse(
    request: Request,
    errorMessage: String? = "CGI error"
): TemporaryFailureResponse(request, errorMessage, 2)

/**
 * Proxy request failed.
 */
class ProxyErrorResponse(
    request: Request,
    errorMessage: String? = "Proxy error"
): TemporaryFailureResponse(request, errorMessage, 3)

/**
 * Client should slow down requests.
 */
class SlowDownResponse(
    request: Request,
    errorMessage: String? = "Slow down"
): TemporaryFailureResponse(request, errorMessage, 4)


/**
 * General permanent failure response.
 *
 * This class also acts as a base class for all temporary failure response classes.
 *
 * @param errorMessage The error message the client should display to the user.
 * @param subStatusCode An integer corresponding to the second digit of the status code.
 */
open class PermanentFailureResponse(
    request: Request,
    errorMessage: String? = "Permanent failure",
    private val subStatusCode: Int = 0,
): ErrorResponse(request, errorMessage, 50, subStatusCode)

/**
 * Resource not found.
 */
class NotFoundResponse(
    request: Request,
    errorMessage: String? = "Not found"
): PermanentFailureResponse(request, errorMessage, 1)

/**
 * Resource no longer available and will not be available again.
 */
class GoneResponse(
    request: Request,
    errorMessage: String? = "Gone"
): PermanentFailureResponse(request, errorMessage, 2)

/**
 * Resource requested is at a different domain and server does not accept proxy requests.
 */
class ProxyRequestRefusedResponse(
    request: Request,
    errorMessage: String? = "Proxy request refused"
): PermanentFailureResponse(request, errorMessage, 3)

/**
 * Request is bad.
 */
class BadRequestResponse(
    request: Request,
    errorMessage: String? = "Bad request"
): PermanentFailureResponse(request, errorMessage, 9)

/**
 * Requested resource requires a client certificate to access.
 *
 * This class also acts as a base class for all client certificate response classes.
 */
open class ClientCertNeededResponse(
    request: Request,
    errorMessage: String? = "Client certificate needed",
    subStatusCode: Int = 0
): ErrorResponse(request, errorMessage, 60, subStatusCode)

/**
 * Certificate not authorised.
 */
class ClientCertNotAuthorizedResponse(
    request: Request,
    errorMessage: String? = "Client certificate not authorised"
): ClientCertNeededResponse(request, errorMessage, 1)

/**
 * Certificate not valid (ie, there is a problem with the certificate itself).
 */
class ClientCertNotValidResponse(
    request: Request,
    errorMessage: String? = "Client certificate not valid"
): ClientCertNeededResponse(request, errorMessage, 2)