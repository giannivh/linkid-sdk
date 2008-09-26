/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.protocol.saml2;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.auth.LoginManager;
import net.link.safeonline.auth.protocol.AuthenticationServiceManager;
import net.link.safeonline.auth.protocol.ProtocolException;
import net.link.safeonline.auth.protocol.ProtocolHandler;
import net.link.safeonline.authentication.LogoutProtocolContext;
import net.link.safeonline.authentication.ProtocolContext;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.AuthenticationInitializationException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.exception.SubscriptionNotFoundException;
import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.pkix.exception.TrustDomainNotFoundException;
import net.link.safeonline.sdk.auth.saml2.RequestUtil;
import net.link.safeonline.sdk.auth.saml2.ResponseUtil;
import net.link.safeonline.sdk.auth.saml2.SamlRequestSecurityPolicyResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.saml2.binding.decoding.HTTPPostDecoder;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.security.SecurityException;


/**
 * Server-side protocol handler for the SAML2 Browser POST authentication protocol.
 * 
 * @author fcorneli
 * 
 */
public class Saml2PostProtocolHandler implements ProtocolHandler {

    private static final Log   LOG                            = LogFactory.getLog(Saml2PostProtocolHandler.class);

    public static final String NAME                           = "SAML v2 Browser POST Authentication Protocol";

    public static final String SAML2_POST_BINDING_VM_RESOURCE = "/net/link/safeonline/device/sdk/saml2/binding/saml2-post-binding.vm";

    static {
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            throw new RuntimeException("could not bootstrap the OpenSAML2 library");
        }
    }


    public String getName() {

        return NAME;
    }

    public ProtocolContext handleRequest(HttpServletRequest authnRequest) throws ProtocolException {

        LOG.debug("request method: " + authnRequest.getMethod());
        if (false == "POST".equals(authnRequest.getMethod()))
            return null;
        LOG.debug("POST request");
        String language = authnRequest.getParameter("Language");
        LOG.debug("Language parameter: " + language);

        String encodedSamlRequest = authnRequest.getParameter("SAMLRequest");
        if (null == encodedSamlRequest)
            return null;
        LOG.debug("SAMLRequest parameter found");

        BasicSAMLMessageContext<SAMLObject, SAMLObject, SAMLObject> messageContext = new BasicSAMLMessageContext<SAMLObject, SAMLObject, SAMLObject>();
        messageContext.setInboundMessageTransport(new HttpServletRequestAdapter(authnRequest));

        messageContext.setSecurityPolicyResolver(new SamlRequestSecurityPolicyResolver());

        HTTPPostDecoder decoder = new HTTPPostDecoder();
        try {
            decoder.decode(messageContext);
        } catch (MessageDecodingException e) {
            LOG.debug("SAML message decoding error: " + e.getMessage());
            throw new ProtocolException("SAML message decoding error");
        } catch (SecurityPolicyException e) {
            LOG.debug("security policy error: " + e.getMessage());
            throw new ProtocolException("security policy error");
        } catch (SecurityException e) {
            LOG.debug("security error: " + e.getMessage());
            throw new ProtocolException("security error");
        }

        SAMLObject samlMessage = messageContext.getInboundSAMLMessage();
        if (false == samlMessage instanceof AuthnRequest) {
            throw new ProtocolException("SAML message not an authentication request message");
        }
        AuthnRequest samlAuthnRequest = (AuthnRequest) samlMessage;

        AuthenticationService authenticationService = AuthenticationServiceManager
                .getAuthenticationService(authnRequest.getSession());
        try {
            return authenticationService.initialize(language, samlAuthnRequest);
        } catch (TrustDomainNotFoundException e) {
            LOG.debug("trust domain not found: " + e.getMessage());
            throw new ProtocolException("Trust domain not found");
        } catch (AuthenticationInitializationException e) {
            LOG.debug("authentication intialization error: " + e.getMessage());
            throw new ProtocolException("authentication intialization error: " + e.getMessage());
        } catch (ApplicationNotFoundException e) {
            LOG.debug("application not found: " + e.getMessage());
            throw new ProtocolException("application not found");
        }
    }

    public void authnResponse(HttpSession session, HttpServletResponse authnResponse) throws ProtocolException {

        String target = LoginManager.getTarget(session);

        String encodedSamlResponseToken;
        try {
            encodedSamlResponseToken = AuthenticationServiceManager.finalizeAuthentication(session);
        } catch (NodeNotFoundException e) {
            throw new ProtocolException("Node not found: " + e.getMessage());
        } catch (SubscriptionNotFoundException e) {
            throw new ProtocolException("Subscription not found: " + e.getMessage());
        } catch (ApplicationNotFoundException e) {
            throw new ProtocolException("Application not found: " + e.getMessage());
        }

        String templateResourceName = SAML2_POST_BINDING_VM_RESOURCE;

        try {
            ResponseUtil.sendResponse(encodedSamlResponseToken, templateResourceName, target, authnResponse);
        } catch (ServletException e) {
            throw new ProtocolException(e.getMessage());
        } catch (IOException e) {
            throw new ProtocolException(e.getMessage());
        }
    }

    public LogoutProtocolContext handleLogoutRequest(HttpServletRequest logoutRequest) throws ProtocolException {

        LOG.debug("request method: " + logoutRequest.getMethod());
        if (false == "POST".equals(logoutRequest.getMethod()))
            return null;
        LOG.debug("POST request");

        String encodedSamlRequest = logoutRequest.getParameter("SAMLRequest");
        if (null == encodedSamlRequest)
            return null;
        LOG.debug("SAMLRequest parameter found");

        BasicSAMLMessageContext<SAMLObject, SAMLObject, SAMLObject> messageContext = new BasicSAMLMessageContext<SAMLObject, SAMLObject, SAMLObject>();
        messageContext.setInboundMessageTransport(new HttpServletRequestAdapter(logoutRequest));

        messageContext.setSecurityPolicyResolver(new SamlRequestSecurityPolicyResolver());

        HTTPPostDecoder decoder = new HTTPPostDecoder();
        try {
            decoder.decode(messageContext);
        } catch (MessageDecodingException e) {
            LOG.debug("SAML message decoding error: " + e.getMessage());
            throw new ProtocolException("SAML message decoding error");
        } catch (SecurityPolicyException e) {
            LOG.debug("security policy error: " + e.getMessage());
            throw new ProtocolException("security policy error");
        } catch (SecurityException e) {
            LOG.debug("security error: " + e.getMessage());
            throw new ProtocolException("security error");
        }

        SAMLObject samlMessage = messageContext.getInboundSAMLMessage();
        if (false == samlMessage instanceof LogoutRequest) {
            throw new ProtocolException("SAML message not a logout request message");
        }
        LogoutRequest samlLogoutRequest = (LogoutRequest) samlMessage;

        AuthenticationService authenticationService = AuthenticationServiceManager
                .getAuthenticationService(logoutRequest.getSession());
        try {
            return authenticationService.initialize(samlLogoutRequest);
        } catch (TrustDomainNotFoundException e) {
            LOG.debug("trust domain not found: " + e.getMessage());
            throw new ProtocolException("Trust domain not found");
        } catch (AuthenticationInitializationException e) {
            LOG.debug("authentication intialization error: " + e.getMessage());
            throw new ProtocolException("authentication intialization error: " + e.getMessage());
        } catch (ApplicationNotFoundException e) {
            LOG.debug("application not found: " + e.getMessage());
            throw new ProtocolException("application not found");
        } catch (SubjectNotFoundException e) {
            LOG.debug("subject not found: " + e.getMessage());
            throw new ProtocolException("subject not found");

        }
    }

    /**
     * {@inheritDoc}
     */
    public String handleLogoutResponse(HttpServletRequest httpRequest) throws ProtocolException {

        AuthenticationService authenticationService = AuthenticationServiceManager.getAuthenticationService(httpRequest
                .getSession());
        String applicationName;
        try {
            applicationName = authenticationService.handleLogoutResponse(httpRequest);
        } catch (NodeNotFoundException e) {
            throw new ProtocolException("Node not found: " + e.getMessage());
        } catch (ServletException e) {
            throw new ProtocolException(e.getMessage());
        }
        LOG.debug("application: " + applicationName);
        return applicationName;
    }

    /**
     * {@inheritDoc}
     */
    public void logoutRequest(ApplicationEntity application, HttpSession session, HttpServletResponse response)
            throws ProtocolException {

        String target = application.getSsoLogoutUrl().toString();

        AuthenticationService authenticationService = AuthenticationServiceManager.getAuthenticationService(session);
        String encodedSamlLogoutRequestToken;
        try {
            encodedSamlLogoutRequestToken = authenticationService.getLogoutRequest(application);
        } catch (NodeNotFoundException e) {
            throw new ProtocolException("Node not found: " + e.getMessage());
        } catch (SubscriptionNotFoundException e) {
            throw new ProtocolException("Subscription not found: " + e.getMessage());
        } catch (ApplicationNotFoundException e) {
            throw new ProtocolException("Application not found: " + e.getMessage());
        }

        String templateResourceName = SAML2_POST_BINDING_VM_RESOURCE;

        try {
            RequestUtil.sendRequest(target, encodedSamlLogoutRequestToken, templateResourceName, response);
        } catch (ServletException e) {
            throw new ProtocolException(e.getMessage());
        } catch (IOException e) {
            throw new ProtocolException(e.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    public void logoutResponse(boolean partialLogout, String target, HttpSession session,
            HttpServletResponse logoutResponse) throws ProtocolException {

        String encodedSamlLogoutResponseToken;
        try {
            encodedSamlLogoutResponseToken = AuthenticationServiceManager.finalizeLogout(partialLogout, session);
        } catch (NodeNotFoundException e) {
            throw new ProtocolException("Node not found: " + e.getMessage());
        }

        String templateResourceName = SAML2_POST_BINDING_VM_RESOURCE;

        try {
            ResponseUtil.sendResponse(encodedSamlLogoutResponseToken, templateResourceName, target, logoutResponse);
        } catch (ServletException e) {
            throw new ProtocolException(e.getMessage());
        } catch (IOException e) {
            throw new ProtocolException(e.getMessage());
        }

    }

}
