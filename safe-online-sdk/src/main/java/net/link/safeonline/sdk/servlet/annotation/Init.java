/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.UnavailableException;

/**
 * Specifies that the field should be resolved from the servlet init parameter
 * list.
 * 
 * @author wvdhaute
 * 
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Init {

	public String NOT_SPECIFIED = "Not Specified";

	/**
	 * The name of the init parameter.
	 * 
	 */
	String name();

	/**
	 * Optional default value if missing.
	 */
	String defaultValue() default NOT_SPECIFIED;

	/**
	 * If not optional {@link UnavailableException} will be thrown during
	 * servlet initialization.
	 */
	boolean optional() default false;
}
