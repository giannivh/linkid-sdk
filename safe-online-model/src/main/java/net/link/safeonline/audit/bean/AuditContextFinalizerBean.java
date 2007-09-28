/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit.bean;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import net.link.safeonline.audit.AuditConstants;
import net.link.safeonline.audit.AuditContextFinalizer;
import net.link.safeonline.audit.AuditMessage;
import net.link.safeonline.audit.dao.AuditAuditDAO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of the audit context finalizer component. Important here is
 * that this component runs within it's own transaction.
 * 
 * @author fcorneli
 * 
 */
@Stateless
public class AuditContextFinalizerBean implements AuditContextFinalizer {

	private static final Log LOG = LogFactory
			.getLog(AuditContextFinalizerBean.class);

	@Resource(mappedName = AuditConstants.CONNECTION_FACTORY_NAME)
	private TopicConnectionFactory factory;

	@Resource(mappedName = AuditConstants.AUDIT_TOPIC_NAME)
	private Topic auditTopic;

	@EJB
	private AuditAuditDAO auditAuditDAO;

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void finalizeAuditContext(Long auditContextId) {
		LOG.debug("finalizing audit context: " + auditContextId);
		AuditMessage auditMessage = new AuditMessage(auditContextId);
		try {
			TopicConnection connection = this.factory.createTopicConnection();
			try {
				TopicSession session = connection.createTopicSession(true,
						Session.AUTO_ACKNOWLEDGE);
				try {
					TopicPublisher publisher = session
							.createPublisher(this.auditTopic);
					try {
						Message message = auditMessage.getJMSMessage(session);
						publisher.publish(message);
						LOG.info("Audit JMS message (id=" + auditContextId
								+ ") published to "
								+ this.auditTopic.getTopicName() + " topic");
					} finally {
						publisher.close();
					}
				} finally {
					session.close();
				}
			} finally {
				connection.close();
			}
		} catch (JMSException e) {
			this.auditAuditDAO.addAuditAudit("unable to publish audit context "
					+ auditContextId + " - reason: " + e.getMessage()
					+ " - errorCode: " + e.getErrorCode());
		}
	}
}