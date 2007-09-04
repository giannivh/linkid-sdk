/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service.bean;

import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.authentication.exception.AlreadySubscribedException;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubscriptionNotFoundException;
import net.link.safeonline.authentication.service.SubscriptionService;
import net.link.safeonline.authentication.service.SubscriptionServiceRemote;
import net.link.safeonline.common.SafeOnlineRoles;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationOwnerEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.model.SubjectManager;
import net.link.safeonline.model.application.Application;
import net.link.safeonline.model.application.ApplicationContext;
import net.link.safeonline.model.application.ApplicationFactory;
import net.link.safeonline.model.subject.Subject;
import net.link.safeonline.model.subject.SubjectContext;
import net.link.safeonline.model.subject.SubjectFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.security.SecurityDomain;

@Stateless
@SecurityDomain(SafeOnlineConstants.SAFE_ONLINE_SECURITY_DOMAIN)
public class SubscriptionServiceBean implements SubscriptionService,
		SubscriptionServiceRemote, SubjectContext, ApplicationContext {

	private static final Log LOG = LogFactory
			.getLog(SubscriptionServiceBean.class);

	@EJB
	private SubjectManager subjectManager;

	@EJB
	private SubscriptionDAO subscriptionDAO;

	@EJB
	private ApplicationDAO applicationDAO;

	@Resource
	private SessionContext sessionContext;

	@RolesAllowed(SafeOnlineRoles.USER_ROLE)
	public List<SubscriptionEntity> listSubscriptions() {
		SubjectEntity subject = this.subjectManager.getCallerSubject();
		List<SubscriptionEntity> subscriptions = this.subscriptionDAO
				.listSubsciptions(subject);
		return subscriptions;
	}

	@RolesAllowed(SafeOnlineRoles.USER_ROLE)
	public void subscribe(String applicationName)
			throws ApplicationNotFoundException, AlreadySubscribedException,
			PermissionDeniedException {
		Subject subject = SubjectFactory.getCallerSubject(this);
		Application application = ApplicationFactory.getApplication(this,
				applicationName);
		subject.subscribe(application);
	}

	@RolesAllowed(SafeOnlineRoles.USER_ROLE)
	public void unsubscribe(String applicationName)
			throws ApplicationNotFoundException, SubscriptionNotFoundException,
			PermissionDeniedException {
		Subject subject = SubjectFactory.getCallerSubject(this);
		Application application = ApplicationFactory.getApplication(this,
				applicationName);
		subject.unsubscribe(application);
	}

	@RolesAllowed( { SafeOnlineRoles.OPERATOR_ROLE, SafeOnlineRoles.OWNER_ROLE })
	public long getNumberOfSubscriptions(String applicationName)
			throws ApplicationNotFoundException, PermissionDeniedException {
		LOG.debug("get number of subscriptions for application: "
				+ applicationName);
		ApplicationEntity application = this.applicationDAO
				.getApplication(applicationName);

		checkReadPermission(application);

		long count = this.subscriptionDAO.getNumberOfSubscriptions(application);
		return count;
	}

	private void checkReadPermission(ApplicationEntity application)
			throws PermissionDeniedException {
		if (this.sessionContext.isCallerInRole(SafeOnlineRoles.OPERATOR_ROLE)) {
			return;
		}
		ApplicationOwnerEntity applicationOwner = application
				.getApplicationOwner();
		SubjectEntity expectedSubject = applicationOwner.getAdmin();
		SubjectEntity actualSubject = this.subjectManager.getCallerSubject();
		if (false == expectedSubject.equals(actualSubject)) {
			throw new PermissionDeniedException();
		}
	}

	@RolesAllowed(SafeOnlineRoles.USER_ROLE)
	public boolean isSubscribed(String applicationName)
			throws ApplicationNotFoundException {
		LOG.debug("is subscribed: " + applicationName);
		Subject subject = SubjectFactory.getCallerSubject(this);
		Application application = ApplicationFactory.getApplication(this,
				applicationName);
		return subject.isSubscribed(application);
	}

	public SubjectManager getSubjectManager() {
		return this.subjectManager;
	}

	public ApplicationDAO getApplicationDAO() {
		return this.applicationDAO;
	}

	public SubscriptionDAO getSubscriptionDAO() {
		return this.subscriptionDAO;
	}
}
