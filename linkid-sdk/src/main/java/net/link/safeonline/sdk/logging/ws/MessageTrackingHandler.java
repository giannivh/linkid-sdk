/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.logging.ws;

import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;


/**
 * JAX-WS SOAP handler to log the SOAP messages.
 *
 * @author fcorneli
 */
public class MessageTrackingHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Log LOG = LogFactory.getLog( MessageTrackingHandler.class );

    private boolean captureMessages;

    private Document inboundMessage;

    private Document outboundMessage;

    public MessageTrackingHandler() {

        captureMessages = true;
    }

    /**
     * Set to <code>true</code> if you want to log the inbound and outbound SOAP messages.
     */
    public void setCaptureMessages(boolean captureMessages) {

        this.captureMessages = captureMessages;
    }

    /**
     * Returns <code>true</code> if this handler will capture the inbound and outbound SOAP messages during following web service
     * invocations.
     */
    public boolean isCaptureMessages() {

        return captureMessages;
    }

    public Set<QName> getHeaders() {

        return null;
    }

    public void close(MessageContext messageContext) {

    }

    public boolean handleFault(SOAPMessageContext soapMessageContext) {

        return true;
    }

    public boolean handleMessage(SOAPMessageContext soapMessageContext) {

        if (false == captureMessages)
            /*
             * Nothing to do.
             */
            return true;
        LOG.debug( "logging message" );

        Boolean outboundProperty = (Boolean) soapMessageContext.get( MessageContext.MESSAGE_OUTBOUND_PROPERTY );

        try {
            SOAPMessage soapMessage = soapMessageContext.getMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();

            if (outboundProperty)
                outboundMessage = soapPart;
            else
                inboundMessage = soapPart;
        } catch (Exception e) {
            LOG.debug( "exception caught: " + e.getMessage(), e );
        }

        return true;
    }

    public Document getInboundMessage() {

        return inboundMessage;
    }

    public Document getOutboundMessage() {

        return outboundMessage;
    }
}
