package test.unit.net.link.safeonline.password.webapp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.UUID;

import junit.framework.TestCase;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.device.sdk.saml2.DeviceOperationType;
import net.link.safeonline.helpdesk.HelpdeskManager;
import net.link.safeonline.model.password.PasswordDeviceService;
import net.link.safeonline.password.webapp.RegistrationPage;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.JmxTestUtils;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.test.util.MBeanActionHandler;
import net.link.safeonline.test.util.PkiTestUtils;
import net.link.safeonline.util.ee.AuthIdentityServiceClient;
import net.link.safeonline.util.ee.IdentityServiceClient;
import net.link.safeonline.webapp.template.TemplatePage;
import net.link.safeonline.wicket.tools.WicketUtil;
import net.link.safeonline.wicket.tools.olas.DummyNameIdentifierMappingClient;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RegistrationPageTest extends TestCase {

    private PasswordDeviceService mockPasswordDeviceService;

    private SamlAuthorityService  mockSamlAuthorityService;

    private HelpdeskManager       mockHelpdeskManager;

    private WicketTester          wicket;

    private JndiTestUtils         jndiTestUtils;


    @Override
    @Before
    public void setUp()
            throws Exception {

        super.setUp();

        WicketUtil.setUnitTesting(true);

        this.jndiTestUtils = new JndiTestUtils();
        this.jndiTestUtils.setUp();

        this.mockPasswordDeviceService = createMock(PasswordDeviceService.class);
        this.mockSamlAuthorityService = createMock(SamlAuthorityService.class);
        this.mockHelpdeskManager = createMock(HelpdeskManager.class);

        // Initialize MBean's
        JmxTestUtils jmxTestUtils = new JmxTestUtils();
        jmxTestUtils.setUp(AuthIdentityServiceClient.AUTH_IDENTITY_SERVICE);

        final KeyPair authKeyPair = PkiTestUtils.generateKeyPair();
        final X509Certificate authCertificate = PkiTestUtils.generateSelfSignedCertificate(authKeyPair, "CN=Test");
        jmxTestUtils.registerActionHandler(AuthIdentityServiceClient.AUTH_IDENTITY_SERVICE, "getCertificate", new MBeanActionHandler() {

            public Object invoke(@SuppressWarnings("unused") Object[] arguments) {

                return authCertificate;
            }
        });

        jmxTestUtils.setUp(IdentityServiceClient.IDENTITY_SERVICE);

        final KeyPair keyPair = PkiTestUtils.generateKeyPair();
        final X509Certificate certificate = PkiTestUtils.generateSelfSignedCertificate(keyPair, "CN=Test");
        jmxTestUtils.registerActionHandler(IdentityServiceClient.IDENTITY_SERVICE, "getCertificate", new MBeanActionHandler() {

            public Object invoke(@SuppressWarnings("unused") Object[] arguments) {

                return certificate;
            }
        });

        this.wicket = new WicketTester(new PasswordTestApplication());

    }

    @Override
    @After
    public void tearDown()
            throws Exception {

        this.jndiTestUtils.tearDown();
    }

    @Test
    public void testRegister()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String password = "test-password";
        DummyNameIdentifierMappingClient.setUserId(userId);

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(this.wicket.getServletSession());
        protocolContext.setDeviceOperation(DeviceOperationType.NEW_ACCOUNT_REGISTER);
        protocolContext.setSubject(userId);

        // Registration Page: Verify.
        RegistrationPage registrationPage = (RegistrationPage) this.wicket.startPage(RegistrationPage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REGISTRATION_FORM_ID, Form.class);

        // setup
        EJBTestUtils.inject(registrationPage, this.mockPasswordDeviceService);
        EJBTestUtils.inject(registrationPage, this.mockSamlAuthorityService);

        // stubs
        this.mockPasswordDeviceService.register(userId, password);
        expect(this.mockSamlAuthorityService.getAuthnAssertionValidity()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(this.mockPasswordDeviceService, this.mockSamlAuthorityService);

        // operate
        FormTester registrationForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REGISTRATION_FORM_ID);
        registrationForm.setValue(RegistrationPage.PASSWORD1_FIELD_ID, password);
        registrationForm.setValue(RegistrationPage.PASSWORD2_FIELD_ID, password);
        registrationForm.submit(RegistrationPage.SAVE_BUTTON_ID);

        // verify
        verify(this.mockPasswordDeviceService, this.mockSamlAuthorityService);
    }

    @Test
    public void testRegisterPasswordsNotEqual()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String password = "test-password";
        DummyNameIdentifierMappingClient.setUserId(userId);

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(this.wicket.getServletSession());
        protocolContext.setDeviceOperation(DeviceOperationType.NEW_ACCOUNT_REGISTER);
        protocolContext.setSubject(userId);

        // Registration Page: Verify.
        RegistrationPage registrationPage = (RegistrationPage) this.wicket.startPage(RegistrationPage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REGISTRATION_FORM_ID, Form.class);

        // setup
        EJBTestUtils.inject(registrationPage, this.mockPasswordDeviceService);
        this.jndiTestUtils.bindComponent(HelpdeskManager.JNDI_BINDING, this.mockHelpdeskManager);

        // stubs
        expect(this.mockHelpdeskManager.getHelpdeskContextLimit()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(this.mockPasswordDeviceService, this.mockHelpdeskManager);

        // operate
        FormTester registrationForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REGISTRATION_FORM_ID);
        registrationForm.setValue(RegistrationPage.PASSWORD1_FIELD_ID, password);
        registrationForm.setValue(RegistrationPage.PASSWORD2_FIELD_ID, "foobar-password");
        registrationForm.submit(RegistrationPage.SAVE_BUTTON_ID);

        // verify
        verify(this.mockPasswordDeviceService, this.mockHelpdeskManager);

        this.wicket.assertRenderedPage(RegistrationPage.class);
        this.wicket.assertErrorMessages(new String[] { RegistrationPage.PASSWORD2_FIELD_ID + ".EqualPasswordInputValidator" });

    }
}
