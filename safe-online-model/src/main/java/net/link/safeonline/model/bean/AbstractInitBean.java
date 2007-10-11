/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.bean;

import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBException;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.Startable;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SafeOnlineException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.IdentityAttributeTypeDO;
import net.link.safeonline.authentication.service.PasswordManager;
import net.link.safeonline.dao.AllowedDeviceDAO;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.ApplicationIdentityDAO;
import net.link.safeonline.dao.ApplicationOwnerDAO;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeProviderDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.DeviceDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityPK;
import net.link.safeonline.entity.ApplicationOwnerEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeProviderEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.entity.SubscriptionOwnerType;
import net.link.safeonline.entity.pkix.TrustDomainEntity;
import net.link.safeonline.entity.pkix.TrustPointEntity;
import net.link.safeonline.model.ApplicationIdentityManager;
import net.link.safeonline.pkix.dao.TrustDomainDAO;
import net.link.safeonline.pkix.dao.TrustPointDAO;
import net.link.safeonline.service.SubjectService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractInitBean implements Startable {

	protected final Log LOG = LogFactory.getLog(this.getClass());

	protected Map<String, String> authorizedUsers;

	protected Map<String, String> applicationOwnersAndLogin;

	protected static class Application {
		final String name;

		final String description;

		final URL applicationUrl;

		final String owner;

		final boolean allowUserSubscription;

		final boolean removable;

		final X509Certificate certificate;

		final boolean idmappingAccess;

		public Application(String name, String owner, String description,
				URL applicationUrl, boolean allowUserSubscription,
				boolean removable, X509Certificate certificate,
				boolean idmappingAccess) {
			this.name = name;
			this.owner = owner;
			this.description = description;
			this.applicationUrl = applicationUrl;
			this.allowUserSubscription = allowUserSubscription;
			this.removable = removable;
			this.certificate = certificate;
			this.idmappingAccess = idmappingAccess;
		}

		public Application(String name, String owner, String description,
				URL applicationUrl, boolean allowUserSubscription,
				boolean removable) {
			this(name, owner, description, applicationUrl,
					allowUserSubscription, removable, null, false);
		}

		public Application(String name, String owner, String description,
				URL applicationUrl) {
			this(name, owner, description, applicationUrl, true, true);
		}

		public Application(String name, String owner, String description,
				URL applicationUrl, X509Certificate certificate) {
			this(name, owner, description, applicationUrl, true, true,
					certificate, false);
		}

		public Application(String name, String owner) {
			this(name, owner, null, null);
		}

		public Application(String name, String owner,
				X509Certificate certificate) {
			this(name, owner, null, null, certificate);
		}
	}

	protected List<Application> registeredApplications;

	protected static class Subscription {
		final String user;

		final String application;

		final SubscriptionOwnerType subscriptionOwnerType;

		public Subscription(SubscriptionOwnerType subscriptionOwnerType,
				String user, String application) {
			this.subscriptionOwnerType = subscriptionOwnerType;
			this.user = user;
			this.application = application;
		}
	}

	protected static class Identity {
		final String application;

		final IdentityAttributeTypeDO[] identityAttributes;

		public Identity(String application,
				IdentityAttributeTypeDO[] identityAttributes) {
			this.application = application;
			this.identityAttributes = identityAttributes;
		}
	}

	protected List<Subscription> subscriptions;

	protected List<AttributeTypeEntity> attributeTypes;

	protected List<AttributeTypeDescriptionEntity> attributeTypeDescriptions;

	protected List<Identity> identities;

	protected List<X509Certificate> trustedCertificates;

	protected List<AttributeProviderEntity> attributeProviders;

	protected Map<String, List<String>> allowedDevices;

	@EJB
	private ApplicationIdentityManager applicationIdentityService;

	public abstract int getPriority();

	protected List<AttributeEntity> attributes;

	protected Map<String, List<AttributeTypeEntity>> devices;

	public AbstractInitBean() {
		this.applicationOwnersAndLogin = new HashMap<String, String>();
		this.attributeTypes = new LinkedList<AttributeTypeEntity>();
		this.authorizedUsers = new HashMap<String, String>();
		this.registeredApplications = new LinkedList<Application>();
		this.subscriptions = new LinkedList<Subscription>();
		this.identities = new LinkedList<Identity>();
		this.attributeTypeDescriptions = new LinkedList<AttributeTypeDescriptionEntity>();
		this.trustedCertificates = new LinkedList<X509Certificate>();
		this.attributeProviders = new LinkedList<AttributeProviderEntity>();
		this.attributes = new LinkedList<AttributeEntity>();
		this.devices = new HashMap<String, List<AttributeTypeEntity>>();
		this.allowedDevices = new HashMap<String, List<String>>();
	}

	public void postStart() {
		try {
			this.LOG.debug("postStart");
			initTrustDomains();
			initAttributeTypes();
			initAttributeTypeDescriptions();
			initSubjects();
			initApplicationOwners();
			initApplications();
			initSubscriptions();
			initIdentities();
			initApplicationTrustPoints();
			initAttributeProviders();
			initAttributes();
			initDevices();
			initAllowedDevices();
		} catch (SafeOnlineException e) {
			this.LOG.fatal("safeonline exception", e);
			throw new EJBException(e);
		}
	}

	public void preStop() {
		this.LOG.debug("preStop");
	}

	@EJB
	private SubjectService subjectService;

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

	@EJB
	private TrustPointDAO trustPointDAO;

	@EJB
	private ApplicationIdentityDAO applicationIdentityDAO;

	@EJB
	private AttributeProviderDAO attributeProviderDAO;

	@EJB
	private DeviceDAO deviceDAO;

	@EJB
	private PasswordManager passwordManager;

	private void initApplicationTrustPoints() {
		for (X509Certificate certificate : this.trustedCertificates) {
			addCertificateAsTrustPoint(certificate);
		}
	}

	private void initAttributes() {
		for (AttributeEntity attribute : this.attributes) {
			String attributeTypeName = attribute.getPk().getAttributeType();
			String subjectLogin = attribute.getPk().getSubject();
			AttributeEntity existingAttribute = this.attributeDAO
					.findAttribute(attributeTypeName, subjectLogin);
			if (null != existingAttribute) {
				continue;
			}
			AttributeTypeEntity attributeType;
			try {
				attributeType = this.attributeTypeDAO
						.getAttributeType(attributeTypeName);
			} catch (AttributeTypeNotFoundException e) {
				throw new EJBException("attribute type not found: "
						+ attributeTypeName);
			}
			SubjectEntity subject;
			try {
				subject = this.subjectService
						.getSubjectFromUserName(subjectLogin);
			} catch (SubjectNotFoundException e) {
				throw new EJBException("subject not found: " + subjectLogin);
			}
			String stringValue = attribute.getStringValue();
			AttributeEntity persistentAttribute = this.attributeDAO
					.addAttribute(attributeType, subject, stringValue);
			persistentAttribute.setBooleanValue(attribute.getBooleanValue());
		}
	}

	private void initAttributeProviders() {
		for (AttributeProviderEntity attributeProvider : this.attributeProviders) {
			String applicationName = attributeProvider.getApplicationName();
			String attributeName = attributeProvider.getAttributeTypeName();
			ApplicationEntity application = this.applicationDAO
					.findApplication(applicationName);
			if (null == application) {
				throw new EJBException("application not found: "
						+ applicationName);
			}
			AttributeTypeEntity attributeType = this.attributeTypeDAO
					.findAttributeType(attributeName);
			if (null == attributeType) {
				throw new EJBException("attribute type not found: "
						+ attributeName);
			}
			AttributeProviderEntity existingAttributeProvider = this.attributeProviderDAO
					.findAttributeProvider(application, attributeType);
			if (null != existingAttributeProvider) {
				continue;
			}
			this.attributeProviderDAO.addAttributeProvider(application,
					attributeType);
		}
	}

	private void addCertificateAsTrustPoint(X509Certificate certificate) {
		TrustDomainEntity applicationTrustDomain = this.trustDomainDAO
				.findTrustDomain(SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN);
		if (null == applicationTrustDomain) {
			this.LOG.fatal("application trust domain not found");
			return;
		}

		TrustPointEntity demoTrustPoint = this.trustPointDAO.findTrustPoint(
				applicationTrustDomain, certificate);
		if (null != demoTrustPoint) {
			try {
				/*
				 * In this case we still update the certificate.
				 */
				demoTrustPoint.setEncodedCert(certificate.getEncoded());
			} catch (CertificateEncodingException e) {
				this.LOG.error("cert encoding error");
			}
			return;
		}

		this.trustPointDAO.addTrustPoint(applicationTrustDomain, certificate);
	}

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
		for (AttributeTypeEntity attributeType : this.attributeTypes) {
			if (null != this.attributeTypeDAO.findAttributeType(attributeType
					.getName())) {
				continue;
			}
			this.attributeTypeDAO.addAttributeType(attributeType);
		}
	}

	private void initAttributeTypeDescriptions() {
		for (AttributeTypeDescriptionEntity attributeTypeDescription : this.attributeTypeDescriptions) {
			AttributeTypeDescriptionEntity existingDescription = this.attributeTypeDAO
					.findDescription(attributeTypeDescription.getPk());
			if (null != existingDescription) {
				continue;
			}
			AttributeTypeEntity attributeType;
			try {
				attributeType = this.attributeTypeDAO
						.getAttributeType(attributeTypeDescription
								.getAttributeTypeName());
			} catch (AttributeTypeNotFoundException e) {
				throw new EJBException("attribute type not found: "
						+ attributeTypeDescription.getAttributeTypeName());
			}
			this.attributeTypeDAO.addAttributeTypeDescription(attributeType,
					attributeTypeDescription);
		}
	}

	private void initSubscriptions() {
		for (Subscription subscription : this.subscriptions) {
			String login = subscription.user;
			String applicationName = subscription.application;
			SubscriptionOwnerType subscriptionOwnerType = subscription.subscriptionOwnerType;
			SubjectEntity subject = this.subjectService
					.findSubjectFromUserName(login);
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
			SubjectEntity adminSubject = this.subjectService
					.findSubjectFromUserName(login);
			this.applicationOwnerDAO.addApplicationOwner(name, adminSubject);
		}
	}

	private void initApplications() {
		for (Application application : this.registeredApplications) {
			String applicationName = application.name;
			ApplicationEntity existingApplication = this.applicationDAO
					.findApplication(applicationName);
			if (null != existingApplication) {
				if (null != application.certificate) {
					existingApplication.setCertificate(application.certificate);
				}
				continue;
			}
			ApplicationOwnerEntity applicationOwner = this.applicationOwnerDAO
					.findApplicationOwner(application.owner);
			long identityVersion = ApplicationIdentityPK.INITIAL_IDENTITY_VERSION;
			ApplicationEntity newApplication = this.applicationDAO
					.addApplication(applicationName, null, applicationOwner,
							application.allowUserSubscription,
							application.removable, application.description,
							application.applicationUrl,
							application.certificate, identityVersion);
			newApplication
					.setIdentifierMappingAllowed(application.idmappingAccess);

			this.applicationIdentityDAO.addApplicationIdentity(newApplication,
					identityVersion);
		}
	}

	private void initSubjects() throws AttributeTypeNotFoundException {
		for (Map.Entry<String, String> authorizedUser : this.authorizedUsers
				.entrySet()) {
			String login = authorizedUser.getKey();
			SubjectEntity subject = this.subjectService
					.findSubjectFromUserName(login);
			if (null != subject) {
				continue;
			}
			subject = this.subjectService.addSubject(login);

			String password = authorizedUser.getValue();
			try {
				this.passwordManager.setPassword(subject, password);
			} catch (PermissionDeniedException e) {
				throw new EJBException("could not set password");
			}
		}
	}

	private void initIdentities() {
		for (Identity identity : this.identities) {
			try {
				this.applicationIdentityService.updateApplicationIdentity(
						identity.application, Arrays
								.asList(identity.identityAttributes));
			} catch (Exception e) {
				this.LOG.debug("Could not update application identity");
				throw new RuntimeException(
						"could not update the application identity: "
								+ e.getMessage(), e);
			}
		}
	}

	private void initDevices() {
		for (String deviceName : this.devices.keySet()) {
			DeviceEntity device = this.deviceDAO.findDevice(deviceName);
			if (device == null) {
				device = this.deviceDAO.addDevice(deviceName);
			}
			device.setAttributeTypes(this.devices.get(deviceName));
		}
	}

	@EJB
	private AllowedDeviceDAO allowedDeviceDAO;

	private void initAllowedDevices() throws ApplicationNotFoundException,
			DeviceNotFoundException {
		for (String applicationName : this.allowedDevices.keySet()) {
			ApplicationEntity application = this.applicationDAO
					.getApplication(applicationName);
			application.setDeviceRestriction(true);
			List<String> deviceNames = this.allowedDevices.get(applicationName);
			for (String deviceName : deviceNames) {
				DeviceEntity device = this.deviceDAO.getDevice(deviceName);
				this.allowedDeviceDAO.addAllowedDevice(application, device, 0);
			}
		}
	}
}
