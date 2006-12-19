/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.ctrl.bean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;

import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.ctrl.LoginBase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.FacesMessages;

public class LoginBaseBean implements LoginBase {

	private static final Log LOG = LogFactory.getLog(LoginBaseBean.class);

	@In
	Context sessionContext;

	private String username;

	private String password;

	@EJB
	private AuthenticationService authenticationService;

	@In(create = true)
	FacesMessages facesMessages;

	private String applicationName;

	public LoginBaseBean(String applicationName) {
		LOG.debug("constructor: " + this);
		this.applicationName = applicationName;
	}

	@PostConstruct
	public void postConstructCallback() {
		LOG.debug("post construct: " + this);
	}

	@PreDestroy
	public void preDestroyCallback() {
		LOG.debug("pre destroy: " + this);
	}

	@PostActivate
	public void postActivateCallback() {
		LOG.debug("post activate: " + this);
	}

	@PrePassivate
	public void prePassivateCallback() {
		LOG.debug("pre passivate: " + this);
	}

	public String getPassword() {
		LOG.debug("get password");
		return "";
	}

	public String getUsername() {
		LOG.debug("get username");
		return this.username;
	}

	public String login() {
		LOG.debug("login with username: " + this.username + " into "
				+ this.applicationName);
		boolean authenticated = this.authenticationService.authenticate(
				this.applicationName, this.username, new String(this.password));
		if (!authenticated) {
			this.facesMessages.add("login failed");
			Seam.invalidateSession();
			return null;
		}

		this.sessionContext.set("username", this.username);
		this.sessionContext.set("password", this.password);

		return "login-success";
	}

	public void setPassword(String password) {
		LOG.debug("set password");
		this.password = password;
	}

	public void setUsername(String username) {
		LOG.debug("set username");
		this.username = username;
	}

	public String logout() {
		LOG.debug("logout");
		this.sessionContext.set("username", null);
		this.sessionContext.set("password", null);
		Seam.invalidateSession();
		return "logout-success";
	}

	public String getLoggedInUsername() {
		LOG.debug("get logged in username");
		String username = (String) this.sessionContext.get("username");
		return username;
	}

	public boolean isLoggedIn() {
		LOG.debug("is logged in");
		String username = (String) this.sessionContext.get("username");
		return (null != username);
	}

	@Remove
	@Destroy
	public void destroyCallback() {
		LOG.debug("destroy: " + this);
		this.username = null;
		this.password = null;
	}
}
