/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit.bean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import net.link.safeonline.audit.AuditContextPolicyContextHandler;
import net.link.safeonline.audit.SecurityAuditLogger;
import net.link.safeonline.audit.dao.AuditAuditDAO;
import net.link.safeonline.audit.dao.AuditContextDAO;
import net.link.safeonline.audit.dao.SecurityAuditDAO;
import net.link.safeonline.audit.exception.AuditContextNotFoundException;
import net.link.safeonline.entity.audit.AuditContextEntity;
import net.link.safeonline.entity.audit.SecurityThreatType;

@Stateless
public class SecurityAuditLoggerBean implements SecurityAuditLogger {

	@EJB
	private AuditAuditDAO auditAuditDAO;

	@EJB
	private AuditContextDAO auditContextDAO;

	@EJB
	private SecurityAuditDAO securityAuditDAO;

	public void addSecurityAudit(SecurityThreatType securityThreat,
			String targetPrincipal, String message) {
		Long auditContextId;
		try {
			auditContextId = (Long) PolicyContext
					.getContext(AuditContextPolicyContextHandler.AUDIT_CONTEXT_KEY);
		} catch (PolicyContextException e) {
			this.auditAuditDAO
					.addAuditAudit("audit context policy context error: "
							+ e.getMessage());
			return;
		}
		if (null == auditContextId) {
			this.auditAuditDAO.addAuditAudit("no audit context available");
			return;
		}

		AuditContextEntity auditContext;
		try {
			auditContext = this.auditContextDAO.getAuditContext(auditContextId);
		} catch (AuditContextNotFoundException e) {
			this.auditAuditDAO.addAuditAudit("audit context not found: "
					+ auditContextId);
			return;
		}

		this.securityAuditDAO.addSecurityAudit(auditContext, securityThreat,
				targetPrincipal, message);
	}

	public void addSecurityAudit(SecurityThreatType securityThreat,
			String message) {
		addSecurityAudit(securityThreat, null, message);
	}
}