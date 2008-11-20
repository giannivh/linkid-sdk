/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package test.unit.net.link.safeonline.model.password;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.SubjectDAO;
import net.link.safeonline.dao.bean.AttributeDAOBean;
import net.link.safeonline.dao.bean.AttributeTypeDAOBean;
import net.link.safeonline.dao.bean.SubjectDAOBean;
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
import net.link.safeonline.entity.DatatypeType;
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
import net.link.safeonline.entity.notification.EndpointReferenceEntity;
import net.link.safeonline.entity.notification.NotificationProducerSubscriptionEntity;
import net.link.safeonline.entity.pkix.TrustDomainEntity;
import net.link.safeonline.entity.pkix.TrustPointEntity;
import net.link.safeonline.entity.tasks.SchedulingEntity;
import net.link.safeonline.entity.tasks.TaskEntity;
import net.link.safeonline.entity.tasks.TaskHistoryEntity;
import net.link.safeonline.model.password.PasswordConstants;
import net.link.safeonline.model.password.PasswordManager;
import net.link.safeonline.model.password.bean.PasswordManagerBean;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.EntityTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PasswordManagerBeanTest {

	private EntityTestManager entityTestManager;

	private PasswordManager testedInstance;

	private AttributeTypeDAO attributeTypeDAO;

	private AttributeDAO attributeDAO;

	private SubjectDAO subjectDAO;

	@Before
	public void setUp() throws Exception {

		this.entityTestManager = new EntityTestManager();
		/*
		 * If you add entities to this list, also add them to
		 * safe-online-sql-ddl.
		 */
		this.entityTestManager.setUp(SubjectEntity.class,
				ApplicationEntity.class, ApplicationOwnerEntity.class,
				AttributeEntity.class, AttributeTypeEntity.class,
				SubscriptionEntity.class, TrustDomainEntity.class,
				ApplicationIdentityEntity.class, ConfigGroupEntity.class,
				ConfigItemEntity.class, SchedulingEntity.class,
				TaskEntity.class, TaskHistoryEntity.class,
				TrustPointEntity.class,
				ApplicationIdentityAttributeEntity.class,
				AttributeTypeDescriptionEntity.class,
				AttributeProviderEntity.class, DeviceEntity.class,
				DeviceClassEntity.class, DeviceDescriptionEntity.class,
				DevicePropertyEntity.class, DeviceClassDescriptionEntity.class,
				AllowedDeviceEntity.class,
				CompoundedAttributeTypeMemberEntity.class,
				SubjectIdentifierEntity.class, UsageAgreementEntity.class,
				UsageAgreementTextEntity.class, NodeEntity.class,
				EndpointReferenceEntity.class,
				NotificationProducerSubscriptionEntity.class,
				ApplicationScopeIdEntity.class, AttributeCacheEntity.class,
				ApplicationPoolEntity.class);

		this.testedInstance = new PasswordManagerBean();
		this.attributeDAO = new AttributeDAOBean();
		this.attributeTypeDAO = new AttributeTypeDAOBean();
		this.subjectDAO = new SubjectDAOBean();

		EJBTestUtils.inject(this.attributeDAO, this.entityTestManager
				.getEntityManager());
		EJBTestUtils.inject(this.attributeTypeDAO, this.entityTestManager
				.getEntityManager());
		EJBTestUtils.inject(this.subjectDAO, this.entityTestManager
				.getEntityManager());
		EJBTestUtils.inject(this.testedInstance, this.attributeDAO);
		EJBTestUtils.inject(this.testedInstance, this.attributeTypeDAO);

		EJBTestUtils.init(this.attributeDAO);
		EJBTestUtils.init(this.attributeTypeDAO);
		EJBTestUtils.init(this.testedInstance);

		this.attributeTypeDAO.addAttributeType(new AttributeTypeEntity(
				PasswordConstants.PASSWORD_HASH_ATTRIBUTE, DatatypeType.STRING,
				false, false));
		this.attributeTypeDAO.addAttributeType(new AttributeTypeEntity(
				PasswordConstants.PASSWORD_SEED_ATTRIBUTE, DatatypeType.STRING,
				false, false));
		this.attributeTypeDAO.addAttributeType(new AttributeTypeEntity(
				PasswordConstants.PASSWORD_DEVICE_DISABLE_ATTRIBUTE,
				DatatypeType.BOOLEAN, false, false));
		this.attributeTypeDAO.addAttributeType(new AttributeTypeEntity(
				PasswordConstants.PASSWORD_ALGORITHM_ATTRIBUTE,
				DatatypeType.STRING, false, false));
		this.attributeTypeDAO.addAttributeType(new AttributeTypeEntity(
				PasswordConstants.PASSWORD_DEVICE_ATTRIBUTE,
				DatatypeType.COMPOUNDED, false, false));

	}

	@After
	public void tearDown() throws Exception {

		this.entityTestManager.tearDown();
	}

	@Test
	public void testSetPassword() throws Exception {

		// prepare
		UUID subjectUUID = UUID.randomUUID();
		SubjectEntity subject = this.subjectDAO.addSubject(subjectUUID
				.toString());
		String password = "password";

		// operate
		this.testedInstance.setPassword(subject, password);

		// validate
		boolean validationResult = this.testedInstance.validatePassword(
				subject, password);
		assertTrue(validationResult);

		try {
			this.testedInstance.setPassword(subject, password);
			fail();
		} catch (PermissionDeniedException e) {
			// empty
		}

	}

	@Test
	public void testChangePassword() throws Exception {

		// prepare
		UUID subjectUUID = UUID.randomUUID();
		SubjectEntity subject = this.subjectDAO.addSubject(subjectUUID
				.toString());
		String password = "password";
		String newPassword = "newpassword";

		// operate
		this.testedInstance.setPassword(subject, password);
		this.testedInstance.changePassword(subject, password, newPassword);

		// validate
		boolean validationResult = this.testedInstance.validatePassword(
				subject, password);
		assertFalse(validationResult);

		validationResult = this.testedInstance.validatePassword(subject,
				newPassword);
		assertTrue(validationResult);
	}

	@Test
	public void validatePassword() throws Exception {

		// prepare
		UUID subjectUUID = UUID.randomUUID();
		SubjectEntity subject = this.subjectDAO.addSubject(subjectUUID
				.toString());
		String password = "password";

		// operate
		this.testedInstance.setPassword(subject, password);

		// validate
		boolean validationResult = this.testedInstance.validatePassword(
				subject, password);
		assertTrue(validationResult);
	}

	@Test
	public void testIsPasswordConfigured() throws Exception {

		// prepare
		UUID subjectUUID = UUID.randomUUID();
		SubjectEntity subject = this.subjectDAO.addSubject(subjectUUID
				.toString());
		String password = "password";

		// operate
		this.testedInstance.setPassword(subject, password);

		// validate
		boolean validationResult = this.testedInstance
				.isPasswordConfigured(subject);
		assertTrue(validationResult);
	}

}