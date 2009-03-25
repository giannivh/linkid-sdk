/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.beid.servlet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.beid.servlet.IdentityServlet;
import net.link.safeonline.device.sdk.operation.saml2.DeviceOperationManager;
import net.link.safeonline.device.sdk.operation.saml2.DeviceOperationType;
import net.link.safeonline.model.beid.BeIdDeviceService;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.test.util.SafeOnlineTestConfig;
import net.link.safeonline.test.util.ServletTestManager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class IdentityServletTest {

    private static final Log     LOG = LogFactory.getLog(IdentityServletTest.class);

    private String               location;

    private HttpClient           httpClient;

    private BeIdDeviceService    mockBeIdDeviceServiceBean;

    private SamlAuthorityService mockSamlAuthorityService;

    private ServletTestManager   servletTestManager;

    private JndiTestUtils        jndiTestUtils;


    @Before
    public void setUp()
            throws Exception {

        jndiTestUtils = new JndiTestUtils();
        jndiTestUtils.setUp();

        mockBeIdDeviceServiceBean = createMock(BeIdDeviceService.class);
        jndiTestUtils.bindComponent(BeIdDeviceService.JNDI_BINDING, mockBeIdDeviceServiceBean);

        mockSamlAuthorityService = createMock(SamlAuthorityService.class);
        jndiTestUtils.bindComponent(SamlAuthorityService.JNDI_BINDING, mockSamlAuthorityService);

        servletTestManager = new ServletTestManager();
        servletTestManager.setUp(IdentityServlet.class);
        servletTestManager.setSessionAttribute(DeviceOperationManager.USERID_SESSION_ATTRIBUTE, UUID.randomUUID().toString());
        servletTestManager.setSessionAttribute(DeviceOperationManager.DEVICE_OPERATION_SESSION_ATTRIBUTE,
                DeviceOperationType.REGISTER.name());
        location = servletTestManager.getServletLocation();

        SafeOnlineTestConfig.loadTest(servletTestManager);

        httpClient = new HttpClient();
    }

    @After
    public void tearDown()
            throws Exception {

        servletTestManager.tearDown();
        jndiTestUtils.tearDown();
    }

    @Test
    public void testWrongContentTypeGivesBadRequestResult()
            throws Exception {

        // setup
        PostMethod postMethod = new PostMethod(location);

        // operate
        int result = httpClient.executeMethod(postMethod);

        // verify
        LOG.debug("result: " + result);
        LOG.debug("output: \n" + postMethod.getResponseBodyAsString());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result);
    }

    @Test
    public void testGetNotAllowed()
            throws Exception {

        // setup
        GetMethod getMethod = new GetMethod(location);

        // operate
        int result = httpClient.executeMethod(getMethod);

        // verify
        LOG.debug("result: " + result);
        assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, result);
    }

    @Test
    public void testDoPost()
            throws Exception {

        // setup
        PostMethod postMethod = new PostMethod(location);
        RequestEntity requestEntity = new StringRequestEntity("test-message", "application/octet-stream", null);
        postMethod.setRequestEntity(requestEntity);

        // expectations
        mockBeIdDeviceServiceBean.register((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                (String) EasyMock.anyObject(), EasyMock.aryEq("test-message".getBytes()));
        EasyMock.expect(mockSamlAuthorityService.getAuthnAssertionValidity()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(mockBeIdDeviceServiceBean);

        // operate
        int result = httpClient.executeMethod(postMethod);

        // verify
        assertEquals(HttpServletResponse.SC_OK, result);
        verify(mockBeIdDeviceServiceBean);
    }

    @Test
    public void testJREOnlyClient()
            throws Exception {

        // setup
        URL url = new URL(location);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setAllowUserInteraction(false);
        httpURLConnection.setRequestProperty("Content-type", "application/octet-stream");
        httpURLConnection.setDoOutput(true);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        IOUtils.write("test-message", outputStream, null);
        outputStream.close();
        httpURLConnection.connect();

        httpURLConnection.disconnect();

        // expectations
        mockBeIdDeviceServiceBean.register((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                (String) EasyMock.anyObject(), EasyMock.aryEq("test-message".getBytes()));
        EasyMock.expect(mockSamlAuthorityService.getAuthnAssertionValidity()).andStubReturn(Integer.MAX_VALUE);

        // prepare
        replay(mockBeIdDeviceServiceBean);

        // operate
        int responseCode = httpURLConnection.getResponseCode();

        // verify
        LOG.debug("response code: " + responseCode);
        assertEquals(HttpServletResponse.SC_OK, responseCode);
        verify(mockBeIdDeviceServiceBean);
    }
}
