/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.ExistingUserException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.ProxyAttributeService;
import net.link.safeonline.authentication.service.UserRegistrationService;
import net.link.safeonline.authentication.service.UserRegistrationServiceRemote;
import net.link.safeonline.device.PasswordDeviceService;
import net.link.safeonline.entity.DeviceRegistrationEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.model.UserRegistrationManager;
import net.link.safeonline.service.DeviceRegistrationService;
import net.link.safeonline.service.SubjectService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of user registration service interface. This component does
 * not live within the SafeOnline core security domain. This because a user that
 * is about to register himself is not yet logged on into the system.
 * 
 * @author fcorneli
 * 
 */
@Stateless
public class UserRegistrationServiceBean implements UserRegistrationService,
		UserRegistrationServiceRemote {

	private static final Log LOG = LogFactory
			.getLog(UserRegistrationServiceBean.class);

	@EJB
	private SubjectService subjectService;

	@EJB
	private UserRegistrationManager userRegistrationManager;

	@EJB
	private DeviceRegistrationService deviceRegistrationService;

	@EJB
	private ProxyAttributeService proxyAttributeService;

	@EJB
	private PasswordDeviceService passwordDeviceService;

	public void registerUser(String login, String password)
			throws ExistingUserException, AttributeTypeNotFoundException,
			SubjectNotFoundException, DeviceNotFoundException {
		LOG.debug("register user: " + login);
		this.userRegistrationManager.registerUser(login);
		this.passwordDeviceService.register(login, password);
	}

	public SubjectEntity checkLogin(String login) throws ExistingUserException,
			AttributeTypeNotFoundException, SubjectNotFoundException,
			PermissionDeniedException {
		SubjectEntity subject = this.subjectService
				.findSubjectFromUserName(login);
		if (null == subject)
			return this.userRegistrationManager.registerUser(login);

		// Subject already exists, check for attached registered devices
		List<DeviceRegistrationEntity> deviceRegistrations = this.deviceRegistrationService
				.listDeviceRegistrations(subject);
		if (deviceRegistrations.isEmpty())
			return subject;

		// For each registered device, poll device issuer if registration
		// actually completed
		for (DeviceRegistrationEntity deviceRegistration : deviceRegistrations) {
			Object deviceAttribute = this.proxyAttributeService
					.getAttributeValue(subject.getUserId(), deviceRegistration
							.getDevice().getAttributeType().getName());
			if (null != deviceAttribute)
				return null;
		}
		return subject;
	}
}
