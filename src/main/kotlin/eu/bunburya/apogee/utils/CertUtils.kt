package eu.bunburya.apogee.utils

import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * Constants representing the possible results of checking whether the client has sent the required certificates for a
 * given request.
 */
enum class ClientCertStatus {
    NO_CERT, // Client has not provided any certificates.
    NO_MATCHING_CERT, // Client has provided one or more certificates but one or more don't match.
    MATCHING_CERT_INVALID, // Client has provided one or more certificates which match, but at least one is invalid.
    MATCHING_CERT, // Client has provided a matching, valid cert for each pattern that applies to the path.
    NO_CERT_NEEDED, // Client does not need to provide a cert because it is not accessing a protected zone.
    OTHER // Catch-all. If this is returned, it probably means there is a problem with our logic.
}

/**
 * Convert a sequence of bytes to a hex string.
 */
private fun getHexString(bytes: ByteArray): String = StringBuilder(2 * bytes.size).apply {
    for (b in bytes) {
        val hex = Integer.toHexString(0xff and b.toInt())
        if (hex.length == 1) {
            append('0')
        }
        append(hex)
    }
}.toString()

/**
 * Get the SHA-256 hash of a certificate, represented as an array of bytes.
 */
private fun getCertHash(cert: Certificate): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(cert.encoded)
}

/**
 * Get the SHA-256 hash of a certificate, represented as a hex string.
 */
fun getCertHashString(cert: Certificate): String = getHexString(getCertHash(cert))


val Certificate.hashString: String get() = getCertHashString(this)
