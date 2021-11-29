package eu.bunburya.apogee.handlers

import eu.bunburya.apogee.Config
import eu.bunburya.apogee.models.*
import eu.bunburya.apogee.utils.compileKeys
import eu.bunburya.apogee.utils.hashString
import eu.bunburya.apogee.utils.writeAndClose
import eu.bunburya.apogee.utils.ClientCertStatus
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.security.cert.Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.logging.Logger

@ChannelHandler.Sharable
class ClientAuthHandler(private val config: Config): ChannelInboundHandlerAdapter() {

    private val logger = Logger.getLogger(javaClass.name)
    private val patterns = compileKeys(config.CLIENT_CERT_ZONES)


    /**
     * Check if a certificate is valid (as distinct from authenticated).
     */
    private fun certIsValid(cert: Certificate): Boolean {
        val x509 = cert as X509Certificate
        return try {
            x509.checkValidity()
            true
        } catch (e: CertificateExpiredException) {
            false
        } catch (e: CertificateNotYetValidException) {
            false
        }
    }

    /**
     * Check if a request is permitted on the basis of its client certificate. The path specified in the request is
     * compared against each pattern-hash pair specified in the config.
     *
     * Where a client provides multiple certificates, we only check the first one (as the others will be authority
     * certs). Where the requested path matches multiple patterns, it must be authenticated with respect to each
     * matching pattern in order to be allowed.
     *
     * @return A constant of type ClientCertStatus, representing the result of the check.
     */
    private fun requestIsAllowed(request: Request): ClientCertStatus {

        val numCerts = request.clientCerts.size

        val clientCert = if (request.clientCerts.isNotEmpty()) request.clientCerts[0] else null

        // The number of patterns which match the given path (indicating that the path is in a protected zone).
        // We increment this each time a pattern matches.
        var matchedPatterns = 0

        // The number of certificates provided by the client which match a pattern.
        // This is incremented (once per pattern only) when a matching client certificate is found.
        var matchingCerts = 0

        // The number of certificates provided by the client which match a pattern and which are valid.
        var validMatchingCerts = 0

        // Iterate through the pattern-hash combinations specified in the config
        for ((pattern, allowedHashes) in patterns) {
            // Check if the current pattern matches the request
            //logger.fine("Testing request ${request.uri!!.path} against pattern $pattern")
            if (pattern.matcher(request.uri!!.path).find()) {
                //logger.fine("Matched pattern.")
                matchedPatterns++
                if (clientCert == null) return ClientCertStatus.NO_CERT
                val certHash = clientCert.hashString
                //logger.fine("Found cert with hash $certHash.")
                // Check if the certificate's hash matches the specified one
                for (hash in allowedHashes) {
                    //logger.fine("Checking client hash $certHash against allowed hash $hash")
                    if (certHash == hash) {
                        //logger.fine("Cert matched.")
                        matchingCerts++
                        if (certIsValid(clientCert)) validMatchingCerts++
                        break
                    } //else logger.fine("No match.")
                }
            } //else logger.fine("Not matched.")
        }

        // We should not have more authenticated patterns than matched patterns, or more matching certs than valid
        // matching certs; if we do, it means the logic above isn't working properly.
        assert((matchedPatterns >= matchingCerts) && (matchingCerts >= validMatchingCerts))

        return when {
            matchedPatterns == 0 -> ClientCertStatus.NO_CERT_NEEDED
            numCerts == 0 -> ClientCertStatus.NO_CERT
            matchedPatterns == validMatchingCerts -> ClientCertStatus.MATCHING_CERT
            matchedPatterns > matchingCerts -> ClientCertStatus.NO_MATCHING_CERT
            matchedPatterns > validMatchingCerts -> ClientCertStatus.MATCHING_CERT_INVALID
            else -> {
                logger.warning(
                    "requestIsAllowed function returned OTHER status. Matched patterns: $matchedPatterns; " +
                            "matching certs: $matchingCerts; valid matching certs: $validMatchingCerts."
                )
                ClientCertStatus.OTHER
            }
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as Request
        when (requestIsAllowed(request)) {
            ClientCertStatus.NO_CERT_NEEDED -> ctx.fireChannelRead(msg)
            ClientCertStatus.MATCHING_CERT -> ctx.fireChannelRead(msg)
            ClientCertStatus.NO_CERT -> ctx.writeAndClose(ClientCertNeededResponse(request), logger)
            ClientCertStatus.NO_MATCHING_CERT -> ctx.writeAndClose(ClientCertNotAuthorizedResponse(request), logger)
            ClientCertStatus.MATCHING_CERT_INVALID -> ctx.writeAndClose(ClientCertNotValidResponse(request), logger)
            ClientCertStatus.OTHER -> ctx.writeAndClose(
                TemporaryFailureResponse(
                    request,
                    "Unspecified error when authenticating client certificates. " +
                            "This has been logged on the server side."
                ),
                logger
            )
        }

    }

}