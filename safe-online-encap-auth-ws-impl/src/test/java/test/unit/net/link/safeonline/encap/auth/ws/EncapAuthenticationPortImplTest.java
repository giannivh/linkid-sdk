/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.encap.auth.ws;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import net.lin_k.safe_online.auth.AuthenticationGetInstanceRequestType;
import net.lin_k.safe_online.auth.AuthenticationGetInstanceResponseType;
import net.lin_k.safe_online.auth.DeviceAuthenticationPort;
import net.lin_k.safe_online.auth.GetDeviceAuthenticationPort;
import net.lin_k.safe_online.auth.GetDeviceAuthenticationService;
import net.lin_k.safe_online.auth.WSAuthenticationRequestType;
import net.lin_k.safe_online.auth.WSAuthenticationResponseType;
import net.link.safeonline.authentication.exception.DeviceDisabledException;
import net.link.safeonline.authentication.exception.MobileException;
import net.link.safeonline.authentication.service.ApplicationAuthenticationService;
import net.link.safeonline.authentication.service.DeviceAuthenticationService;
import net.link.safeonline.authentication.service.NodeAuthenticationService;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.device.auth.ws.DeviceAuthenticationServiceFactory;
import net.link.safeonline.device.auth.ws.GetDeviceAuthenticationServiceFactory;
import net.link.safeonline.encap.auth.ws.EncapAuthenticationPortImpl;
import net.link.safeonline.encap.auth.ws.GetEncapAuthenticationPortImpl;
import net.link.safeonline.model.WSSecurityConfiguration;
import net.link.safeonline.model.encap.EncapConstants;
import net.link.safeonline.model.encap.EncapDeviceService;
import net.link.safeonline.pkix.model.PkiValidator;
import net.link.safeonline.pkix.model.PkiValidator.PkiResult;
import net.link.safeonline.sdk.ws.WSSecurityClientHandler;
import net.link.safeonline.sdk.ws.WSSecurityConfigurationService;
import net.link.safeonline.sdk.ws.auth.AuthenticationUtil;
import net.link.safeonline.test.util.DummyLoginModule;
import net.link.safeonline.test.util.JaasTestUtils;
import net.link.safeonline.test.util.JmxTestUtils;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.test.util.MBeanActionHandler;
import net.link.safeonline.test.util.PkiTestUtils;
import net.link.safeonline.test.util.WebServiceTestUtils;
import net.link.safeonline.util.ee.AuthIdentityServiceClient;
import net.link.safeonline.ws.common.WSAuthenticationErrorCode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class EncapAuthenticationPortImplTest {

    private static final Log                 LOG               = LogFactory.getLog(EncapAuthenticationPortImplTest.class);

    private WebServiceTestUtils              webServiceTestUtils;
    private WebServiceTestUtils              getWebServiceTestUtils;

    private DeviceAuthenticationPort         clientPort;

    private JndiTestUtils                    jndiTestUtils;

    private WSSecurityConfigurationService   mockWSSecurityConfigurationService;

    private PkiValidator                     mockPkiValidator;

    private ApplicationAuthenticationService mockApplicationAuthenticationService;

    private DeviceAuthenticationService      mockDeviceAuthenticationService;

    private NodeAuthenticationService        mockNodeAuthenticationService;

    private SamlAuthorityService             mockSamlAuthorityService;

    private EncapDeviceService               mockEncapDeviceServce;

    private Object[]                         mockObjects;

    private PublicKey                        testpublicKey;

    private X509Certificate                  certificate;

    private X509Certificate                  olasCertificate;

    private PrivateKey                       olasPrivateKey;

    private String                           testLanguage      = Locale.ENGLISH.getLanguage();

    private String                           testIssuerName    = "test-issuer-name";

    private String                           testApplicationId = "test-application-name";


    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
            throws Exception {

        LOG.debug("setup");

        // setup JMX
        JmxTestUtils jmxTestUtils = new JmxTestUtils();
        jmxTestUtils.setUp("jboss.security:service=JaasSecurityManager");
        jmxTestUtils.setUp(AuthIdentityServiceClient.AUTH_IDENTITY_SERVICE);

        final KeyPair authKeyPair = PkiTestUtils.generateKeyPair();
        final X509Certificate authCertificate = PkiTestUtils.generateSelfSignedCertificate(authKeyPair, "CN=Test");
        jmxTestUtils.registerActionHandler(AuthIdentityServiceClient.AUTH_IDENTITY_SERVICE, "getCertificate", new MBeanActionHandler() {

            public Object invoke(@SuppressWarnings("unused") Object[] arguments) {

                return authCertificate;
            }
        });

        this.jndiTestUtils = new JndiTestUtils();
        this.jndiTestUtils.setUp();
        this.jndiTestUtils.bindComponent("java:comp/env/wsSecurityConfigurationServiceJndiName", WSSecurityConfiguration.JNDI_BINDING);
        this.jndiTestUtils.bindComponent("java:comp/env/wsSecurityOptionalInboudSignature", false);
        this.jndiTestUtils.bindComponent("java:comp/env/wsLocation", "wsLocation");

        this.mockWSSecurityConfigurationService = createMock(WSSecurityConfigurationService.class);
        this.mockPkiValidator = createMock(PkiValidator.class);
        this.mockApplicationAuthenticationService = createMock(ApplicationAuthenticationService.class);
        this.mockDeviceAuthenticationService = createMock(DeviceAuthenticationService.class);
        this.mockNodeAuthenticationService = createMock(NodeAuthenticationService.class);
        this.mockSamlAuthorityService = createMock(SamlAuthorityService.class);
        this.mockEncapDeviceServce = createMock(EncapDeviceService.class);

        this.mockObjects = new Object[] { this.mockWSSecurityConfigurationService, this.mockPkiValidator,
                this.mockApplicationAuthenticationService, this.mockSamlAuthorityService, this.mockEncapDeviceServce };

        this.jndiTestUtils.bindComponent(WSSecurityConfiguration.JNDI_BINDING, this.mockWSSecurityConfigurationService);
        this.jndiTestUtils.bindComponent(PkiValidator.JNDI_BINDING, this.mockPkiValidator);
        this.jndiTestUtils.bindComponent(ApplicationAuthenticationService.JNDI_BINDING, this.mockApplicationAuthenticationService);
        this.jndiTestUtils.bindComponent(DeviceAuthenticationService.JNDI_BINDING, this.mockDeviceAuthenticationService);
        this.jndiTestUtils.bindComponent(NodeAuthenticationService.JNDI_BINDING, this.mockNodeAuthenticationService);
        this.jndiTestUtils.bindComponent(SamlAuthorityService.JNDI_BINDING, this.mockSamlAuthorityService);
        this.jndiTestUtils.bindComponent(EncapDeviceService.JNDI_BINDING, this.mockEncapDeviceServce);

        expect(this.mockPkiValidator.validateCertificate((String) EasyMock.anyObject(), (X509Certificate) EasyMock.anyObject()))
                                                                                                                                .andStubReturn(
                                                                                                                                        PkiResult.VALID);
        expect(this.mockWSSecurityConfigurationService.getMaximumWsSecurityTimestampOffset()).andStubReturn(Long.MAX_VALUE);

        JaasTestUtils.initJaasLoginModule(DummyLoginModule.class);

        // Init Password Authentication Port
        DeviceAuthenticationPort wsPort = new EncapAuthenticationPortImpl();
        this.webServiceTestUtils = new WebServiceTestUtils();
        this.webServiceTestUtils.setUp(wsPort);

        // Get stateful Authentication Port instance
        this.getWebServiceTestUtils = new WebServiceTestUtils();
        GetDeviceAuthenticationService getService = GetDeviceAuthenticationServiceFactory.newInstance();
        GetDeviceAuthenticationPort wsGetPort = new GetEncapAuthenticationPortImpl();
        this.getWebServiceTestUtils.setUp(wsGetPort);
        GetDeviceAuthenticationPort getPort = getService.getGetDeviceAuthenticationPort();
        this.getWebServiceTestUtils.setEndpointAddress(getPort);
        AuthenticationGetInstanceResponseType response = getPort.getInstance(new AuthenticationGetInstanceRequestType());
        W3CEndpointReference endpoint = response.getEndpoint();

        net.lin_k.safe_online.auth.DeviceAuthenticationService service = DeviceAuthenticationServiceFactory.newInstance();
        this.clientPort = service.getPort(endpoint, DeviceAuthenticationPort.class, new AddressingFeature(true));
        this.webServiceTestUtils.setEndpointAddress(this.clientPort);

        KeyPair keyPair = PkiTestUtils.generateKeyPair();
        this.certificate = PkiTestUtils.generateSelfSignedCertificate(keyPair, "CN=Test");

        KeyPair olasKeyPair = PkiTestUtils.generateKeyPair();
        this.olasCertificate = PkiTestUtils.generateSelfSignedCertificate(olasKeyPair, "CN=OLAS");
        this.olasPrivateKey = olasKeyPair.getPrivate();

        BindingProvider bindingProvider = (BindingProvider) this.clientPort;
        Binding binding = bindingProvider.getBinding();
        List<Handler> handlerChain = binding.getHandlerChain();
        Handler<SOAPMessageContext> wsSecurityHandler = new WSSecurityClientHandler(this.certificate, keyPair.getPrivate());
        handlerChain.add(wsSecurityHandler);
        binding.setHandlerChain(handlerChain);
    }

    @After
    public void tearDown()
            throws Exception {

        LOG.debug("tearDown");
        this.webServiceTestUtils.tearDown();
        this.jndiTestUtils.tearDown();
    }

    @Test
    public void testAuthenticate()
            throws Exception {

        // setup
        String testUserId = UUID.randomUUID().toString();
        String testMobile = "+32000000";
        String testOtp = "00000000";
        String testChallengeId = UUID.randomUUID().toString();

        Map<String, String> nameValuePairs = new HashMap<String, String>();
        nameValuePairs.put(EncapConstants.ENCAP_WS_AUTH_MOBILE_ATTRIBUTE, testMobile);

        WSAuthenticationRequestType request = AuthenticationUtil.getAuthenticationRequest(this.testApplicationId,
                EncapConstants.ENCAP_DEVICE_ID, this.testLanguage, nameValuePairs, this.testpublicKey);

        // expectations
        this.mockEncapDeviceServce.checkMobile(testMobile);
        expect(this.mockEncapDeviceServce.requestOTP(testMobile)).andReturn(testChallengeId);
        expect(this.mockSamlAuthorityService.getIssuerName()).andStubReturn(this.testIssuerName);
        expect(this.mockApplicationAuthenticationService.authenticate(this.certificate)).andReturn("test-application-name");
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.getCertificate()).andStubReturn(this.olasCertificate);
        expect(this.mockWSSecurityConfigurationService.getPrivateKey()).andStubReturn(this.olasPrivateKey);

        // prepare
        replay(this.mockObjects);

        // operate
        WSAuthenticationResponseType response = this.clientPort.authenticate(request);

        // verify
        verify(this.mockObjects);
        assertNotNull(response);
        assertEquals(EncapConstants.ENCAP_DEVICE_ID, response.getDeviceName());
        assertNull(response.getUserId());
        assertEquals(WSAuthenticationErrorCode.SUCCESS.getErrorCode(), response.getStatus().getStatusCode().getValue());

        outputAuthenticationResponse(response);

        // reset
        reset(this.mockObjects);

        // setup
        nameValuePairs = new HashMap<String, String>();
        nameValuePairs.put(EncapConstants.ENCAP_WS_AUTH_OTP_ATTRIBUTE, testOtp);

        request = AuthenticationUtil.getAuthenticationRequest(this.testApplicationId, EncapConstants.ENCAP_DEVICE_ID, this.testLanguage,
                nameValuePairs, this.testpublicKey);

        // expectations
        expect(this.mockEncapDeviceServce.authenticate(testMobile, testChallengeId, testOtp)).andReturn(testUserId);
        expect(this.mockSamlAuthorityService.getIssuerName()).andStubReturn(this.testIssuerName);
        expect(this.mockApplicationAuthenticationService.authenticate(this.certificate)).andReturn("test-application-name");
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.getCertificate()).andStubReturn(this.olasCertificate);
        expect(this.mockWSSecurityConfigurationService.getPrivateKey()).andStubReturn(this.olasPrivateKey);
        expect(this.mockPkiValidator.validateCertificate((String) EasyMock.anyObject(), (X509Certificate) EasyMock.anyObject()))
                                                                                                                                .andStubReturn(
                                                                                                                                        PkiResult.VALID);
        expect(this.mockWSSecurityConfigurationService.getMaximumWsSecurityTimestampOffset()).andStubReturn(Long.MAX_VALUE);

        // prepare
        replay(this.mockObjects);

        // operate
        response = this.clientPort.authenticate(request);

        // verify
        verify(this.mockObjects);
        assertNotNull(response);
        assertEquals(EncapConstants.ENCAP_DEVICE_ID, response.getDeviceName());
        assertEquals(testUserId, response.getUserId());
        assertEquals(WSAuthenticationErrorCode.SUCCESS.getErrorCode(), response.getStatus().getStatusCode().getValue());

        outputAuthenticationResponse(response);
    }

    @Test
    public void testAuthenticationFailedOtpVerificationFailed()
            throws Exception {

        // setup
        String testMobile = "+32000000";
        String testOtp = "00000000";
        String testChallengeId = UUID.randomUUID().toString();

        Map<String, String> nameValuePairs = new HashMap<String, String>();
        nameValuePairs.put(EncapConstants.ENCAP_WS_AUTH_MOBILE_ATTRIBUTE, testMobile);

        WSAuthenticationRequestType request = AuthenticationUtil.getAuthenticationRequest(this.testApplicationId,
                EncapConstants.ENCAP_DEVICE_ID, this.testLanguage, nameValuePairs, this.testpublicKey);

        // expectations
        this.mockEncapDeviceServce.checkMobile(testMobile);
        expect(this.mockEncapDeviceServce.requestOTP(testMobile)).andReturn(testChallengeId);
        expect(this.mockSamlAuthorityService.getIssuerName()).andStubReturn(this.testIssuerName);
        expect(this.mockApplicationAuthenticationService.authenticate(this.certificate)).andReturn("test-application-name");
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.getCertificate()).andStubReturn(this.olasCertificate);
        expect(this.mockWSSecurityConfigurationService.getPrivateKey()).andStubReturn(this.olasPrivateKey);

        // prepare
        replay(this.mockObjects);

        // operate
        WSAuthenticationResponseType response = this.clientPort.authenticate(request);

        // verify
        verify(this.mockObjects);
        assertNotNull(response);
        assertEquals(EncapConstants.ENCAP_DEVICE_ID, response.getDeviceName());
        assertNull(response.getUserId());
        assertEquals(WSAuthenticationErrorCode.SUCCESS.getErrorCode(), response.getStatus().getStatusCode().getValue());

        outputAuthenticationResponse(response);

        // reset
        reset(this.mockObjects);

        // setup
        nameValuePairs = new HashMap<String, String>();
        nameValuePairs.put(EncapConstants.ENCAP_WS_AUTH_OTP_ATTRIBUTE, testOtp);

        request = AuthenticationUtil.getAuthenticationRequest(this.testApplicationId, EncapConstants.ENCAP_DEVICE_ID, this.testLanguage,
                nameValuePairs, this.testpublicKey);

        // expectations
        expect(this.mockEncapDeviceServce.authenticate(testMobile, testChallengeId, testOtp)).andThrow(new MobileException("foo"));
        expect(this.mockSamlAuthorityService.getIssuerName()).andStubReturn(this.testIssuerName);
        expect(this.mockApplicationAuthenticationService.authenticate(this.certificate)).andReturn("test-application-name");
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.getCertificate()).andStubReturn(this.olasCertificate);
        expect(this.mockWSSecurityConfigurationService.getPrivateKey()).andStubReturn(this.olasPrivateKey);
        expect(this.mockPkiValidator.validateCertificate((String) EasyMock.anyObject(), (X509Certificate) EasyMock.anyObject()))
                                                                                                                                .andStubReturn(
                                                                                                                                        PkiResult.VALID);
        expect(this.mockWSSecurityConfigurationService.getMaximumWsSecurityTimestampOffset()).andStubReturn(Long.MAX_VALUE);

        // prepare
        replay(this.mockObjects);

        // operate
        response = this.clientPort.authenticate(request);

        // verify
        verify(this.mockObjects);
        assertNotNull(response);
        assertEquals(EncapConstants.ENCAP_DEVICE_ID, response.getDeviceName());
        assertNull(response.getUserId());
        assertEquals(WSAuthenticationErrorCode.AUTHENTICATION_FAILED.getErrorCode(), response.getStatus().getStatusCode().getValue());

        outputAuthenticationResponse(response);
    }

    @Test
    public void testAuthenticationDeviceDisabled()
            throws Exception {

        // setup
        String testMobile = "+32000000";

        Map<String, String> nameValuePairs = new HashMap<String, String>();
        nameValuePairs.put(EncapConstants.ENCAP_WS_AUTH_MOBILE_ATTRIBUTE, testMobile);

        WSAuthenticationRequestType request = AuthenticationUtil.getAuthenticationRequest(this.testApplicationId,
                EncapConstants.ENCAP_DEVICE_ID, this.testLanguage, nameValuePairs, this.testpublicKey);

        // expectations
        this.mockEncapDeviceServce.checkMobile(testMobile);
        expectLastCall().andThrow(new DeviceDisabledException());
        expect(this.mockSamlAuthorityService.getIssuerName()).andStubReturn(this.testIssuerName);
        expect(this.mockApplicationAuthenticationService.authenticate(this.certificate)).andReturn("test-application-name");
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.skipMessageIntegrityCheck(this.certificate)).andReturn(false);
        expect(this.mockWSSecurityConfigurationService.getCertificate()).andStubReturn(this.olasCertificate);
        expect(this.mockWSSecurityConfigurationService.getPrivateKey()).andStubReturn(this.olasPrivateKey);

        // prepare
        replay(this.mockObjects);

        // operate
        WSAuthenticationResponseType response = this.clientPort.authenticate(request);

        // verify
        verify(this.mockObjects);
        assertNotNull(response);
        assertEquals(EncapConstants.ENCAP_DEVICE_ID, response.getDeviceName());
        assertNull(response.getUserId());
        assertEquals(WSAuthenticationErrorCode.DEVICE_DISABLED.getErrorCode(), response.getStatus().getStatusCode().getValue());

        outputAuthenticationResponse(response);
    }

    private void outputAuthenticationResponse(WSAuthenticationResponseType response)
            throws Exception {

        JAXBContext context = JAXBContext.newInstance(net.lin_k.safe_online.auth.ObjectFactory.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter stringWriter = new StringWriter();
        net.lin_k.safe_online.auth.ObjectFactory objectFactory = new net.lin_k.safe_online.auth.ObjectFactory();
        marshaller.marshal(objectFactory.createWSAuthenticationResponse(response), stringWriter);
        LOG.debug("response: " + stringWriter);
    }

}
