/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.mandate.bean;

import javax.ejb.Stateless;

import net.link.safeonline.demo.mandate.MandateLogon;
import net.link.safeonline.sdk.auth.seam.SafeOnlineLoginUtils;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.log.Log;

@Stateless
@Name("mandateLogon")
@LocalBinding(jndiBinding = "SafeOnlineMandateDemo/MandateLogonBean/local")
public class MandateLogonBean implements MandateLogon {

	@Logger
	private Log log;

	@In
	Context sessionContext;

	@In(create = true)
	FacesMessages facesMessages;

	public String login() {
		log.debug("login");
		String result = SafeOnlineLoginUtils.login(this.facesMessages,
				this.log, "login");
		return result;
	}

	public String logout() {
		log.debug("logout");
		this.sessionContext.set("username", null);
		Seam.invalidateSession();
		return "logout-success";
	}
}