/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.jpa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.NoResultException;


/**
 * Used to mark a method as being a JPA named query execution method.
 * 
 * @author fcorneli
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface QueryMethod {

    /**
     * The name of the named query to execute.
     * 
     */
    String value();

    /**
     * Allows the method to return <code>null</code> instead of a {@link NoResultException}. Useful to implement
     * <code>findXXX</code> methods.
     * 
     */
    boolean nullable() default false;
}
