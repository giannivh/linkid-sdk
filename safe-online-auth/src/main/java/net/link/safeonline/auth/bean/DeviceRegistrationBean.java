/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.bean;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.application.FacesMessage;

import net.link.safeonline.auth.AuthenticationConstants;
import net.link.safeonline.auth.DeviceRegistration;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.service.AuthenticationDevice;
import net.link.safeonline.authentication.service.CredentialService;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

@Stateful
@Name("deviceRegistration")
@LocalBinding(jndiBinding = AuthenticationConstants.JNDI_PREFIX
		+ "DeviceRegistrationBean/local")
@SecurityDomain(AuthenticationConstants.SECURITY_DOMAIN)
public class DeviceRegistrationBean extends AbstractLoginBean implements
		DeviceRegistration {

	@Logger
	private Log log;

	private String device;

	private String password;

	@EJB
	private CredentialService credentialService;

	@Remove
	@Destroy
	public void destroyCallback() {
		this.log.debug("destroy");
	}

	@RolesAllowed(AuthenticationConstants.USER_ROLE)
	public String deviceNext() {
		this.log.debug("deviceNext: " + this.device);
		return this.device;
	}

	public String getDevice() {
		return this.device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public String getPassword() {
		return this.password;
	}

	@RolesAllowed(AuthenticationConstants.USER_ROLE)
	public String passwordNext() {
		this.log.debug("passwordNext");
		try {
			this.credentialService.setPassword(this.password);
		} catch (PermissionDeniedException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorPermissionDenied");
			return null;
		}
		super.relogin(AuthenticationDevice.PASSWORD);
		return null;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@RolesAllowed(AuthenticationConstants.USER_ROLE)
	public String getUsername() {
		return this.subjectService.getSubjectLogin(this.username);
	}

}
