/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.ws.data;

/**
 * Data-container class for a SafeOnline attribute.
 * 
 * @param <Type>
 *            the type of the attribute value.
 * 
 * @author fcorneli
 * 
 */
public class Attribute<Type> {

	private final String name;

	private final Type value;

	public Attribute(String name, Type value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public Type getValue() {
		return this.value;
	}
}