/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.lawyer.bean;

import javax.ejb.Stateful;

import net.link.safeonline.demo.lawyer.LawyerLogon;
import net.link.safeonline.sdk.auth.seam.SafeOnlineLoginUtils;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.log.Log;

@Stateful
@Name("lawyerLogon")
@LocalBinding(jndiBinding = "SafeOnlineLawyerDemo/LawyerLogonBean/local")
public class LawyerLogonBean extends AbstractLawyerDataClientBean implements
		LawyerLogon {

	@Logger
	private Log log;

	@In(create = true)
	private FacesMessages facesMessages;

	@In
	Context sessionContext;

	public String login() {
		log.debug("login");
		String result = SafeOnlineLoginUtils.login(this.facesMessages,
				this.log, "login");
		return result;
	}

	public String logout() {
		log.debug("logout");
		Seam.invalidateSession();
		return "logout-success";
	}

	public String getUsername() {
		String userId = (String) this.sessionContext.get("username");
		String username = getUsername(userId);
		return username;
	}
}
