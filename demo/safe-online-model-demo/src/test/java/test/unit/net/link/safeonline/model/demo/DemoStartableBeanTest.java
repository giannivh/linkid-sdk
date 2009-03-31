/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.model.demo;

import static org.easymock.EasyMock.checkOrder;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.net.URL;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.link.safeonline.Startable;
import net.link.safeonline.audit.bean.ResourceAuditLoggerBean;
import net.link.safeonline.audit.dao.bean.AccessAuditDAOBean;
import net.link.safeonline.audit.dao.bean.AuditAuditDAOBean;
import net.link.safeonline.audit.dao.bean.AuditContextDAOBean;
import net.link.safeonline.audit.dao.bean.ResourceAuditDAOBean;
import net.link.safeonline.audit.dao.bean.SecurityAuditDAOBean;
import net.link.safeonline.authentication.service.bean.DevicePolicyServiceBean;
import net.link.safeonline.config.dao.bean.ConfigGroupDAOBean;
import net.link.safeonline.config.dao.bean.ConfigItemDAOBean;
import net.link.safeonline.dao.bean.AllowedDeviceDAOBean;
import net.link.safeonline.dao.bean.ApplicationDAOBean;
import net.link.safeonline.dao.bean.ApplicationIdentityDAOBean;
import net.link.safeonline.dao.bean.ApplicationOwnerDAOBean;
import net.link.safeonline.dao.bean.ApplicationPoolDAOBean;
import net.link.safeonline.dao.bean.ApplicationScopeIdDAOBean;
import net.link.safeonline.dao.bean.AttributeCacheDAOBean;
import net.link.safeonline.dao.bean.AttributeDAOBean;
import net.link.safeonline.dao.bean.AttributeProviderDAOBean;
import net.link.safeonline.dao.bean.AttributeTypeDAOBean;
import net.link.safeonline.dao.bean.DeviceClassDAOBean;
import net.link.safeonline.dao.bean.DeviceDAOBean;
import net.link.safeonline.dao.bean.NodeDAOBean;
import net.link.safeonline.dao.bean.SubjectDAOBean;
import net.link.safeonline.dao.bean.SubjectIdentifierDAOBean;
import net.link.safeonline.dao.bean.SubscriptionDAOBean;
import net.link.safeonline.dao.bean.UsageAgreementDAOBean;
import net.link.safeonline.entity.AllowedDeviceEntity;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityAttributeEntity;
import net.link.safeonline.entity.ApplicationIdentityEntity;
import net.link.safeonline.entity.ApplicationOwnerEntity;
import net.link.safeonline.entity.ApplicationPoolEntity;
import net.link.safeonline.entity.ApplicationScopeIdEntity;
import net.link.safeonline.entity.AttributeCacheEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeProviderEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.CompoundedAttributeTypeMemberEntity;
import net.link.safeonline.entity.DeviceClassDescriptionEntity;
import net.link.safeonline.entity.DeviceClassEntity;
import net.link.safeonline.entity.DeviceDescriptionEntity;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.DevicePropertyEntity;
import net.link.safeonline.entity.NodeEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubjectIdentifierEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.entity.UsageAgreementEntity;
import net.link.safeonline.entity.UsageAgreementTextEntity;
import net.link.safeonline.entity.config.ConfigGroupEntity;
import net.link.safeonline.entity.config.ConfigItemEntity;
import net.link.safeonline.entity.config.ConfigItemValueEntity;
import net.link.safeonline.entity.notification.EndpointReferenceEntity;
import net.link.safeonline.entity.notification.NotificationProducerSubscriptionEntity;
import net.link.safeonline.entity.pkix.TrustDomainEntity;
import net.link.safeonline.entity.pkix.TrustPointEntity;
import net.link.safeonline.entity.tasks.SchedulingEntity;
import net.link.safeonline.entity.tasks.TaskEntity;
import net.link.safeonline.entity.tasks.TaskHistoryEntity;
import net.link.safeonline.keystore.SafeOnlineNodeKeyStore;
import net.link.safeonline.keystore.service.KeyService;
import net.link.safeonline.model.bean.ApplicationIdentityManagerBean;
import net.link.safeonline.model.bean.DevicesBean;
import net.link.safeonline.model.bean.IdGeneratorBean;
import net.link.safeonline.model.bean.SystemInitializationStartableBean;
import net.link.safeonline.model.bean.UsageAgreementManagerBean;
import net.link.safeonline.model.beid.bean.BeIdStartableBean;
import net.link.safeonline.model.demo.bean.DemoStartableBean;
import net.link.safeonline.model.digipass.bean.DigipassStartableBean;
import net.link.safeonline.model.encap.bean.EncapStartableBean;
import net.link.safeonline.model.password.bean.PasswordManagerBean;
import net.link.safeonline.model.password.bean.PasswordStartableBean;
import net.link.safeonline.notification.dao.bean.EndpointReferenceDAOBean;
import net.link.safeonline.notification.dao.bean.NotificationMessageDAOBean;
import net.link.safeonline.notification.dao.bean.NotificationProducerDAOBean;
import net.link.safeonline.notification.service.bean.NotificationProducerServiceBean;
import net.link.safeonline.pkix.dao.bean.TrustDomainDAOBean;
import net.link.safeonline.pkix.dao.bean.TrustPointDAOBean;
import net.link.safeonline.service.bean.SubjectServiceBean;
import net.link.safeonline.tasks.dao.bean.SchedulingDAOBean;
import net.link.safeonline.tasks.dao.bean.TaskDAOBean;
import net.link.safeonline.tasks.dao.bean.TaskHistoryDAOBean;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.EntityTestManager;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.test.util.PkiTestUtils;
import net.link.safeonline.test.util.SafeOnlineTestConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DemoStartableBeanTest {

    private EntityTestManager entityTestManager;

    private KeyService        mockKeyService;

    private JndiTestUtils     jndiTestUtils;

    private KeyPair           nodeKeyPair;

    private X509Certificate   nodeCertificate;

    private static Class<?>[] container = new Class[] { SubjectDAOBean.class, ApplicationDAOBean.class, SubscriptionDAOBean.class,
            AttributeDAOBean.class, TrustDomainDAOBean.class, ApplicationOwnerDAOBean.class, AttributeTypeDAOBean.class,
            ApplicationIdentityDAOBean.class, ConfigGroupDAOBean.class, ConfigItemDAOBean.class, TaskDAOBean.class,
            SchedulingDAOBean.class, TaskHistoryDAOBean.class, ApplicationIdentityManagerBean.class, TrustPointDAOBean.class,
            AttributeProviderDAOBean.class, DeviceDAOBean.class, DeviceClassDAOBean.class, AllowedDeviceDAOBean.class,
            SubjectServiceBean.class, SubjectIdentifierDAOBean.class, IdGeneratorBean.class, UsageAgreementDAOBean.class,
            UsageAgreementManagerBean.class, NodeDAOBean.class, DevicePolicyServiceBean.class, ResourceAuditLoggerBean.class,
            AuditAuditDAOBean.class, AuditContextDAOBean.class, AccessAuditDAOBean.class, SecurityAuditDAOBean.class,
            ResourceAuditDAOBean.class, DevicesBean.class, NotificationProducerServiceBean.class, NotificationProducerDAOBean.class,
            EndpointReferenceDAOBean.class, ApplicationScopeIdDAOBean.class, AttributeCacheDAOBean.class, ApplicationPoolDAOBean.class,
            NotificationMessageDAOBean.class, PasswordManagerBean.class };


    @Before
    public void setUp()
            throws Exception {

        entityTestManager = new EntityTestManager();
        entityTestManager.setUp(SubjectEntity.class, ApplicationEntity.class, ApplicationOwnerEntity.class, AttributeEntity.class,
                AttributeTypeEntity.class, SubscriptionEntity.class, TrustDomainEntity.class, ApplicationIdentityEntity.class,
                ConfigGroupEntity.class, ConfigItemEntity.class, ConfigItemValueEntity.class, SchedulingEntity.class, TaskEntity.class,
                TaskHistoryEntity.class, TrustPointEntity.class, ApplicationIdentityAttributeEntity.class,
                AttributeTypeDescriptionEntity.class, AttributeProviderEntity.class, DeviceEntity.class, DeviceClassEntity.class,
                DeviceDescriptionEntity.class, DevicePropertyEntity.class, DeviceClassDescriptionEntity.class, AllowedDeviceEntity.class,
                CompoundedAttributeTypeMemberEntity.class, SubjectIdentifierEntity.class, UsageAgreementEntity.class,
                UsageAgreementTextEntity.class, NodeEntity.class, EndpointReferenceEntity.class,
                NotificationProducerSubscriptionEntity.class, ApplicationScopeIdEntity.class, AttributeCacheEntity.class,
                ApplicationPoolEntity.class);

        EntityManager entityManager = entityTestManager.getEntityManager();

        mockKeyService = createMock(KeyService.class);
        checkOrder(mockKeyService, false);

        nodeKeyPair = PkiTestUtils.generateKeyPair();
        nodeCertificate = PkiTestUtils.generateSelfSignedCertificate(nodeKeyPair, "CN=Test");
        expect(mockKeyService.getPrivateKeyEntry(SafeOnlineNodeKeyStore.class)).andReturn(
                new PrivateKeyEntry(nodeKeyPair.getPrivate(), new Certificate[] { nodeCertificate })).times(2);
        replay(mockKeyService);

        jndiTestUtils = new JndiTestUtils();
        jndiTestUtils.setUp();
        jndiTestUtils.bindComponent(KeyService.JNDI_BINDING, mockKeyService);

        SafeOnlineTestConfig.loadTestNode(new URL("http://127.0.0.1/"));
        Startable systemStartable = EJBTestUtils.newInstance(SystemInitializationStartableBean.class, container, entityManager);
        systemStartable.postStart();

        verify(mockKeyService);
        reset(mockKeyService);

        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.commit();
        entityTransaction.begin();
    }

    @After
    public void tearDown()
            throws Exception {

        entityTestManager.tearDown();
        jndiTestUtils.tearDown();
    }

    @Test
    public void postStart()
            throws Exception {

        // setup
        SafeOnlineTestConfig.loadTestNode(new URL("http://127.0.0.1/"));
        EntityManager entityManager = entityTestManager.getEntityManager();
        BeIdStartableBean beIdStartableBean = EJBTestUtils.newInstance(BeIdStartableBean.class, container, entityManager);
        DigipassStartableBean digipassStartableBean = EJBTestUtils.newInstance(DigipassStartableBean.class, container, entityManager);
        EncapStartableBean encapStartableBean = EJBTestUtils.newInstance(EncapStartableBean.class, container, entityManager);
        PasswordStartableBean passwordStartableBean = EJBTestUtils.newInstance(PasswordStartableBean.class, container, entityManager);
        DemoStartableBean demoStartableBean = EJBTestUtils.newInstance(DemoStartableBean.class, container, entityManager);

        EJBTestUtils.setJBossPrincipal("test-operator", "operator");

        // expectations
        expect(mockKeyService.getPrivateKeyEntry(SafeOnlineNodeKeyStore.class)).andReturn(
                new PrivateKeyEntry(nodeKeyPair.getPrivate(), new Certificate[] { nodeCertificate })).times(2 * 5);
        replay(mockKeyService);

        // operate
        beIdStartableBean.postStart();
        digipassStartableBean.postStart();
        encapStartableBean.postStart();
        passwordStartableBean.postStart();
        demoStartableBean.postStart();

        // verify
        verify(mockKeyService);
        reset(mockKeyService);
    }
}