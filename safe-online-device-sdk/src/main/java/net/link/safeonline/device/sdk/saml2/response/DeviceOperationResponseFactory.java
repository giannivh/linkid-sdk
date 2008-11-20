/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.device.sdk.saml2.response;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import net.link.safeonline.device.sdk.saml2.DeviceOperationType;
import net.link.safeonline.sdk.auth.saml2.DomUtils;

import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.impl.SignatureBuilder;
import org.w3c.dom.Element;


/**
 * Factory for SAML2 authentication responses.
 * 
 * @author fcorneli
 * 
 */
public class DeviceOperationResponseFactory {

    static {
        /*
         * Next is because Sun loves to endorse crippled versions of Xerces.
         */
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema",
                "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        try {
            DefaultBootstrap.bootstrap();
            Configuration.registerObjectProvider(DeviceOperationResponse.DEFAULT_ELEMENT_NAME, new DeviceOperationResponseBuilder(),
                    new DeviceOperationResponseMarshaller(), new DeviceOperationResponseUnmarshaller(), null);
        } catch (ConfigurationException e) {
            throw new RuntimeException("could not bootstrap the OpenSAML2 library");
        }
    }


    private DeviceOperationResponseFactory() {

        // empty
    }

    /**
     * Creates a signed device operation response with status SUCCESS.
     * 
     * @param deviceOperation
     *            The device operation executed
     */
    public static String createDeviceOperationResponse(String inResponseTo, DeviceOperationType deviceOperation, String issuerName,
                                                       String subjectName, String device, KeyPair signerKeyPair, int validity, String target) {

        return createDeviceOperationResponse(inResponseTo, deviceOperation, issuerName, subjectName, device, signerKeyPair, validity,
                target, StatusCode.SUCCESS_URI);
    }

    /**
     * Creates a signed device operation response with status failed.
     */
    public static String createDeviceOperationResponseFailed(String inResponseTo, DeviceOperationType deviceOperation, String issuerName,
                                                             String subjectName, String device, KeyPair signerKeyPair, int validity,
                                                             String target) {

        return createDeviceOperationResponse(inResponseTo, deviceOperation, issuerName, subjectName, device, signerKeyPair, validity,
                target, DeviceOperationResponse.FAILED_URI);
    }

    /**
     * Creates a signed authentication response with status unsupported.
     */
    public static String createDeviceOperationResponseUnsupported(String inResponseTo, DeviceOperationType deviceOperation,
                                                                  String issuerName, String subjectName, String device,
                                                                  KeyPair signerKeyPair, int validity, String target) {

        return createDeviceOperationResponse(inResponseTo, deviceOperation, issuerName, subjectName, device, signerKeyPair, validity,
                target, StatusCode.REQUEST_UNSUPPORTED_URI);
    }

    private static String createDeviceOperationResponse(String inResponseTo, DeviceOperationType deviceOperation, String issuerName,
                                                        String subjectName, String device, KeyPair signerKeyPair, int validity,
                                                        String target, String statusCodeURI) {

        if (null == signerKeyPair)
            throw new IllegalArgumentException("signer key pair should not be null");
        if (null == issuerName)
            throw new IllegalArgumentException("issuer name should not be null");
        if (null == deviceOperation)
            throw new IllegalArgumentException("deviceOperation should not be null");
        if (null == device)
            throw new IllegalArgumentException("device should not be null");
        if (null == subjectName)
            throw new IllegalArgumentException("subjectName should not be null");

        DeviceOperationResponse response = buildXMLObject(DeviceOperationResponse.class, DeviceOperationResponse.DEFAULT_ELEMENT_NAME);

        DateTime now = new DateTime();

        SecureRandomIdentifierGenerator idGenerator;
        try {
            idGenerator = new SecureRandomIdentifierGenerator();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("secure random init error: " + e.getMessage(), e);
        }
        response.setID(idGenerator.generateIdentifier());
        response.setVersion(SAMLVersion.VERSION_20);
        response.setInResponseTo(inResponseTo);
        response.setIssueInstant(now);

        Issuer responseIssuer = buildXMLObject(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
        responseIssuer.setValue(issuerName);
        response.setIssuer(responseIssuer);

        response.setDestination(target);
        response.setDeviceOperation(deviceOperation.name());
        response.setDevice(device);
        response.setSubjectName(subjectName);

        Status status = buildXMLObject(Status.class, Status.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = buildXMLObject(StatusCode.class, StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(statusCodeURI);
        status.setStatusCode(statusCode);
        response.setStatus(status);

        if (statusCodeURI.equals(StatusCode.SUCCESS_URI) && deviceOperation.equals(DeviceOperationType.NEW_ACCOUNT_REGISTER)) {
            addAssertion(response, inResponseTo, subjectName, issuerName, device, validity, target, new DateTime());
        }

        return signResponse(response, signerKeyPair);
    }

    /**
     * Adds an assertion to the unsigned response.
     * 
     * @param response
     * @param subjectName
     * @param audienceName
     *            This can be or the application name authenticated for, or the device operation executed
     */
    private static void addAssertion(DeviceOperationResponse response, String inResponseTo, String subjectName, String issuerName,
                                     String samlName, int validity, String target, DateTime authenticationDate) {

        DateTime now = new DateTime();
        DateTime notAfter = now.plusSeconds(validity);

        SecureRandomIdentifierGenerator idGenerator;
        try {
            idGenerator = new SecureRandomIdentifierGenerator();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("secure random init error: " + e.getMessage(), e);
        }

        Assertion assertion = buildXMLObject(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(idGenerator.generateIdentifier());
        assertion.setIssueInstant(now);
        response.getAssertions().add(assertion);

        Issuer assertionIssuer = buildXMLObject(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
        assertionIssuer.setValue(issuerName);
        assertion.setIssuer(assertionIssuer);

        Subject subject = buildXMLObject(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
        NameID nameID = buildXMLObject(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue(subjectName);
        nameID.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        subject.setNameID(nameID);
        assertion.setSubject(subject);

        Conditions conditions = buildXMLObject(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(now);
        conditions.setNotOnOrAfter(notAfter);
        assertion.setConditions(conditions);

        List<SubjectConfirmation> subjectConfirmations = subject.getSubjectConfirmations();
        SubjectConfirmation subjectConfirmation = buildXMLObject(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
        SubjectConfirmationData subjectConfirmationData = buildXMLObject(SubjectConfirmationData.class,
                SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        subjectConfirmationData.setRecipient(target);
        subjectConfirmationData.setInResponseTo(inResponseTo);
        subjectConfirmationData.setNotBefore(now);
        subjectConfirmationData.setNotOnOrAfter(notAfter);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subjectConfirmations.add(subjectConfirmation);

        AuthnStatement authnStatement = buildXMLObject(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
        assertion.getAuthnStatements().add(authnStatement);
        authnStatement.setAuthnInstant(authenticationDate);
        AuthnContext authnContext = buildXMLObject(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnContext(authnContext);

        AuthnContextClassRef authnContextClassRef = buildXMLObject(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnContextClassRef.setAuthnContextClassRef(samlName);
    }

    /**
     * Sign the unsigned device operation response.
     */
    private static String signResponse(DeviceOperationResponse response, KeyPair signerKeyPair) {

        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        SignatureBuilder signatureBuilder = (SignatureBuilder) builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME);
        Signature signature = signatureBuilder.buildObject();
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        String algorithm = signerKeyPair.getPrivate().getAlgorithm();
        if ("RSA".equals(algorithm)) {
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA);
        } else if ("DSA".equals(algorithm)) {
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_DSA);
        }
        response.setSignature(signature);
        BasicCredential signingCredential = SecurityHelper.getSimpleCredential(signerKeyPair.getPublic(), signerKeyPair.getPrivate());
        signature.setSigningCredential(signingCredential);

        // marshalling
        MarshallerFactory marshallerFactory = Configuration.getMarshallerFactory();
        Marshaller marshaller = marshallerFactory.getMarshaller(response);
        Element requestElement;
        try {
            requestElement = marshaller.marshall(response);
        } catch (MarshallingException e) {
            throw new RuntimeException("opensaml2 marshalling error: " + e.getMessage(), e);
        }

        // sign after marshaling of course
        try {
            Signer.signObject(signature);
        } catch (SignatureException e) {
            throw new RuntimeException("opensaml2 signing error: " + e.getMessage(), e);
        }

        String result;
        try {
            result = DomUtils.domToString(requestElement);
        } catch (TransformerException e) {
            throw new RuntimeException("DOM to string error: " + e.getMessage(), e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <Type extends SAMLObject> Type buildXMLObject(@SuppressWarnings("unused") Class<Type> clazz, QName objectQName) {

        XMLObjectBuilder<Type> builder = Configuration.getBuilderFactory().getBuilder(objectQName);
        if (builder == null)
            throw new RuntimeException("Unable to retrieve builder for object QName " + objectQName);
        Type object = builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(), objectQName.getPrefix());
        return object;
    }
}