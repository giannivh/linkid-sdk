/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.entity;

import static net.link.safeonline.entity.DeviceEntity.QUERY_LIST_ALL;
import static net.link.safeonline.entity.DeviceEntity.QUERY_LIST_WHERE_CLASS;
import static net.link.safeonline.entity.DeviceEntity.QUERY_LIST_WHERE_CLASS_AUTH_CTX;
import static net.link.safeonline.entity.DeviceEntity.QUERY_WHERE_CERTID;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.link.safeonline.jpa.annotation.QueryMethod;
import net.link.safeonline.jpa.annotation.QueryParam;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.IndexColumn;

@Entity
@Table(name = "devices")
@NamedQueries( {
		@NamedQuery(name = QUERY_LIST_ALL, query = "FROM DeviceEntity d"),
		@NamedQuery(name = QUERY_LIST_WHERE_CLASS, query = "SELECT d FROM DeviceEntity d "
				+ "WHERE d.deviceClass = :deviceClass"),
		@NamedQuery(name = QUERY_LIST_WHERE_CLASS_AUTH_CTX, query = "SELECT d FROM DeviceEntity d "
				+ "WHERE d.deviceClass.authenticationContextClass = :authenticationContextClass"),
		@NamedQuery(name = QUERY_WHERE_CERTID, query = "SELECT device "
				+ "FROM DeviceEntity AS device "
				+ "WHERE device.certificateIdentifier = :certificateIdentifier") })
public class DeviceEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_ALL = "dev.all";

	public static final String QUERY_LIST_WHERE_CLASS = "dev.class";

	public static final String QUERY_LIST_WHERE_CLASS_AUTH_CTX = "dev.cl.actx";

	public static final String QUERY_WHERE_CERTID = "dev.certid";

	private String name;

	private DeviceClassEntity deviceClass;

	private OlasEntity location;

	private String authenticationURL;

	private String registrationURL;

	private String removalURL;

	private String updateURL;

	private byte[] encodedCert;

	private String certificateIdentifier;

	private transient X509Certificate certificate;

	private AttributeTypeEntity attributeType;

	private AttributeTypeEntity userAttributeType;

	private Map<String, DevicePropertyEntity> properties;

	private Map<String, DeviceDescriptionEntity> descriptions;

	public DeviceEntity() {
		// empty
	}

	public DeviceEntity(String name, DeviceClassEntity deviceClass,
			OlasEntity location, String authenticationURL,
			String registrationURL, String removalURL, String updateURL,
			X509Certificate certificate) {
		this.name = name;
		this.deviceClass = deviceClass;
		this.location = location;
		this.authenticationURL = authenticationURL;
		this.registrationURL = registrationURL;
		this.removalURL = removalURL;
		this.updateURL = updateURL;
		this.properties = new HashMap<String, DevicePropertyEntity>();
		this.descriptions = new HashMap<String, DeviceDescriptionEntity>();
		if (null != certificate) {
			try {
				this.encodedCert = certificate.getEncoded();
			} catch (CertificateEncodingException e) {
				throw new EJBException("certificate encoding error: "
						+ e.getMessage(), e);
			}
			this.certificateIdentifier = toCertificateIdentifier(this.encodedCert);
		}
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * This device attribute holds all the information returned to OLAS.
	 * 
	 */
	@ManyToOne
	public AttributeTypeEntity getAttributeType() {
		return this.attributeType;
	}

	public void setAttributeType(AttributeTypeEntity attributeType) {
		this.attributeType = attributeType;
	}

	/**
	 * This device attribute holds the information for a user to recognize his
	 * device registration.
	 * 
	 */
	@ManyToOne
	public AttributeTypeEntity getUserAttributeType() {
		return this.userAttributeType;
	}

	public void setUserAttributeType(AttributeTypeEntity userAttributeType) {
		this.userAttributeType = userAttributeType;
	}

	/**
	 * Gives back the device class.
	 * 
	 */
	@ManyToOne
	public DeviceClassEntity getDeviceClass() {
		return this.deviceClass;
	}

	public void setDeviceClass(DeviceClassEntity deviceClass) {
		this.deviceClass = deviceClass;
	}

	@Transient
	public String getAuthenticationContextClass() {
		return this.deviceClass.getAuthenticationContextClass() + ":"
				+ this.name;
	}

	/**
	 * Gives back the location of this device.
	 * 
	 */
	@ManyToOne
	public OlasEntity getLocation() {
		return this.location;
	}

	public void setLocation(OlasEntity location) {
		this.location = location;
	}

	/**
	 * Retrieve the local URL for authentication of this device.
	 * 
	 */
	public String getAuthenticationURL() {
		return this.authenticationURL;
	}

	public void setAuthenticationURL(String authenticationURL) {
		this.authenticationURL = authenticationURL;
	}

	/**
	 * Returns the full URL for authentication of this device.
	 * 
	 * TODO: naming consistent URL - path
	 */
	@Transient
	public String getAuthenticationPath() {
		if (null == this.location)
			return this.authenticationURL;
		return this.location.getLocation() + "/" + this.authenticationURL;
	}

	/**
	 * Retrieves the local URL for registration of this device.
	 * 
	 */
	public String getRegistrationURL() {
		return this.registrationURL;
	}

	public void setRegistrationURL(String registrationURL) {
		this.registrationURL = registrationURL;
	}

	/**
	 * Returns the full URL for registration of this device.
	 * 
	 */
	@Transient
	public String getRegistrationPath() {
		if (null == this.location)
			return this.registrationURL;
		return this.location.getLocation() + "/" + this.registrationURL;
	}

	/**
	 * Retrieves the local URL for removal of this device.
	 * 
	 */
	public String getRemovalURL() {
		return this.removalURL;
	}

	public void setRemovalURL(String removalURL) {
		this.removalURL = removalURL;
	}

	/**
	 * Returns the full URL for removal of this device.
	 * 
	 */
	@Transient
	public String getRemovalPath() {
		if (null == this.location)
			return this.removalURL;
		return this.location.getLocation() + "/" + this.removalURL;
	}

	/**
	 * Retrieves the URL for updating of this device.
	 * 
	 */
	public String getUpdateURL() {
		return this.updateURL;
	}

	public void setUpdateURL(String updateURL) {
		this.updateURL = updateURL;
	}

	/**
	 * Returns the full URL for updating of this device.
	 * 
	 */
	@Transient
	public String getUpdatePath() {
		if (null == this.location)
			return this.updateURL;
		return this.location.getLocation() + "/" + this.updateURL;
	}

	/**
	 * Gives back the encoded device certificate. Each device has a
	 * corresponding certificate. This certificate is used in the
	 * authentication/registration process.
	 * 
	 */
	@Lob
	@Column(length = 4 * 1024, nullable = true)
	public byte[] getEncodedCert() {
		return this.encodedCert;
	}

	/**
	 * Sets the encoded certificate data. Do not use this method directly. Use
	 * {@link #setCertificate(X509Certificate)} instead. This method should only
	 * be used by JPA.
	 * 
	 * @param encodedCert
	 */
	public void setEncodedCert(byte[] encodedCert) {
		this.encodedCert = encodedCert;
	}

	/**
	 * The certificate identifier is used during application authentication
	 * phase to associate a given certificate with it's corresponding
	 * application.
	 * 
	 */
	@Column(unique = true)
	@IndexColumn(name = "certID")
	public String getCertificateIdentifier() {
		return this.certificateIdentifier;
	}

	/**
	 * Sets the certificate identifier. Do not use this method directly. Use
	 * {@link #setCertificate(X509Certificate) setCertificate} instead. JPA
	 * requires this setter.
	 * 
	 * @param certificateIdentifier
	 * @see #setCertificate(X509Certificate)
	 */
	public void setCertificateIdentifier(String certificateIdentifier) {
		this.certificateIdentifier = certificateIdentifier;
	}

	@Transient
	public X509Certificate getCertificate() {
		if (null != this.certificate)
			return this.certificate;
		if (null == this.encodedCert)
			return null;
		try {
			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509");
			InputStream inputStream = new ByteArrayInputStream(this.encodedCert);
			this.certificate = (X509Certificate) certificateFactory
					.generateCertificate(inputStream);
		} catch (CertificateException e) {
			throw new EJBException("cert factory error: " + e.getMessage());
		}
		return this.certificate;
	}

	/**
	 * Sets the X509 certificate of the application. Use this method to update
	 * the application certificate since this method keeps the certificate
	 * identifier in sync with the certificate.
	 * 
	 * @param certificate
	 */
	@Transient
	public void setCertificate(X509Certificate certificate) {
		byte[] encodedCertificate;
		try {
			encodedCertificate = certificate.getEncoded();
		} catch (CertificateEncodingException e) {
			throw new EJBException("certificate encoding error");
		}
		setEncodedCert(encodedCertificate);
		setCertificateIdentifier(toCertificateIdentifier(encodedCertificate));
	}

	/**
	 * Gives back the certificate identifier for a given encoded X509
	 * certificate.
	 * 
	 * @param encodedCertificate
	 */
	public static String toCertificateIdentifier(byte[] encodedCertificate) {
		String certificateIdentifier = DigestUtils.shaHex(encodedCertificate);
		return certificateIdentifier;
	}

	/**
	 * Returns map of device properties.
	 * 
	 */
	@OneToMany(mappedBy = "device")
	@MapKey(name = "name")
	public Map<String, DevicePropertyEntity> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, DevicePropertyEntity> properties) {
		this.properties = properties;
	}

	/**
	 * Returns map of i18n device descriptions.
	 * 
	 */
	@OneToMany(mappedBy = "device")
	@MapKey(name = "language")
	public Map<String, DeviceDescriptionEntity> getDescriptions() {
		return this.descriptions;
	}

	public void setDescriptions(
			Map<String, DeviceDescriptionEntity> descriptions) {
		this.descriptions = descriptions;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (null == obj)
			return false;
		if (false == obj instanceof DeviceEntity)
			return false;
		DeviceEntity rhs = (DeviceEntity) obj;
		return new EqualsBuilder().append(this.name, rhs.name).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.name).toHashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", this.name).toString();
	}

	public interface QueryInterface {
		@QueryMethod(QUERY_LIST_ALL)
		List<DeviceEntity> listDevices();

		@QueryMethod(QUERY_LIST_WHERE_CLASS)
		List<DeviceEntity> listDevices(
				@QueryParam("deviceClass") DeviceClassEntity deviceClass);

		@QueryMethod(QUERY_LIST_WHERE_CLASS_AUTH_CTX)
		List<DeviceEntity> listDevices(
				@QueryParam("authenticationContextClass") String authenticationContextClass);
	}

	public static Query createQueryWhereCertificate(
			EntityManager entityManager, X509Certificate certificate) {
		byte[] encodedCertificate;
		try {
			encodedCertificate = certificate.getEncoded();
		} catch (CertificateEncodingException e) {
			throw new EJBException("Certificate encoding error: "
					+ e.getMessage(), e);
		}
		String certificateIdentifier = toCertificateIdentifier(encodedCertificate);
		Query query = entityManager.createNamedQuery(QUERY_WHERE_CERTID);
		query.setParameter("certificateIdentifier", certificateIdentifier);
		return query;
	}

}
