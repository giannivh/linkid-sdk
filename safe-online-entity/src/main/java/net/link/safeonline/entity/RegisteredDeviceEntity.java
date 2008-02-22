/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity;

import static net.link.safeonline.entity.RegisteredDeviceEntity.QUERY_LIST_SUBJECT;
import static net.link.safeonline.entity.RegisteredDeviceEntity.QUERY_SUBJECT_DEVICE;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.link.safeonline.jpa.annotation.QueryMethod;
import net.link.safeonline.jpa.annotation.QueryParam;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Entity
@Table(name = "registeredDevices", uniqueConstraints = @UniqueConstraint(columnNames = {
		"subject", "device" }))
@NamedQueries( {
		@NamedQuery(name = QUERY_LIST_SUBJECT, query = "SELECT regDevice "
				+ "FROM RegisteredDeviceEntity AS regDevice "
				+ "WHERE regDevice.subject = :subject"),
		@NamedQuery(name = QUERY_SUBJECT_DEVICE, query = "SELECT regDevice "
				+ "FROM RegisteredDeviceEntity AS regDevice "
				+ "WHERE regDevice.subject = :subject AND regDevice.device = :device") })
public class RegisteredDeviceEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_SUBJECT = "reg.dev.sub";

	public static final String QUERY_SUBJECT_DEVICE = "reg.dev.subdev";

	private SubjectEntity subject;

	private DeviceEntity device;

	private String id;

	public RegisteredDeviceEntity() {
		// empty
	}

	public RegisteredDeviceEntity(SubjectEntity subject, String id,
			DeviceEntity device) {
		this.subject = subject;
		this.id = id;
		this.device = device;
	}

	@ManyToOne
	@JoinColumn(name = "subject", nullable = false)
	public SubjectEntity getSubject() {
		return this.subject;
	}

	public void setSubject(SubjectEntity subject) {
		this.subject = subject;
	}

	@Id
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "device", nullable = false)
	public DeviceEntity getDevice() {
		return this.device;
	}

	public void setDevice(DeviceEntity device) {
		this.device = device;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (false == obj instanceof RegisteredDeviceEntity) {
			return false;
		}
		RegisteredDeviceEntity rhs = (RegisteredDeviceEntity) obj;
		return new EqualsBuilder().append(this.id, rhs.id).isEquals();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append(
				"id", this.id).append("subject", this.subject.getUserId())
				.append("device", this.device.getName()).toString();
	}

	public interface QueryInterface {
		@QueryMethod(value = QUERY_SUBJECT_DEVICE, nullable = true)
		RegisteredDeviceEntity getRegisteredDevice(@QueryParam("subject")
		SubjectEntity subject, @QueryParam("device")
		DeviceEntity device);

		@QueryMethod(QUERY_LIST_SUBJECT)
		List<RegisteredDeviceEntity> listRegisteredDevices(
				@QueryParam("subject")
				SubjectEntity subject);
	}
}
