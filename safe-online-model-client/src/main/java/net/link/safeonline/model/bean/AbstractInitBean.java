/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.bean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.EJBException;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.Startable;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.DeviceClassNotFoundException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SafeOnlineException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.exception.UsageAgreementNotFoundException;
import net.link.safeonline.authentication.service.IdentityAttributeTypeDO;
import net.link.safeonline.dao.AllowedDeviceDAO;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.ApplicationIdentityDAO;
import net.link.safeonline.dao.ApplicationOwnerDAO;
import net.link.safeonline.dao.ApplicationPoolDAO;
import net.link.safeonline.dao.ApplicationScopeIdDAO;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeProviderDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.DeviceClassDAO;
import net.link.safeonline.dao.DeviceDAO;
import net.link.safeonline.dao.NodeDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.dao.UsageAgreementDAO;
import net.link.safeonline.entity.AllowedDeviceEntity;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityPK;
import net.link.safeonline.entity.ApplicationOwnerEntity;
import net.link.safeonline.entity.ApplicationPoolEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeProviderEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.DeviceClassDescriptionEntity;
import net.link.safeonline.entity.DeviceClassDescriptionPK;
import net.link.safeonline.entity.DeviceClassEntity;
import net.link.safeonline.entity.DeviceDescriptionEntity;
import net.link.safeonline.entity.DeviceDescriptionPK;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.DevicePropertyEntity;
import net.link.safeonline.entity.DevicePropertyPK;
import net.link.safeonline.entity.IdScopeType;
import net.link.safeonline.entity.NodeEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.entity.SubscriptionOwnerType;
import net.link.safeonline.entity.UsageAgreementEntity;
import net.link.safeonline.entity.UsageAgreementPK;
import net.link.safeonline.entity.pkix.TrustDomainEntity;
import net.link.safeonline.entity.pkix.TrustPointEntity;
import net.link.safeonline.model.ApplicationIdentityManager;
import net.link.safeonline.model.UsageAgreementManager;
import net.link.safeonline.notification.dao.NotificationProducerDAO;
import net.link.safeonline.notification.service.NotificationProducerService;
import net.link.safeonline.pkix.dao.TrustDomainDAO;
import net.link.safeonline.pkix.dao.TrustPointDAO;
import net.link.safeonline.service.SubjectService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractInitBean implements Startable {

    protected final Log LOG = LogFactory.getLog(getClass());


    protected static class Node {

        final String          name;

        final String          protocol;

        final String          hostname;

        final int             port;

        final int             sslPort;

        final X509Certificate authnCertificate;

        final X509Certificate signingCertificate;


        public Node(String name, String protocol, String hostname, int port, int sslPort, X509Certificate authnCertificate,
                    X509Certificate signingCertificate) {

            this.name = name;
            this.protocol = protocol;
            this.hostname = hostname;
            this.port = port;
            this.sslPort = sslPort;
            this.authnCertificate = authnCertificate;
            this.signingCertificate = signingCertificate;
        }
    }


    protected List<String>        users;

    protected Map<String, String> applicationOwnersAndLogin;


    protected static class Application {

        final String          name;

        final String          description;

        final URL             applicationUrl;

        final byte[]          applicationLogo;

        final String          owner;

        final boolean         allowUserSubscription;

        final boolean         removable;

        final X509Certificate certificate;

        final boolean         idmappingAccess;

        final IdScopeType     idScope;

        final boolean         ssoEnabled;

        final URL             ssoLogoutUrl;


        public Application(String name, String owner, String description, URL applicationUrl, byte[] applicationLogo,
                           boolean allowUserSubscription, boolean removable, X509Certificate certificate, boolean idmappingAccess,
                           IdScopeType idScope, boolean ssoEnabled, URL ssoLogoutUrl) {

            this.name = name;
            this.owner = owner;
            this.description = description;
            this.applicationUrl = applicationUrl;
            this.applicationLogo = applicationLogo;
            this.allowUserSubscription = allowUserSubscription;
            this.removable = removable;
            this.certificate = certificate;
            this.idmappingAccess = idmappingAccess;
            this.idScope = idScope;
            this.ssoEnabled = ssoEnabled;
            this.ssoLogoutUrl = ssoLogoutUrl;
        }

        public Application(String name, String owner, X509Certificate certificate, IdScopeType idScope) {

            this(name, owner, null, null, null, true, true, certificate, false, idScope, false, null);
        }
    }


    protected List<Application> registeredApplications;


    protected static class ApplicationPool {

        final String   name;

        final long     timeout;

        final String[] applications;


        public ApplicationPool(String name, long timeout, String[] applications) {

            this.name = name;
            this.timeout = timeout;
            this.applications = applications;
        }

    }


    protected List<ApplicationPool> applicationPools;


    protected static class Subscription {

        final String                user;

        final String                application;

        final SubscriptionOwnerType subscriptionOwnerType;


        public Subscription(SubscriptionOwnerType subscriptionOwnerType, String user, String application) {

            this.subscriptionOwnerType = subscriptionOwnerType;
            this.user = user;
            this.application = application;
        }
    }

    protected static class Identity {

        final String                    application;

        final IdentityAttributeTypeDO[] identityAttributes;


        public Identity(String application, IdentityAttributeTypeDO[] identityAttributes) {

            this.application = application;
            this.identityAttributes = identityAttributes;
        }
    }

    protected static class UsageAgreement {

        final String                  application;

        final Set<UsageAgreementText> usageAgreementTexts;


        public UsageAgreement(String application) {

            this.application = application;
            this.usageAgreementTexts = new HashSet<UsageAgreementText>();
        }

        public void addUsageAgreementText(UsageAgreementText usageAgreementText) {

            this.usageAgreementTexts.add(usageAgreementText);
        }
    }

    protected static class UsageAgreementText {

        final String language;

        final String text;


        public UsageAgreementText(String language, String text) {

            this.language = language;
            this.text = text;
        }
    }

    protected static class DeviceClass {

        final String name;

        final String authenticationContextClass;


        public DeviceClass(String name, String authenticationContextClass) {

            this.name = name;
            this.authenticationContextClass = authenticationContextClass;
        }
    }

    protected static class DeviceClassDescription {

        final String deviceClassName;

        final String language;

        final String description;


        public DeviceClassDescription(String deviceClassName, String language, String description) {

            this.deviceClassName = deviceClassName;
            this.language = language;
            this.description = description;
        }
    }

    protected static class Device {

        final String              deviceName;

        final String              deviceClassName;

        final String              nodeName;

        final X509Certificate     certificate;

        final String              authenticationPath;

        final String              registrationPath;

        final String              removalPath;

        final String              updatePath;

        final String              disablePath;

        final AttributeTypeEntity deviceAttribute;

        final AttributeTypeEntity deviceUserAttribute;

        final AttributeTypeEntity deviceDisableAttribute;


        public Device(String deviceName, String deviceClassName, String nodeName, String authenticationPath, String registrationPath,
                      String removalPath, String updatePath, String disablePath, X509Certificate certificate,
                      AttributeTypeEntity deviceAttribute, AttributeTypeEntity deviceUserAttribute,
                      AttributeTypeEntity deviceDisableAttribute) {

            this.deviceName = deviceName;
            this.deviceClassName = deviceClassName;
            this.nodeName = nodeName;
            this.authenticationPath = authenticationPath;
            this.registrationPath = registrationPath;
            this.removalPath = removalPath;
            this.updatePath = updatePath;
            this.disablePath = disablePath;
            this.certificate = certificate;
            this.deviceAttribute = deviceAttribute;
            this.deviceUserAttribute = deviceUserAttribute;
            this.deviceDisableAttribute = deviceDisableAttribute;
        }
    }

    protected static class DeviceDescription {

        final String deviceName;

        final String language;

        final String description;


        public DeviceDescription(String deviceName, String language, String description) {

            this.deviceName = deviceName;
            this.language = language;
            this.description = description;
        }
    }

    protected static class DeviceProperty {

        final String deviceName;

        final String name;

        final String value;


        public DeviceProperty(String deviceName, String name, String value) {

            this.deviceName = deviceName;
            this.name = name;
            this.value = value;
        }
    }

    protected static class NotificationSubscription {

        final String          topic;

        final String          address;

        final X509Certificate certificate;


        public NotificationSubscription(String topic, String address, X509Certificate certificate) {

            this.topic = topic;
            this.address = address;
            this.certificate = certificate;
        }
    }


    protected List<Subscription>                   subscriptions;

    protected List<AttributeTypeEntity>            attributeTypes;

    protected List<AttributeTypeDescriptionEntity> attributeTypeDescriptions;

    protected List<Identity>                       identities;

    protected List<UsageAgreement>                 usageAgreements;

    protected Map<X509Certificate, String>         trustedCertificates;

    protected List<AttributeProviderEntity>        attributeProviders;

    protected Map<String, List<String>>            allowedDevices;

    @EJB(mappedName = ApplicationIdentityManager.JNDI_BINDING)
    private ApplicationIdentityManager             applicationIdentityService;

    @EJB(mappedName = UsageAgreementManager.JNDI_BINDING)
    private UsageAgreementManager                  usageAgreementManager;


    public abstract int getPriority();


    protected List<AttributeEntity>          attributes;

    protected List<DeviceClass>              deviceClasses;

    protected List<DeviceClassDescription>   deviceClassDescriptions;

    protected List<Device>                   devices;

    protected List<DeviceDescription>        deviceDescriptions;

    protected List<DeviceProperty>           deviceProperties;

    protected List<String>                   notificationTopics;

    protected List<NotificationSubscription> notificationSubcriptions;

    protected Node                           node;


    public AbstractInitBean() {

        this.applicationOwnersAndLogin = new HashMap<String, String>();
        this.attributeTypes = new LinkedList<AttributeTypeEntity>();
        this.users = new LinkedList<String>();
        this.registeredApplications = new LinkedList<Application>();
        this.applicationPools = new LinkedList<ApplicationPool>();
        this.subscriptions = new LinkedList<Subscription>();
        this.identities = new LinkedList<Identity>();
        this.usageAgreements = new LinkedList<UsageAgreement>();
        this.attributeTypeDescriptions = new LinkedList<AttributeTypeDescriptionEntity>();
        this.trustedCertificates = new HashMap<X509Certificate, String>();
        this.attributeProviders = new LinkedList<AttributeProviderEntity>();
        this.attributes = new LinkedList<AttributeEntity>();
        this.deviceClasses = new LinkedList<DeviceClass>();
        this.deviceClassDescriptions = new LinkedList<DeviceClassDescription>();
        this.devices = new LinkedList<Device>();
        this.deviceDescriptions = new LinkedList<DeviceDescription>();
        this.deviceProperties = new LinkedList<DeviceProperty>();
        this.allowedDevices = new HashMap<String, List<String>>();
        this.notificationTopics = new LinkedList<String>();
        this.notificationSubcriptions = new LinkedList<NotificationSubscription>();
    }

    protected byte[] getLogo(String logoResource) {

        // Load logo from JAR that contains the class that overrides us.
        InputStream logoStream = getClass().getResourceAsStream(logoResource);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        try {
            for (int inByte = logoStream.read(); inByte >= 0; inByte = logoStream.read()) {
                outBytes.write(inByte);
            }
        } catch (IOException e) {
            this.LOG.warn("Couldn't successfully read in logo.", e);
        }

        // For logging purposes: calculate the MD5 of the logo.
        byte[] logo = outBytes.toByteArray();
        StringBuffer md5 = new StringBuffer();
        try {
            for (byte b : MessageDigest.getInstance("MD5").digest(logo)) {
                md5.append(String.format("%02x", b));
            }
        } catch (NoSuchAlgorithmException e) {
            this.LOG.error("no md5 digest.", e);
        }
        this.LOG.debug("Loading logo " + logoResource + " with MD5: " + md5);

        return logo;
    }

    public void postStart() {

        try {
            this.LOG.debug("postStart");
            initNode();
            initTrustDomains();
            initAttributeTypes();
            initAttributeTypeDescriptions();
            initDeviceClasses();
            initDeviceClassDescriptions();
            initDevices();
            initDeviceDescriptions();
            initDeviceProperties();
            initSubjects();
            initApplicationOwners();
            initApplications();
            initApplicationPools();
            initSubscriptions();
            initIdentities();
            initUsageAgreements();
            initAllowedDevices();
            initApplicationTrustPoints();
            initAttributeProviders();
            initAttributes();
            initNotificationTopics();
            initNotifications();
        } catch (SafeOnlineException e) {
            this.LOG.fatal("safeonline exception", e);
            throw new EJBException(e);
        }
    }

    public void preStop() {

        this.LOG.debug("preStop");
    }


    @EJB(mappedName = SubjectService.JNDI_BINDING)
    private SubjectService         subjectService;

    @EJB(mappedName = ApplicationDAO.JNDI_BINDING)
    private ApplicationDAO         applicationDAO;

    @EJB(mappedName = SubscriptionDAO.JNDI_BINDING)
    private SubscriptionDAO        subscriptionDAO;

    @EJB(mappedName = ApplicationScopeIdDAO.JNDI_BINDING)
    private ApplicationScopeIdDAO  applicationScopeIdDAO;

    @EJB(mappedName = ApplicationOwnerDAO.JNDI_BINDING)
    private ApplicationOwnerDAO    applicationOwnerDAO;

    @EJB(mappedName = AttributeTypeDAO.JNDI_BINDING)
    private AttributeTypeDAO       attributeTypeDAO;

    @EJB(mappedName = AttributeDAO.JNDI_BINDING)
    private AttributeDAO           attributeDAO;

    @EJB(mappedName = TrustDomainDAO.JNDI_BINDING)
    protected TrustDomainDAO       trustDomainDAO;

    @EJB(mappedName = TrustPointDAO.JNDI_BINDING)
    private TrustPointDAO          trustPointDAO;

    @EJB(mappedName = ApplicationIdentityDAO.JNDI_BINDING)
    private ApplicationIdentityDAO applicationIdentityDAO;

    @EJB(mappedName = UsageAgreementDAO.JNDI_BINDING)
    private UsageAgreementDAO      usageAgreementDAO;

    @EJB(mappedName = AttributeProviderDAO.JNDI_BINDING)
    private AttributeProviderDAO   attributeProviderDAO;

    @EJB(mappedName = DeviceDAO.JNDI_BINDING)
    private DeviceDAO              deviceDAO;

    @EJB(mappedName = DeviceClassDAO.JNDI_BINDING)
    private DeviceClassDAO         deviceClassDAO;


    private void initApplicationTrustPoints() {

        for (Map.Entry<X509Certificate, String> certificateEntry : this.trustedCertificates.entrySet()) {
            addCertificateAsTrustPoint(certificateEntry.getValue(), certificateEntry.getKey());
        }
    }

    private void initAttributes() {

        for (AttributeEntity attribute : this.attributes) {
            String attributeTypeName = attribute.getPk().getAttributeType();
            String subjectLogin = attribute.getPk().getSubject();

            SubjectEntity subject;
            try {
                subject = this.subjectService.getSubjectFromUserName(subjectLogin);
            } catch (SubjectNotFoundException e) {
                throw new EJBException("subject not found: " + subjectLogin);
            }

            AttributeEntity existingAttribute = this.attributeDAO.findAttribute(attributeTypeName, subject);
            if (null != existingAttribute) {
                continue;
            }

            AttributeTypeEntity attributeType;
            try {
                attributeType = this.attributeTypeDAO.getAttributeType(attributeTypeName);
            } catch (AttributeTypeNotFoundException e) {
                throw new EJBException("attribute type not found: " + attributeTypeName);
            }

            String stringValue = attribute.getStringValue();
            AttributeEntity persistentAttribute = this.attributeDAO.addAttribute(attributeType, subject, stringValue);
            persistentAttribute.setBooleanValue(attribute.getBooleanValue());
        }
    }

    private void initAttributeProviders() {

        for (AttributeProviderEntity attributeProvider : this.attributeProviders) {
            String applicationName = attributeProvider.getApplicationName();
            String attributeName = attributeProvider.getAttributeTypeName();
            ApplicationEntity application = this.applicationDAO.findApplication(applicationName);
            if (null == application)
                throw new EJBException("application not found: " + applicationName);
            AttributeTypeEntity attributeType = this.attributeTypeDAO.findAttributeType(attributeName);
            if (null == attributeType)
                throw new EJBException("attribute type not found: " + attributeName);
            AttributeProviderEntity existingAttributeProvider = this.attributeProviderDAO.findAttributeProvider(application, attributeType);
            if (null != existingAttributeProvider) {
                continue;
            }
            this.attributeProviderDAO.addAttributeProvider(application, attributeType);
        }
    }

    private void addCertificateAsTrustPoint(String trustDomainName, X509Certificate certificate) {

        TrustDomainEntity trustDomain = this.trustDomainDAO.findTrustDomain(trustDomainName);
        if (null == trustDomain) {
            this.LOG.fatal("trust domain not found: " + trustDomainName);
            return;
        }

        TrustPointEntity demoTrustPoint = this.trustPointDAO.findTrustPoint(trustDomain, certificate);
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

        this.trustPointDAO.addTrustPoint(trustDomain, certificate);
    }

    private void initTrustDomains() {

        TrustDomainEntity applicationsTrustDomain = this.trustDomainDAO
                                                                       .findTrustDomain(SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN);
        if (null != applicationsTrustDomain)
            return;

        applicationsTrustDomain = this.trustDomainDAO.addTrustDomain(SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN, true);

        TrustDomainEntity devicesTrustDomain = this.trustDomainDAO.findTrustDomain(SafeOnlineConstants.SAFE_ONLINE_DEVICES_TRUST_DOMAIN);
        if (null != devicesTrustDomain)
            return;
        devicesTrustDomain = this.trustDomainDAO.addTrustDomain(SafeOnlineConstants.SAFE_ONLINE_DEVICES_TRUST_DOMAIN, true);

        TrustDomainEntity olasTrustDomain = this.trustDomainDAO.findTrustDomain(SafeOnlineConstants.SAFE_ONLINE_OLAS_TRUST_DOMAIN);
        if (null != olasTrustDomain)
            return;
        olasTrustDomain = this.trustDomainDAO.addTrustDomain(SafeOnlineConstants.SAFE_ONLINE_OLAS_TRUST_DOMAIN, true);
    }

    private void initAttributeTypes() {

        NodeEntity location;
        try {
            location = this.olasDAO.getNode(this.node.name);
        } catch (NodeNotFoundException e) {
            throw new EJBException("olas node " + this.node.name + " not found");
        }
        for (AttributeTypeEntity attributeType : this.attributeTypes) {
            if (null != this.attributeTypeDAO.findAttributeType(attributeType.getName())) {
                continue;
            }
            attributeType.setLocation(location);
            this.attributeTypeDAO.addAttributeType(attributeType);
        }
    }

    private void initAttributeTypeDescriptions() {

        for (AttributeTypeDescriptionEntity attributeTypeDescription : this.attributeTypeDescriptions) {
            AttributeTypeDescriptionEntity existingDescription = this.attributeTypeDAO.findDescription(attributeTypeDescription.getPk());
            if (null != existingDescription) {
                continue;
            }
            AttributeTypeEntity attributeType;
            try {
                attributeType = this.attributeTypeDAO.getAttributeType(attributeTypeDescription.getAttributeTypeName());
            } catch (AttributeTypeNotFoundException e) {
                throw new EJBException("attribute type not found: " + attributeTypeDescription.getAttributeTypeName());
            }
            this.attributeTypeDAO.addAttributeTypeDescription(attributeType, attributeTypeDescription);
        }
    }

    private void initSubscriptions() {

        for (Subscription subscription : this.subscriptions) {
            String login = subscription.user;
            String applicationName = subscription.application;
            SubscriptionOwnerType subscriptionOwnerType = subscription.subscriptionOwnerType;
            SubjectEntity subject = this.subjectService.findSubjectFromUserName(login);
            ApplicationEntity application = this.applicationDAO.findApplication(applicationName);
            SubscriptionEntity subscriptionEntity = this.subscriptionDAO.findSubscription(subject, application);
            if (null != subscriptionEntity) {
                continue;
            }
            this.subscriptionDAO.addSubscription(subscriptionOwnerType, subject, application);
            if (application.getIdScope().equals(IdScopeType.APPLICATION)) {
                this.applicationScopeIdDAO.addApplicationScopeId(subject, application);
            }
        }
    }

    private void initApplicationOwners() {

        for (Map.Entry<String, String> applicationOwnerAndLogin : this.applicationOwnersAndLogin.entrySet()) {
            String name = applicationOwnerAndLogin.getKey();
            String login = applicationOwnerAndLogin.getValue();
            if (null != this.applicationOwnerDAO.findApplicationOwner(name)) {
                continue;
            }
            SubjectEntity adminSubject = this.subjectService.findSubjectFromUserName(login);
            this.applicationOwnerDAO.addApplicationOwner(name, adminSubject);
        }
    }

    private void initApplications() {

        for (Application application : this.registeredApplications) {
            String applicationName = application.name;
            ApplicationEntity existingApplication = this.applicationDAO.findApplication(applicationName);
            if (null != existingApplication) {
                if (null != application.certificate) {
                    existingApplication.setCertificate(application.certificate);
                }
                continue;
            }
            ApplicationOwnerEntity applicationOwner = this.applicationOwnerDAO.findApplicationOwner(application.owner);
            long identityVersion = ApplicationIdentityPK.INITIAL_IDENTITY_VERSION;
            long usageAgreementVersion = UsageAgreementPK.EMPTY_USAGE_AGREEMENT_VERSION;
            ApplicationEntity newApplication = this.applicationDAO.addApplication(applicationName, null, applicationOwner,
                    application.allowUserSubscription, application.removable, application.description, application.applicationUrl,
                    application.applicationLogo, application.certificate, identityVersion, usageAgreementVersion);
            newApplication.setIdentifierMappingAllowed(application.idmappingAccess);
            newApplication.setIdScope(application.idScope);
            newApplication.setSsoEnabled(application.ssoEnabled);
            newApplication.setSsoLogoutUrl(application.ssoLogoutUrl);

            this.applicationIdentityDAO.addApplicationIdentity(newApplication, identityVersion);
        }
    }


    @EJB(mappedName = ApplicationPoolDAO.JNDI_BINDING)
    private ApplicationPoolDAO applicationPoolDAO;


    private void initApplicationPools() {

        for (ApplicationPool applicationPool : this.applicationPools) {
            ApplicationPoolEntity existingApplicationPool = this.applicationPoolDAO.findApplicationPool(applicationPool.name);
            if (null != existingApplicationPool) {
                continue;
            }
            ApplicationPoolEntity newApplicationPool = this.applicationPoolDAO.addApplicationPool(applicationPool.name,
                    applicationPool.timeout);
            List<ApplicationEntity> applications = new LinkedList<ApplicationEntity>();
            for (String applicationName : applicationPool.applications) {
                ApplicationEntity application = this.applicationDAO.findApplication(applicationName);
                if (null == application) {
                    this.LOG.debug("Could not find application: " + applicationName);
                    throw new RuntimeException("Could not find application: " + applicationName);
                }
                applications.add(application);
            }
            newApplicationPool.setApplications(applications);
        }
    }

    private void initSubjects()
            throws AttributeTypeNotFoundException {

        for (String login : this.users) {
            SubjectEntity subject = this.subjectService.findSubjectFromUserName(login);
            if (null != subject) {
                continue;
            }
            subject = this.subjectService.addSubject(login);
        }
    }

    private void initIdentities() {

        for (Identity identity : this.identities) {
            try {
                this.applicationIdentityService.updateApplicationIdentity(identity.application, Arrays.asList(identity.identityAttributes));
            } catch (Exception e) {
                this.LOG.debug("Could not update application identity");
                throw new RuntimeException("could not update the application identity: " + e.getMessage(), e);
            }
        }
    }

    private void initUsageAgreements() {

        for (UsageAgreement usageAgreement : this.usageAgreements) {
            ApplicationEntity application = this.applicationDAO.findApplication(usageAgreement.application);
            UsageAgreementEntity usageAgreementEntity = this.usageAgreementDAO.addUsageAgreement(application,
                    UsageAgreementPK.INITIAL_USAGE_AGREEMENT_VERSION);
            for (UsageAgreementText usageAgreementText : usageAgreement.usageAgreementTexts) {
                this.usageAgreementDAO.addUsageAgreementText(usageAgreementEntity, usageAgreementText.text, usageAgreementText.language);
            }
            try {
                this.usageAgreementManager.setUsageAgreement(application, UsageAgreementPK.INITIAL_USAGE_AGREEMENT_VERSION);
            } catch (UsageAgreementNotFoundException e) {
                this.LOG.debug("could not set usage agreement for application: " + application.getName());
                throw new RuntimeException("could not set usage agreement for application: " + application.getName() + " : "
                        + e.getMessage(), e);
            }
        }
    }

    private void initDeviceClasses() {

        for (DeviceClass deviceClass : this.deviceClasses) {
            DeviceClassEntity deviceClassEntity = this.deviceClassDAO.findDeviceClass(deviceClass.name);
            if (null == deviceClassEntity) {
                deviceClassEntity = this.deviceClassDAO.addDeviceClass(deviceClass.name, deviceClass.authenticationContextClass);
            }
        }
    }

    private void initDeviceClassDescriptions() {

        for (DeviceClassDescription deviceClassDescription : this.deviceClassDescriptions) {
            DeviceClassDescriptionEntity existingDescription = this.deviceClassDAO.findDescription(new DeviceClassDescriptionPK(
                    deviceClassDescription.deviceClassName, deviceClassDescription.language));
            if (null != existingDescription) {
                continue;
            }
            DeviceClassEntity deviceClass;
            try {
                deviceClass = this.deviceClassDAO.getDeviceClass(deviceClassDescription.deviceClassName);
            } catch (DeviceClassNotFoundException e) {
                throw new EJBException("device class not found: " + deviceClassDescription.deviceClassName);
            }
            this.deviceClassDAO.addDescription(deviceClass, new DeviceClassDescriptionEntity(deviceClass, deviceClassDescription.language,
                    deviceClassDescription.description));
        }
    }

    private void initDevices()
            throws DeviceClassNotFoundException, NodeNotFoundException {

        for (Device device : this.devices) {
            DeviceEntity deviceEntity = this.deviceDAO.findDevice(device.deviceName);
            if (deviceEntity == null) {
                DeviceClassEntity deviceClassEntity = this.deviceClassDAO.getDeviceClass(device.deviceClassName);
                NodeEntity olasNode = null;
                /*
                 * If no node, local device
                 */
                if (null != device.nodeName) {
                    olasNode = this.olasDAO.getNode(device.nodeName);
                }
                deviceEntity = this.deviceDAO.addDevice(device.deviceName, deviceClassEntity, olasNode, device.authenticationPath,
                        device.registrationPath, device.removalPath, device.updatePath, device.disablePath, device.certificate,
                        device.deviceAttribute, device.deviceUserAttribute, device.deviceDisableAttribute);
            }
        }
    }

    private void initDeviceDescriptions() {

        for (DeviceDescription deviceDescription : this.deviceDescriptions) {
            DeviceDescriptionEntity existingDescription = this.deviceDAO.findDescription(new DeviceDescriptionPK(
                    deviceDescription.deviceName, deviceDescription.language));
            if (null != existingDescription) {
                continue;
            }
            DeviceEntity device;
            try {
                device = this.deviceDAO.getDevice(deviceDescription.deviceName);
            } catch (DeviceNotFoundException e) {
                throw new EJBException("device not found: " + deviceDescription.deviceName);
            }
            this.deviceDAO.addDescription(device, new DeviceDescriptionEntity(device, deviceDescription.language,
                    deviceDescription.description));
        }
    }

    private void initDeviceProperties() {

        for (DeviceProperty deviceProperty : this.deviceProperties) {
            DevicePropertyEntity existingProperty = this.deviceDAO.findProperty(new DevicePropertyPK(deviceProperty.deviceName,
                    deviceProperty.name));
            if (null != existingProperty) {
                continue;
            }
            DeviceEntity device;
            try {
                device = this.deviceDAO.getDevice(deviceProperty.deviceName);
            } catch (DeviceNotFoundException e) {
                throw new EJBException("device not found: " + deviceProperty.deviceName);
            }
            this.deviceDAO.addProperty(device, new DevicePropertyEntity(device, deviceProperty.name, deviceProperty.value));
        }
    }


    @EJB(mappedName = AllowedDeviceDAO.JNDI_BINDING)
    private AllowedDeviceDAO allowedDeviceDAO;


    private void initAllowedDevices()
            throws ApplicationNotFoundException, DeviceNotFoundException {

        for (String applicationName : this.allowedDevices.keySet()) {
            ApplicationEntity application = this.applicationDAO.getApplication(applicationName);
            application.setDeviceRestriction(true);
            List<String> deviceNames = this.allowedDevices.get(applicationName);
            for (String deviceName : deviceNames) {
                DeviceEntity device = this.deviceDAO.getDevice(deviceName);
                AllowedDeviceEntity allowedDevice = this.allowedDeviceDAO.findAllowedDevice(application, device);
                if (null == allowedDevice) {
                    this.allowedDeviceDAO.addAllowedDevice(application, device, 0);
                }
            }
        }
    }


    @EJB(mappedName = NotificationProducerDAO.JNDI_BINDING)
    private NotificationProducerDAO notificationProducerDAO;


    private void initNotificationTopics() {

        for (String topic : this.notificationTopics) {
            if (null == this.notificationProducerDAO.findSubscription(topic)) {
                this.notificationProducerDAO.addSubscription(topic);
            }
        }
    }


    @EJB(mappedName = NotificationProducerService.JNDI_BINDING)
    private NotificationProducerService notificationProducerService;


    private void initNotifications()
            throws PermissionDeniedException {

        for (NotificationSubscription subscription : this.notificationSubcriptions) {
            this.notificationProducerService.subscribe(subscription.topic, subscription.address, subscription.certificate);
        }
    }


    @EJB(mappedName = NodeDAO.JNDI_BINDING)
    private NodeDAO olasDAO;


    private void initNode() {

        if (null == this.node)
            throw new EJBException("No Olas node specified");
        NodeEntity olasNode = this.olasDAO.findNode(this.node.name);
        if (null == olasNode) {
            this.olasDAO.addNode(this.node.name, this.node.protocol, this.node.hostname, this.node.port, this.node.sslPort,
                    this.node.authnCertificate, this.node.signingCertificate);
        }
    }
}