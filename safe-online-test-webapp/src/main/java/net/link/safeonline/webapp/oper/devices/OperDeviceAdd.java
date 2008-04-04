/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.webapp.oper.devices;

import net.link.safeonline.webapp.oper.OperTemplate;

public class OperDeviceAdd extends OperTemplate {

	public static final String PAGE_NAME = SAFE_ONLINE_OPER_WEBAPP_PREFIX
			+ "/devices/device-add.seam";

	public OperDeviceAdd() {
		super(PAGE_NAME);
	}
}
