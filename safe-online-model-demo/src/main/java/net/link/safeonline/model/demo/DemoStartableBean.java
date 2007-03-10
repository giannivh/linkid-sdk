/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.demo;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.Startable;
import net.link.safeonline.dao.TrustPointDAO;
import net.link.safeonline.demo.keystore.DemoKeyStoreUtils;
import net.link.safeonline.entity.SubscriptionOwnerType;
import net.link.safeonline.entity.TrustDomainEntity;
import net.link.safeonline.entity.TrustPointEntity;
import net.link.safeonline.model.bean.AbstractInitBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;

@Stateless
@Local(Startable.class)
@LocalBinding(jndiBinding = Startable.JNDI_PREFIX + "DemoStartableBean")
public class DemoStartableBean extends AbstractInitBean {

	private static final Log LOG = LogFactory.getLog(DemoStartableBean.class);

	@EJB
	private TrustPointDAO trustPointDAO;

	public DemoStartableBean() {
		this.authorizedUsers.put("fcorneli", "secret");
		this.authorizedUsers.put("dieter", "secret");
		this.authorizedUsers.put("mario", "secret");

		this.subscriptions.add(new Subscription(SubscriptionOwnerType.SUBJECT,
				"fcorneli", "demo-application"));
		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "fcorneli",
				SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME));

		this.subscriptions.add(new Subscription(SubscriptionOwnerType.SUBJECT,
				"dieter", "demo-application"));
		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "dieter",
				SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME));

		this.subscriptions.add(new Subscription(SubscriptionOwnerType.SUBJECT,
				"mario", "demo-application"));
		this.subscriptions.add(new Subscription(
				SubscriptionOwnerType.APPLICATION, "mario",
				SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME));
	}

	public int getPriority() {
		return Startable.PRIORITY_DONT_CARE;
	}

	private void addDemoCertificateAsTrustPoint(X509Certificate certificate) {
		TrustDomainEntity applicationTrustDomain = this.trustDomainDAO
				.findTrustDomain(SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN);
		if (null == applicationTrustDomain) {
			LOG.fatal("application trust domain not found");
			return;
		}

		TrustPointEntity demoTrustPoint = this.trustPointDAO.findTrustPoint(
				applicationTrustDomain, certificate);
		if (null != demoTrustPoint) {
			// TODO: should update the certificate instead
			return;
		}

		this.trustPointDAO.addTrustPoint(applicationTrustDomain, certificate);
	}

	@Override
	public void postStart() {
		PrivateKeyEntry privateKeyEntry = DemoKeyStoreUtils
				.getPrivateKeyEntry();
		X509Certificate certificate = (X509Certificate) privateKeyEntry
				.getCertificate();

		this.registeredApplications.add(new Application("demo-application",
				"owner", certificate));

		super.postStart();

		addDemoCertificateAsTrustPoint(certificate);
	}
}
