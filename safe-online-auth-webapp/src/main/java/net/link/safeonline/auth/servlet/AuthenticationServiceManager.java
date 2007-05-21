/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.servlet;

import javax.ejb.NoSuchEJBException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.SubscriptionNotFoundException;
import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.util.ee.EjbUtils;

/**
 * This HTTP session listener manages the lifecycle of the authentication
 * service instance used by the authentication web application.
 * 
 * @author fcorneli
 * 
 */
public class AuthenticationServiceManager implements HttpSessionListener {

	public static final String AUTH_SERVICE_ATTRIBUTE = "authenticationService";

	private static final Log LOG = LogFactory
			.getLog(AuthenticationServiceManager.class);

	public void sessionCreated(HttpSessionEvent event) {
		/*
		 * When the HTTP session starts we assign it an authentication service
		 * instance.
		 */
		AuthenticationService authenticationService = EjbUtils.getEJB(
				"SafeOnline/AuthenticationServiceBean/local",
				AuthenticationService.class);

		HttpSession session = event.getSession();

		session.setAttribute(AUTH_SERVICE_ATTRIBUTE, authenticationService);
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession session = event.getSession();

		AuthenticationService authenticationService = (AuthenticationService) session
				.getAttribute(AUTH_SERVICE_ATTRIBUTE);
		if (null == authenticationService) {
			/*
			 * This is the normal thing to happen. This means that the
			 * authentication service was already properly terminated.
			 */
			return;
		}

		/*
		 * Make sure we do a proper cleanup of the authentication service
		 * instance. This can happen in the event of an unusual, well, event.
		 */
		try {
			LOG.debug("aborting authentication service instance");
			authenticationService.abort();
		} catch (NoSuchEJBException e) {
			/*
			 * This means that the authentication service instances did throw a
			 * system exception, which has put it in the non-existing state, or
			 * that someone already invoked a remove method on the bean and
			 * forgot to remove the bean reference from the HTTP session.
			 */
			LOG.warn("no such EJB exception received");
		}
	}

	/**
	 * Gives back the authentication service instance associated with the given
	 * HTTP session. Later on we could limit the usage of this method to certain
	 * states on the authentication service. It is clear that this method should
	 * not be used to finalize the authentication service via
	 * {@link AuthenticationService#commitAuthentication(String)} or
	 * {@link AuthenticationService#abort()}. These operations should be
	 * performed via this authentication service manager class.
	 * 
	 * @param session
	 * @return
	 */
	public static AuthenticationService getAuthenticationService(
			HttpSession session) {
		AuthenticationService authenticationService = (AuthenticationService) session
				.getAttribute(AUTH_SERVICE_ATTRIBUTE);
		if (null == authenticationService) {
			throw new IllegalStateException(
					"authentication service instance not present");
		}
		return authenticationService;
	}

	/**
	 * Commits the authentication for the given application.
	 * 
	 * @param session
	 * @param applicationId
	 * @throws SubscriptionNotFoundException
	 * @throws ApplicationNotFoundException
	 */
	public static void commitAuthentication(HttpSession session,
			String applicationId) throws SubscriptionNotFoundException,
			ApplicationNotFoundException {
		AuthenticationService authenticationService = getAuthenticationService(session);
		try {
			authenticationService.commitAuthentication(applicationId);
		} finally {
			/*
			 * No matter what happens, we don't want the sessionDestroyed method
			 * to call abort on our finished authentication service instance.
			 */
			session.removeAttribute(AUTH_SERVICE_ATTRIBUTE);
		}
	}

	/**
	 * Aborts the authentication process.
	 * 
	 * @param session
	 */
	public static void abort(HttpSession session) {
		AuthenticationService authenticationService = getAuthenticationService(session);
		try {
			authenticationService.abort();
		} finally {
			session.removeAttribute(AUTH_SERVICE_ATTRIBUTE);
		}
	}
}