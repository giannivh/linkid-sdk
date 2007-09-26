/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit;

import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import net.link.safeonline.audit.dao.AuditAuditDAO;
import net.link.safeonline.audit.dao.AuditContextDAO;
import net.link.safeonline.audit.exception.ExistingAuditContextException;
import net.link.safeonline.audit.exception.MissingAuditContextException;
import net.link.safeonline.entity.audit.AuditContextEntity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * EJB3 Interceptor that manages the audit context. Also publishes the finalized
 * audit context id's to the audit topic
 * 
 * @author fcorneli
 * 
 */
public class AuditContextManager {

	private static final Log LOG = LogFactory.getLog(AuditContextManager.class);

	@EJB
	private AuditContextFinalizer auditContextFinalizer;

	@EJB
	private AuditContextDAO auditContextDAO;

	@EJB
	private AuditAuditDAO auditAuditDAO;

	@AroundInvoke
	public Object interceptor(InvocationContext context) throws Exception {
		initAuditContext();

		Object result;
		try {
			result = context.proceed();
		} finally {
			cleanupAuditContext();
		}
		return result;
	}

	private void cleanupAuditContext() {
		LOG.debug("cleanup audit context");
		Long auditContextId;
		try {
			auditContextId = AuditContextPolicyContextHandler
					.getAuditContextId();
			boolean isMainEntry = AuditContextPolicyContextHandler
					.removeAuditContext();
			if (isMainEntry) {
				this.auditContextFinalizer.finalizeAuditContext(auditContextId);
			}
		} catch (MissingAuditContextException e) {
			this.auditAuditDAO.addAuditAudit("missing audit context");
		}
	}

	private void initAuditContext() {
		boolean hasAuditContext = AuditContextPolicyContextHandler
				.lockAuditContext();
		if (true == hasAuditContext) {
			return;
		}
		/*
		 * In this case we need to create a new audit context and associate it
		 * with the current caller thread.
		 */
		long newAuditContextId = createNewAuditContextId();
		LOG.debug("init new audit context: " + newAuditContextId);
		try {
			AuditContextPolicyContextHandler
					.setAuditContextId(newAuditContextId);
		} catch (ExistingAuditContextException e) {
			this.auditAuditDAO.addAuditAudit("existing audit context: "
					+ e.getAuditContextId());
		}
	}

	private long createNewAuditContextId() {
		AuditContextEntity auditContext = this.auditContextDAO
				.createAuditContext();
		long auditContextId = auditContext.getId();
		return auditContextId;
	}
}
