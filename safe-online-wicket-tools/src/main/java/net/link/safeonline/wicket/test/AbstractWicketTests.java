/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.wicket.test;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServlet;

import net.link.safeonline.sdk.auth.AuthenticationProtocol;
import net.link.safeonline.sdk.auth.AuthenticationProtocolManager;
import net.link.safeonline.sdk.auth.seam.SafeOnlineAuthenticationUtils;
import net.link.safeonline.sdk.test.DummyAttributeClient;
import net.link.safeonline.sdk.test.DummyNameIdentifierMappingClient;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.EntityTestManager;
import net.link.safeonline.test.util.JndiTestUtils;
import net.link.safeonline.test.util.SafeOnlineTestConfig;
import net.link.safeonline.util.ee.FieldNamingStrategy;
import net.link.safeonline.util.servlet.SafeOnlineConfig;
import net.link.safeonline.wicket.web.OlasLoginLink;
import net.link.safeonline.wicket.web.OlasLogoutLink;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.protocol.http.MockServletContext;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.ws.security.util.UUIDGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;


/**
 * <h2>{@link AbstractWicketTests}<br>
 * <sub>[in short] (TODO).</sub></h2>
 * 
 * <p>
 * [description / usage].
 * </p>
 * 
 * <p>
 * <i>Oct 7, 2008</i>
 * </p>
 * 
 * @author lhunath
 */
public abstract class AbstractWicketTests {

    protected static JndiTestUtils jndiTestUtils;

    protected final Log            LOG = LogFactory.getLog(getClass());

    protected EntityTestManager    entityTestManager;
    protected WicketTester         wicket;
    private String                 userId;


    @BeforeClass
    public static void init() {

        jndiTestUtils = new JndiTestUtils();
        jndiTestUtils.setUp();
        jndiTestUtils.setNamingStrategy(new FieldNamingStrategy());
    }

    @Before
    public void setUp()
            throws Exception {

        // Dummy OLAS Setup.
        userId = UUIDGenerator.getUUID();
        DummyAttributeClient.setAttribute(getOLASUserId(), "urn:net:lin-k:safe-online:attribute:login", getOLASUserId());
        DummyNameIdentifierMappingClient.setUserId(getOLASUserId());

        // Set up an HSQL entity manager.
        entityTestManager = new EntityTestManager();
        Class<?>[] serviceBeans = getServiceBeans();
        try {
            entityTestManager.setUp(getEntities());
        } catch (Exception err) {
            LOG.error("Couldn't set up entity manager", err);
            throw new IllegalStateException(err);
        }

        // Perform injections on our service beans.
        EntityManager entityManager = entityTestManager.getEntityManager();
        for (Class<?> beanClass : serviceBeans) {
            jndiTestUtils.bindComponent(beanClass, EJBTestUtils.newInstance(beanClass, serviceBeans, entityManager));
        }

        // Initialize our dummy web container and set our dummy authentication protocol as the one to use.
        wicket = new WicketTester(getApplication());
        MockServletContext wicketContext = (MockServletContext) wicket.getServletSession().getServletContext();
        wicketContext.addInitParameter(AuthenticationProtocolManager.LOGOUT_LANDING_PAGE_INIT_PARAM, "/");
        wicketContext.addInitParameter(AuthenticationProtocolManager.LANDING_PAGE_INIT_PARAM, "/");
        wicketContext.addInitParameter(SafeOnlineAuthenticationUtils.LOGOUT_SERVICE_PATH_INIT_PARAM, "/");
        wicketContext.addInitParameter(SafeOnlineAuthenticationUtils.AUTH_SERVICE_PATH_CONTEXT_PARAM, "/");
        wicketContext.addInitParameter(SafeOnlineAuthenticationUtils.APPLICATION_NAME_CONTEXT_PARAM, getClass().toString());
        wicketContext.addInitParameter(SafeOnlineAuthenticationUtils.AUTHN_PROTOCOL_CONTEXT_PARAM, AuthenticationProtocol.UNIT_TEST.name());
        wicketContext.addInitParameter(SafeOnlineConfig.WEBAPP_PATH_CONTEXT_PARAM, "/BankApplication/");
        wicket.processRequestCycle();

        SafeOnlineTestConfig.loadTestApp("", "", "https://127.0.0.1/olas-auth/", "https://127.0.0.1");
    }

    @After
    public void tearDown()
            throws Exception {

        entityTestManager.tearDown();
        jndiTestUtils.tearDown();
    }

    /**
     * Replace the {@link OlasLoginLink} on this page with a mocked one.
     */
    protected void mockOLASLoginLink()
            throws Exception {

        WicketTestUtils.mockLoginLink(wicket.getLastRenderedPage(), getOLASUserId(), "test-device");
    }

    /**
     * Replace the {@link OlasLogoutLink} on this page with a mocked one.
     */
    protected void mockOLASLogoutLink()
            throws Exception {

        WicketTestUtils.mockLogoutLink(wicket.getLastRenderedPage(), getLogoutServlet());
    }

    /**
     * @return The index at which the {@link FormComponent} with the given id in the form of the given {@link FormTester} has a value that
     *         matches the given pattern.
     */
    @SuppressWarnings("unchecked")
    protected int findSelectIndex(FormTester form, String id, String pattern) {

        AbstractChoice<?, ?> choiceComponent = (AbstractChoice<?, ?>) form.getForm().get(id);
        List<?> values = choiceComponent.getChoices();

        for (int index = 0; index < values.size(); ++index) {
            Object value = values.get(index);
            if (value == null) {
                if (pattern == null)
                    return index;
            }

            else {
                Locale currentLocale = wicket.getServletRequest().getLocale();
                IConverter converter = choiceComponent.getConverter(value.getClass());
                String stringValue = converter.convertToString(value, currentLocale);

                System.err.println("checking " + stringValue);
                if (stringValue.matches(".*" + pattern + ".*"))
                    return index;
            }
        }

        throw new IllegalArgumentException("Component " + id + " in form " + form.getForm().getPath() + " has no element that matches "
                + pattern);
    }

    /**
     * @return The userId that is provided by and for the dummy OLAS services.
     */
    protected String getOLASUserId() {

        return userId;
    }

    /**
     * @return The application specific logout servlet.
     */
    protected abstract Class<? extends HttpServlet> getLogoutServlet();

    /**
     * @return The wicket application that is being tested.
     */
    protected abstract WebApplication getApplication();

    /**
     * @return All the service beans that are used by the wicket application that is being tested.
     */
    protected abstract Class<?>[] getServiceBeans();

    /**
     * @return All the entity beans that are used by the wicket application that is being tested.
     */
    protected abstract Class<?>[] getEntities();
}
