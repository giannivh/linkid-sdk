/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.auth.servlet;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.link.safeonline.auth.LoginManager;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.device.sdk.ErrorPage;
import net.link.safeonline.device.sdk.RegistrationContext;
import net.link.safeonline.device.sdk.exception.RegistrationFinalizationException;
import net.link.safeonline.device.sdk.exception.RegistrationInitializationException;
import net.link.safeonline.device.sdk.reg.saml2.Saml2Handler;
import net.link.safeonline.entity.RegisteredDeviceEntity;
import net.link.safeonline.service.RegisteredDeviceService;
import net.link.safeonline.util.ee.EjbUtils;
import net.link.safeonline.util.ee.IdentityServiceClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Device registration landing page.
 * 
 * This landing page handles the SAML requests sent out by an external device
 * provider, and sends back a response containing the UUID for the registering
 * subject.
 * 
 * @author wvdhaute
 * 
 */
public class DeviceRegistrationLandingServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(DeviceRegistrationLandingServlet.class);

	private Map<String, String> configParams;

	private RegisteredDeviceService registeredDeviceService;

	private SamlAuthorityService samlAuthorityService;

	@SuppressWarnings("unchecked")
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		loadDependencies();
		this.configParams = new HashMap<String, String>();
		Enumeration<String> initParamsEnum = config.getServletContext()
				.getInitParameterNames();
		while (initParamsEnum.hasMoreElements()) {
			String paramName = initParamsEnum.nextElement();
			String paramValue = getInitParameter(config, paramName);
			this.configParams.put(paramName, paramValue);
		}
	}

	private void loadDependencies() {
		this.registeredDeviceService = EjbUtils.getEJB(
				"SafeOnline/RegisteredDeviceServiceBean/local",
				RegisteredDeviceService.class);
		this.samlAuthorityService = EjbUtils.getEJB(
				"SafeOnline/SamlAuthorityServiceBean/local",
				SamlAuthorityService.class);
	}

	private String getInitParameter(ServletConfig config, String initParamName)
			throws UnavailableException {
		String initParamValue = config.getServletContext().getInitParameter(
				initParamName);
		if (null == initParamValue)
			throw new UnavailableException("missing init parameter: "
					+ initParamName);
		return initParamValue;
	}

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
			HttpServletResponse response) throws IOException {
		Saml2Handler handler = Saml2Handler.getSaml2Handler(request);
		IdentityServiceClient identityServiceClient = new IdentityServiceClient();
		KeyPair keyPair = new KeyPair(identityServiceClient.getPublicKey(),
				identityServiceClient.getPrivateKey());

		handler.init(this.configParams, identityServiceClient.getCertificate(),
				keyPair);

		try {
			handler.initRegistration(request);
		} catch (RegistrationInitializationException e) {
			ErrorPage.errorPage(e.getMessage(), response);
			return;
		}

		RegistrationContext registrationContext = RegistrationContext
				.getRegistrationContext(request.getSession());
		String deviceName = registrationContext.getRegisteredDevice();
		String userName = LoginManager.getUsername(request.getSession());
		try {
			LOG.debug("register device " + deviceName + " for " + userName);
			RegisteredDeviceEntity registeredDevice = this.registeredDeviceService
					.registerDevice(userName, deviceName);
			LOG.debug("registered device id: " + registeredDevice.getId());
			registrationContext.setUserId(registeredDevice.getId());
			registrationContext.setValidity(this.samlAuthorityService
					.getAuthnAssertionValidity());
		} catch (SubjectNotFoundException e) {
			ErrorPage.errorPage(e.getMessage(), response);
			return;
		} catch (DeviceNotFoundException e) {
			ErrorPage.errorPage(e.getMessage(), response);
			return;
		}

		try {
			handler.finalizeRegistration(request, response);
		} catch (RegistrationFinalizationException e) {
			ErrorPage.errorPage(e.getMessage(), response);
			return;
		}
	}
}