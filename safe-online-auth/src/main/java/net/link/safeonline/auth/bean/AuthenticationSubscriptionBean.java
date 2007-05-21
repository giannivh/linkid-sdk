/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.bean;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.auth.AuthenticationConstants;
import net.link.safeonline.auth.AuthenticationSubscription;
import net.link.safeonline.auth.AuthenticationUtils;
import net.link.safeonline.authentication.exception.AlreadySubscribedException;
import net.link.safeonline.authentication.exception.ApplicationIdentityNotFoundException;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.IdentityConfirmationRequiredException;
import net.link.safeonline.authentication.exception.MissingAttributeException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubscriptionNotFoundException;
import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.authentication.service.IdentityService;
import net.link.safeonline.authentication.service.SubscriptionService;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.log.Log;

@Stateless
@Name("authSubscription")
@LocalBinding(jndiBinding = AuthenticationConstants.JNDI_PREFIX
		+ "AuthenticationSubscriptionBean/local")
@SecurityDomain(AuthenticationConstants.SECURITY_DOMAIN)
public class AuthenticationSubscriptionBean implements
		AuthenticationSubscription {

	@Logger
	private Log log;

	@In(value = "applicationId", required = true)
	private String applicationId;

	@EJB
	private SubscriptionService subscriptionService;

	@In(create = true)
	FacesMessages facesMessages;

	@EJB
	private IdentityService identityService;

	@In(required = true)
	private String target;

	@In(required = true)
	private String username;

	@RolesAllowed(AuthenticationConstants.USER_ROLE)
	public String subscribe() {
		log.debug("subscribe to application #0", this.applicationId);
		try {
			this.subscriptionService.subscribe(this.applicationId);
		} catch (ApplicationNotFoundException e) {
			this.facesMessages.add("application not found");
			return null;
		} catch (AlreadySubscribedException e) {
			this.facesMessages.add("already subscribed");
			return null;
		} catch (PermissionDeniedException e) {
			this.facesMessages.add("permission denied");
			return null;
		}

		/*
		 * After successful subscription we continue the workflow as usual.
		 */

		boolean confirmationRequired;
		try {
			confirmationRequired = this.identityService
					.isConfirmationRequired(applicationId);
		} catch (SubscriptionNotFoundException e) {
			this.facesMessages.add("subscription not found");
			return null;
		} catch (ApplicationNotFoundException e) {
			this.facesMessages.add("application not found");
			return null;
		} catch (ApplicationIdentityNotFoundException e) {
			this.facesMessages.add("application identity not found");
			return null;
		}
		log.debug("confirmation required: " + confirmationRequired);
		if (true == confirmationRequired) {
			return "confirmation-required";
		}

		boolean hasMissingAttributes;
		try {
			hasMissingAttributes = this.identityService
					.hasMissingAttributes(this.applicationId);
		} catch (ApplicationNotFoundException e) {
			String msg = "application not found.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		} catch (ApplicationIdentityNotFoundException e) {
			String msg = "application identity not found.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		}

		if (true == hasMissingAttributes) {
			return "missing-attributes";
		}

		try {
			commitAuthentication();
		} catch (SubscriptionNotFoundException e) {
			String msg = "subscription not found.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		} catch (ApplicationNotFoundException e) {
			String msg = "application not found.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		} catch (ApplicationIdentityNotFoundException e) {
			String msg = "application identity not found.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		} catch (IdentityConfirmationRequiredException e) {
			String msg = "identity confirmation required.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		} catch (MissingAttributeException e) {
			String msg = "missing attributes.";
			log.debug(msg);
			this.facesMessages.add(msg);
			return null;
		}

		AuthenticationUtils.redirectToApplication(this.target, this.username,
				this.facesMessages);

		return null;
	}

	@In
	private Context sessionContext;

	@In
	private AuthenticationService authenticationService;

	private void commitAuthentication() throws SubscriptionNotFoundException,
			ApplicationNotFoundException, ApplicationIdentityNotFoundException,
			IdentityConfirmationRequiredException, MissingAttributeException {
		try {
			this.authenticationService.commitAuthentication(this.applicationId);
		} finally {
			/*
			 * We have to remove the authentication service reference from the
			 * http session, else the authentication service manager will try to
			 * abort on it.
			 */
			cleanupAuthenticationServiceReference();
		}
	}

	public static final String AUTH_SERVICE_ATTRIBUTE = "authenticationService";

	private void cleanupAuthenticationServiceReference() {
		this.sessionContext.set(AUTH_SERVICE_ATTRIBUTE, null);
	}
}
