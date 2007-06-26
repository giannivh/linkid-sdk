/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.payment.bean;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;

import net.link.safeonline.demo.payment.PaymentConstants;
import net.link.safeonline.demo.payment.CustomerSearch;
import net.link.safeonline.demo.payment.CustomerStatus;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.log.Log;

@Stateful
@Name("customerSearch")
@LocalBinding(jndiBinding = "SafeOnlinePaymentDemo/CustomerSearchBean/local")
@SecurityDomain(PaymentConstants.SECURITY_DOMAIN)
public class CustomerSearchBean extends AbstractPaymentDataClientBean implements
		CustomerSearch {

	@Logger
	private Log log;

	@In(create = true)
	FacesMessages facesMessages;

	@In("name")
	@Out(scope = ScopeType.SESSION)
	private String name;

	@SuppressWarnings("unused")
	@Out(value = "customerEditableStatus", required = false, scope = ScopeType.SESSION)
	private CustomerStatus customerStatus;

	@RolesAllowed(PaymentConstants.ADMIN_ROLE)
	public String search() {
		log.debug("search: " + this.name);
		CustomerStatus customerStatus = getCustomerStatus(this.name);
		if (null == customerStatus) {
			return null;
		}
		this.customerStatus = customerStatus;
		return "success";
	}
}