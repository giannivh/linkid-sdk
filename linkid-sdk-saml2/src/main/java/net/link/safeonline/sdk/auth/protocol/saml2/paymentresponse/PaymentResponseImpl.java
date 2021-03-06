package net.link.safeonline.sdk.auth.protocol.saml2.paymentresponse;

import org.opensaml.saml2.core.impl.AttributeStatementImpl;


public class PaymentResponseImpl extends AttributeStatementImpl implements PaymentResponse {

    /**
     * Constructor
     *
     * @param namespaceURI     the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix  the prefix for the given namespace
     */
    public PaymentResponseImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {

        super( namespaceURI, elementLocalName, namespacePrefix );
    }
}
