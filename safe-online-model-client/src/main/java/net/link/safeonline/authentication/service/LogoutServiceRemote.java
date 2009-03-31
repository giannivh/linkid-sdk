/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service;

import javax.ejb.Remote;

import net.link.safeonline.SafeOnlineService;


@Remote
public interface LogoutServiceRemote extends LogoutService {

    public static final String JNDI_BINDING = SafeOnlineService.JNDI_PREFIX + "LogoutServiceBean/remote";

}