/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity.listener;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Set;

import javax.ejb.EJBException;
import javax.persistence.PreUpdate;
import javax.security.auth.Subject;

import net.link.safeonline.common.SafeOnlineRoles;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationOwnerEntity;
import net.link.safeonline.entity.SubjectEntity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimplePrincipal;

/**
 * Implementation of application ownership security constraint.
 * 
 * This implementation is very dependent on the way the JBoss Application Server
 * propagates the user's credentials.
 * 
 * In JBoss 5 they've reworked the SecurityAssociation class as a stack of
 * SecurityContext(s). See also:
 * http://wiki.jboss.org/wiki/Wiki.jsp?page=SecurityContextReplaceSecurityAssociation
 * 
 * @author fcorneli
 * 
 */
public class SecurityApplicationEntityListener {

	private static final Log LOG = LogFactory
			.getLog(SecurityApplicationEntityListener.class);

	private boolean isCallerInRole(Subject subject, String role) {
		Set<Group> groups = subject.getPrincipals(Group.class);
		if (null == groups) {
			return false;
		}
		SimplePrincipal rolePrincipal = new SimplePrincipal(role);
		for (Group group : groups) {
			while (!"Roles".equals(group.getName())) {
				continue;
			}
			if (group.isMember(rolePrincipal)) {
				return true;
			}
		}
		return false;
	}

	@PreUpdate
	public void preUpdateCallback(ApplicationEntity application) {
		LOG.debug("pre update callback on application: "
				+ application.getName());

		Principal principal = SecurityAssociation.getPrincipal();
		Subject subject = SecurityAssociation.getSubject();
		if (null == subject) {
			String msg = "subject is null";
			LOG.error(msg);
			throw new EJBException(msg);
		}
		if (null == principal) {
			String msg = "principal is null";
			LOG.error(msg);
			throw new EJBException(msg);
		}

		boolean isOperator = isCallerInRole(subject,
				SafeOnlineRoles.OPERATOR_ROLE);
		if (isOperator) {
			LOG.debug("operator ok");
			return;
		}

		boolean isOwner = isCallerInRole(subject, SafeOnlineRoles.OWNER_ROLE);
		if (!isOwner) {
			String msg = "caller has no owner role";
			LOG.error(msg);
			throw new EJBException(msg);
		}

		String login = principal.getName();
		ApplicationOwnerEntity applicationOwner = application
				.getApplicationOwner();
		SubjectEntity adminSubject = applicationOwner.getAdmin();
		if (login.equals(adminSubject.getLogin())) {
			return;
		}
		String msg = "only the application owner admin can change the application";
		LOG.error(msg);
		throw new EJBException(msg);
	}
}
