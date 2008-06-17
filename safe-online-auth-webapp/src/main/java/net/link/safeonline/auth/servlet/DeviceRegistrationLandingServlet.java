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

import net.link.safeonline.auth.LoginManager;
import net.link.safeonline.auth.protocol.AuthenticationServiceManager;
import net.link.safeonline.authentication.exception.DeviceMappingNotFoundException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.authentication.service.AuthenticationState;
import net.link.safeonline.entity.DeviceMappingEntity;
import net.link.safeonline.sdk.servlet.AbstractInjectionServlet;
import net.link.safeonline.sdk.servlet.ErrorMessage;
import net.link.safeonline.sdk.servlet.annotation.Init;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Device registration landing page.
 * 
 * This landing page handles the SAML response returned by the remote device
 * issuer to notify the success of the registration.
 * 
 * @author wvdhaute
 * 
 */
public class DeviceRegistrationLandingServlet extends AbstractInjectionServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(DeviceRegistrationLandingServlet.class);

	public static final String RESOURCE_BASE = "messages.webapp";

	public static final String DEVICE_ERROR_MESSAGE_ATTRIBUTE = "deviceErrorMessage";

	@Init(name = "DeviceErrorUrl")
	private String deviceErrorUrl;

	@Override
	protected void invokePost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doPost");

		AuthenticationService authenticationService = AuthenticationServiceManager
				.getAuthenticationService(request.getSession());
		DeviceMappingEntity deviceMapping;
		try {
			deviceMapping = authenticationService.register(request);
		} catch (NodeNotFoundException e) {
			redirectToErrorPage(request, response, this.deviceErrorUrl,
					RESOURCE_BASE, new ErrorMessage("deviceErrorMessage",
							"errorProtocolHandlerFinalization"));
			return;
		} catch (DeviceMappingNotFoundException e) {
			redirectToErrorPage(request, response, this.deviceErrorUrl,
					RESOURCE_BASE, new ErrorMessage("deviceErrorMessage",
							"errorDeviceRegistrationNotFound"));
			return;
		}
		if (null == deviceMapping) {
			/*
			 * Registration failed, redirect to register-device or
			 * new-user-device
			 */
			if (authenticationService.getAuthenticationState().equals(
					AuthenticationState.USER_AUTHENTICATED)) {
				response.sendRedirect("../register-device.seam");
			} else {
				response.sendRedirect("../new-user-device.seam");
			}

		} else {
			/*
			 * Registration ok, redirect to login servlet
			 */
			LoginManager.relogin(request.getSession(), deviceMapping
					.getDevice());
			response.sendRedirect("../login");
		}
	}
}
