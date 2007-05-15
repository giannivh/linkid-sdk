/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.ws;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.util.WSSecurityUtil;
import org.w3c.dom.Document;

/**
 * JAX-WS SOAP Handler that provides the client-side WS-Security. This handler
 * will add the WS-Security SOAP header element as required by the SafeOnline
 * web service authentication module. Per default this handler will sign the
 * Body element of the SOAP envelope. You can make this handler to sign
 * additional XML elements via the
 * {@link #addToBeSignedId(String, SOAPMessageContext)} method.
 * 
 * @author fcorneli
 * 
 */
public class WSSecurityClientHandler implements SOAPHandler<SOAPMessageContext> {

	private static final Log LOG = LogFactory
			.getLog(WSSecurityClientHandler.class);

	public static final String TO_BE_SIGNED_IDS_SET = WSSecurityClientHandler.class
			+ ".tbs";

	private final X509Certificate certificate;

	private final PrivateKey privateKey;

	/**
	 * Main constructor.
	 * 
	 * @param certificate
	 *            the client X509 certificate.
	 * @param privateKey
	 *            the private key corresponding with the client X509
	 *            certificate.
	 */
	public WSSecurityClientHandler(X509Certificate certificate,
			PrivateKey privateKey) {
		this.certificate = certificate;
		this.privateKey = privateKey;
	}

	public Set<QName> getHeaders() {
		return null;
	}

	public void close(MessageContext messageContext) {
		// empty
	}

	public boolean handleFault(SOAPMessageContext soapMessageContext) {
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean handleMessage(SOAPMessageContext soapMessageContext) {
		Boolean outboundProperty = (Boolean) soapMessageContext
				.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		if (false == outboundProperty.booleanValue()) {
			/*
			 * We only need to add the WS-Security SOAP header to the outbound
			 * messages.
			 */
			return true;
		}

		SOAPMessage soapMessage = soapMessageContext.getMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();

		Set<String> tbsIds = (Set<String>) soapMessageContext
				.get(TO_BE_SIGNED_IDS_SET);

		handleDocument(soapPart, tbsIds);

		return true;
	}

	/**
	 * @param document
	 * @param tbsIds
	 *            the optional set of XML Id's to be signed.
	 */
	private void handleDocument(Document document, Set<String> tbsIds) {
		LOG.debug("adding WS-Security SOAP header");
		WSSecSignature wsSecSignature = new WSSecSignature();
		wsSecSignature.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
		Crypto crypto = new ClientCrypto(this.certificate, this.privateKey);
		WSSecHeader wsSecHeader = new WSSecHeader();
		wsSecHeader.insertSecurityHeader(document);
		try {
			wsSecSignature.prepare(document, crypto, wsSecHeader);

			SOAPConstants soapConstants = WSSecurityUtil
					.getSOAPConstants(document.getDocumentElement());

			Vector<WSEncryptionPart> wsEncryptionParts = new Vector<WSEncryptionPart>();
			WSEncryptionPart wsEncryptionPart = new WSEncryptionPart(
					soapConstants.getBodyQName().getLocalPart(), soapConstants
							.getEnvelopeURI(), "Content");
			wsEncryptionParts.add(wsEncryptionPart);

			WSSecTimestamp wsSecTimeStamp = new WSSecTimestamp();
			wsSecTimeStamp.setTimeToLive(0);
			wsSecTimeStamp.prepare(document);
			wsSecTimeStamp.prependToHeader(wsSecHeader);
			wsEncryptionParts.add(new WSEncryptionPart(wsSecTimeStamp.getId()));

			if (null != tbsIds) {
				for (String tbsId : tbsIds) {
					wsEncryptionParts.add(new WSEncryptionPart(tbsId));
				}
			}

			wsSecSignature.addReferencesToSign(wsEncryptionParts, wsSecHeader);

			wsSecSignature.prependToHeader(wsSecHeader);

			wsSecSignature.prependBSTElementToHeader(wsSecHeader);

			wsSecSignature.computeSignature();

		} catch (WSSecurityException e) {
			throw new RuntimeException("WSS4J error: " + e.getMessage(), e);
		}
	}

	/**
	 * Add an XML Id that needs to be included in the WS-Security signature
	 * digest.
	 * 
	 * @param id
	 * @param context
	 */
	@SuppressWarnings("unchecked")
	public static void addToBeSignedId(String id, SOAPMessageContext context) {
		Set<String> toBeSignedIds = (Set<String>) context
				.get(TO_BE_SIGNED_IDS_SET);
		if (null == toBeSignedIds) {
			toBeSignedIds = new TreeSet<String>();
			context.put(TO_BE_SIGNED_IDS_SET, toBeSignedIds);
		}
		toBeSignedIds.add(id);
	}
}