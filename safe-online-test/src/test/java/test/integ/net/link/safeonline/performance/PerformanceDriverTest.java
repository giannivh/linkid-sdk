package test.integ.net.link.safeonline.performance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import net.link.safeonline.model.performance.PerformanceService;
import net.link.safeonline.performance.drivers.AttribDriver;
import net.link.safeonline.performance.drivers.AuthDriver;
import net.link.safeonline.performance.drivers.IdMappingDriver;
import net.link.safeonline.performance.entity.DriverExceptionEntity;
import net.link.safeonline.performance.entity.DriverProfileEntity;
import net.link.safeonline.performance.entity.ExecutionEntity;
import net.link.safeonline.performance.entity.MeasurementEntity;
import net.link.safeonline.performance.entity.ProfileDataEntity;
import net.link.safeonline.performance.entity.StartTimeEntity;
import net.link.safeonline.performance.keystore.PerformanceKeyStoreUtils;
import net.link.safeonline.performance.service.bean.ExecutionServiceBean;
import net.link.safeonline.performance.service.bean.ProfilingServiceBean;
import net.link.safeonline.test.util.EntityTestManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author mbillemo
 *
 */
public class PerformanceDriverTest {

	static final Log LOG = LogFactory.getLog(PerformanceDriverTest.class);

	private static final String OLAS_HOSTNAME = "sebeco-dev-10:8443";
	// private static final String OLAS_HOSTNAME = "localhost:8443";

	private static final String testUser = "performance";
	private static final String testPass = "performance";
	private static PrivateKeyEntry applicationKey;

	static {

		Hashtable<String, String> environment = new Hashtable<String, String>();
		environment.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.jnp.interfaces.NamingContextFactory");
		environment.put(Context.PROVIDER_URL, "jnp://" + OLAS_HOSTNAME
				+ ":1099");
		try {
			PerformanceService service = (PerformanceService) new InitialContext(
					environment).lookup(PerformanceService.BINDING);
			applicationKey = new KeyStore.PrivateKeyEntry(service
					.getPrivateKey(), new Certificate[] { service
					.getCertificate() });
		} catch (Exception e) {
			LOG.error("application keys unavailable; will try local keystore.",
					e);
			applicationKey = PerformanceKeyStoreUtils.getPrivateKeyEntry();
		}
	}

	private AttribDriver attribDriver;
	private AuthDriver authDriver;
	private IdMappingDriver idDriver;

	private EntityTestManager entityTestManager;

	@Before
	public void setUp() {

		this.entityTestManager = new EntityTestManager();

		try {
			this.entityTestManager.setUp(DriverExceptionEntity.class,
					DriverProfileEntity.class, ExecutionEntity.class,
					MeasurementEntity.class, ProfileDataEntity.class,
					StartTimeEntity.class);

			ProfilingServiceBean.setDefaultEntityManager(this.entityTestManager
					.getEntityManager());

			ExecutionEntity execution = new ExecutionServiceBean()
					.addExecution(getClass().getName(), OLAS_HOSTNAME);

			this.idDriver = new IdMappingDriver(execution);
			this.attribDriver = new AttribDriver(execution);
			this.authDriver = new AuthDriver(execution);
		}

		catch (Exception e) {
			LOG.fatal("JPA annotations incorrect: " + e.getMessage(), e);
			throw new RuntimeException("JPA annotations incorrect: "
					+ e.getMessage(), e);
		}
	}

	@After
	public void tearDown() throws Exception {

		this.entityTestManager.tearDown();
	}

	@Test
	public void annotationCorrectness() throws Exception {

		assertNotNull("JPA annotations incorrect?", this.entityTestManager
				.getEntityManager());
	}

	@Test
	public void testAttrib() throws Exception {

		// User needs to authenticate before we can get to the attributes.
		String uuid = this.authDriver.login(applicationKey,
				"performance-application", testUser, testPass);

		getAttributes(applicationKey, uuid);
	}

	@Test
	public void testLogin() throws Exception {

		login(testUser, testPass);
	}

	@Test
	public void testMapping() throws Exception {

		getUserId(applicationKey, testUser);
	}

	private Map<String, Object> getAttributes(PrivateKeyEntry application,
			String uuid) throws Exception {

		// Get attributes for given UUID.
		Map<String, Object> attributes = this.attribDriver.getAttributes(
				application, uuid);

		// State assertions.
		assertNotNull(attributes);
		assertFalse(attributes.isEmpty());
		assertFalse(isEmptyOrOnlyNulls(this.attribDriver.getProfile()
				.getProfileData()));
		assertTrue(isEmptyOrOnlyNulls(this.attribDriver.getProfile()
				.getProfileError()));
		return attributes;

	}

	/**
	 * Get the UUID of the given username for the given application.
	 */
	private String getUserId(PrivateKeyEntry application, String username)
			throws Exception {

		String uuid = this.idDriver.getUserId(application, username);

		// State assertions.
		assertNotNull(uuid);
		assertNotSame("", uuid);
		assertFalse(isEmptyOrOnlyNulls(this.idDriver.getProfile()
				.getProfileData()));
		assertTrue(isEmptyOrOnlyNulls(this.idDriver.getProfile()
				.getProfileError()));

		return uuid;
	}

	private boolean isEmptyOrOnlyNulls(Collection<?> profileDataOrErrors) {

		if (profileDataOrErrors == null || profileDataOrErrors.isEmpty())
			return true;

		for (Object data : profileDataOrErrors)
			if (null != data)
				return false;

		return true;
	}

	private String login(String username, String password) throws Exception {

		// Authenticate User.
		String uuid = this.authDriver.login(applicationKey,
				"performance-application", username, password);

		// State assertions.
		assertNotNull(uuid);
		assertNotSame("", uuid);
		assertFalse(isEmptyOrOnlyNulls(this.authDriver.getProfile()
				.getProfileData()));
		assertTrue(isEmptyOrOnlyNulls(this.authDriver.getProfile()
				.getProfileError()));

		return uuid;

	}
}