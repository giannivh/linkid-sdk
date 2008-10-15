/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.sdk.auth.saml2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.util.UUID;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import net.link.safeonline.sdk.auth.saml2.Challenge;
import net.link.safeonline.sdk.auth.saml2.LogoutRequestFactory;
import net.link.safeonline.test.util.DomTestUtils;
import net.link.safeonline.test.util.PkiTestUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Unit test for logout request factory.
 * 
 * @author wvdhaute
 * 
 */
public class LogoutRequestFactoryTest {

    private static final Log LOG = LogFactory.getLog(LogoutRequestFactoryTest.class);


    @Test
    public void createLogoutRequest() throws Exception {

        // setup
        String subjectName = UUID.randomUUID().toString();
        String applicationName = "test-application-id";
        KeyPair keyPair = PkiTestUtils.generateKeyPair();
        Challenge<String> challenge = new Challenge<String>();
        String destinationURL = "https://test.idp.com/entry";

        // operate
        long begin = System.currentTimeMillis();
        String result = LogoutRequestFactory.createLogoutRequest(subjectName, applicationName, keyPair, destinationURL,
                challenge);
        long end = System.currentTimeMillis();

        // verify
        assertNotNull(result);
        LOG.debug("duration: " + (end - begin) + " ms");
        LOG.debug("result message: " + result);
        File tmpFile = File.createTempFile("saml-authn-request-", ".xml");
        FileOutputStream tmpOutput = new FileOutputStream(tmpFile);
        IOUtils.write(result, tmpOutput);
        IOUtils.closeQuietly(tmpOutput);

        String challengeValue = challenge.getValue();
        LOG.debug("challenge value: " + challengeValue);
        assertNotNull(challengeValue);

        Document resultDocument = DomTestUtils.parseDocument(result);

        Element nsElement = createNsElement(resultDocument);
        Element logoutRequestElement = (Element) XPathAPI.selectSingleNode(resultDocument, "/samlp2:LogoutRequest",
                nsElement);
        assertNotNull(logoutRequestElement);

        Element issuerElement = (Element) XPathAPI.selectSingleNode(resultDocument,
                "/samlp2:LogoutRequest/saml2:Issuer", nsElement);
        assertNotNull(issuerElement);
        assertEquals(applicationName, issuerElement.getTextContent());

        Node destinationNode = XPathAPI.selectSingleNode(resultDocument, "/samlp2:LogoutRequest/@Destination",
                nsElement);
        assertNotNull(destinationNode);
        assertEquals(destinationURL, destinationNode.getTextContent());

        Element nameIDElement = (Element) XPathAPI.selectSingleNode(resultDocument,
                "/samlp2:LogoutRequest/saml2:NameID", nsElement);
        assertNotNull(nameIDElement);
        assertEquals(subjectName, nameIDElement.getTextContent());

        Node formatNode = XPathAPI.selectSingleNode(resultDocument, "/samlp2:LogoutRequest/saml2:NameID/@Format",
                nsElement);
        assertNotNull(formatNode);
        assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:entity", formatNode.getTextContent());

        // verify signature
        NodeList signatureNodeList = resultDocument.getElementsByTagNameNS(javax.xml.crypto.dsig.XMLSignature.XMLNS,
                "Signature");
        assertEquals(1, signatureNodeList.getLength());

        DOMValidateContext validateContext = new DOMValidateContext(keyPair.getPublic(), signatureNodeList.item(0));
        XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM",
                new org.jcp.xml.dsig.internal.dom.XMLDSigRI());

        XMLSignature signature = signatureFactory.unmarshalXMLSignature(validateContext);
        boolean resultValidity = signature.validate(validateContext);
        assertTrue(resultValidity);

        Element dsNsElement = resultDocument.createElement("nsElement");
        dsNsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
        XObject xObject = XPathAPI.eval(resultDocument, "count(//ds:Reference)", dsNsElement);
        LOG.debug("count: " + xObject.num());
        assertEquals(1.0, xObject.num(), 0);
    }

    private Element createNsElement(Document document) {

        Element nsElement = document.createElement("nsElement");
        nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:samlp2", "urn:oasis:names:tc:SAML:2.0:protocol");
        nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:saml2", "urn:oasis:names:tc:SAML:2.0:assertion");
        return nsElement;
    }
}