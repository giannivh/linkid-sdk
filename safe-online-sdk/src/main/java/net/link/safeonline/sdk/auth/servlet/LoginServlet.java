/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.auth.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.link.safeonline.sdk.auth.AuthenticationProtocolHandler;
import net.link.safeonline.sdk.auth.AuthenticationProtocolManager;
import net.link.safeonline.sdk.auth.filter.LoginManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Login Servlet. This servlet contains the landing page to finalize the
 * authentication process initiated by the web application.
 * 
 * @author fcorneli
 * 
 */
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(LoginServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleLanding(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleLanding(request, response);
	}

	private void handleLanding(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		AuthenticationProtocolHandler protocolHandler = AuthenticationProtocolManager
				.findAuthenticationProtocolHandler(request);
		if (null == protocolHandler) {
			/*
			 * The landing page can only be used for finalizing an ongoing
			 * authentication process. If no protocol handler is active then
			 * something must be going wrong here.
			 */
			String msg = "no protocol handler active";
			LOG.error(msg);
			writeErrorPage(msg, response);
			return;
		}

		String username = protocolHandler.finalizeAuthentication(request,
				response);
		if (null == username) {
			String msg = "protocol handler could not finalize";
			LOG.error(msg);
			writeErrorPage(msg, response);
			return;
		}

		LOG.debug("username: " + username);
		LoginManager.setUsername(username, request);
		AuthenticationProtocolManager.cleanupAuthenticationHandler(request);
		String target = AuthenticationProtocolManager.getTarget(request);
		LOG.debug("target: " + target);
		response.sendRedirect(target);
	}

	private void writeErrorPage(String message, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		PrintWriter out = response.getWriter();
		out.println("<html>");
		{
			out.println("<head><title>Error</title></head>");
			out.println("<body>");
			{
				out.println("<h1>Error</h1>");
				out.println("<p>");
				{
					out.println(message);
				}
				out.println("</p>");
			}
			out.println("</body>");
		}
		out.println("</html>");
		out.close();
	}
}