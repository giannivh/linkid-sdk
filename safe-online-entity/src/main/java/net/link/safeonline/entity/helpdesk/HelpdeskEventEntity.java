/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity.helpdesk;

import static net.link.safeonline.entity.helpdesk.HelpdeskEventEntity.QUERY_DELETE_WHERE_CONTEXTID;
import static net.link.safeonline.entity.helpdesk.HelpdeskEventEntity.QUERY_DELETE_WHERE_OLDER;
import static net.link.safeonline.entity.helpdesk.HelpdeskEventEntity.QUERY_LIST_USERS;
import static net.link.safeonline.entity.helpdesk.HelpdeskEventEntity.QUERY_LIST_USER_CONTEXTS;
import static net.link.safeonline.entity.helpdesk.HelpdeskEventEntity.QUERY_WHERE_CONTEXTID;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import net.link.safeonline.jpa.annotation.QueryMethod;
import net.link.safeonline.jpa.annotation.QueryParam;
import net.link.safeonline.jpa.annotation.UpdateMethod;
import net.link.safeonline.shared.helpdesk.LogLevelType;

@Entity
@Table(name = "helpdesk_event")
@NamedQueries( {
		@NamedQuery(name = QUERY_WHERE_CONTEXTID, query = "SELECT event "
				+ "FROM HelpdeskEventEntity AS event "
				+ "WHERE event.helpdeskContext.id = :contextId"),
		@NamedQuery(name = QUERY_DELETE_WHERE_CONTEXTID, query = "DELETE "
				+ "FROM HelpdeskEventEntity AS event "
				+ "WHERE event.helpdeskContext.id = :contextId"),
		@NamedQuery(name = QUERY_DELETE_WHERE_OLDER, query = "DELETE "
				+ "FROM HelpdeskEventEntity AS event "
				+ "WHERE event.time < :ageLimit AND event.logLevel = :logLevel"),
		@NamedQuery(name = QUERY_LIST_USER_CONTEXTS, query = "SELECT DISTINCT event.helpdeskContext "
				+ "FROM HelpdeskEventEntity AS event "
				+ "WHERE event.principal = :principal"),
		@NamedQuery(name = QUERY_LIST_USERS, query = "SELECT DISTINCT event.principal "
				+ "FROM HelpdeskEventEntity AS event") })
public class HelpdeskEventEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_WHERE_CONTEXTID = "hd.event.logid";

	public static final String QUERY_LIST_USER_CONTEXTS = "hd.event.usercontexts";

	public static final String QUERY_LIST_USERS = "hd.event.users";

	public static final String QUERY_DELETE_WHERE_CONTEXTID = "hd.event.del.logid";

	public static final String QUERY_DELETE_WHERE_OLDER = "hd.event.old";

	private Long id;

	private HelpdeskContextEntity helpdeskContext;

	private Date time;

	private String message;

	private String principal;

	private LogLevelType logLevel;

	public HelpdeskEventEntity() {
		// empty
	}

	public HelpdeskEventEntity(String message, String principal,
			LogLevelType logLevel) {
		this.message = message;
		this.principal = principal;
		this.logLevel = logLevel;
		this.time = new Date();
	}

	public HelpdeskEventEntity(HelpdeskContextEntity helpdeskContext,
			Date time, String message, String principal, LogLevelType logLevel) {
		this.helpdeskContext = helpdeskContext;
		this.time = time;
		this.message = message;
		this.principal = principal;
		this.logLevel = logLevel;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setHelpdeskContext(HelpdeskContextEntity helpdeskContext) {
		this.helpdeskContext = helpdeskContext;
	}

	@ManyToOne
	public HelpdeskContextEntity getHelpdeskContext() {
		return this.helpdeskContext;
	}

	@Temporal(value = TemporalType.TIME)
	public Date getTime() {
		return this.time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPrincipal() {
		return this.principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	@Enumerated(EnumType.STRING)
	public LogLevelType getLogLevel() {
		return this.logLevel;
	}

	public void setLogLevel(LogLevelType logLevel) {
		this.logLevel = logLevel;
	}

	public interface QueryInterface {
		@QueryMethod(QUERY_WHERE_CONTEXTID)
		List<HelpdeskEventEntity> listLogs(@QueryParam("contextId")
		Long contextId);

		@UpdateMethod(QUERY_DELETE_WHERE_CONTEXTID)
		void deleteEvents(@QueryParam("contextId")
		Long contextId);

		@UpdateMethod(QUERY_DELETE_WHERE_OLDER)
		void deleteEvents(@QueryParam("ageLimit")
		Date ageLimit, @QueryParam("logLevel")
		LogLevelType logLevel);

		@QueryMethod(QUERY_LIST_USER_CONTEXTS)
		List<HelpdeskContextEntity> listUserContexts(@QueryParam("principal")
		String user);

		@QueryMethod(QUERY_LIST_USERS)
		List<String> listUsers();
	}

}