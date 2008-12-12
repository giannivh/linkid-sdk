package test.unit.net.link.safeonline.encap.webapp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.data.AttributeDO;
import net.link.safeonline.encap.webapp.MainPage;
import net.link.safeonline.encap.webapp.RegisterPage;
import net.link.safeonline.encap.webapp.RemovePage;
import net.link.safeonline.entity.DatatypeType;
import net.link.safeonline.model.encap.EncapConstants;
import net.link.safeonline.model.encap.EncapDeviceService;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.JmxTestUtils;
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
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;


public class RemovePageTest extends TestCase {

    private EncapDeviceService mockEncapDeviceService;

    private WicketTester       wicket;


    @Override
    @Before
    public void setUp()
            throws Exception {

        super.setUp();

        WicketUtil.setUnitTesting(true);

        this.mockEncapDeviceService = createMock(EncapDeviceService.class);

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

        this.wicket = new WicketTester(new EncapTestApplication());
        this.wicket.processRequestCycle();

    }

    @Test
    public void testRemoveEncap()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        String serialNumber = "12345678";
        DummyNameIdentifierMappingClient.setUserId(userId);

        List<AttributeDO> encapAttributes = new LinkedList<AttributeDO>();
        AttributeDO encap = new AttributeDO(EncapConstants.ENCAP_DEVICE_ATTRIBUTE, DatatypeType.STRING);
        encap.setStringValue(serialNumber);
        encapAttributes.add(encap);

        // MainPage: Verify.
        this.wicket.assertRenderedPage(MainPage.class);

        this.wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REGISTER_ID, RegisterPage.class);
        this.wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID, RemovePage.class);

        // MainPage: Click to remove encap
        this.wicket.clickLink(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID);

        // RemovePage: Verify.
        this.wicket.assertRenderedPage(RemovePage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RemovePage.GET_FORM_ID, Form.class);
        this.wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RemovePage.LIST_FORM_ID);

        // setup
        RemovePage removePage = (RemovePage) this.wicket.getLastRenderedPage();
        EJBTestUtils.inject(removePage, this.mockEncapDeviceService);

        // stubs
        expect(this.mockEncapDeviceService.getEncapes(userId, removePage.getLocale())).andStubReturn(encapAttributes);

        // prepare
        replay(this.mockEncapDeviceService);

        // RemovePage: retrieve encap for user
        FormTester getForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RemovePage.GET_FORM_ID);
        getForm.setValue(RemovePage.LOGIN_FIELD_ID, UUID.randomUUID().toString());
        getForm.submit(RemovePage.VIEW_BUTTON_ID);

        // verify
        verify(this.mockEncapDeviceService);

        // RemovePage: Verify
        this.wicket.assertRenderedPage(RemovePage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RemovePage.LIST_FORM_ID, Form.class);
        this.wicket.assertListView(TemplatePage.CONTENT_ID + ":" + RemovePage.LIST_FORM_ID + ":" + RemovePage.ENCAPS_LIST_ID,
                encapAttributes);

        // setup
        EasyMock.reset(this.mockEncapDeviceService);

        // stubs
        this.mockEncapDeviceService.remove(serialNumber);

        // prepare
        replay(this.mockEncapDeviceService);

        // RemovePage: remove encap
        this.wicket.clickLink(TemplatePage.CONTENT_ID + ":" + RemovePage.LIST_FORM_ID + ":" + RemovePage.ENCAPS_LIST_ID + ":0:"
                + RemovePage.REMOVE_LINK_ID);

        // verify
        verify(this.mockEncapDeviceService);

        this.wicket.assertRenderedPage(MainPage.class);
    }

    @Test
    public void testRemoveEncapSubjectNotFound()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        DummyNameIdentifierMappingClient.setUserId(userId);

        // MainPage: Verify.
        this.wicket.assertRenderedPage(MainPage.class);

        this.wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REGISTER_ID, RegisterPage.class);
        this.wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID, RemovePage.class);

        // MainPage: Click to remove encap
        this.wicket.clickLink(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID);

        // RemovePage: Verify.
        this.wicket.assertRenderedPage(RemovePage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RemovePage.GET_FORM_ID, Form.class);
        this.wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RemovePage.LIST_FORM_ID);

        // setup
        RemovePage removePage = (RemovePage) this.wicket.getLastRenderedPage();
        EJBTestUtils.inject(removePage, this.mockEncapDeviceService);

        // stubs
        expect(this.mockEncapDeviceService.getEncapes(userId, removePage.getLocale())).andThrow(new SubjectNotFoundException());

        // prepare
        replay(this.mockEncapDeviceService);

        // RemovePage: retrieve encap for user
        FormTester getForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RemovePage.GET_FORM_ID);
        getForm.setValue(RemovePage.LOGIN_FIELD_ID, UUID.randomUUID().toString());
        getForm.submit(RemovePage.VIEW_BUTTON_ID);

        // verify
        verify(this.mockEncapDeviceService);

        // RemovePage: Verify
        this.wicket.assertRenderedPage(RemovePage.class);
        this.wicket.assertErrorMessages(new String[] { "errorSubjectNotFound" });
    }

    @Test
    public void testRemoveEncapNoRegistrationsFound()
            throws Exception {

        // setup
        String userId = UUID.randomUUID().toString();
        DummyNameIdentifierMappingClient.setUserId(userId);

        // MainPage: Verify.
        this.wicket.assertRenderedPage(MainPage.class);

        this.wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REGISTER_ID, RegisterPage.class);
        this.wicket.assertPageLink(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID, RemovePage.class);

        // MainPage: Click to remove encap
        this.wicket.clickLink(TemplatePage.CONTENT_ID + ":" + MainPage.REMOVE_ID);

        // RemovePage: Verify.
        this.wicket.assertRenderedPage(RemovePage.class);
        this.wicket.assertComponent(TemplatePage.CONTENT_ID + ":" + RemovePage.GET_FORM_ID, Form.class);
        this.wicket.assertInvisible(TemplatePage.CONTENT_ID + ":" + RemovePage.LIST_FORM_ID);

        // setup
        RemovePage removePage = (RemovePage) this.wicket.getLastRenderedPage();
        EJBTestUtils.inject(removePage, this.mockEncapDeviceService);

        // stubs
        expect(this.mockEncapDeviceService.getEncapes(userId, removePage.getLocale())).andStubReturn(new LinkedList<AttributeDO>());

        // prepare
        replay(this.mockEncapDeviceService);

        // RemovePage: retrieve encap for user
        FormTester getForm = this.wicket.newFormTester(TemplatePage.CONTENT_ID + ":" + RemovePage.GET_FORM_ID);
        getForm.setValue(RemovePage.LOGIN_FIELD_ID, UUID.randomUUID().toString());
        getForm.submit(RemovePage.VIEW_BUTTON_ID);

        // verify
        verify(this.mockEncapDeviceService);

        // RemovePage: Verify
        this.wicket.assertRenderedPage(RemovePage.class);
        this.wicket.assertErrorMessages(new String[] { "errorNoDeviceRegistrationsFound" });
    }
}
