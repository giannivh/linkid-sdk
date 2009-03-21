/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.util.servlet;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * {@link HttpServletRequest} wrapper used to provide the correct endpoint URI when behind a proxy or load balancer.
 * 
 * @author lhunath
 * 
 */
public class HttpServletRequestEndpointWrapper extends HttpServletRequestWrapper {

    private static final Log LOG = LogFactory.getLog(HttpServletRequestEndpointWrapper.class);

    private URI              requestBaseUri;


    public HttpServletRequestEndpointWrapper(HttpServletRequest request, String responseBase) {

        super(request);

        try {
            URI responseBaseUri = new URI(responseBase);

            requestBaseUri = new URI(responseBaseUri.getScheme(), responseBaseUri.getAuthority(), responseBaseUri.getPath(), null, null);
        }

        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestURI() {

        // FIXME
        String originalRequest = super.getRequestURI();
        String[] requestURIElements = originalRequest.split("/");
        URI locationUri = URI.create(requestURIElements[requestURIElements.length - 1]);

        String rebasedRequestURI = requestBaseUri.resolve(locationUri).toASCIIString();
        LOG.debug("wrapper " + this + ": Rebased request URI '" + originalRequest + "' to: " + rebasedRequestURI);

        return rebasedRequestURI;
    }

    @Override
    public StringBuffer getRequestURL() {

        return new StringBuffer(getRequestURI());
    }

}
