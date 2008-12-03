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
import net.link.safeonline.demo.wicket.tools.WicketUtil;
import net.link.safeonline.demo.wicket.tools.olas.DummyNameIdentifierMappingClient;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.helpdesk.HelpdeskManager;
import net.link.safeonline.model.password.PasswordDeviceService;
import net.link.safeonline.password.webapp.UpdatePage;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.JmxTestUtils;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.test.util.MBeanActionHandler;
import net.link.safeonline.test.util.PkiTestUtils;
import net.link.safeonline.util.ee.AuthIdentityServiceClient;
import net.link.safeonline.util.ee.IdentityServiceClient;
import net.link.safeonline.webapp.template.TemplatePage;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class UpdatePageTest extends TestCase {

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
    public void testUpdate()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String oldPassword = "test-old-password";
        String newPassword = "test-new-password";
        DummyNameIdentifierMappingClient.setUserId(userId);

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(this.wicket.getServletSession());
        protocolContext.setSubject(userId);

        // Update Page: Verify.
        UpdatePage updatePage = (UpdatePage) this.wicket.startPage(UpdatePage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + UpdatePage.UPDATE_FORM_ID, Form.class);

        // setup
        EJBTestUtils.inject(updatePage, this.mockPasswordDeviceService);
        EJBTestUtils.inject(updatePage, this.mockSamlAuthorityService);

        // stubs
        this.mockPasswordDeviceService.update(userId, oldPassword, newPassword);
        expect(this.mockSamlAuthorityService.getAuthnAssertionValidity()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(this.mockPasswordDeviceService, this.mockSamlAuthorityService);

        // operate
        FormTester updateForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + UpdatePage.UPDATE_FORM_ID);
        updateForm.setValue(UpdatePage.OLDPASSWORD_FIELD_ID, oldPassword);
        updateForm.setValue(UpdatePage.PASSWORD1_FIELD_ID, newPassword);
        updateForm.setValue(UpdatePage.PASSWORD2_FIELD_ID, newPassword);
        updateForm.submit(UpdatePage.SAVE_BUTTON_ID);

        // verify
        verify(this.mockPasswordDeviceService, this.mockSamlAuthorityService);
    }

    @Test
    public void testUpdatePasswordsNotEqual()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String oldPassword = "test-old-password";
        String newPassword = "test-new-password";
        DummyNameIdentifierMappingClient.setUserId(userId);

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(this.wicket.getServletSession());
        protocolContext.setSubject(userId);

        // Update Page: Verify.
        UpdatePage updatePage = (UpdatePage) this.wicket.startPage(UpdatePage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + UpdatePage.UPDATE_FORM_ID, Form.class);

        // setup
        EJBTestUtils.inject(updatePage, this.mockPasswordDeviceService);
        this.jndiTestUtils.bindComponent(HelpdeskManager.JNDI_BINDING, this.mockHelpdeskManager);

        // stubs
        expect(this.mockHelpdeskManager.getHelpdeskContextLimit()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(this.mockPasswordDeviceService, this.mockHelpdeskManager);

        // operate
        FormTester updateForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + UpdatePage.UPDATE_FORM_ID);
        updateForm.setValue(UpdatePage.OLDPASSWORD_FIELD_ID, oldPassword);
        updateForm.setValue(UpdatePage.PASSWORD1_FIELD_ID, newPassword);
        updateForm.setValue(UpdatePage.PASSWORD2_FIELD_ID, "foobar-password");
        updateForm.submit(UpdatePage.SAVE_BUTTON_ID);

        // verify
        verify(this.mockPasswordDeviceService, this.mockHelpdeskManager);

        this.wicket.assertRenderedPage(UpdatePage.class);
        this.wicket.assertErrorMessages(new String[] { UpdatePage.PASSWORD2_FIELD_ID + ".EqualPasswordInputValidator" });

    }
}