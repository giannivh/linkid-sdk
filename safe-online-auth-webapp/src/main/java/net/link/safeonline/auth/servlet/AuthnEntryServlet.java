/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.auth.LoginManager;
import net.link.safeonline.auth.protocol.ProtocolContext;
import net.link.safeonline.auth.protocol.ProtocolException;
import net.link.safeonline.auth.protocol.ProtocolHandlerManager;
import net.link.safeonline.helpdesk.HelpdeskLogger;
import net.link.safeonline.sdk.auth.saml2.HttpServletRequestEndpointWrapper;
import net.link.safeonline.util.servlet.AbstractInjectionServlet;
import net.link.safeonline.util.servlet.ErrorMessage;
import net.link.safeonline.util.servlet.annotation.Init;

/**
 * Generic entry point for the authentication web application. This servlet will
 * try to find out which authentication protocol is being used by the client web
 * browser to initiate an authentication procedure. We manage the authentication
 * entry via a bare-bone servlet since we:
 * <ul>
 * <li>need to be able to do some low-level GET or POST parameter parsing and
 * processing.</li>
 * <li>we want the entry point to be UI technology independent.</li>
 * </ul>
 * 
 * <p>
 * The following servlet init parameters are required:
 * </p>
 * <ul>
 * <li><code>StartUrl</code>: points to the relative/absolute URL to which
 * this servlet will redirect after successful authentication protocol entry.</li>
 * <li><code>FirstTimeUrl</code>: points to the relative/absolute URL to
 * which this servlet will redirect after first visit and successful
 * authentication protocol entry.</li>
 * <li><code>UnsupportedProtocolUrl</code>: will be used to redirect to when
 * an unsupported authentication protocol is encountered.</li>
 * <li><code>ProtocolErrorUrl</code>: will be used to redirect to when an
 * authentication protocol error is encountered.</li>
 * </ul>
 * 
 * @author fcorneli
 * 
 */
public class AuthnEntryServlet extends AbstractInjectionServlet {

	private static final long serialVersionUID = 1L;

	public static final String AUTH_LANGUAGE_COOKIE = "OLAS.auth.language";

	public static final String PROTOCOL_ERROR_MESSAGE_ATTRIBUTE = "protocolErrorMessage";

	public static final String PROTOCOL_NAME_ATTRIBUTE = "protocolName";

	@Init(name = "StartUrl")
	private String startUrl;

	@Init(name = "FirstTimeUrl")
	private String firstTimeUrl;

	@Init(name = "ServletEndpointUrl")
	private String servletEndpointUrl;

	@Init(name = "UnsupportedProtocolUrl")
	private String unsupportedProtocolUrl;

	@Init(name = "ProtocolErrorUrl")
	private String protocolErrorUrl;

	@Override
	protected void invokeGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleLanding(request, response);
	}

	@Override
	protected void invokePost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleLanding(request, response);
	}

	private void handleLanding(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		/**
		 * Wrap the request to use the servlet endpoint url. To prevent failure
		 * when behind a reverse proxy or loadbalancer when opensaml is checking
		 * the destination field.
		 */
		HttpServletRequestEndpointWrapper authnRequestWrapper = new HttpServletRequestEndpointWrapper(
				request, this.servletEndpointUrl);

		ProtocolContext protocolContext;
		try {
			protocolContext = ProtocolHandlerManager
					.handleRequest(authnRequestWrapper);
		} catch (ProtocolException e) {
			redirectToErrorPage(request, response, this.protocolErrorUrl, null,
					new ErrorMessage(PROTOCOL_NAME_ATTRIBUTE, e
							.getProtocolName()), new ErrorMessage(
							PROTOCOL_ERROR_MESSAGE_ATTRIBUTE, e.getMessage()));
			return;
		}

		if (null == protocolContext) {
			response.sendRedirect(this.unsupportedProtocolUrl);
			return;
		}

		/*
		 * Set the locale if language was specified in the browser post
		 */
		if (null != protocolContext.getLanguage()) {
			Cookie authLanguageCookie = new Cookie(AUTH_LANGUAGE_COOKIE,
					protocolContext.getLanguage());
			authLanguageCookie.setPath("/olas-auth/");
			authLanguageCookie.setMaxAge(60 * 60 * 24 * 30 * 6);
			response.addCookie(authLanguageCookie);
		}

		/*
		 * We save the result of the protocol handler into the HTTP session.
		 */
		HttpSession session = request.getSession();
		LoginManager
				.setApplication(session, protocolContext.getApplicationId());
		LoginManager.setApplicationFriendlyName(session, protocolContext
				.getApplicationFriendlyName());
		LoginManager.setTarget(session, protocolContext.getTarget());
		LoginManager.setRequiredDevices(session, protocolContext
				.getRequiredDevices());

		/*
		 * create new helpdesk volatile context
		 */
		HelpdeskLogger.clear(session);

		if (isFirstTime(request, response)) {
			response.sendRedirect(this.firstTimeUrl);
		} else {
			response.sendRedirect(this.startUrl);
		}
	}

	private boolean isFirstTime(HttpServletRequest request,
			HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (null == cookies) {
			setDefloweredCookie(response);
			return true;
		}
		Cookie defloweredCookie = findDefloweredCookie(cookies);
		if (null == defloweredCookie) {
			setDefloweredCookie(response);
			return true;
		}
		return false;
	}

	private final static String DEFLOWER_COOKIE_NAME = "deflowered";

	private Cookie findDefloweredCookie(Cookie[] cookies) {
		for (Cookie cookie : cookies) {
			if (DEFLOWER_COOKIE_NAME.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	private void setDefloweredCookie(HttpServletResponse response) {
		Cookie defloweredCookie = new Cookie(DEFLOWER_COOKIE_NAME, "true");
		defloweredCookie.setMaxAge(60 * 60 * 24 * 30 * 6);
		response.addCookie(defloweredCookie);
	}
}
