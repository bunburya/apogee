package eu.bunburya.apogee

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil

const val UTF_8_SPACE = 32
val UTF_8_CRLF = byteArrayOf(13, 10)

/**
 * This file contains classes corresponding to specific Gemini responses. The RequestHandler class should send responses
 * as relevant Response subclasses, and ResponseEncoder should encode Response objects to ByteBuf objects for sending
 * to the client over a socket.
 */

/**
 * Base class for responses to requests on the server.
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
): Response(statusCode, meta, request)

/**
 * Input expected.
 *
 * @param prompt The message the client should display to the user when requesting input.
 * @param sensitive Whether the input is sensitive (ie, not to be echoed to the screen).
 */
class InputResponse(
    val prompt: String,
    val sensitive: Boolean = false,
    request: Request
): NonErrorResponse(if (sensitive) 11 else 11, prompt, request)

/**
 * Request was successful.
 *
 * @param mimetype The MIME type of the content being returned.
 * @param body The main data to be returned.
 */
class SuccessResponse(
    val mimetype: String,
    body: ByteArray,
    request: Request
): NonErrorResponse(20, mimetype, request, body) {
    constructor(mimetype: String, body: String, request: Request):
            this(mimetype, body.toByteArray(CharsetUtil.UTF_8), request)
}

/**
 * Redirecting client to a new location.
 *
 * @param uri The new location the client should request.
 * @param permanent Whether the redirection is permanent as opposed to temporary.
 */
class RedirectionResponse(
    val uri: String,
    val permanent: Boolean,
    request: Request
): NonErrorResponse(if (permanent) 31 else 30, uri, request)

/**
 * Base class for all error responses (status codes 40-69). These responses may have a null meta value.
 *
 * @param errorMessage The optional error message that the client should display to the user.
 * @param baseStatusCode An integer corresponding to the first digit of the response status code.
 * @param subStatusCode An integer corresponding to the second digit of the response status code.
 */
abstract class ErrorResponse(
    errorMessage: String? = null,
    request: Request,
    baseStatusCode: Int,
    subStatusCode: Int
): Response(baseStatusCode + subStatusCode, errorMessage, request)


/**
 * An unspecified temporary failure occurred.
 *
 * This class also acts as a base class for all temporary failure response classes.
 */
open class TemporaryFailureResponse(
    errorMessage: String? = null,
    request: Request,
    private val subStatusCode: Int = 0,
): ErrorResponse(errorMessage, request, 40,subStatusCode)

/**
 * Server currently unavailable.
 */
class ServerUnavailableResponse(
    errorMessage: String? = null,
    request: Request
): TemporaryFailureResponse(errorMessage, request, 1)

/**
 * A CGI process, or similar system for generating dynamic content, died unexpectedly or timed out.
 */
class CGIErrorResponse(
    errorMessage: String? = null,
    request: Request
): TemporaryFailureResponse(errorMessage, request, 2)

/**
 * Proxy request failed.
 */
class ProxyErrorResponse(
    errorMessage: String? = null,
    request: Request
): TemporaryFailureResponse(errorMessage, request, 3)

/**
 * Client should slow down requests.
 */
class SlowDownResponse(
    errorMessage: String? = null,
    request: Request
): TemporaryFailureResponse(errorMessage, request, 4)


/**
 * General permanent failure response.
 *
 * This class also acts as a base class for all temporary failure response classes.
 *
 * @param errorMessage The error message the client should display to the user.
 * @param subStatusCode An integer corresponding to the second digit of the status code.
 */
open class PermanentFailureResponse(
    errorMessage: String? = null,
    request: Request,
    private val subStatusCode: Int = 0,
): ErrorResponse(errorMessage, request, 50, subStatusCode)

/**
 * Resource not found.
 */
class NotFoundResponse(
    errorMessage: String? = null,
    request: Request
): PermanentFailureResponse(errorMessage, request, 1)

/**
 * Resource no longer available and will not be available again.
 */
class GoneResponse(
    errorMessage: String? = null,
    request: Request
): PermanentFailureResponse(errorMessage, request, 2)

/**
 * Resource requested is at a different domain and server does not accept proxy requests.
 */
class ProxyRequestRefusedResponse(
    errorMessage: String? = null,
    request: Request
): PermanentFailureResponse(errorMessage, request, 3)

/**
 * Request is bad.
 */
class BadRequestResponse(
    errorMessage: String? = null,
    request: Request
): PermanentFailureResponse(errorMessage, request, 9)

/**
 * Requested resource requires a client certificate to access.
 *
 * This class also acts as a base class for all client certificate response classes.
 */
open class ClientCertificateResponse(
    errorMessage: String? = null,
    request: Request,
    subStatusCode: Int = 0
): ErrorResponse(errorMessage, request, 60, subStatusCode)

/**
 * Certificate not authorised.
 */
class CertificateNotAuthorizedResponse(
    errorMessage: String? = null,
    request: Request
): ClientCertificateResponse(errorMessage, request, 1)

/**
 * Certificate not valid (ie, there is a problem with the certificate itself).
 */
class CertificateNotValidResponse(
    errorMessage: String? = null,
    request: Request
): ClientCertificateResponse(errorMessage, request, 2)