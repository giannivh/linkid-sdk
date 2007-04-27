/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;

import static net.link.safeonline.entity.SubjectIdentifierEntity.DELETE_WHERE_OTHER_IDENTIFIERS;

/**
 * Subject Identifier entity. This entity allows us to unambiguously map from an
 * identifier within a certains domain to its subject. For example, within the
 * domain of Belgian eID, we will map from the SHA-1 of the encoded
 * authentication certificate to a subject.
 * 
 * @author fcorneli
 * 
 */
@Entity
@Table(name = "subject_identifier")
@NamedQueries( { @NamedQuery(name = DELETE_WHERE_OTHER_IDENTIFIERS, query = "DELETE FROM SubjectIdentifierEntity AS subjectIdentifier "
		+ "WHERE subjectIdentifier.pk.domain = :domain AND "
		+ "subjectIdentifier.subject = :subject AND "
		+ "subjectIdentifier.pk.identifier <> :identifier") })
public class SubjectIdentifierEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String DELETE_WHERE_OTHER_IDENTIFIERS = "sie.del";

	private SubjectIdentifierPK pk;

	private SubjectEntity subject;

	public SubjectIdentifierEntity() {
		// empty
	}

	public SubjectIdentifierEntity(String domain, String identifier,
			SubjectEntity subject) {
		this.pk = new SubjectIdentifierPK(domain, identifier);
		this.subject = subject;
	}

	@EmbeddedId
	public SubjectIdentifierPK getPk() {
		return pk;
	}

	public void setPk(SubjectIdentifierPK pk) {
		this.pk = pk;
	}

	@ManyToOne(optional = false)
	@JoinColumn(name = "subject")
	public SubjectEntity getSubject() {
		return this.subject;
	}

	public void setSubject(SubjectEntity subject) {
		this.subject = subject;
	}

	public static Query createDeleteWhereOtherIdentifiers(
			EntityManager entityManager, String domain, String identifier,
			SubjectEntity subject) {
		Query query = entityManager
				.createNamedQuery(DELETE_WHERE_OTHER_IDENTIFIERS);
		query.setParameter("domain", domain);
		query.setParameter("subject", subject);
		query.setParameter("identifier", identifier);
		return query;
	}
}
