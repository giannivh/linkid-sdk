/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.annotation.Init;
import net.link.safeonline.auth.protocol.ProtocolException;
import net.link.safeonline.auth.protocol.ProtocolHandlerManager;
import net.link.safeonline.authentication.exception.SafeOnlineException;
import net.link.safeonline.servlet.AbstractInjectionServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic exit point for the authentication web application.
 * 
 * <p>
 * This servlet has two tasks:
 * </p>
 * <ul>
 * <li>Committing the authentication process via the authentication service.</li>
 * <li>Make sure the correct protocol handler is activated to handle the
 * application response.</li>
 * </ul>
 * 
 * <p>
 * It's crucial to keep the authentication commit together with the response
 * generation as an atomic unit of work.
 * </p>
 * 
 * <p>
 * Servlet init parameters:
 * <ul>
 * <li><code>ProtocolErrorUrl</code>: the URL of the page to display in case
 * a protocol error took place.</li>
 * </ul>
 * </p>
 * 
 * @author fcorneli
 * 
 */
public class ExitServlet extends AbstractInjectionServlet {

	private static final Log LOG = LogFactory.getLog(ExitServlet.class);

	private static final long serialVersionUID = 1L;

	public static final String PROTOCOL_ERROR_MESSAGE_ATTRIBUTE = "protocolErrorMessage";

	public static final String PROTOCOL_NAME_ATTRIBUTE = "protocolName";

	@Init(name = "ProtocolErrorUrl")
	private String protocolErrorUrl;

	@Override
	protected void invokeGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("handleInvocation");
		HttpSession session = request.getSession();

		String application = (String) session.getAttribute("applicationId");
		if (null == application) {
			throw new ServletException(
					"no applicationId session attribute found");
		}

		try {
			AuthenticationServiceManager.commitAuthentication(session,
					application);
		} catch (SafeOnlineException e) {
			throw new ServletException(
					"error committing the authentication process");
		}

		try {
			ProtocolHandlerManager.authnResponse(session, response);
		} catch (ProtocolException e) {
			LOG.debug("protocol error: " + e.getMessage());
			String protocolName = e.getProtocolName();
			session.setAttribute(PROTOCOL_NAME_ATTRIBUTE, protocolName);
			String protocolErrorMessage = e.getMessage();
			session.setAttribute(PROTOCOL_ERROR_MESSAGE_ATTRIBUTE,
					protocolErrorMessage);
			response.sendRedirect(this.protocolErrorUrl);
		}
	}
}
