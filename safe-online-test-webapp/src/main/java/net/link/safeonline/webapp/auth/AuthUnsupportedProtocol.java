/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.webapp.auth;

import net.link.safeonline.webapp.Page;

public class AuthUnsupportedProtocol extends Page {

	public static final String PAGE_NAME = SAFE_ONLINE_AUTH_WEBAPP_PREFIX
			+ "/unsupported-protocol.seam";

	public AuthUnsupportedProtocol() {
		super(PAGE_NAME);
	}

}