/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.ws.attrib;

import java.net.ConnectException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.BindingProvider;

import net.link.safeonline.attrib.ws.SAMLAttributeServiceFactory;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.ApplicationAuthenticationUtils;
import oasis.names.tc.saml._2_0.assertion.AssertionType;
import oasis.names.tc.saml._2_0.assertion.AttributeStatementType;
import oasis.names.tc.saml._2_0.assertion.AttributeType;
import oasis.names.tc.saml._2_0.assertion.NameIDType;
import oasis.names.tc.saml._2_0.assertion.ObjectFactory;
import oasis.names.tc.saml._2_0.assertion.StatementAbstractType;
import oasis.names.tc.saml._2_0.assertion.SubjectType;
import oasis.names.tc.saml._2_0.protocol.AttributeQueryType;
import oasis.names.tc.saml._2_0.protocol.ResponseType;
import oasis.names.tc.saml._2_0.protocol.SAMLAttributePort;
import oasis.names.tc.saml._2_0.protocol.SAMLAttributeService;
import oasis.names.tc.saml._2_0.protocol.StatusCodeType;
import oasis.names.tc.saml._2_0.protocol.StatusType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.xml.ws.client.ClientTransportException;

/**
 * Implementation of attribute client. This class is using JAX-WS, secured via
 * WS-Security and server-side SSL.
 * 
 * @author fcorneli
 * 
 */
public class AttributeClientImpl implements AttributeClient {

	private static final Log LOG = LogFactory.getLog(AttributeClientImpl.class);

	private SAMLAttributePort port;

	public AttributeClientImpl(String location,
			X509Certificate clientCertificate, PrivateKey clientPrivateKey) {
		SAMLAttributeService attributeService = SAMLAttributeServiceFactory
				.newInstance();
		this.port = attributeService.getSAMLAttributePort();

		setEndpointAddress(location);

		ApplicationAuthenticationUtils.initWsSecurity(this.port,
				clientCertificate, clientPrivateKey);
	}

	public String getAttributeValue(String subjectLogin, String attributeName)
			throws AttributeNotFoundException, RequestDeniedException,
			ConnectException {
		LOG.debug("get attribute value for subject " + subjectLogin
				+ " attribute name " + attributeName);

		AttributeQueryType request = getAttributeQuery(subjectLogin,
				attributeName);

		ApplicationAuthenticationUtils.configureSsl();

		ResponseType response = getResponse(request);

		checkStatus(response);

		Map<String, String> attributes = new HashMap<String, String>();
		getAttributeValues(response, attributes);

		String value = attributes.get(attributeName);
		if (null == value) {
			throw new RuntimeException(
					"requested attribute not in result attributes");
		}
		return value;
	}

	private ResponseType getResponse(AttributeQueryType request)
			throws ConnectException {
		ResponseType response;
		try {
			response = this.port.attributeQuery(request);
		} catch (ClientTransportException e) {
			throw new ConnectException(e.getMessage());
		}
		return response;
	}

	private void checkStatus(ResponseType response)
			throws AttributeNotFoundException, RequestDeniedException {
		StatusType status = response.getStatus();
		StatusCodeType statusCode = status.getStatusCode();
		String statusCodeValue = statusCode.getValue();
		if (false == "urn:oasis:names:tc:SAML:2.0:status:Success"
				.equals(statusCodeValue)) {
			LOG.error("status code: " + statusCodeValue);
			LOG.error("status message: " + status.getStatusMessage());
			StatusCodeType secondLevelStatusCode = statusCode.getStatusCode();
			if (null != secondLevelStatusCode) {
				String secondLevelStatusCodeValue = secondLevelStatusCode
						.getValue();
				if ("urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue"
						.equals(secondLevelStatusCodeValue)) {
					throw new AttributeNotFoundException();
				} else if ("urn:oasis:names:tc:SAML:2.0:status:RequestDenied"
						.equals(secondLevelStatusCodeValue)) {
					throw new RequestDeniedException();
				}
				LOG.debug("second level status code: "
						+ secondLevelStatusCode.getValue());
			}
			throw new RuntimeException("error: " + statusCodeValue);
		}
	}

	private void setEndpointAddress(String location) {
		BindingProvider bindingProvider = (BindingProvider) this.port;

		bindingProvider.getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				"https://" + location + "/safe-online-ws/attrib");
	}

	private AttributeQueryType getAttributeQuery(String subjectLogin,
			String attributeName) {
		Set<String> attributeNames = Collections.singleton(attributeName);
		AttributeQueryType attributeQuery = getAttributeQuery(subjectLogin,
				attributeNames);
		return attributeQuery;
	}

	private AttributeQueryType getAttributeQuery(String subjectLogin,
			Set<String> attributeNames) {
		ObjectFactory samlObjectFactory = new ObjectFactory();
		AttributeQueryType attributeQuery = new AttributeQueryType();
		SubjectType subject = new SubjectType();
		NameIDType subjectName = new NameIDType();
		subjectName.setValue(subjectLogin);
		subject.getContent().add(samlObjectFactory.createNameID(subjectName));
		attributeQuery.setSubject(subject);

		List<AttributeType> attributes = attributeQuery.getAttribute();
		for (String attributeName : attributeNames) {
			AttributeType attribute = new AttributeType();
			attribute.setName(attributeName);
			attributes.add(attribute);
		}
		return attributeQuery;
	}

	private AttributeQueryType getAttributeQuery(String subjectLogin,
			Map<String, String> attributes) {
		Set<String> attributeNames = attributes.keySet();
		AttributeQueryType attributeQuery = getAttributeQuery(subjectLogin,
				attributeNames);
		return attributeQuery;
	}

	public void getAttributeValues(String subjectLogin,
			Map<String, String> attributes) throws AttributeNotFoundException,
			RequestDeniedException, ConnectException {
		AttributeQueryType request = getAttributeQuery(subjectLogin, attributes);
		ApplicationAuthenticationUtils.configureSsl();
		ResponseType response = getResponse(request);
		checkStatus(response);
		getAttributeValues(response, attributes);
	}

	private void getAttributeValues(ResponseType response,
			Map<String, String> attributes) {
		List<Object> assertions = response.getAssertionOrEncryptedAssertion();
		if (0 == assertions.size()) {
			throw new RuntimeException("No assertions in response");
		}
		AssertionType assertion = (AssertionType) assertions.get(0);

		List<StatementAbstractType> statements = assertion
				.getStatementOrAuthnStatementOrAuthzDecisionStatement();
		if (0 == statements.size()) {
			throw new RuntimeException("No statements in response assertion");
		}
		AttributeStatementType attributeStatement = (AttributeStatementType) statements
				.get(0);
		List<Object> attributeObjects = attributeStatement
				.getAttributeOrEncryptedAttribute();
		for (Object attributeObject : attributeObjects) {
			AttributeType attribute = (AttributeType) attributeObject;
			String attributeName = attribute.getName();
			List<Object> attributeValues = attribute.getAttributeValue();
			String attributeValue = (String) attributeValues.get(0);
			attributes.put(attributeName, attributeValue);
		}
	}

	public Map<String, String> getAttributeValues(String subjectLogin)
			throws RequestDeniedException, ConnectException,
			AttributeNotFoundException {
		Map<String, String> attributes = new HashMap<String, String>();
		AttributeQueryType request = getAttributeQuery(subjectLogin, attributes);
		ApplicationAuthenticationUtils.configureSsl();
		ResponseType response = getResponse(request);
		checkStatus(response);
		getAttributeValues(response, attributes);
		return attributes;
	}
}