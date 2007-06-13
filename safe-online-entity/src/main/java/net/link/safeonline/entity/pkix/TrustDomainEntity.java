/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity.pkix;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Index;

import static net.link.safeonline.entity.pkix.TrustDomainEntity.QUERY_ALL;
import static net.link.safeonline.entity.pkix.TrustDomainEntity.QUERY_WHERE_NAME;

@Entity
@Table(name = "trust_domain")
@NamedQueries( {
		@NamedQuery(name = QUERY_WHERE_NAME, query = "SELECT trustDomain "
				+ "FROM TrustDomainEntity AS trustDomain "
				+ "WHERE trustDomain.name = :name"),
		@NamedQuery(name = QUERY_ALL, query = "FROM TrustDomainEntity") })
public class TrustDomainEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int NAME_SIZE = 64;

	public static final String QUERY_WHERE_NAME = "td.name";

	public static final String QUERY_ALL = "td.all";

	private long id;

	private String name;

	private boolean performOcspCheck;

	private long ocspCacheTimeOutMillis;

	public TrustDomainEntity() {
		// empty
	}

	public TrustDomainEntity(String name, boolean performOcspCheck) {
		this(name, performOcspCheck, 0);
	}

	public TrustDomainEntity(String name, boolean performOcspCheck,
			long ocspCacheTimeOutMillis) {
		this.name = name;
		this.performOcspCheck = performOcspCheck;
		this.ocspCacheTimeOutMillis = ocspCacheTimeOutMillis;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(unique = true, nullable = false, length = NAME_SIZE)
	@Index(name = "trust_domain_name_idx")
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Marks whether the certificate validator should perform an OCSP check when
	 * OCSP access location information is available within a certificate.
	 * 
	 * @return
	 */
	public boolean isPerformOcspCheck() {
		return this.performOcspCheck;
	}

	public void setPerformOcspCheck(boolean performOcspCheck) {
		this.performOcspCheck = performOcspCheck;
	}

	/**
	 * Indicates how long a cached OCSP lookup stays valid.
	 * 
	 * @return
	 */
	public long getOcspCacheTimeOutMillis() {
		return this.ocspCacheTimeOutMillis;
	}

	public void setOcspCacheTimeOutMillis(long ocspCacheTimeOutMillis) {
		this.ocspCacheTimeOutMillis = ocspCacheTimeOutMillis;
	}

	public static Query createQueryWhereName(EntityManager entityManager,
			String name) {
		Query query = entityManager.createNamedQuery(QUERY_WHERE_NAME);
		query.setParameter("name", name);
		return query;
	}

	public static Query createQueryAll(EntityManager entityManager) {
		Query query = entityManager.createNamedQuery(QUERY_ALL);
		return query;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", this.id).append("name",
				this.name).toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (false == obj instanceof TrustDomainEntity) {
			return false;
		}
		TrustDomainEntity rhs = (TrustDomainEntity) obj;
		return new EqualsBuilder().append(this.name, rhs.name).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.name).toHashCode();
	}
}