/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.auth.protocol.saml2;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.link.safeonline.sdk.logging.exception.ValidationFailedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.RequestAbstractType;


/**
 * Utility class for SAML2 requests.
 *
 * @author lhunath
 */
public abstract class RequestUtil {

    private static final Log LOG = LogFactory.getLog( RequestUtil.class );

    /**
     * Sends a SAML2 Request.
     *
     * @param consumerUrl          The URL of the SAML response message consumer.
     * @param requestBinding       The SAML Binding to use for communicating the request to the consumer.
     * @param samlRequest          The SAML request token.
     * @param signingKeyPair       The {@link KeyPair} to use for generating the message signature.
     * @param certificateChain     The optional certificate chain for the signing keypair, if specified it is included in the message's
     *                             signature KeyInfo and can be used for offline signature validation
     * @param response             The {@link HttpServletResponse} to write the response to.
     * @param relayState           optional relay state
     * @param postTemplateResource The resource that contains the template of the SAML HTTP POST Binding message.
     * @param language             A language hint to make the linkID authentication application use the same locale as the requesting
     *                             application.
     * @param themeName            The name of the theme to apply in linkID.
     *
     * @throws IOException IO Exception
     */
    public static void sendRequest(String consumerUrl, SAMLBinding requestBinding, RequestAbstractType samlRequest, KeyPair signingKeyPair,
                                   List<X509Certificate> certificateChain, HttpServletResponse response, String relayState,
                                   String postTemplateResource, Locale language, String themeName)
            throws IOException {

        switch (requestBinding) {
            case HTTP_POST:
                PostBindingUtil.sendRequest( samlRequest, signingKeyPair, certificateChain, relayState, postTemplateResource, consumerUrl,
                                             response, language, themeName );
                break;

            case HTTP_REDIRECT:
                RedirectBindingUtil.sendRequest( samlRequest, signingKeyPair, relayState, consumerUrl, response );
                break;
        }
    }

    /**
     * The SAML {@link LogoutRequest} that is in the HTTP request<br> <code>null</code> if there is no SAML message in the HTTP request.
     *
     * @param request                 HTTP Servlet Request
     * @param logoutRequest           SAML v2.0 Request
     * @param applicationCertificate  application certificate
     * @param applicationPrivateKey   application private key
     * @param serviceCertificates     linkID service certificates used for validation of non-XML DSig signatures, e.g. SAML HTTP-Redirect.
     * @param serviceRootCertificates The linkID service root certificates, optionally used for trust validation of the cert.chain returned
     *                                in signed authentication responses.
     *
     * @return optional embedded certificate chain in the LogoutRequest's signature.
     *
     * @throws ValidationFailedException validation failed for some reason
     */
    public static List<X509Certificate> validateRequest(HttpServletRequest request, LogoutRequest logoutRequest,
                                                        X509Certificate applicationCertificate, PrivateKey applicationPrivateKey,
                                                        List<X509Certificate> serviceCertificates,
                                                        List<X509Certificate> serviceRootCertificates)
            throws ValidationFailedException {

        // validate signature
        List<X509Certificate> certificateChain = Saml2Util.validateSignature( logoutRequest.getSignature(), serviceCertificates, request );

        // validate cert.chain trust
        if (null != serviceRootCertificates && !serviceRootCertificates.isEmpty()) {
            LOG.debug( "Logout Response: LinkID certificate chain trust validation" );
            Saml2Util.validateCertificateChain( serviceRootCertificates, certificateChain );
        }

        // validate logout request
        if (null == logoutRequest.getIssuer() || null == logoutRequest.getIssuer().getValue()) {
            throw new ValidationFailedException( "Missing Issuer for the SAML v2.0 Logout Request" );
        }
        if (null == logoutRequest.getNameID() || null == logoutRequest.getNameID().getValue()) {
            throw new ValidationFailedException( "Missing NameID field for the SAML v2.0 Logout Request" );
        }

        return certificateChain;
    }

    /**
     * @param request HTTP Servlet Request
     *
     * @return The SAML {@link AuthnRequest} that is in the HTTP request<br> <code>null</code> if there is no SAML message in the HTTP
     *         request.
     */
    public static AuthnRequest findAuthnRequest(HttpServletRequest request) {

        return BindingUtil.findSAMLObject( request, AuthnRequest.class );
    }

    /**
     * @param request HTTP Servlet Request
     *
     * @return The {@link LogoutRequest} in the HTTP request.<br> <code>null</code> if there is no SAML message in the HTTP request.
     */
    public static LogoutRequest findLogoutRequest(HttpServletRequest request) {

        return BindingUtil.findSAMLObject( request, LogoutRequest.class );
    }
}