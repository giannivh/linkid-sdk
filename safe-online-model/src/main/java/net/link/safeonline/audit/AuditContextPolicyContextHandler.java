/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit;

import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;

import net.link.safeonline.audit.exception.ExistingAuditContextException;
import net.link.safeonline.audit.exception.MissingAuditContextException;

/**
 * JACC policy context handler for audit context information.
 * 
 * @author fcorneli
 * 
 */
public class AuditContextPolicyContextHandler implements PolicyContextHandler {

	public static final String AUDIT_CONTEXT_KEY = "net.link.safeonline.audit.context";

	private static ThreadLocal<Long> auditContext = new ThreadLocal<Long>();

	public Object getContext(String key, Object data)
			throws PolicyContextException {
		if (false == key.equalsIgnoreCase(AUDIT_CONTEXT_KEY)) {
			return null;
		}
		return auditContext.get();
	}

	public String[] getKeys() throws PolicyContextException {
		String[] keys = { AUDIT_CONTEXT_KEY };
		return keys;
	}

	public boolean supports(String key) throws PolicyContextException {
		return key.equalsIgnoreCase(AUDIT_CONTEXT_KEY);
	}

	/**
	 * Sets the audit context Id for the current thread.
	 * 
	 * @param auditContextId
	 * @throws ExistingAuditContextException
	 */
	public static synchronized void setAuditContextId(long auditContextId)
			throws ExistingAuditContextException {
		Long previousAuditContextId = auditContext.get();
		if (null != previousAuditContextId) {
			throw new ExistingAuditContextException(previousAuditContextId);
		}
		auditContext.set(auditContextId);
	}

	/**
	 * Removes the audit context for the current thread.
	 * 
	 * @throws MissingAuditContextException
	 */
	public static synchronized void removeAuditContext()
			throws MissingAuditContextException {
		Long auditContextId = auditContext.get();
		if (null == auditContextId) {
			throw new MissingAuditContextException();
		}
		auditContext.remove();
	}
}