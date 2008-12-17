package test.unit.net.link.safeonline.encap.webapp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.UUID;

import net.link.safeonline.authentication.exception.MobileRegistrationException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.encap.webapp.AuthenticationPage;
import net.link.safeonline.encap.webapp.EncapApplication;
import net.link.safeonline.encap.webapp.MainPage;
import net.link.safeonline.encap.webapp.RegisterPage;
import net.link.safeonline.helpdesk.HelpdeskManager;
import net.link.safeonline.model.encap.EncapDeviceService;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.webapp.template.TemplatePage;
import net.link.safeonline.wicket.test.UrlPageSource;
import net.link.safeonline.wicket.tools.WicketUtil;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RegisterPageTest {

    private JndiTestUtils        jndiTestUtils;
    private EncapDeviceService   mockEncapDeviceService;
    private SamlAuthorityService mockSamlAuthorityService;
    private HelpdeskManager      mockHelpdeskManager;
    private WicketTester         wicket;

    private static final String  TEST_USERID     = UUID.randomUUID().toString();
    private static final String  TEST_MOBILE     = "0523012295";
    private static final String  TEST_ACTIVATION = "0123456789";
    private static final String  TEST_CHALLENGE  = "0123456789";
    private static final String  TEST_OTP        = "000000";


    @Before
    public void setUp()
            throws Exception {

        jndiTestUtils = new JndiTestUtils();
        jndiTestUtils.setUp();

        mockEncapDeviceService = createMock(EncapDeviceService.class);
        mockSamlAuthorityService = createMock(SamlAuthorityService.class);
        mockHelpdeskManager = createMock(HelpdeskManager.class);

        WicketUtil.setUnitTesting(true);
        wicket = new WicketTester(new EncapTestApplication());
        wicket.processRequestCycle();
    }

    @After
    public void tearDown()
            throws Exception {

        jndiTestUtils.tearDown();
    }

    /**
     * Sets wicket up to begin authentication and injects the Encap device service, SAML authority service and HelpDesk service.
     * 
     * @return The {@link FormTester} for the authentication for on the authentication page.
     */
    private FormTester prepareRegistration()
            throws Exception {

        // Initialize contexts.
        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(wicket.getServletSession());
        protocolContext.setSubject(TEST_USERID);
        protocolContext.setAttribute(TEST_MOBILE);

        // Load Authentication Page.
        wicket.assertRenderedPage(MainPage.class);
        wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REGISTER_ID, RegisterPage.class);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID, ExternalLink.class);

        // MainPage: Click to register encap
        wicket.clickLink(TemplatePage.CONTENT_ID + ":" + MainPage.REGISTER_ID);
        wicket.assertRenderedPage(RegisterPage.class);

        // Check whether our mount point also sends us to the registration page.
        wicket.startPage(new UrlPageSource(EncapApplication.REGISTRATION_MOUNTPOINT));
        wicket.assertRenderedPage(RegisterPage.class);

        // Inject EJBs.
        EJBTestUtils.inject(wicket.getLastRenderedPage(), mockEncapDeviceService);
        EJBTestUtils.inject(wicket.getLastRenderedPage(), mockSamlAuthorityService);
        jndiTestUtils.bindComponent(HelpdeskManager.JNDI_BINDING, mockHelpdeskManager);

        // Setup mocks.
        expect(mockSamlAuthorityService.getAuthnAssertionValidity()).andStubReturn(Integer.MAX_VALUE);
        expect(mockHelpdeskManager.getHelpdeskContextLimit()).andStubReturn(Integer.MAX_VALUE);

        // Return Authentication Form.
        return getRegistrationForm(wicket);
    }

    public static FormTester getRegistrationForm(WicketTester wicket) {

        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegisterPage.REGISTER_FORM_ID, Form.class);
        return wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegisterPage.REGISTER_FORM_ID);
    }

    @Test
    public void testRegisterEncap()
            throws Exception {

        // Setup.
        FormTester form = prepareRegistration();

        // Describe Expected Scenario.
        expect(mockEncapDeviceService.register(TEST_MOBILE, wicket.getServletSession().getId())).andStubReturn(TEST_ACTIVATION);
        expect(mockEncapDeviceService.requestOTP(TEST_MOBILE)).andStubReturn(TEST_CHALLENGE);
        expect(mockEncapDeviceService.authenticateEncap(TEST_CHALLENGE, TEST_OTP)).andStubReturn(true);
        mockEncapDeviceService.commitRegistration(TEST_USERID, TEST_MOBILE);
        replay(mockEncapDeviceService, mockSamlAuthorityService, mockHelpdeskManager);

        // Request activation code for our mobile.
        form.setValue(RegisterPage.MOBILE_FIELD_ID, TEST_MOBILE);
        form.submit(RegisterPage.ACTIVATE_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(RegisterPage.class);
        wicket.assertNoErrorMessage();

        // Proceed to authentication with finalize registration as goal.
        form = getRegistrationForm(wicket);
        form.submit(RegisterPage.REGISTER_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(AuthenticationPage.class);
        wicket.assertNoErrorMessage();

        // Setup Authentication Page.
        EJBTestUtils.inject(wicket.getLastRenderedPage(), mockEncapDeviceService);
        EJBTestUtils.inject(wicket.getLastRenderedPage(), mockSamlAuthorityService);

        // Request OTP for our mobile.
        form = AuthenticationPageTest.getAuthenticationForm(wicket);
        form.setValue(AuthenticationPage.MOBILE_FIELD_ID, TEST_MOBILE);
        form.submit(AuthenticationPage.CHALLENGE_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(AuthenticationPage.class);
        wicket.assertNoErrorMessage();

        // Specify our OTP and begin login -> finalize registration.
        form = AuthenticationPageTest.getAuthenticationForm(wicket);
        form.setValue(AuthenticationPage.OTP_FIELD_ID, TEST_OTP);
        form.submit(AuthenticationPage.LOGIN_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(AuthenticationPage.class);
        wicket.assertNoErrorMessage();
        verify(mockEncapDeviceService, mockSamlAuthorityService, mockHelpdeskManager);
    }

    @Test
    public void testRegisterFailed()
            throws Exception {

        // Setup.
        FormTester form = prepareRegistration();

        // Describe Expected Scenario.
        expect(mockEncapDeviceService.register(TEST_MOBILE, wicket.getServletSession().getId()))
                                                                                                .andThrow(new MobileRegistrationException());
        replay(mockEncapDeviceService, mockSamlAuthorityService, mockHelpdeskManager);

        // Request activation code for our mobile.
        form.setValue(RegisterPage.MOBILE_FIELD_ID, TEST_MOBILE);
        form.submit(RegisterPage.ACTIVATE_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(RegisterPage.class);
        wicket.assertErrorMessages(new String[] { "mobileRegistrationFailed" });
        verify(mockEncapDeviceService, mockSamlAuthorityService, mockHelpdeskManager);
    }

    @Test
    public void testRegisterSubjectNotFound()
            throws Exception {

        // Setup.
        FormTester form = prepareRegistration();

        // Describe Expected Scenario.
        expect(mockEncapDeviceService.register(TEST_MOBILE, wicket.getServletSession().getId())).andStubReturn(TEST_ACTIVATION);
        expect(mockEncapDeviceService.requestOTP(TEST_MOBILE)).andStubReturn(TEST_CHALLENGE);
        expect(mockEncapDeviceService.authenticateEncap(TEST_CHALLENGE, TEST_OTP)).andStubReturn(true);
        mockEncapDeviceService.commitRegistration(TEST_USERID, TEST_MOBILE);
        expectLastCall().andThrow(new SubjectNotFoundException());
        replay(mockEncapDeviceService, mockSamlAuthorityService, mockHelpdeskManager);

        // Request activation code for our mobile.
        form.setValue(RegisterPage.MOBILE_FIELD_ID, TEST_MOBILE);
        form.submit(RegisterPage.ACTIVATE_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(RegisterPage.class);
        wicket.assertNoErrorMessage();

        // Proceed to authentication with finalize registration as goal.
        form = getRegistrationForm(wicket);
        form.submit(RegisterPage.REGISTER_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(AuthenticationPage.class);
        wicket.assertNoErrorMessage();

        // Setup Authentication Page.
        EJBTestUtils.inject(wicket.getLastRenderedPage(), mockEncapDeviceService);
        EJBTestUtils.inject(wicket.getLastRenderedPage(), mockSamlAuthorityService);

        // Request OTP for our mobile.
        form = AuthenticationPageTest.getAuthenticationForm(wicket);
        form.setValue(AuthenticationPage.MOBILE_FIELD_ID, TEST_MOBILE);
        form.submit(AuthenticationPage.CHALLENGE_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(AuthenticationPage.class);
        wicket.assertNoErrorMessage();

        // Specify our OTP and begin login -> finalize registration.
        form = AuthenticationPageTest.getAuthenticationForm(wicket);
        form.setValue(AuthenticationPage.OTP_FIELD_ID, TEST_OTP);
        form.submit(AuthenticationPage.LOGIN_BUTTON_ID);

        // Check for errors.
        wicket.assertRenderedPage(AuthenticationPage.class);
        wicket.assertErrorMessages(new String[] { "errorSubjectNotFound" });
        verify(mockEncapDeviceService, mockSamlAuthorityService, mockHelpdeskManager);
    }
}
