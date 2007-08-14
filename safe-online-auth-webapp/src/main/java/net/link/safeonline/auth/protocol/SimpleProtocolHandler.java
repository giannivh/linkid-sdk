/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.protocol;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Server-side authentication protocol handler for the simple authentication
 * protocol.
 * 
 * @author fcorneli
 * 
 */
public class SimpleProtocolHandler implements ProtocolHandler {

	private static final Log LOG = LogFactory
			.getLog(SimpleProtocolHandler.class);

	public static final String NAME = "Simple Authentication Protocol";

	public String handleRequest(HttpServletRequest authnRequest)
			throws ProtocolException {
		if (false == "GET".equals(authnRequest.getMethod())) {
			return null;
		}
		String applicationId = authnRequest.getParameter("application");
		if (null == applicationId) {
			return null;
		}
		LOG.debug("application: " + applicationId);
		/*
		 * From this moment on we're sure that the user is really trying to use
		 * the simple authentication protocol.
		 */
		String target = authnRequest.getParameter("target");
		if (null == target) {
			/*
			 * The simple authentication protocol really requires the "target"
			 * request parameter.
			 */
			throw new ProtocolException("target request parameter not found");
		}
		HttpSession session = authnRequest.getSession();
		LOG.debug("setting target: " + target);
		session.setAttribute("target", target);
		return applicationId;
	}

	public String getName() {
		return NAME;
	}
}