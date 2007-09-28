/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.oper.audit.bean;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import net.link.safeonline.audit.exception.AuditContextNotFoundException;
import net.link.safeonline.audit.service.AuditService;
import net.link.safeonline.entity.audit.AccessAuditEntity;
import net.link.safeonline.entity.audit.AuditAuditEntity;
import net.link.safeonline.entity.audit.AuditContextEntity;
import net.link.safeonline.entity.audit.ResourceAuditEntity;
import net.link.safeonline.entity.audit.SecurityAuditEntity;
import net.link.safeonline.oper.OperatorConstants;
import net.link.safeonline.oper.audit.AuditSearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.core.FacesMessages;

@Stateful
@Name("audit")
@Scope(ScopeType.CONVERSATION)
@LocalBinding(jndiBinding = OperatorConstants.JNDI_PREFIX
		+ "AuditSearchBean/local")
@SecurityDomain(OperatorConstants.SAFE_ONLINE_OPER_SECURITY_DOMAIN)
public class AuditSearchBean implements AuditSearch {

	private static final Log LOG = LogFactory.getLog(AuditSearchBean.class);

	private static final String AUDIT_CONTEXT_LIST_NAME = "auditContextList";

	private static final String ACCESS_AUDIT_LIST_NAME = "accessAuditRecordList";
	private static final String SECURITY_AUDIT_LIST_NAME = "securityAuditRecordList";
	private static final String RESOURCE_AUDIT_LIST_NAME = "resourceAuditRecordList";
	private static final String AUDIT_AUDIT_LIST_NAME = "auditAuditRecordList";

	private enum SearchMode {
		ID, USER, TIME, ALL
	}

	@In(create = true)
	FacesMessages facesMessages;

	@EJB
	private AuditService auditService;

	private Long searchContextId;

	private Integer searchLastTimeDays = 0;

	private Integer searchLastTimeHours = 0;

	private Integer searchLastTimeMinutes = 0;

	@Out(required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private SearchMode searchMode;

	@Out(required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private String searchAuditUser;

	@Out(required = false, scope = ScopeType.SESSION)
	@In(required = false)
	Date ageLimit;

	private void setMode(SearchMode searchMode) {
		this.searchMode = searchMode;
	}

	/*
	 * 
	 * Datamodels
	 * 
	 */
	@SuppressWarnings("unused")
	@DataModel(AUDIT_CONTEXT_LIST_NAME)
	private List<AuditContextEntity> auditContextList;

	@DataModelSelection(AUDIT_CONTEXT_LIST_NAME)
	@Out(value = "auditContext", required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private AuditContextEntity auditContext;

	@SuppressWarnings("unused")
	@DataModel(ACCESS_AUDIT_LIST_NAME)
	private List<AccessAuditEntity> accessAuditRecordList;

	@SuppressWarnings("unused")
	@DataModel(SECURITY_AUDIT_LIST_NAME)
	private List<SecurityAuditEntity> securityAuditRecordList;

	@SuppressWarnings("unused")
	@DataModel(RESOURCE_AUDIT_LIST_NAME)
	private List<ResourceAuditEntity> resourceAuditRecordList;

	@SuppressWarnings("unused")
	@DataModel(AUDIT_AUDIT_LIST_NAME)
	private List<AuditAuditEntity> auditAuditRecordList;

	/*
	 * 
	 * Accessors
	 * 
	 */
	public Long getSearchContextId() {
		return this.searchContextId;
	}

	public void setSearchContextId(Long searchContextId) {
		this.searchContextId = searchContextId;
	}

	public String getSearchAuditUser() {
		return this.searchAuditUser;
	}

	public void setSearchAuditUser(String searchAuditUser) {
		this.searchAuditUser = searchAuditUser;
	}

	public Integer getSearchLastTimeDays() {
		return searchLastTimeDays;
	}

	public void setSearchLastTimeDays(Integer searchLastTimeDays) {
		this.searchLastTimeDays = searchLastTimeDays;
	}

	public Integer getSearchLastTimeHours() {
		return searchLastTimeHours;
	}

	public void setSearchLastTimeHours(Integer searchLastTimeHours) {
		this.searchLastTimeHours = searchLastTimeHours;
	}

	public Integer getSearchLastTimeMinutes() {
		return searchLastTimeMinutes;
	}

	public void setSearchLastTimeMinutes(Integer searchLastTimeMinutes) {
		this.searchLastTimeMinutes = searchLastTimeMinutes;
	}

	public boolean getAccessAuditListIsEmpty() {
		if (null == this.accessAuditRecordList)
			return true;
		if (this.accessAuditRecordList.size() == 0)
			return true;
		return false;
	}

	public boolean getAuditAuditListIsEmpty() {
		if (null == this.auditAuditRecordList)
			return true;
		if (this.auditAuditRecordList.size() == 0)
			return true;
		return false;
	}

	public boolean getResourceAuditListIsEmpty() {
		if (null == this.resourceAuditRecordList)
			return true;
		if (this.resourceAuditRecordList.size() == 0)
			return true;
		return false;
	}

	public boolean getSecurityAuditListIsEmpty() {
		if (null == this.securityAuditRecordList)
			return true;
		if (this.securityAuditRecordList.size() == 0)
			return true;
		return false;
	}

	/*
	 * 
	 * Factories
	 * 
	 */
	@Factory(AUDIT_CONTEXT_LIST_NAME)
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void auditContextListFactory() {
		LOG.debug("Retrieve audit contexts");
		this.auditContextList = this.auditService.listLastContexts();
	}

	@Factory(ACCESS_AUDIT_LIST_NAME)
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void accessAuditRecordListFactory() {
		if (SearchMode.ID == this.searchMode) {
			LOG.debug("Retrieve access audit records for context "
					+ this.auditContext.getId());
			this.accessAuditRecordList = this.auditService
					.listAccessAuditRecords(this.auditContext.getId());
		} else if (SearchMode.USER == this.searchMode) {
			LOG.debug("Retrieve access audit records for user "
					+ this.searchAuditUser);
			this.accessAuditRecordList = this.auditService
					.listAccessAuditRecords(this.searchAuditUser);
		} else if (SearchMode.TIME == this.searchMode) {
			LOG.debug("Retrieve access audit records since " + this.ageLimit);
			this.accessAuditRecordList = this.auditService
					.listAccessAuditRecordsSince(this.ageLimit);

		}
	}

	@Factory(SECURITY_AUDIT_LIST_NAME)
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void securityAuditRecordListFactory() {
		if (SearchMode.ID == this.searchMode) {
			LOG.debug("Retrieve security audit records for context "
					+ this.auditContext.getId());
			this.securityAuditRecordList = this.auditService
					.listSecurityAuditRecords(this.auditContext.getId());
		} else if (SearchMode.USER == this.searchMode) {
			LOG.debug("Retrieve security audit records for user "
					+ this.searchAuditUser);
			this.securityAuditRecordList = this.auditService
					.listSecurityAuditRecords(this.searchAuditUser);
		} else if (SearchMode.TIME == this.searchMode) {
			LOG.debug("Retrieve security audit records since " + this.ageLimit);
			this.securityAuditRecordList = this.auditService
					.listSecurityAuditRecordsSince(this.ageLimit);
		} else if (SearchMode.ALL == this.searchMode) {
			LOG.debug("Show all security audit records");
			this.securityAuditRecordList = this.auditService
					.listSecurityAuditRecords();
		}
	}

	@Factory(RESOURCE_AUDIT_LIST_NAME)
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void resourceAuditRecordListFactory() {
		if (SearchMode.ID == this.searchMode) {
			LOG.debug("Retrieve resource audit records for context "
					+ this.auditContext.getId());
			this.resourceAuditRecordList = this.auditService
					.listResourceAuditRecords(this.auditContext.getId());
		} else if (SearchMode.TIME == this.searchMode) {
			LOG.debug("Retrieve resource audit records since " + this.ageLimit);
			this.resourceAuditRecordList = this.auditService
					.listResourceAuditRecordsSince(this.ageLimit);
		} else if (SearchMode.ALL == this.searchMode) {
			LOG.debug("Show all resource audit records");
			this.resourceAuditRecordList = this.auditService
					.listResourceAuditRecords();
		}
	}

	@Factory(AUDIT_AUDIT_LIST_NAME)
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void auditAuditRecordListFactory() {
		if (SearchMode.ID == this.searchMode) {
			LOG.debug("Retrieve audit audit records for context "
					+ this.auditContext.getId());
			this.auditAuditRecordList = this.auditService
					.listAuditAuditRecords(this.auditContext.getId());
		} else if (SearchMode.TIME == this.searchMode) {
			LOG.debug("Retrieve audit audit records since " + this.ageLimit);
			this.auditAuditRecordList = this.auditService
					.listAuditAuditRecordsSince(this.ageLimit);
		} else if (SearchMode.ALL == this.searchMode) {
			LOG.debug("retrieving all audit audit");
			this.auditAuditRecordList = this.auditService
					.listAuditAuditRecords();
		}
	}

	/*
	 * 
	 * Actions
	 * 
	 */
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String view() {
		LOG.debug("View context " + this.auditContext.getId());
		setMode(SearchMode.ID);
		return "view";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String viewSecurityRecords() {
		LOG.debug("View all security records");
		setMode(SearchMode.ALL);
		return "view-security";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String viewResourceRecords() {
		LOG.debug("View all resource records");
		setMode(SearchMode.ALL);
		return "view-resource";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String viewAuditRecords() {
		LOG.debug("view all audit records");
		setMode(SearchMode.ALL);
		return "view-audit";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String removeContext() {
		LOG.debug("Remove context " + this.auditContext.getId());
		try {
			this.auditService.removeAuditContext(this.auditContext.getId());
		} catch (AuditContextNotFoundException e) {
			String msg = "audit context not found";
			LOG.debug(msg);
			this.facesMessages.add(msg);
			return null;
		}
		auditContextListFactory();
		return "success";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String searchUser() {
		LOG.debug("Search user " + this.searchAuditUser);
		setMode(SearchMode.USER);
		return "search-user";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String searchId() {
		LOG.debug("Search context id " + this.searchContextId);
		setMode(SearchMode.ID);
		return "search-id";
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public String searchLastTime() {
		Long timeLimitInMillis = System.currentTimeMillis()
				- ((this.searchLastTimeMinutes
						+ (this.searchLastTimeHours * 60) + (this.searchLastTimeDays * 60 * 24)) * 60 * 1000);
		this.ageLimit = new Date(timeLimitInMillis);
		LOG.debug("Search audit records since " + this.ageLimit);
		setMode(SearchMode.TIME);
		return "search-time";
	}

	/*
	 * 
	 * Validators
	 * 
	 */
	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void validateId(FacesContext context, UIComponent toValidate,
			Object value) {
		Long id = (Long) value;
		LOG.debug("validateId = " + id);
		try {
			this.auditContext = this.auditService.getAuditContext(id);
		} catch (AuditContextNotFoundException e) {
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("Unknown audit context");
			context.addMessage(toValidate.getClientId(context), message);
		}
		if (null == this.auditContext) {
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("Unknown audit context");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}

	@RolesAllowed(OperatorConstants.OPERATOR_ROLE)
	public void validateUser(FacesContext context, UIComponent toValidate,
			Object value) {
		String userName = (String) value;
		Set<String> users = this.auditService.listUsers();
		if (!users.contains(userName)) {
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("Unknown user");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}

	@Remove
	@Destroy
	public void destroyCallback() {
		LOG.debug("destroy");
	}

}