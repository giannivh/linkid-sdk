/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.jpa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.persistence.EntityManager;

import net.link.safeonline.jpa.annotation.QueryMethod;
import net.link.safeonline.jpa.annotation.QueryParam;
import net.link.safeonline.jpa.annotation.UpdateMethod;

/**
 * Factory for query objects.
 * 
 * @author fcorneli
 * 
 */
public class QueryObjectFactory {

	private QueryObjectFactory() {
		// empty
	}

	/**
	 * Creates a new query object from the given query object interface. The
	 * methods on this interface should be annotated via {@link QueryMethod} or
	 * {@link UpdateMethod}. If the method has parameters one should annotated
	 * these via {@link QueryParam}.
	 * 
	 * @param <T>
	 * @param entityManager
	 *            the JPA entity manager used by the returned query object.
	 * @param queryObjectInterface
	 *            the interface that the query object will implement.
	 * @return a new query object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createQueryObject(EntityManager entityManager,
			Class<T> queryObjectInterface) {
		if (false == queryObjectInterface.isInterface()) {
			throw new IllegalArgumentException(
					"query object class is not an interface");
		}
		Thread currentThread = Thread.currentThread();
		ClassLoader classLoader = currentThread.getContextClassLoader();
		InvocationHandler invocationHandler = new QueryObjectInvocationHandler(
				entityManager);
		T queryObject = (T) Proxy.newProxyInstance(classLoader,
				new Class[] { queryObjectInterface }, invocationHandler);
		return queryObject;
	}
}
