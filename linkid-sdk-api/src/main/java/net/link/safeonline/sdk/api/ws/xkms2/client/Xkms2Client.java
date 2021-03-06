/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.api.ws.xkms2.client;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import net.link.safeonline.sdk.api.exception.ValidationFailedException;
import net.link.safeonline.sdk.api.exception.WSClientTransportException;


/**
 * Interface for XKMS 2.0 client.
 *
 * @author wvdhaute
 */
public interface Xkms2Client {

    String USE_KEY_WITH_APPLICATION_IDENTIFIER = "urn:net:lin-k:safe-online";

    /**
     * Validate the certificate chain.
     *
     * @param certificateChain the  certificate chain.
     *
     * @throws WSClientTransportException   something went wrong sending the XKMS 2.0 Validation Request.
     * @throws ValidationFailedException    validation failed.
     * @throws CertificateEncodingException failed to encode a certificate in the chain.
     */
    void validate(X509Certificate... certificateChain)
            throws WSClientTransportException, ValidationFailedException, CertificateEncodingException;

    /**
     * Validate the certificate chain.
     *
     * @param certificateChain the certificate chain.
     *
     * @throws WSClientTransportException   something went wrong sending the XKMS 2.0 Validation Request.
     * @throws ValidationFailedException    validation failed.
     * @throws CertificateEncodingException failed to encode a certificate in the chain.
     */
    void validate(String useKeyWithApplication, String useKeyWithIdentifier, X509Certificate... certificateChain)
            throws WSClientTransportException, ValidationFailedException, CertificateEncodingException;
}
