/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.sdk.auth.filter;

import static net.link.safeonline.sdk.configuration.TestConfigHolder.testConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.link.safeonline.sdk.auth.filter.AuthnRequestFilter;
import net.link.safeonline.sdk.configuration.GeneratedKeyStore;
import net.link.safeonline.sdk.configuration.TestConfigHolder;
import net.link.util.test.j2ee.TestClassLoader;
import net.link.util.test.pkix.PkiTestUtils;
import net.link.util.test.web.ContainerSetup;
import net.link.util.test.web.FilterSetup;
import net.link.util.test.web.ServletSetup;
import net.link.util.test.web.ServletTestManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class AuthnRequestFilterTest {

    private static final Log LOG = LogFactory.getLog( AuthnRequestFilterTest.class );

    private ServletTestManager servletTestManager;

    private ClassLoader originalContextClassLoader;

    private TestClassLoader testClassLoader;

    @Before
    public void setUp()
            throws Exception {

        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        testClassLoader = new TestClassLoader();
        Thread.currentThread().setContextClassLoader( testClassLoader );
        servletTestManager = new ServletTestManager();
    }

    @After
    public void tearDown()
            throws Exception {

        servletTestManager.tearDown();
        Thread.currentThread().setContextClassLoader( originalContextClassLoader );
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException {

            throw new UnsupportedOperationException( "should never get called" );
        }
    }

    @Test
    public void performSaml2AuthnRequest()
            throws Exception {

        // Setup Data
        servletTestManager.setUp( new ContainerSetup( //
                new ServletSetup( TestServlet.class ), new FilterSetup( AuthnRequestFilter.class ) ) );

        new TestConfigHolder( servletTestManager.createSocketConnector(), servletTestManager.getServletContext() ).install();
        testConfig().linkID().app().keyStore = new GeneratedKeyStore() {
            @Override
            protected KeyStore.PrivateKeyEntry load()
                    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, SignatureException, IOException,
                           InvalidKeyException, CertificateException {

                return PkiTestUtils.generateKeyEntry( "CN=TestApplication" );
            }
        };

        // Test
        GetMethod getMethod = new GetMethod( servletTestManager.getServletLocation() );
        int statusCode = new HttpClient().executeMethod( getMethod );

        // Verify
        LOG.debug( "status code: " + statusCode );
        assertEquals( HttpStatus.SC_OK, statusCode );
        String response = getMethod.getResponseBodyAsString();
        LOG.debug( "response body: " + response );
    }

    @Test
    public void performSaml2AuthnRequestWithCustomTemplate()
            throws Exception {

        // Setup Data
        servletTestManager.setUp( new ContainerSetup( //
                new ServletSetup( TestServlet.class ), new FilterSetup( AuthnRequestFilter.class ) ) );

        new TestConfigHolder( servletTestManager.createSocketConnector(), servletTestManager.getServletContext() ).install();
        testConfig().proto().saml().postBindingTemplate = "test-saml2-post-binding.vm";
        testConfig().linkID().app().keyStore = new GeneratedKeyStore() {
            @Override
            protected KeyStore.PrivateKeyEntry load()
                    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, SignatureException, IOException,
                           InvalidKeyException, CertificateException {

                return PkiTestUtils.generateKeyEntry( "CN=TestApplication" );
            }
        };

        GetMethod getMethod = new GetMethod( servletTestManager.getServletLocation() );
        HttpClient httpClient = new HttpClient();

        // Test
        int statusCode = httpClient.executeMethod( getMethod );

        // Verify
        LOG.debug( "status code: " + statusCode );
        assertEquals( HttpStatus.SC_OK, statusCode );
        String response = getMethod.getResponseBodyAsString();
        LOG.debug( "response body: " + response );
        assertTrue( response.indexOf( "custom template" ) != -1 );
    }
}
