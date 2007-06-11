/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.integ.net.link.safeonline.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.integ.net.link.safeonline.IntegrationTestUtils.getApplicationService;
import static test.integ.net.link.safeonline.IntegrationTestUtils.getAttributeTypeService;
import static test.integ.net.link.safeonline.IntegrationTestUtils.getIdentityService;
import static test.integ.net.link.safeonline.IntegrationTestUtils.getPkiService;
import static test.integ.net.link.safeonline.IntegrationTestUtils.getSubscriptionService;
import static test.integ.net.link.safeonline.IntegrationTestUtils.getUserRegistrationService;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.naming.InitialContext;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.authentication.service.ApplicationService;
import net.link.safeonline.authentication.service.AttributeDO;
import net.link.safeonline.authentication.service.IdentityAttributeTypeDO;
import net.link.safeonline.authentication.service.IdentityService;
import net.link.safeonline.authentication.service.SubscriptionService;
import net.link.safeonline.authentication.service.UserRegistrationService;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.attrib.AttributeClientImpl;
import net.link.safeonline.service.AttributeTypeService;
import net.link.safeonline.service.PkiService;
import net.link.safeonline.test.util.DomTestUtils;
import net.link.safeonline.test.util.PkiTestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import test.integ.net.link.safeonline.IntegrationTestUtils;

/**
 * Integration tests for SafeOnline attribute web service.
 * 
 * @author fcorneli
 * 
 */
public class AttributeWebServiceTest {

	private static final Log LOG = LogFactory.getLog(DataWebServiceTest.class);

	private X509Certificate certificate;

	private AttributeClient attributeClient;

	@Before
	public void setUp() throws Exception {

		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		this.certificate = PkiTestUtils.generateSelfSignedCertificate(keyPair,
				"CN=Test");

		this.attributeClient = new AttributeClientImpl("localhost",
				this.certificate, keyPair.getPrivate());
	}

	@Test
	public void attributeService() throws Exception {
		// setup
		InitialContext initialContext = IntegrationTestUtils
				.getInitialContext();

		IntegrationTestUtils.setupLoginConfig();

		UserRegistrationService userRegistrationService = getUserRegistrationService(initialContext);
		IdentityService identityService = getIdentityService(initialContext);

		String testName = "test-name-" + UUID.randomUUID().toString();
		String testApplicationName = UUID.randomUUID().toString();

		String testAttributeName = "attr-" + UUID.randomUUID().toString();
		String testAttributeValue = "test-attribute-value";

		// operate: register user
		String login = "login-" + UUID.randomUUID().toString();
		String password = "pwd-" + UUID.randomUUID().toString();
		userRegistrationService.registerUser(login, password, null);

		// operate: save name attribute
		IntegrationTestUtils.login(login, password);
		AttributeDO attribute = new AttributeDO(
				SafeOnlineConstants.NAME_ATTRIBUTE, "string");
		attribute.setStringValue(testName);
		identityService.saveAttribute(attribute);

		// operate: register new attribute type
		AttributeTypeService attributeTypeService = getAttributeTypeService(initialContext);
		IntegrationTestUtils.login("admin", "admin");
		AttributeTypeEntity attributeType = new AttributeTypeEntity(
				testAttributeName, SafeOnlineConstants.STRING_TYPE, true, true);
		attributeTypeService.add(attributeType);

		// operate: register certificate as application trust point
		PkiService pkiService = getPkiService(initialContext);
		IntegrationTestUtils.login("admin", "admin");
		pkiService.addTrustPoint(
				SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN,
				this.certificate.getEncoded());

		// operate: add application with certificate
		ApplicationService applicationService = getApplicationService(initialContext);
		applicationService.addApplication(testApplicationName, "owner", null,
				this.certificate.getEncoded(),
				Arrays.asList(new IdentityAttributeTypeDO[] {
						new IdentityAttributeTypeDO(
								SafeOnlineConstants.NAME_ATTRIBUTE),
						new IdentityAttributeTypeDO(testAttributeName) }));

		// operate: subscribe onto the application and confirm identity usage
		SubscriptionService subscriptionService = getSubscriptionService(initialContext);
		IntegrationTestUtils.login(login, password);
		subscriptionService.subscribe(testApplicationName);
		identityService.confirmIdentity(testApplicationName);

		// operate: retrieve name attribute via web service
		String result = this.attributeClient.getAttributeValue(login,
				SafeOnlineConstants.NAME_ATTRIBUTE, String.class);

		// verify
		LOG.debug("result attribute value: " + result);
		LOG.debug("application name: " + testApplicationName);
		assertEquals(testName, result);

		// operate: retrieve all accessible attributes.
		Map<String, Object> resultAttributes = this.attributeClient
				.getAttributeValues(login);

		// verify
		assertEquals(2, resultAttributes.size());
		LOG.info("resultAttributes: " + resultAttributes);
		result = (String) resultAttributes
				.get(SafeOnlineConstants.NAME_ATTRIBUTE);
		assertEquals(testName, result);
		assertNull(resultAttributes.get(testAttributeName));

		// operate: set attribute
		IntegrationTestUtils.login(login, password);
		AttributeDO attributeDO = new AttributeDO(testAttributeName,
				SafeOnlineConstants.STRING_TYPE);
		attributeDO.setStringValue(testAttributeValue);
		identityService.saveAttribute(attributeDO);

		String resultValue = this.attributeClient.getAttributeValue(login,
				testAttributeName, String.class);
		assertEquals(testAttributeValue, resultValue);

		// operate: retrieve all attributes
		resultAttributes = this.attributeClient.getAttributeValues(login);
		LOG.info("resultAttributes: " + resultAttributes);
		assertEquals(2, resultAttributes.size());
		assertEquals(testAttributeValue, resultAttributes
				.get(testAttributeName));
	}

	@Test
	public void nullAttributeValue() throws Exception {
		// setup
		InitialContext initialContext = IntegrationTestUtils
				.getInitialContext();

		IntegrationTestUtils.setupLoginConfig();

		UserRegistrationService userRegistrationService = getUserRegistrationService(initialContext);
		IdentityService identityService = getIdentityService(initialContext);

		String testApplicationName = UUID.randomUUID().toString();

		String testAttributeName = "attr-" + UUID.randomUUID().toString();

		// operate: register user
		String login = "login-" + UUID.randomUUID().toString();
		String password = "pwd-" + UUID.randomUUID().toString();
		userRegistrationService.registerUser(login, password, null);

		// operate: register new attribute type
		AttributeTypeService attributeTypeService = getAttributeTypeService(initialContext);
		IntegrationTestUtils.login("admin", "admin");
		AttributeTypeEntity attributeType = new AttributeTypeEntity(
				testAttributeName, SafeOnlineConstants.STRING_TYPE, true, true);
		attributeTypeService.add(attributeType);

		// operate: register certificate as application trust point
		PkiService pkiService = getPkiService(initialContext);
		IntegrationTestUtils.login("admin", "admin");
		pkiService.addTrustPoint(
				SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN,
				this.certificate.getEncoded());

		// operate: add application with certificate
		ApplicationService applicationService = getApplicationService(initialContext);
		applicationService.addApplication(testApplicationName, "owner", null,
				this.certificate.getEncoded(),
				Arrays.asList(new IdentityAttributeTypeDO[] {
						new IdentityAttributeTypeDO(
								SafeOnlineConstants.NAME_ATTRIBUTE),
						new IdentityAttributeTypeDO(testAttributeName) }));

		// operate: subscribe onto the application and confirm identity usage
		SubscriptionService subscriptionService = getSubscriptionService(initialContext);
		IntegrationTestUtils.login(login, password);
		subscriptionService.subscribe(testApplicationName);
		identityService.confirmIdentity(testApplicationName);

		// operate: retrieve attribute via web service
		this.attributeClient.setCaptureMessages(true);
		String result = this.attributeClient.getAttributeValue(login,
				testAttributeName, String.class);

		// verify
		LOG.debug("result message: "
				+ DomTestUtils.domToString(this.attributeClient
						.getInboundMessage()));
		LOG.debug("result attribute value: " + result);
		assertNull(result);

		// operate: remove attribute
		AttributeDO attribute = new AttributeDO(testAttributeName,
				SafeOnlineConstants.STRING_TYPE, true, -1, null, null, true,
				true, null, null);
		identityService.removeAttribute(attribute);

		// operate: retrieve the missing attribute via attrib web service
		try {
			this.attributeClient.getAttributeValue(login, testAttributeName,
					String.class);
			fail();
		} catch (AttributeNotFoundException e) {
			// expected
		}
	}

	@Test
	public void retrievingMultivaluedAttributes() throws Exception {
		// setup
		InitialContext initialContext = IntegrationTestUtils
				.getInitialContext();

		IntegrationTestUtils.setupLoginConfig();

		UserRegistrationService userRegistrationService = getUserRegistrationService(initialContext);
		IdentityService identityService = getIdentityService(initialContext);

		String testApplicationName = UUID.randomUUID().toString();

		String testAttributeName = "attr-" + UUID.randomUUID().toString();

		// operate: register user
		String login = "login-" + UUID.randomUUID().toString();
		String password = "pwd-" + UUID.randomUUID().toString();
		userRegistrationService.registerUser(login, password, null);

		// operate: register new multivalued attribute type
		AttributeTypeService attributeTypeService = getAttributeTypeService(initialContext);
		IntegrationTestUtils.login("admin", "admin");
		AttributeTypeEntity attributeType = new AttributeTypeEntity(
				testAttributeName, SafeOnlineConstants.STRING_TYPE, true, true);
		attributeType.setMultivalued(true);
		attributeTypeService.add(attributeType);

		// operate: add multivalued attributes
		IntegrationTestUtils.login(login, password);
		String attributeValue1 = "value 1";
		AttributeDO attribute1 = new AttributeDO(testAttributeName,
				SafeOnlineConstants.STRING_TYPE, true, -1, null, null, true,
				true, attributeValue1, null);
		identityService.addAttribute(attribute1);

		String attributeValue2 = "value 2";
		AttributeDO attribute2 = new AttributeDO(testAttributeName,
				SafeOnlineConstants.STRING_TYPE, true, -1, null, null, true,
				true, attributeValue2, null);
		identityService.addAttribute(attribute2);

		// operate: register certificate as application trust point
		PkiService pkiService = getPkiService(initialContext);
		IntegrationTestUtils.login("admin", "admin");
		pkiService.addTrustPoint(
				SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN,
				this.certificate.getEncoded());

		// operate: add application with certificate
		ApplicationService applicationService = getApplicationService(initialContext);
		applicationService.addApplication(testApplicationName, "owner", null,
				this.certificate.getEncoded(),
				Arrays.asList(new IdentityAttributeTypeDO[] {
						new IdentityAttributeTypeDO(
								SafeOnlineConstants.NAME_ATTRIBUTE),
						new IdentityAttributeTypeDO(testAttributeName) }));

		// operate: subscribe onto the application and confirm identity usage
		SubscriptionService subscriptionService = getSubscriptionService(initialContext);
		IntegrationTestUtils.login(login, password);
		subscriptionService.subscribe(testApplicationName);
		identityService.confirmIdentity(testApplicationName);

		// operate: retrieve name attribute via web service
		this.attributeClient.setCaptureMessages(true);
		String[] result = this.attributeClient.getAttributeValue(login,
				testAttributeName, String[].class);

		// verify
		Document resultDocument = this.attributeClient.getInboundMessage();
		LOG
				.debug("result message: "
						+ DomTestUtils.domToString(resultDocument));
		LOG.debug("result: " + result);

		// verify number of attribute values returned.
		Element nsElement = resultDocument.createElement("nsElement");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:soap",
				"http://schemas.xmlsoap.org/soap/envelope/");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:samlp",
				"urn:oasis:names:tc:SAML:2.0:protocol");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:saml",
				"urn:oasis:names:tc:SAML:2.0:assertion");
		XObject xObject = XPathAPI
				.eval(
						resultDocument,
						"count(/soap:Envelope/soap:Body/samlp:Response/saml:Assertion/saml:AttributeStatement/saml:Attribute/saml:AttributeValue)",
						nsElement);
		double countResult = xObject.num();
		LOG.debug("count result: " + countResult);
		assertEquals(2.0, countResult);
		assertTrue(contains(attributeValue1, result));
		assertTrue(contains(attributeValue2, result));
		assertFalse(contains("foo-bar", result));
	}

	private boolean contains(String value, Object[] items) {
		for (Object item : items) {
			if (value.equals(item)) {
				return true;
			}
		}
		return false;
	}
}