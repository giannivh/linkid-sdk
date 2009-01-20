package test.unit.net.link.safeonline.otpoversms.webapp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.ConnectException;
import java.util.UUID;

import junit.framework.TestCase;
import net.link.safeonline.audit.SecurityAuditLogger;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.wicket.tools.WicketUtil;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.device.sdk.saml2.DeviceOperationType;
import net.link.safeonline.helpdesk.HelpdeskManager;
import net.link.safeonline.model.otpoversms.OtpOverSmsDeviceService;
import net.link.safeonline.otpoversms.webapp.RegistrationPage;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.webapp.template.TemplatePage;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RegistrationPageTest extends TestCase {

    private OtpOverSmsDeviceService mockOtpOverSmsDeviceService;

    private SamlAuthorityService    mockSamlAuthorityService;

    private HelpdeskManager         mockHelpdeskManager;

    private SecurityAuditLogger     mockSecurityAuditLogger;

    private WicketTester            wicket;

    private JndiTestUtils           jndiTestUtils;


    @Override
    @Before
    public void setUp()
            throws Exception {

        super.setUp();

        WicketUtil.setUnitTesting(true);

        jndiTestUtils = new JndiTestUtils();
        jndiTestUtils.setUp();

        mockOtpOverSmsDeviceService = createMock(OtpOverSmsDeviceService.class);
        mockSamlAuthorityService = createMock(SamlAuthorityService.class);
        mockHelpdeskManager = createMock(HelpdeskManager.class);
        mockSecurityAuditLogger = createMock(SecurityAuditLogger.class);

        wicket = new WicketTester(new OtpOverSmsTestApplication());

    }

    @Override
    @After
    public void tearDown()
            throws Exception {

        jndiTestUtils.tearDown();
    }

    @Test
    public void testRegister()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String mobile = "+32 494 575 697";
        String convertedMobile = net.link.safeonline.custom.converter.PhoneNumberConverter.convertNumber(mobile);
        String otp = UUID.randomUUID().toString();
        String pin = "0000";

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(wicket.getServletSession());
        protocolContext.setDeviceOperation(DeviceOperationType.NEW_ACCOUNT_REGISTER);
        protocolContext.setSubject(userId);

        // verify
        RegistrationPage registrationPage = (RegistrationPage) wicket.startPage(RegistrationPage.class);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID, Form.class);
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);

        // setup
        EJBTestUtils.inject(registrationPage, mockOtpOverSmsDeviceService);
        EJBTestUtils.inject(registrationPage, mockSamlAuthorityService);

        // stubs
        mockOtpOverSmsDeviceService.requestOtp(wicket.getServletSession(), convertedMobile);

        // prepare
        replay(mockOtpOverSmsDeviceService);

        // operate
        FormTester requestOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        requestOtpForm.setValue(RegistrationPage.MOBILE_FIELD_ID, mobile);
        requestOtpForm.submit(RegistrationPage.REQUEST_OTP_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService);

        // verify
        registrationPage = (RegistrationPage) wicket.getLastRenderedPage();
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID, Form.class);

        // setup
        EasyMock.reset(mockOtpOverSmsDeviceService);

        // stubs
        expect(mockOtpOverSmsDeviceService.verifyOtp(wicket.getServletSession(), otp)).andStubReturn(true);
        mockOtpOverSmsDeviceService.register(userId, convertedMobile, pin);
        expect(mockSamlAuthorityService.getAuthnAssertionValidity()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(mockOtpOverSmsDeviceService, mockSamlAuthorityService);

        // operate
        FormTester verifyOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);
        verifyOtpForm.setValue(RegistrationPage.OTP_FIELD_ID, otp);
        verifyOtpForm.setValue(RegistrationPage.PIN1_FIELD_ID, pin);
        verifyOtpForm.setValue(RegistrationPage.PIN2_FIELD_ID, pin);
        verifyOtpForm.submit(RegistrationPage.SAVE_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService, mockSamlAuthorityService);
    }

    @Test
    public void testRegisterConnectException()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String mobile = "+32 494 575 697";
        String convertedMobile = net.link.safeonline.custom.converter.PhoneNumberConverter.convertNumber(mobile);

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(wicket.getServletSession());
        protocolContext.setDeviceOperation(DeviceOperationType.NEW_ACCOUNT_REGISTER);
        protocolContext.setSubject(userId);

        // verify
        RegistrationPage registrationPage = (RegistrationPage) wicket.startPage(RegistrationPage.class);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID, Form.class);
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);

        // setup
        EJBTestUtils.inject(registrationPage, mockOtpOverSmsDeviceService);
        jndiTestUtils.bindComponent(HelpdeskManager.JNDI_BINDING, mockHelpdeskManager);
        jndiTestUtils.bindComponent(SecurityAuditLogger.JNDI_BINDING, mockSecurityAuditLogger);

        // stubs
        mockOtpOverSmsDeviceService.requestOtp(wicket.getServletSession(), convertedMobile);
        org.easymock.EasyMock.expectLastCall().andThrow(new ConnectException());
        expect(mockHelpdeskManager.getHelpdeskContextLimit()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(mockOtpOverSmsDeviceService, mockHelpdeskManager, mockSecurityAuditLogger);

        // operate
        FormTester requestOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        requestOtpForm.setValue(RegistrationPage.MOBILE_FIELD_ID, mobile);
        requestOtpForm.submit(RegistrationPage.REQUEST_OTP_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService, mockHelpdeskManager, mockSecurityAuditLogger);

        wicket.assertRenderedPage(RegistrationPage.class);
        wicket.assertErrorMessages(new String[] { "errorServiceConnection" });
    }

    @Test
    public void testRegisterVerifyFailed()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String mobile = "+32 494 575 697";
        String convertedMobile = net.link.safeonline.custom.converter.PhoneNumberConverter.convertNumber(mobile);
        String otp = UUID.randomUUID().toString();
        String pin = "0000";

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(wicket.getServletSession());
        protocolContext.setDeviceOperation(DeviceOperationType.NEW_ACCOUNT_REGISTER);
        protocolContext.setSubject(userId);

        // verify
        RegistrationPage registrationPage = (RegistrationPage) wicket.startPage(RegistrationPage.class);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID, Form.class);
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);

        // setup
        EJBTestUtils.inject(registrationPage, mockOtpOverSmsDeviceService);
        EJBTestUtils.inject(registrationPage, mockSamlAuthorityService);

        // stubs
        mockOtpOverSmsDeviceService.requestOtp(wicket.getServletSession(), convertedMobile);

        // prepare
        replay(mockOtpOverSmsDeviceService);

        // operate
        FormTester requestOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        requestOtpForm.setValue(RegistrationPage.MOBILE_FIELD_ID, mobile);
        requestOtpForm.submit(RegistrationPage.REQUEST_OTP_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService);

        // verify
        registrationPage = (RegistrationPage) wicket.getLastRenderedPage();
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID, Form.class);

        // setup
        EasyMock.reset(mockOtpOverSmsDeviceService);
        jndiTestUtils.bindComponent(HelpdeskManager.JNDI_BINDING, mockHelpdeskManager);

        // stubs
        expect(mockOtpOverSmsDeviceService.verifyOtp(wicket.getServletSession(), otp)).andStubReturn(false);
        expect(mockHelpdeskManager.getHelpdeskContextLimit()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(mockOtpOverSmsDeviceService, mockHelpdeskManager);

        // operate
        FormTester verifyOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);
        verifyOtpForm.setValue(RegistrationPage.OTP_FIELD_ID, otp);
        verifyOtpForm.setValue(RegistrationPage.PIN1_FIELD_ID, pin);
        verifyOtpForm.setValue(RegistrationPage.PIN2_FIELD_ID, pin);
        verifyOtpForm.submit(RegistrationPage.SAVE_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService, mockHelpdeskManager);

        wicket.assertRenderedPage(RegistrationPage.class);
        wicket.assertErrorMessages(new String[] { "authenticationFailedMsg" });
    }

    @Test
    public void testRegisterPinIncorrect()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String mobile = "+32 494 575 697";
        String convertedMobile = net.link.safeonline.custom.converter.PhoneNumberConverter.convertNumber(mobile);
        String otp = UUID.randomUUID().toString();
        String pin = "0000";

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(wicket.getServletSession());
        protocolContext.setDeviceOperation(DeviceOperationType.NEW_ACCOUNT_REGISTER);
        protocolContext.setSubject(userId);

        // verify
        RegistrationPage registrationPage = (RegistrationPage) wicket.startPage(RegistrationPage.class);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID, Form.class);
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);

        // setup
        EJBTestUtils.inject(registrationPage, mockOtpOverSmsDeviceService);
        EJBTestUtils.inject(registrationPage, mockSamlAuthorityService);

        // stubs
        mockOtpOverSmsDeviceService.requestOtp(wicket.getServletSession(), convertedMobile);

        // prepare
        replay(mockOtpOverSmsDeviceService);

        // operate
        FormTester requestOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        requestOtpForm.setValue(RegistrationPage.MOBILE_FIELD_ID, mobile);
        requestOtpForm.submit(RegistrationPage.REQUEST_OTP_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService);

        // verify
        registrationPage = (RegistrationPage) wicket.getLastRenderedPage();
        wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RegistrationPage.REQUEST_OTP_FORM_ID);
        wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID, Form.class);

        // setup
        EasyMock.reset(mockOtpOverSmsDeviceService);
        jndiTestUtils.bindComponent(HelpdeskManager.JNDI_BINDING, mockHelpdeskManager);
        jndiTestUtils.bindComponent(SecurityAuditLogger.JNDI_BINDING, mockSecurityAuditLogger);

        // stubs
        expect(mockOtpOverSmsDeviceService.verifyOtp(wicket.getServletSession(), otp)).andStubReturn(true);
        mockOtpOverSmsDeviceService.register(userId, convertedMobile, pin);
        org.easymock.EasyMock.expectLastCall().andThrow(new PermissionDeniedException(""));
        expect(mockHelpdeskManager.getHelpdeskContextLimit()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(mockOtpOverSmsDeviceService, mockHelpdeskManager, mockSecurityAuditLogger);

        // operate
        FormTester verifyOtpForm = wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RegistrationPage.VERIFY_OTP_FORM_ID);
        verifyOtpForm.setValue(RegistrationPage.OTP_FIELD_ID, otp);
        verifyOtpForm.setValue(RegistrationPage.PIN1_FIELD_ID, pin);
        verifyOtpForm.setValue(RegistrationPage.PIN2_FIELD_ID, pin);
        verifyOtpForm.submit(RegistrationPage.SAVE_BUTTON_ID);

        // verify
        verify(mockOtpOverSmsDeviceService, mockHelpdeskManager, mockSecurityAuditLogger);

        wicket.assertRenderedPage(RegistrationPage.class);
        wicket.assertErrorMessages(new String[] { "errorPinNotCorrect" });
    }

}
