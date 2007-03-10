/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.test.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.Local;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.security.auth.Subject;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

/**
 * Util class for EJB3 unit testing.
 * 
 * @author fcorneli
 * 
 */
public final class EJBTestUtils {

	private static final Log LOG = LogFactory.getLog(EJBTestUtils.class);

	private EJBTestUtils() {
		// empty
	}

	/**
	 * Injects a value into a given object.
	 * 
	 * @param fieldName
	 *            the name of the field to set.
	 * @param object
	 *            the object on which to inject the value.
	 * @param value
	 *            the value to inject.
	 * @throws Exception
	 */
	public static void inject(String fieldName, Object object, Object value)
			throws Exception {
		if (null == fieldName) {
			throw new IllegalArgumentException("field name should not be null");
		}
		if (null == value) {
			throw new IllegalArgumentException("the value should not be null");
		}
		if (null == object) {
			throw new IllegalArgumentException("the object should not be null");
		}
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(object, value);
	}

	/**
	 * Injects a resource value into an object.
	 * 
	 * @param bean
	 *            the bean object on which to inject a value.
	 * @param resourceName
	 *            the source name.
	 * @param value
	 *            the value to inject.
	 */
	public static void injectResource(Object bean, String resourceName,
			Object value) throws Exception {
		if (null == bean) {
			throw new IllegalArgumentException(
					"the bean object should not be null");
		}
		if (null == resourceName) {
			throw new IllegalArgumentException(
					"the resource name should not be null");
		}
		if (null == value) {
			throw new IllegalArgumentException(
					"the value object should not be null");
		}
		Class beanClass = bean.getClass();
		Field[] fields = beanClass.getDeclaredFields();
		for (Field field : fields) {
			Resource resourceAnnotation = field.getAnnotation(Resource.class);
			if (null == resourceAnnotation) {
				continue;
			}
			if (!resourceName.equals(resourceAnnotation.name())) {
				continue;
			}
			field.setAccessible(true);
			field.set(bean, value);
			return;
		}
		throw new IllegalArgumentException("resource field not found");
	}

	/**
	 * Injects a value object into a given bean object.
	 * 
	 * @param object
	 *            the bean object in which to inject.
	 * @param value
	 *            the value object to inject.
	 * @throws Exception
	 */
	public static void inject(Object object, Object value) throws Exception {
		if (null == value) {
			throw new IllegalArgumentException("the value should not be null");
		}
		if (null == object) {
			throw new IllegalArgumentException("the object should not be null");
		}
		Field[] fields = object.getClass().getDeclaredFields();
		Field selectedField = null;
		for (Field field : fields) {
			if (field.getType().isInstance(value)) {
				if (null != selectedField) {
					throw new IllegalStateException(
							"two field found of same injection type");
				}
				selectedField = field;
			}
		}
		if (null == selectedField) {
			throw new IllegalStateException("field of injection type not found");
		}
		selectedField.setAccessible(true);
		selectedField.set(object, value);
	}

	/**
	 * Initializes a bean.
	 * 
	 * @param bean
	 *            the bean to initialize.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	public static void init(Object bean) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		LOG.debug("Initializing: " + bean);
		Class clazz = bean.getClass();
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			PostConstruct postConstruct = method
					.getAnnotation(PostConstruct.class);
			if (null == postConstruct) {
				continue;
			}
			method.invoke(bean, new Object[] {});
		}
	}

	public static void setJBossPrincipal(String principalName, String role) {
		Principal principal = new SimplePrincipal(principalName);
		SecurityAssociation.setPrincipal(principal);
		Subject subject = new Subject();
		subject.getPrincipals().add(principal);
		SimpleGroup rolesGroup = new SimpleGroup("Roles");
		rolesGroup.addMember(new SimplePrincipal(role));
		subject.getPrincipals().add(rolesGroup);
		SecurityAssociation.setSubject(subject);
	}

	public static <Type> Type newInstance(Class<Type> clazz, Class[] container,
			EntityManager entityManager) {
		return newInstance(clazz, container, entityManager, (String) null);
	}

	public static <Type> Type newInstance(Class<Type> clazz, Class[] container,
			EntityManager entityManager, String callerPrincipalName, String role) {
		TestSessionContext testSessionContext = new TestSessionContext(
				callerPrincipalName, role);
		return newInstance(clazz, container, entityManager, testSessionContext);
	}

	public static <Type> Type newInstance(Class<Type> clazz, Class[] container,
			EntityManager entityManager, String callerPrincipalName) {
		TestSessionContext testSessionContext = new TestSessionContext(
				callerPrincipalName, null);
		return newInstance(clazz, container, entityManager, testSessionContext);
	}

	@SuppressWarnings("unchecked")
	public static <Type> Type newInstance(Class<Type> clazz, Class[] container,
			EntityManager entityManager, SessionContext sessionContext) {
		Type instance;
		try {
			instance = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("instantiation error");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("illegal access error");
		}
		TestContainerMethodInterceptor testContainerMethodInterceptor = new TestContainerMethodInterceptor(
				instance, container, entityManager, sessionContext);
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(testContainerMethodInterceptor);
		Type object = (Type) enhancer.create();
		return object;
	}

	/**
	 * Test EJB3 Container method interceptor. Be careful here not to start
	 * writing an entire EJB3 container.
	 * 
	 * @author fcorneli
	 * 
	 */
	private static class TestContainerMethodInterceptor implements
			MethodInterceptor {

		private static final Log LOG = LogFactory
				.getLog(TestContainerMethodInterceptor.class);

		private final Object object;

		private final Class[] container;

		private final EntityManager entityManager;

		private final SessionContext sessionContext;

		public TestContainerMethodInterceptor(Object object, Class[] container,
				EntityManager entityManager, SessionContext sessionContext) {
			this.object = object;
			this.container = container;
			this.entityManager = entityManager;
			this.sessionContext = sessionContext;
		}

		public Object intercept(Object obj, Method method, Object[] args,
				MethodProxy proxy) throws Throwable {
			checkSessionBean();
			Class clazz = this.object.getClass();
			checkSecurity(clazz, method);
			injectDependencies(clazz);
			injectEntityManager(clazz);
			injectResources(clazz);
			manageTransaction(method);
			try {
				Object result = method.invoke(this.object, args);
				return result;
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

		private void manageTransaction(Method method) {
			TransactionAttribute transactionAttributeAnnotation = method
					.getAnnotation(TransactionAttribute.class);
			if (null == transactionAttributeAnnotation) {
				return;
			}
			TransactionAttributeType transactionAttributeType = transactionAttributeAnnotation
					.value();
			switch (transactionAttributeType) {
			case REQUIRES_NEW:
				EntityTransaction entityTransaction = this.entityManager
						.getTransaction();
				/*
				 * The following is not 100% correct, but will do for most of
				 * the tests.
				 */
				LOG.debug("transaction management: REQUIRED_NEW");
				entityTransaction.commit();
				entityTransaction.begin();
				break;
			default:
				break;
			}
		}

		@SuppressWarnings("unchecked")
		private void checkSecurity(Class clazz, Method method) {
			SecurityDomain securityDomainAnnotation = (SecurityDomain) clazz
					.getAnnotation(SecurityDomain.class);
			if (null == securityDomainAnnotation) {
				return;
			}
			LOG.debug("security domain: " + securityDomainAnnotation.value());
			Principal callerPrincipal = this.sessionContext
					.getCallerPrincipal();
			if (null == callerPrincipal) {
				throw new EJBException("caller principal should not be null");
			}
			LOG.debug("caller principal: " + callerPrincipal.getName());
			RolesAllowed rolesAllowedAnnotation = method
					.getAnnotation(RolesAllowed.class);
			if (rolesAllowedAnnotation == null) {
				return;
			}
			String[] roles = rolesAllowedAnnotation.value();
			LOG.debug("number of roles: " + roles.length);
			for (String role : roles) {
				LOG.debug("checking role: " + role);
				if (true == this.sessionContext.isCallerInRole(role)) {
					return;
				}
			}
			StringBuffer message = new StringBuffer();
			message
					.append("user is not allowed to invoke the method. [allowed roles: ");
			for (String role : roles) {
				message.append("\"" + role + "\" ");
			}
			message.append("]");
			throw new EJBException(message.toString());
		}

		@SuppressWarnings("unchecked")
		private void checkSessionBean() {
			Class clazz = this.object.getClass();
			Stateless statelessAnnotation = (Stateless) clazz
					.getAnnotation(Stateless.class);
			if (null == statelessAnnotation) {
				throw new EJBException("no @Stateless annotation found");
			}
		}

		private void injectResources(Class clazz) {
			if (false == clazz.equals(Object.class)) {
				injectResources(clazz.getSuperclass());
			}
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				Resource resourceAnnotation = field
						.getAnnotation(Resource.class);
				if (null == resourceAnnotation) {
					continue;
				}
				Class fieldType = field.getType();
				if (true == SessionContext.class.isAssignableFrom(fieldType)) {
					setField(field, this.sessionContext);
					return;
				}
				if (true == TimerService.class.isAssignableFrom(fieldType)) {
					TimerService testTimerService = new TestTimerService();
					setField(field, testTimerService);
					return;
				}
				throw new EJBException("unsupported resource type: "
						+ fieldType.getName());
			}
		}

		@SuppressWarnings("unchecked")
		private void injectDependencies(Class clazz) {
			if (false == clazz.equals(Object.class)) {
				injectDependencies(clazz.getSuperclass());
			}
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				EJB ejbAnnotation = field.getAnnotation(EJB.class);
				if (null == ejbAnnotation) {
					continue;
				}
				Class fieldType = field.getType();
				if (false == fieldType.isInterface()) {
					throw new EJBException("field is not an interface type");
				}
				Local localAnnotation = (Local) fieldType
						.getAnnotation(Local.class);
				if (null == localAnnotation) {
					throw new EJBException(
							"interface has no @Local annotation: "
									+ fieldType.getName());
				}
				Class beanType = getBeanType(fieldType);
				Object bean = EJBTestUtils
						.newInstance(beanType, this.container,
								this.entityManager, this.sessionContext);
				setField(field, bean);
			}
		}

		private void setField(Field field, Object value) {
			field.setAccessible(true);
			try {
				field.set(this.object, value);
			} catch (IllegalArgumentException e) {
				throw new EJBException("illegal argument error");
			} catch (IllegalAccessException e) {
				throw new EJBException("illegal access error");
			}
		}

		private void injectEntityManager(Class clazz) {
			if (false == clazz.equals(Object.class)) {
				injectEntityManager(clazz.getSuperclass());
			}
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				PersistenceContext persistenceContextAnnotation = field
						.getAnnotation(PersistenceContext.class);
				if (null == persistenceContextAnnotation) {
					continue;
				}
				Class fieldType = field.getType();
				if (false == EntityManager.class.isAssignableFrom(fieldType)) {
					throw new EJBException("field type not correct");
				}
				setField(field, this.entityManager);
			}
		}

		@SuppressWarnings("unchecked")
		private Class getBeanType(Class interfaceType) {
			for (Class containerClass : this.container) {
				if (false == interfaceType.isAssignableFrom(containerClass)) {
					continue;
				}
				return containerClass;
			}
			throw new EJBException("did not find a container class for type: "
					+ interfaceType.getName());
		}

	}

	private static class TestSessionContext implements SessionContext {

		private final Principal principal;

		private final String role;

		public TestSessionContext(String principalName, String role) {
			if (null != principalName) {
				this.principal = new SimplePrincipal(principalName);
				this.role = role;
			} else {
				this.principal = null;
				this.role = null;
			}
		}

		public Object getBusinessObject(Class arg0)
				throws IllegalStateException {
			return null;
		}

		public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
			return null;
		}

		public EJBObject getEJBObject() throws IllegalStateException {
			return null;
		}

		public Class getInvokedBusinessInterface() throws IllegalStateException {
			return null;
		}

		public MessageContext getMessageContext() throws IllegalStateException {
			return null;
		}

		@SuppressWarnings("deprecation")
		public java.security.Identity getCallerIdentity() {
			return null;
		}

		public Principal getCallerPrincipal() {
			if (null == this.principal) {
				throw new EJBException("caller principal not set");
			}
			return this.principal;
		}

		public EJBHome getEJBHome() {
			return null;
		}

		public EJBLocalHome getEJBLocalHome() {
			return null;
		}

		public Properties getEnvironment() {
			return null;
		}

		public boolean getRollbackOnly() throws IllegalStateException {
			return false;
		}

		public TimerService getTimerService() throws IllegalStateException {
			return null;
		}

		public UserTransaction getUserTransaction()
				throws IllegalStateException {
			return null;
		}

		@SuppressWarnings("deprecation")
		public boolean isCallerInRole(java.security.Identity arg0) {
			return false;
		}

		public boolean isCallerInRole(String expectedRole) {
			if (null == role) {
				return false;
			}
			if (true == role.equals(expectedRole)) {
				return true;
			}
			return false;
		}

		public Object lookup(String arg0) {
			return null;
		}

		public void setRollbackOnly() throws IllegalStateException {
		}
	}

	private static class TestTimer implements Timer {

		public void cancel() throws IllegalStateException,
				NoSuchObjectLocalException, EJBException {
		}

		public TimerHandle getHandle() throws IllegalStateException,
				NoSuchObjectLocalException, EJBException {
			return null;
		}

		public Serializable getInfo() throws IllegalStateException,
				NoSuchObjectLocalException, EJBException {
			return null;
		}

		public Date getNextTimeout() throws IllegalStateException,
				NoSuchObjectLocalException, EJBException {
			return null;
		}

		public long getTimeRemaining() throws IllegalStateException,
				NoSuchObjectLocalException, EJBException {
			return 0;
		}
	}

	private static class TestTimerService implements TimerService {

		private static final Log LOG = LogFactory
				.getLog(TestTimerService.class);

		public Timer createTimer(long arg0, Serializable arg1)
				throws IllegalArgumentException, IllegalStateException,
				EJBException {
			return null;
		}

		public Timer createTimer(Date arg0, Serializable arg1)
				throws IllegalArgumentException, IllegalStateException,
				EJBException {
			LOG.debug("createTimer");
			Timer testTimer = new TestTimer();
			return testTimer;
		}

		public Timer createTimer(long arg0, long arg1, Serializable arg2)
				throws IllegalArgumentException, IllegalStateException,
				EJBException {
			return null;
		}

		public Timer createTimer(Date arg0, long arg1, Serializable arg2)
				throws IllegalArgumentException, IllegalStateException,
				EJBException {
			return null;
		}

		public Collection getTimers() throws IllegalStateException,
				EJBException {
			return null;
		}
	}
}
