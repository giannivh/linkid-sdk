/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service.bean;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineApplicationRoles;
import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.authentication.exception.ApplicationIdentityNotFoundException;
import net.link.safeonline.authentication.exception.AttributeNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.AttributeService;
import net.link.safeonline.dao.ApplicationIdentityDAO;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.SubjectDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityAttributeEntity;
import net.link.safeonline.entity.ApplicationIdentityEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.model.ApplicationManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.security.SecurityDomain;

/**
 * Attribute Service Implementation for applications.
 * 
 * @author fcorneli
 * 
 */
@Stateless
@SecurityDomain(SafeOnlineConstants.SAFE_ONLINE_APPLICATION_SECURITY_DOMAIN)
public class AttributeServiceBean implements AttributeService {

	private static final Log LOG = LogFactory
			.getLog(AttributeServiceBean.class);

	@EJB
	private AttributeDAO attributeDAO;

	@EJB
	private ApplicationManager applicationManager;

	@EJB
	private ApplicationIdentityDAO applicationIdentityDAO;

	@EJB
	private SubscriptionDAO subscriptionDAO;

	@EJB
	private SubjectDAO subjectDAO;

	@RolesAllowed(SafeOnlineApplicationRoles.APPLICATION_ROLE)
	public String getAttribute(String subjectLogin, String attributeName)
			throws AttributeNotFoundException, PermissionDeniedException,
			SubjectNotFoundException {
		LOG.debug("get attribute " + attributeName + " for login "
				+ subjectLogin);

		SubjectEntity subject = this.subjectDAO.getSubject(subjectLogin);
		ApplicationEntity application = this.applicationManager
				.getCallerApplication();

		/*
		 * The subject needs to be subscribed onto this application.
		 */
		SubscriptionEntity subscription = this.subscriptionDAO
				.findSubscription(subject, application);
		if (null == subscription) {
			LOG.debug("subject is not subscribed");
			throw new PermissionDeniedException();
		}

		/*
		 * The subject needs to have a confirmed identity version.
		 */
		Long confirmedIdentityVersion = subscription
				.getConfirmedIdentityVersion();
		if (null == confirmedIdentityVersion) {
			LOG.debug("subject has no confirmed identity version");
			throw new PermissionDeniedException();
		}

		ApplicationIdentityEntity confirmedApplicationIdentity;
		try {
			confirmedApplicationIdentity = this.applicationIdentityDAO
					.getApplicationIdentity(application,
							confirmedIdentityVersion);
		} catch (ApplicationIdentityNotFoundException e) {
			throw new EJBException(
					"application identity not found for version: "
							+ confirmedIdentityVersion);
		}
		List<ApplicationIdentityAttributeEntity> attributes = confirmedApplicationIdentity
				.getAttributes();
		boolean hasAttribute = false;
		for (ApplicationIdentityAttributeEntity attribute : attributes) {
			LOG
					.debug("identity attribute: "
							+ attribute.getAttributeTypeName());
			if (attribute.getAttributeTypeName().equals(attributeName)) {
				hasAttribute = true;
				break;
			}
		}
		if (false == hasAttribute) {
			LOG.debug("attribute not in set of confirmed identity attributes");
			throw new PermissionDeniedException();
		}

		AttributeEntity attribute = this.attributeDAO.findAttribute(
				attributeName, subjectLogin);
		if (null == attribute) {
			throw new AttributeNotFoundException();
		}

		String value = attribute.getStringValue();
		return value;
	}
}
