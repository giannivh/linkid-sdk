/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;

@Entity
@Table(name = "attribute_type")
public class AttributeTypeEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private String type;

	private boolean userVisible;

	private boolean userEditable;

	public AttributeTypeEntity() {
		// empty
	}

	public AttributeTypeEntity(String name, String type, boolean userVisible,
			boolean userEditable) {
		this.name = name;
		this.type = type;
		this.userVisible = userVisible;
		this.userEditable = userEditable;
	}

	@Id
	@Column(name = "name")
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "type", nullable = false)
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AttributeTypeEntity rhs = (AttributeTypeEntity) obj;
		return new EqualsBuilder().append(this.name, rhs.name).append(
				this.type, rhs.type).isEquals();
	}

	public boolean isUserVisible() {
		return this.userVisible;
	}

	public void setUserVisible(boolean userVisible) {
		this.userVisible = userVisible;
	}

	public boolean isUserEditable() {
		return this.userEditable;
	}

	public void setUserEditable(boolean userEditable) {
		this.userEditable = userEditable;
	}
}
