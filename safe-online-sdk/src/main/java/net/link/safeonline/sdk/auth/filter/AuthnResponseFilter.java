/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.auth.filter;

import com.google.common.base.Function;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.link.safeonline.sdk.auth.protocol.AuthnProtocolResponseContext;
import net.link.safeonline.sdk.auth.protocol.ProtocolManager;
import net.link.safeonline.sdk.configuration.AuthenticationContext;
import net.link.util.error.ValidationFailedException;
import net.link.util.j2ee.AbstractInjectionFilter;
import net.link.util.servlet.ErrorMessage;
import net.link.util.servlet.ServletUtils;
import net.link.util.servlet.annotation.Init;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This filter performs the actual login using the identity as received from the SafeOnline authentication web application.
 *
 * @author fcorneli
 */
public class AuthnResponseFilter extends AbstractInjectionFilter {

    private static final Log LOG = LogFactory.getLog( AuthnResponseFilter.class );

    public static final String ERROR_PAGE = "ErrorPage";

    @Init(name = ERROR_PAGE, optional = true)
    private String errorPage;

    public void destroy() {

        LOG.debug( "destroy" );
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        LOG.debug( "doFilter: " + httpRequest.getRequestURL() );

        try {
            AuthnProtocolResponseContext authnResponse = ProtocolManager.findAndValidateAuthnResponse( httpRequest );
            if (null == authnResponse)
                authnResponse = ProtocolManager.findAndValidateAuthnAssertion( httpRequest, getContextFunction() );
            if (null != authnResponse)
                onLogin( httpRequest.getSession(), authnResponse );
        } catch (ValidationFailedException e) {
            LOG.error( ServletUtils.redirectToErrorPage( httpRequest, httpResponse, errorPage, null, new ErrorMessage( e ) ) );
        } catch (RuntimeException e) {
            LOG.error( "Internal error", e );
        }

        chain.doFilter( request, response );
    }

    /**
     * Override this method if you want to create a custom context for detached authentication responses.
     *
     * The standard implementation uses {@link AuthenticationContext#AuthenticationContext()}.
     *
     * @return A function that provides the context for validating detached authentication responses (assertions).
     */
    protected Function<AuthnProtocolResponseContext, AuthenticationContext> getContextFunction() {

        return new Function<AuthnProtocolResponseContext, AuthenticationContext>() {
            public AuthenticationContext apply(final AuthnProtocolResponseContext from) {

                return new AuthenticationContext();
            }
        };
    }

    /**
     * Invoked when an authentication response is received.  The default implementation sets the user's credentials on the session if the
     * response was successful and does nothing if it wasn't.
     *
     * @param session       The HTTP session within which the response was received.
     * @param authnResponse The response that was received.
     */
    protected void onLogin(HttpSession session, AuthnProtocolResponseContext authnResponse) {

        if (authnResponse.isSuccess()) {
            LOG.debug( "username: " + authnResponse.getUserId() );
            LoginManager.set( session, authnResponse.getUserId(), authnResponse.getAttributes(), authnResponse.getAuthenticatedDevices(), authnResponse.getCertificateChain() );
        }
    }
}
