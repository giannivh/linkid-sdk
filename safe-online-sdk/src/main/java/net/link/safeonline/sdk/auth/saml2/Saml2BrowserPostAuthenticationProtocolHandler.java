/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.auth.saml2;

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.sdk.auth.AuthenticationProtocol;
import net.link.safeonline.sdk.auth.AuthenticationProtocolContext;
import net.link.safeonline.sdk.auth.AuthenticationProtocolHandler;
import net.link.safeonline.sdk.auth.SupportedAuthenticationProtocol;
import net.link.safeonline.sdk.ws.sts.TrustDomainType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Base64;
import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.Subject;
import org.opensaml.xml.ConfigurationException;


/**
 * Implementation class for the SAML2 browser POST authentication protocol.
 * 
 * <p>
 * Optional configuration parameters:
 * </p>
 * <ul>
 * <li><code>Saml2BrowserPostTemplate</code>: contains the path to the custom SAML2 Browser POST template resource.</li>
 * <li><code>Saml2Devices</code>: contains the list of allowed authentication devices, comma separated string</li>
 * <li><code>WsLocation</code>: contains the location of the OLAS web services. If present this handler will use the STS web service for
 * SAML authentication token validation.</li>
 * </ul>
 * 
 * <p>
 * Optional session configuration attributes:
 * </p>
 * <ul>
 * <li><code>Saml2Devices</code>: contains the <code>Set&lt;String&gt;</code> of allowed authentication devices.</li>
 * </ul>
 * 
 * @author fcorneli
 * 
 */
@SupportedAuthenticationProtocol(AuthenticationProtocol.SAML2_BROWSER_POST)
public class Saml2BrowserPostAuthenticationProtocolHandler implements AuthenticationProtocolHandler {

    private static final long   serialVersionUID                         = 1L;

    public static final String  SAML2_POST_BINDING_VM_RESOURCE           = "/net/link/safeonline/sdk/auth/saml2/saml2-post-binding.vm";

    public static final String  SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM = "Saml2BrowserPostTemplate";

    public static final String  SAML2_DEVICES_CONFIG_PARAM               = "Saml2Devices";

    public static final String  SAML2_DEVICES_ATTRIBUTE                  = "Saml2Devices";

    private static final Log    LOG                                      = LogFactory
                                                                                     .getLog(Saml2BrowserPostAuthenticationProtocolHandler.class);

    static {
        /*
         * Next is because Sun loves to endorse crippled versions of Xerces.
         */
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            throw new RuntimeException("could not bootstrap the OpenSAML2 library");
        }
    }

    private String              authnServiceUrl;

    private String              applicationName;

    private String              applicationFriendlyName;

    private KeyPair             applicationKeyPair;

    private X509Certificate     applicationCertificate;

    private Map<String, String> configParams;

    private Challenge<String>   challenge;

    private String              wsLocation;

    private boolean             ssoEnabled;


    public void init(String inAuthnServiceUrl, String inApplicationName, String inApplicationFriendlyName, KeyPair inApplicationKeyPair,
                     X509Certificate inApplicationCertificate, boolean inSsoEnabled, Map<String, String> inConfigParams) {

        LOG.debug("init");
        this.authnServiceUrl = inAuthnServiceUrl;
        this.applicationName = inApplicationName;
        this.applicationFriendlyName = inApplicationFriendlyName;
        this.applicationKeyPair = inApplicationKeyPair;
        this.applicationCertificate = inApplicationCertificate;
        this.configParams = inConfigParams;
        this.challenge = new Challenge<String>();
        this.ssoEnabled = inSsoEnabled;
        this.wsLocation = inConfigParams.get("WsLocation");
        if (null == this.wsLocation)
            throw new RuntimeException("Initialization param \"WsLocation\" not specified.");
    }

    @SuppressWarnings("unchecked")
    public Set<String> getDevices(HttpServletRequest httpRequest) {

        String staticDeviceList = this.configParams.get(SAML2_DEVICES_CONFIG_PARAM);
        HttpSession session = httpRequest.getSession();
        Set<String> runtimeDevices = (Set<String>) session.getAttribute(SAML2_DEVICES_ATTRIBUTE);
        if (null == staticDeviceList && null == runtimeDevices)
            return null;
        Set<String> staticDevices;
        if (null == staticDeviceList) {
            staticDevices = null;
        } else {
            staticDevices = new HashSet<String>();
            StringTokenizer stringTokenizer = new StringTokenizer(staticDeviceList, ",");
            while (stringTokenizer.hasMoreTokens()) {
                staticDevices.add(stringTokenizer.nextToken());
            }
        }
        if (null != staticDevices && null != runtimeDevices) {
            Set<String> intersection = new HashSet<String>(staticDevices);
            intersection.retainAll(runtimeDevices);
            if (intersection.isEmpty())
                throw new RuntimeException("intersection of static and runtime device lists is empty");
            return intersection;
        }
        if (null != staticDevices)
            return staticDevices;
        if (null != runtimeDevices)
            return runtimeDevices;
        throw new RuntimeException("WTF");
    }

    public void initiateAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String targetUrl)
                                                                                                                          throws IOException,
                                                                                                                          ServletException {

        LOG.debug("target url: " + targetUrl);
        Set<String> devices = getDevices(httpRequest);
        String samlRequestToken = AuthnRequestFactory.createAuthnRequest(this.applicationName, this.applicationName,
                this.applicationFriendlyName, this.applicationKeyPair, targetUrl, this.authnServiceUrl, this.challenge, devices,
                this.ssoEnabled);

        String encodedSamlRequestToken = Base64.encode(samlRequestToken.getBytes());

        String templateResourceName;
        if (this.configParams.containsKey(SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM)) {
            templateResourceName = this.configParams.get(SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM);
        } else {
            templateResourceName = SAML2_POST_BINDING_VM_RESOURCE;
        }

        String language = getLanguage(httpRequest);
        if (null == language) {
            language = httpRequest.getLocale().getLanguage();
        }

        RequestUtil.sendRequest(this.authnServiceUrl, encodedSamlRequestToken, language, templateResourceName, httpResponse);
    }

    private String getLanguage(HttpServletRequest httpRequest) {

        Cookie[] cookies = httpRequest.getCookies();
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("OLAS.language".equals(cookie.getName()))
                return cookie.getValue();
        }
        return null;
    }

    public AuthenticationProtocolContext finalizeAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
                                                                                                                                 throws ServletException {

        DateTime now = new DateTime();

        Response samlResponse = ResponseUtil.validateResponse(now, httpRequest, this.challenge.getValue(), this.applicationName,
                this.wsLocation, this.applicationCertificate, this.applicationKeyPair.getPrivate(), TrustDomainType.NODE);
        if (null == samlResponse)
            return null;

        Assertion assertion = samlResponse.getAssertions().get(0);
        Subject subject = assertion.getSubject();
        NameID subjectName = subject.getNameID();
        String subjectNameValue = subjectName.getValue();
        LOG.debug("subject name value: " + subjectNameValue);

        AuthnStatement statement = assertion.getAuthnStatements().get(0);
        String authenticatedDevice = statement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef();

        return new AuthenticationProtocolContext(subjectNameValue, authenticatedDevice);
    }

    /**
     * {@inheritDoc}
     */
    public void initiateLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String targetUrl, String subjectName)
                                                                                                                                      throws IOException,
                                                                                                                                      ServletException {

        LOG.debug("target url: " + targetUrl);
        String samlRequestToken = LogoutRequestFactory.createLogoutRequest(subjectName, this.applicationName, this.applicationKeyPair,
                this.authnServiceUrl, this.challenge);

        String encodedSamlRequestToken = Base64.encode(samlRequestToken.getBytes());

        String templateResourceName;
        if (this.configParams.containsKey(SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM)) {
            templateResourceName = this.configParams.get(SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM);
        } else {
            templateResourceName = SAML2_POST_BINDING_VM_RESOURCE;
        }

        RequestUtil.sendRequest(this.authnServiceUrl, encodedSamlRequestToken, null, templateResourceName, httpResponse);
    }

    /**
     * {@inheritDoc}
     */
    public boolean finalizeLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException {

        LogoutResponse samlLogoutResponse = ResponseUtil.validateLogoutResponse(httpRequest, this.challenge.getValue(), this.wsLocation,
                this.applicationCertificate, this.applicationKeyPair.getPrivate(), TrustDomainType.NODE);

        if (null == samlLogoutResponse)
            return false;

        if (!samlLogoutResponse.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS_URI))
            return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String handleLogoutRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        LogoutRequest samlLogoutRequest = RequestUtil.validateLogoutRequest(request, this.wsLocation, this.applicationCertificate,
                this.applicationKeyPair.getPrivate(), TrustDomainType.NODE);
        if (null == samlLogoutRequest)
            return null;

        this.challenge.setValue(samlLogoutRequest.getID());

        return samlLogoutRequest.getNameID().getValue();
    }

    /**
     * {@inheritDoc}
     */
    public void sendLogoutResponse(boolean success, HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                                             IOException {

        String samlResponseToken = LogoutResponseFactory.createLogoutResponse(this.challenge.getValue(), this.applicationName,
                this.applicationKeyPair, this.authnServiceUrl);

        String encodedSamlResponseToken = Base64.encode(samlResponseToken.getBytes());

        String templateResourceName;
        if (this.configParams.containsKey(SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM)) {
            templateResourceName = this.configParams.get(SAML2_BROWSER_POST_TEMPLATE_CONFIG_PARAM);
        } else {
            templateResourceName = SAML2_POST_BINDING_VM_RESOURCE;
        }

        ResponseUtil.sendResponse(encodedSamlResponseToken, templateResourceName, this.authnServiceUrl, response);
    }
}
