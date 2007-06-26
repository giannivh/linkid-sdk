/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.payment.service;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

/**
 * Lawyer JAAS login module. This login module will retrieve the username and
 * role attribute from the HTTP servlet request using JACC. It uses these
 * attribute values to populate the subject for usage within the JBoss
 * Application Server.
 * 
 * @author fcorneli
 * 
 */
public class PaymentLoginModule implements LoginModule {

	private static final Log LOG = LogFactory.getLog(PaymentLoginModule.class);

	private Subject subject;

	private CallbackHandler callbackHandler;

	private Principal authenticatedPrincipal;

	private String role;

	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		LOG.debug("initialize");
		this.subject = subject;
		this.callbackHandler = callbackHandler;
	}

	public boolean login() throws LoginException {

		HttpServletRequest httpServletRequest;
		try {
			httpServletRequest = (HttpServletRequest) PolicyContext
					.getContext("javax.servlet.http.HttpServletRequest");
		} catch (PolicyContextException e) {
			throw new LoginException("JACC policy context error: "
					+ e.getMessage());
		}

		HttpSession httpSession = httpServletRequest.getSession();
		String sessionUsername = (String) httpSession.getAttribute("username");
		LOG.debug("jacc http username: " + sessionUsername);

		NameCallback nameCallback = new NameCallback("username");
		Callback[] callbacks = new Callback[] { nameCallback };

		try {
			this.callbackHandler.handle(callbacks);
		} catch (IOException e) {
			throw new LoginException("IO error: " + e.getMessage());
		} catch (UnsupportedCallbackException e) {
			throw new LoginException("Unsupported callback: " + e.getMessage());
		}

		String jaasUsername = nameCallback.getName();
		LOG.debug("jaas username: " + jaasUsername);

		if (false == jaasUsername.equals(sessionUsername)) {
			throw new LoginException(
					"JAAS login username should equal session username");
		}

		// authentication
		this.authenticatedPrincipal = new SimplePrincipal(sessionUsername);

		// authorization
		String role = (String) httpSession.getAttribute("role");
		this.role = role;

		return true;
	}

	public boolean commit() throws LoginException {
		Set<Principal> principals = this.subject.getPrincipals();
		principals.add(this.authenticatedPrincipal);
		setRole(principals, this.role);
		return true;
	}

	private void setRole(Set<Principal> principals, String role) {
		if (null == role) {
			return;
		}
		Group rolesGroup = getGroup("Roles", principals);
		Principal rolePrincipal = new SimplePrincipal(role);
		rolesGroup.addMember(rolePrincipal);
	}

	private Group getGroup(String groupName, Set<Principal> principals) {
		for (Principal principal : principals) {
			if (false == principal instanceof Group) {
				continue;
			}
			Group group = (Group) principal;
			if (groupName.equals(group.getName())) {
				return group;
			}
		}
		/*
		 * If the group did not yet exist, create it and add it to the subject
		 * principals.
		 */
		Group group = new SimpleGroup(groupName);
		principals.add(group);
		return group;
	}

	public boolean abort() throws LoginException {
		this.authenticatedPrincipal = null;
		this.role = null;
		return true;
	}

	public boolean logout() throws LoginException {
		this.subject.getPrincipals().clear();
		this.subject.getPublicCredentials().clear();
		this.subject.getPrivateCredentials().clear();
		return true;
	}
}