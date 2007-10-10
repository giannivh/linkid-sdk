/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.bean;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import net.link.safeonline.auth.AccountRegistration;
import net.link.safeonline.auth.AuthenticationConstants;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.ExistingUserException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.AuthenticationDevice;
import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.authentication.service.UserRegistrationService;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.log.Log;

import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.ImageCaptchaService;

@Stateful
@Name("accountRegistration")
@LocalBinding(jndiBinding = AuthenticationConstants.JNDI_PREFIX
		+ "AccountRegistrationBean/local")
public class AccountRegistrationBean extends AbstractLoginBean implements
		AccountRegistration {

	@EJB
	private UserRegistrationService userRegistrationService;

	@In
	private AuthenticationService authenticationService;

	@Logger
	private Log log;

	private String login;

	private String device;

	private String password;

	private String captcha;

	@SuppressWarnings("unused")
	@In(value = AccountRegistration.REQUESTED_USERNAME_ATTRIBUTE, required = false, scope = ScopeType.SESSION)
	@Out(value = AccountRegistration.REQUESTED_USERNAME_ATTRIBUTE, required = false, scope = ScopeType.SESSION)
	private String requestedUsername;

	@Remove
	@Destroy
	public void destroyCallback() {
		this.log.debug("destroy");
	}

	@Create
	@Begin
	public void begin() {
		this.log.debug("begin");
	}

	public String getLogin() {
		return this.login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String loginNext() {
		this.log.debug("loginNext");

		this.log.debug("captcha: " + this.captcha);

		if (null == this.captchaService) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorNoCaptcha");
			return null;
		}

		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		HttpSession httpSession = (HttpSession) externalContext
				.getSession(false);
		String captchaId = httpSession.getId();
		this.log.debug("captcha Id: " + captchaId);

		boolean valid;
		try {
			valid = this.captchaService.validateResponseForID(captchaId,
					this.captcha);
		} catch (CaptchaServiceException e) {
			/*
			 * It's possible that a data race occurs between the Captcha servlet
			 * and this validation call. In that case we just ask the user to
			 * try again.
			 */
			this.facesMessages.addToControlFromResourceBundle("captcha",
					FacesMessage.SEVERITY_ERROR, "errorNoCaptchaValidation");
			return null;
		}
		if (false == valid) {
			this.facesMessages.addToControlFromResourceBundle("captcha",
					FacesMessage.SEVERITY_ERROR, "errorInvalidCaptcha");
			return null;
		}

		boolean loginFree = this.userRegistrationService
				.isLoginFree(this.login);
		if (false == loginFree) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorLoginTaken");
			return null;
		}

		/*
		 * The requestedUsername session attribute can be used during the device
		 * specific registration process. For example, the registration
		 * statement is using it.
		 */
		this.requestedUsername = this.login;

		return "next";
	}

	public String deviceNext() {
		this.log.debug("deviceNext");
		if (null == this.device) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorDeviceSelection");
			return null;
		}
		this.log.debug("device: " + this.device);
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

	@In(required = false, value = "CaptchaService", scope = ScopeType.SESSION)
	ImageCaptchaService captchaService;

	public String passwordNext() {
		this.log.debug("passwordNext");

		super.clearUsername();

		try {
			this.userRegistrationService
					.registerUser(this.login, this.password);
		} catch (ExistingUserException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorLoginTaken");
			return null;
		} catch (AttributeTypeNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorAttributeTypeNotFound");
			return null;
		}

		try {
			boolean authenticated = this.authenticationService.authenticate(
					this.login, this.password);
			if (false == authenticated) {
				this.facesMessages.addFromResourceBundle(
						FacesMessage.SEVERITY_ERROR, "authenticationFailedMsg");
				return null;
			}
		} catch (SubjectNotFoundException e) {
			this.facesMessages.addToControlFromResourceBundle("username",
					FacesMessage.SEVERITY_ERROR, "subjectNotFoundMsg");
			return null;
		} catch (DeviceNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorPasswordNotFound");
			return null;
		}

		super.login(this.login, AuthenticationDevice.PASSWORD);
		return null;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCaptcha() {
		return this.captcha;
	}

	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}
}
