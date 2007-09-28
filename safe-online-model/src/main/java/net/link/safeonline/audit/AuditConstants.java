/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit;

public class AuditConstants {

	private AuditConstants() {
		// empty
	}

	/**
	 * The audit topic where finalized audit contexts are published on.
	 */
	public final static String AUDIT_TOPIC_NAME = "topic/audit";

	/**
	 * The audit queue where audit contexts are cleaned up.
	 */
	public final static String AUDIT_QUEUE_NAME = "queue/audit";

	/**
	 * The name of the connection factory used for publishing JMS messages.
	 */
	public static final String CONNECTION_FACTORY_NAME = "java:/JmsXA";
}