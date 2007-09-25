/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit.bean;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import net.link.safeonline.audit.AuditConstants;
import net.link.safeonline.audit.AuditMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Audit log sanitization. Fetches audit contexts from the audit queue and
 * cleans up the ones containing no relevant information.
 * 
 * @author wvdhaute
 * 
 */

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = AuditConstants.AUDIT_QUEUE_NAME) })
public class AuditLogSanitizer implements MessageListener {

	private static final Log LOG = LogFactory.getLog(AuditLogSanitizer.class);

	public void onMessage(Message msg) {
		LOG.debug("onMessage");
		try {
			AuditMessage auditMessage = new AuditMessage(msg);
			Long auditContextId = auditMessage.getAuditContextId();
			LOG.debug("sanitizing audit context: " + auditContextId);
			LOG.debug("implement me");
			// TODO: implement me
		} catch (JMSException e) {
			throw new EJBException();
		}
	}
}
