/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.auth.LoginManager;
import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.service.AuthenticationDevice;
import net.link.safeonline.authentication.service.CredentialService;
import net.link.safeonline.pkix.exception.TrustDomainNotFoundException;
import net.link.safeonline.shared.SharedConstants;
import net.link.safeonline.util.ee.EjbUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The identity servlet implementation. This servlet receives its data from the
 * BeID via the IdentityApplet.
 * 
 * @author fcorneli
 * 
 */
public class IdentityServlet extends AbstractStatementServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(IdentityServlet.class);

	private CredentialService credentialService;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		loadCredentialService();
	}

	private void loadCredentialService() {
		this.credentialService = EjbUtils.getEJB(
				"SafeOnline/CredentialServiceBean/local",
				CredentialService.class);
	}

	@Override
	protected void processStatement(byte[] statementData, HttpSession session,
			HttpServletResponse response) throws ServletException, IOException {
		String username = LoginManager.getUsername(session);
		LOG.debug("processing statement for: " + username);

		PrintWriter writer = response.getWriter();
		try {
			this.credentialService.mergeIdentityStatement(statementData);
			LoginManager.relogin(session, AuthenticationDevice.BEID);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (TrustDomainNotFoundException e) {
			LOG.error("trust domain not found: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e
					.getErrorCode());
			writer.println("trust domain not found");
		} catch (PermissionDeniedException e) {
			LOG.error("permission denied: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e
					.getErrorCode());
			writer.println("permission denied");
		} catch (ArgumentIntegrityException e) {
			LOG.error("integrity error: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e
					.getErrorCode());
			writer.println("integrity check failed");
		} catch (Exception e) {
			LOG.error("credential service error: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writer.println("internal error");
		}
	}
}
