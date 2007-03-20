/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.bean;

import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.link.safeonline.authentication.exception.ApplicationIdentityNotFoundException;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.ApplicationIdentityDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.model.ApplicationIdentityService;

@Stateless
public class ApplicationIdentityServiceBean implements
		ApplicationIdentityService {

	private static final Log LOG = LogFactory
			.getLog(ApplicationIdentityServiceBean.class);

	@EJB
	private ApplicationDAO applicationDAO;

	@EJB
	private ApplicationIdentityDAO applicationIdentityDAO;

	@EJB
	private AttributeTypeDAO attributeTypeDAO;

	public void updateApplicationIdentity(String applicationId,
			String[] applicationIdentityAttributeTypes)
			throws ApplicationNotFoundException,
			ApplicationIdentityNotFoundException,
			AttributeTypeNotFoundException {
		LOG.debug("update application identity for application: "
				+ applicationId);
		ApplicationEntity application = this.applicationDAO
				.getApplication(applicationId);
		long currentIdentityVersion = application
				.getCurrentApplicationIdentity();
		ApplicationIdentityEntity applicationIdentity = this.applicationIdentityDAO
				.getApplicationIdentity(application, currentIdentityVersion);
		List<AttributeTypeEntity> currentAttributeTypes = applicationIdentity
				.getAttributeTypes();
		for (AttributeTypeEntity currentAttributeType : currentAttributeTypes) {
			LOG.debug("current identity attribute: "
					+ currentAttributeType.getName());
		}
		List<AttributeTypeEntity> newAttributeTypes = new LinkedList<AttributeTypeEntity>();
		for (String newAttributeTypeName : applicationIdentityAttributeTypes) {
			LOG.debug("new identity attribute: " + newAttributeTypeName);
			AttributeTypeEntity newAttributeType = this.attributeTypeDAO
					.getAttributeType(newAttributeTypeName);
			newAttributeTypes.add(newAttributeType);
		}
		boolean requireNewIdentity = CollectionUtils.isProperSubCollection(
				currentAttributeTypes, newAttributeTypes);
		LOG.debug("require new identity: " + requireNewIdentity);
		if (true == requireNewIdentity) {
			long newIdentityVersion = currentIdentityVersion + 1;
			LOG.debug("new identity version: " + newIdentityVersion);
			this.applicationIdentityDAO.addApplicationIdentity(application,
					newIdentityVersion, newAttributeTypes);
			LOG.debug("setting new identity version on application");
			application.setCurrentApplicationIdentity(newIdentityVersion);
			return;
		}
		/*
		 * Else we still need to update the current application identity.
		 */
		LOG.debug("changing current identity version: "
				+ currentIdentityVersion);
		applicationIdentity.setAttributeTypes(newAttributeTypes);
	}

}
