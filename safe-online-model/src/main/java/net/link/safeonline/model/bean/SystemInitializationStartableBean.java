/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.bean;

import javax.ejb.Local;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.Startable;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.SubscriptionOwnerType;

import org.jboss.annotation.ejb.LocalBinding;

/**
 * This component will initialize the system at startup.
 * 
 * For now it creates initial users, applications and subscriptions. This to
 * allow for admins to gain access to the system and thus to bootstrap the
 * SafeOnline core.
 * 
 * @author fcorneli
 * 
 */
@Stateless
@Local(Startable.class)
@LocalBinding(jndiBinding = Startable.JNDI_PREFIX
		+ "SystemInitializationStartableBean")
public class SystemInitializationStartableBean extends AbstractInitBean {

	public SystemInitializationStartableBean() {
		this.attributeTypes.add(new AttributeTypeEntity(
				SafeOnlineConstants.NAME_ATTRIBUTE, "string", true, true));
		this.attributeTypes
				.add(new AttributeTypeEntity(
						SafeOnlineConstants.PASSWORD_ATTRIBUTE, "string",
						false, false));

		this.authorizedUsers.put("admin", "admin");
		this.authorizedUsers.put("owner", "secret");

		this.applicationOwnersAndLogin.put("owner", "owner");

		this.registeredApplications.add(new Application(
				SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME, "owner",
				"The SafeOnline User Web Application."));
		this.registeredApplications.add(new Application(
				SafeOnlineConstants.SAFE_ONLINE_OPERATOR_APPLICATION_NAME,
				"owner", "The SafeOnline Operator Web Application.", false,
				false));
		this.registeredApplications.add(new Application(
				SafeOnlineConstants.SAFE_ONLINE_OWNER_APPLICATION_NAME,
				"owner", "The SafeOnline Application Owner Web Application.",
				false, false));

		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "admin",
				SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME));
		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "admin",
				SafeOnlineConstants.SAFE_ONLINE_OPERATOR_APPLICATION_NAME));

		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "owner",
				SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME));
		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "owner",
				SafeOnlineConstants.SAFE_ONLINE_OWNER_APPLICATION_NAME));
	}

	public int getPriority() {
		return Startable.PRIORITY_BOOTSTRAP;
	}
}
