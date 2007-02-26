/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.bean;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.Startable;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.ApplicationOwnerDAO;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.SubjectDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.dao.TrustDomainDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationOwnerEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.entity.SubscriptionOwnerType;
import net.link.safeonline.entity.TrustDomainEntity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractInitBean implements Startable {

	protected final Log LOG = LogFactory.getLog(this.getClass());

	protected Map<String, String> authorizedUsers;

	protected Map<String, String> applicationOwnersAndLogin;

	protected static class Application {
		private final String name;

		private final String description;

		private final String owner;

		private final boolean allowUserSubscription;

		private final boolean removable;

		public Application(String name, String owner, String description,
				boolean allowUserSubscription, boolean removable) {
			this.name = name;
			this.owner = owner;
			this.description = description;
			this.allowUserSubscription = allowUserSubscription;
			this.removable = removable;
		}

		public Application(String name, String owner, String description) {
			this(name, owner, description, true, true);
		}

		public Application(String name, String owner) {
			this(name, owner, null);
		}
	}

	protected List<Application> registeredApplications;

	protected static class Subscription {
		private final String user;

		private final String application;

		private final SubscriptionOwnerType subscriptionOwnerType;

		public Subscription(SubscriptionOwnerType subscriptionOwnerType,
				String user, String application) {
			this.subscriptionOwnerType = subscriptionOwnerType;
			this.user = user;
			this.application = application;
		}
	}

	protected List<Subscription> subscriptions;

	protected List<AttributeTypeEntity> attributeTypes;

	public abstract int getPriority();

	public AbstractInitBean() {
		this.applicationOwnersAndLogin = new HashMap<String, String>();
		this.attributeTypes = new LinkedList<AttributeTypeEntity>();
		this.authorizedUsers = new HashMap<String, String>();
		this.registeredApplications = new LinkedList<Application>();
		this.subscriptions = new LinkedList<Subscription>();
	}

	public void postStart() {
		LOG.debug("postStart");
		initTrustDomains();
		initAttributeTypes();
		initSubjectsAndAttributes();
		initApplicationOwners();
		initApplications();
		initSubscriptions();
	}

	public void preStop() {
		LOG.debug("preStop");
	}

	@EJB
	private SubjectDAO subjectDAO;

	@EJB
	private ApplicationDAO applicationDAO;

	@EJB
	private SubscriptionDAO subscriptionDAO;

	@EJB
	private ApplicationOwnerDAO applicationOwnerDAO;

	@EJB
	private AttributeTypeDAO attributeTypeDAO;

	@EJB
	private AttributeDAO attributeDAO;

	@EJB
	protected TrustDomainDAO trustDomainDAO;

	private void initTrustDomains() {
		TrustDomainEntity applicationsTrustDomain = this.trustDomainDAO
				.findTrustDomain(SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN);
		if (null != applicationsTrustDomain) {
			return;
		}

		applicationsTrustDomain = this.trustDomainDAO
				.addTrustDomain(
						SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN,
						true);
	}

	private void initAttributeTypes() {
		for (AttributeTypeEntity attributeType : attributeTypes) {
			if (null != this.attributeTypeDAO.findAttributeType(attributeType
					.getName())) {
				continue;
			}
			this.attributeTypeDAO.addAttributeType(attributeType);
		}
	}

	private void initSubscriptions() {
		for (Subscription subscription : this.subscriptions) {
			String login = subscription.user;
			String applicationName = subscription.application;
			SubscriptionOwnerType subscriptionOwnerType = subscription.subscriptionOwnerType;
			SubjectEntity subject = this.subjectDAO.findSubject(login);
			ApplicationEntity application = this.applicationDAO
					.findApplication(applicationName);
			SubscriptionEntity subscriptionEntity = this.subscriptionDAO
					.findSubscription(subject, application);
			if (null != subscriptionEntity) {
				continue;
			}
			this.subscriptionDAO.addSubscription(subscriptionOwnerType,
					subject, application);
		}
	}

	private void initApplicationOwners() {
		for (Map.Entry<String, String> applicationOwnerAndLogin : this.applicationOwnersAndLogin
				.entrySet()) {
			String name = applicationOwnerAndLogin.getKey();
			String login = applicationOwnerAndLogin.getValue();
			if (null != this.applicationOwnerDAO.findApplicationOwner(name)) {
				continue;
			}
			SubjectEntity adminSubject = this.subjectDAO.findSubject(login);
			this.applicationOwnerDAO.addApplicationOwner(name, adminSubject);
		}
	}

	private void initApplications() {
		for (Application application : this.registeredApplications) {
			String applicationName = application.name;
			ApplicationEntity existingApplication = this.applicationDAO
					.findApplication(applicationName);
			if (null != existingApplication) {
				continue;
			}
			ApplicationOwnerEntity applicationOwner = this.applicationOwnerDAO
					.findApplicationOwner(application.owner);
			this.applicationDAO.addApplication(applicationName,
					applicationOwner, application.allowUserSubscription,
					application.removable, application.description);
		}
	}

	private void initSubjectsAndAttributes() {
		for (Map.Entry<String, String> authorizedUser : this.authorizedUsers
				.entrySet()) {
			String login = authorizedUser.getKey();
			SubjectEntity subject = this.subjectDAO.findSubject(login);
			if (null != subject) {
				continue;
			}
			subject = this.subjectDAO.addSubject(login);
			AttributeEntity passwordAttribute = this.attributeDAO
					.findAttribute(SafeOnlineConstants.PASSWORD_ATTRIBUTE,
							login);
			if (null != passwordAttribute) {
				continue;
			}
			String password = authorizedUser.getValue();
			this.attributeDAO.addAttribute(
					SafeOnlineConstants.PASSWORD_ATTRIBUTE, login, password);
		}
	}
}
