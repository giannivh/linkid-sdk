/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.ws.xkms2;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import net.link.safeonline.sdk.logging.exception.ValidationFailedException;
import net.link.safeonline.sdk.logging.exception.WSClientTransportException;
import net.link.safeonline.sdk.ws.WSClient;


/**
 * Interface for XKMS 2.0 client.
 *
 * @author wvdhaute
 */
public interface Xkms2Client extends WSClient {

    /**
     * Validate the linkID certificate chain.
     *
     * @param certificateChain the linkID certificate chain.
     *
     * @throws WSClientTransportException   something went wrong sending the XKMS 2.0 Validation Request.
     * @throws ValidationFailedException    validation failed.
     * @throws CertificateEncodingException failed to encode a certificate in the chain.
     */
    void validate(List<X509Certificate> certificateChain)
            throws WSClientTransportException, ValidationFailedException, CertificateEncodingException;
}