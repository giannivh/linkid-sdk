/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.oper.audit;

import javax.ejb.Local;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

@Local
public interface AuditSearch {

	/*
	 * Accessors
	 */
	Long getSearchContextId();

	void setSearchContextId(Long searchContextId);

	String getSearchAuditUser();

	void setSearchAuditUser(String searchAuditUser);

	Integer getSearchLastTimeDays();

	void setSearchLastTimeDays(Integer searchLastTimeDays);

	Integer getSearchLastTimeHours();

	void setSearchLastTimeHours(Integer searchLastTimeHours);

	Integer getSearchLastTimeMinutes();

	void setSearchLastTimeMinutes(Integer searchLastTimeMinutes);

	boolean getAccessAuditListIsEmpty();

	boolean getAuditAuditListIsEmpty();

	boolean getResourceAuditListIsEmpty();

	boolean getSecurityAuditListIsEmpty();

	/*
	 * Factories
	 */
	void auditContextListFactory();

	void accessAuditRecordListFactory();

	void securityAuditRecordListFactory();

	void resourceAuditRecordListFactory();

	void auditAuditRecordListFactory();

	/*
	 * Actions
	 */
	String view();

	String viewResourceRecords();

	String viewSecurityRecords();

	String viewAuditRecords();

	String removeContext();

	String searchId();

	String searchUser();

	String searchLastTime();

	/*
	 * Validators
	 */
	void validateId(FacesContext context, UIComponent toValidate, Object value);

	void validateUser(FacesContext context, UIComponent toValidate, Object value);

	/*
	 * Lifecycle
	 */
	void destroyCallback();

}