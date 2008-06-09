/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.payment.bean;

import javax.ejb.Stateful;

import net.link.safeonline.demo.payment.PaymentLogon;
import net.link.safeonline.sdk.auth.seam.SafeOnlineLoginUtils;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.ejb.cache.simple.CacheConfig;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.log.Log;
import org.jboss.seam.web.Session;

@Stateful
@Name("paymentLogon")
@Scope(ScopeType.SESSION)
@CacheConfig(idleTimeoutSeconds = (5 + 1) * 60)
@LocalBinding(jndiBinding = "SafeOnlinePaymentDemo/PaymentLogonBean/local")
public class PaymentLogonBean extends AbstractPaymentDataClientBean implements
		PaymentLogon {

	public static final String APPLICATION_NAME = "safe-online-demo-payment";

	@Logger
	private Log log;

	@In
	Context sessionContext;

	public String login() {
		this.log.debug("login");
		String result = SafeOnlineLoginUtils.login("login");
		return result;
	}

	public String logout() {
		this.log.debug("logout");
		this.sessionContext.set("username", null);
        Session.getInstance().invalidate();
		return "logout-success";
	}

	public String getUsername() {
		String userId = (String) this.sessionContext.get("username");
		return getUsername(userId);
	}
}
