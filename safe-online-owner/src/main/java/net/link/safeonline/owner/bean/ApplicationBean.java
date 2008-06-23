/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.owner.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.FacesContext;
import javax.interceptor.Interceptors;

import net.link.safeonline.authentication.exception.ApplicationIdentityNotFoundException;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.service.ApplicationService;
import net.link.safeonline.authentication.service.DevicePolicyService;
import net.link.safeonline.authentication.service.SubscriptionService;
import net.link.safeonline.authentication.service.UsageAgreementService;
import net.link.safeonline.ctrl.error.ErrorMessageInterceptor;
import net.link.safeonline.entity.AllowedDeviceEntity;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityAttributeEntity;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.UsageAgreementEntity;
import net.link.safeonline.owner.Application;
import net.link.safeonline.owner.DeviceEntry;
import net.link.safeonline.owner.OwnerConstants;
import net.link.safeonline.service.DeviceService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;

@Stateful
@Name("ownerApplication")
@LocalBinding(jndiBinding = OwnerConstants.JNDI_PREFIX
		+ "ApplicationBean/local")
@SecurityDomain(OwnerConstants.SAFE_ONLINE_OWNER_SECURITY_DOMAIN)
@Interceptors(ErrorMessageInterceptor.class)
public class ApplicationBean implements Application {

	private static final Log LOG = LogFactory.getLog(ApplicationBean.class);

	private static final String selectedApplicationUsageAgreementsModel = "selectedApplicationUsageAgreements";

	@EJB
	private ApplicationService applicationService;

	@EJB
	private UsageAgreementService usageAgreementService;

	@EJB
	private SubscriptionService subscriptionService;

	@EJB
	private DeviceService deviceService;

	@EJB
	private DevicePolicyService devicePolicyService;

	@In(create = true)
	FacesMessages facesMessages;

	@SuppressWarnings("unused")
	@Out
	private long numberOfSubscriptions;

	/*
	 * Lifecycle
	 */
	@Remove
	@Destroy
	public void destroyCallback() {
	}

	/*
	 * Seam Data models
	 */
	@SuppressWarnings("unused")
	@DataModel
	private List<ApplicationEntity> ownerApplicationList;

	@DataModelSelection("ownerApplicationList")
	@Out(required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private ApplicationEntity selectedApplication;

	@SuppressWarnings("unused")
	@DataModel(value = "selectedApplicationIdentity")
	private Set<ApplicationIdentityAttributeEntity> selectedApplicationIdentity;

	@SuppressWarnings("unused")
	@DataModel(value = selectedApplicationUsageAgreementsModel)
	private List<UsageAgreementEntity> selectedApplicationUsageAgreements;

	@DataModelSelection(selectedApplicationUsageAgreementsModel)
	@Out(required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private UsageAgreementEntity selectedUsageAgreement;

	@SuppressWarnings("unused")
	@Out(required = false, scope = ScopeType.SESSION)
	private UsageAgreementEntity draftUsageAgreement;

	@SuppressWarnings("unused")
	@Out(required = false, scope = ScopeType.SESSION)
	private UsageAgreementEntity currentUsageAgreement;

	@DataModel
	private List<DeviceEntry> allowedDevices;

	/*
	 * Seam Data model Factories
	 */
	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	@Factory("ownerApplicationList")
	public void applicationListFactory() {
		LOG.debug("application list factory");
		this.ownerApplicationList = this.applicationService
				.getOwnedApplications();
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	@Factory("allowedDevices")
	public void allowedDevices() {
		if (this.selectedApplication == null) {
			return;
		}
		FacesContext facesContext = FacesContext.getCurrentInstance();
		Locale viewLocale = facesContext.getViewRoot().getLocale();

		List<DeviceEntity> deviceList = this.deviceService.listDevices();
		List<AllowedDeviceEntity> allowedDeviceList = this.deviceService
				.listAllowedDevices(this.selectedApplication);

		this.allowedDevices = new ArrayList<DeviceEntry>();

		boolean defaultValue = false;

		for (DeviceEntity deviceEntity : deviceList) {
			String deviceDescription = this.devicePolicyService
					.getDeviceDescription(deviceEntity.getName(), viewLocale);
			this.allowedDevices.add(new DeviceEntry(deviceEntity,
					deviceDescription, defaultValue, 0));
		}

		for (AllowedDeviceEntity allowedDevice : allowedDeviceList) {
			for (DeviceEntry deviceEntry : this.allowedDevices) {
				if (deviceEntry.getDevice().equals(allowedDevice.getDevice())) {
					deviceEntry.setAllowed(true);
					deviceEntry.setWeight(allowedDevice.getWeight());
				}
			}
		}
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	@Factory("selectedApplicationUsageAgreements")
	public void usageAgreementListFactory()
			throws ApplicationNotFoundException, PermissionDeniedException {
		if (null == this.selectedApplication) {
			return;
		}
		LOG.debug("usage agreement list factory");
		this.selectedApplicationUsageAgreements = this.usageAgreementService
				.getUsageAgreements(this.selectedApplication.getName());
	}

	/*
	 * Actions
	 */
	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	public String view() throws ApplicationNotFoundException,
			PermissionDeniedException, ApplicationIdentityNotFoundException {
		String applicationName = this.selectedApplication.getName();
		LOG.debug("view: " + applicationName);
		this.numberOfSubscriptions = this.subscriptionService
				.getNumberOfSubscriptions(applicationName);
		this.selectedApplicationIdentity = this.applicationService
				.getCurrentApplicationIdentity(applicationName);
		return "view-application";
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	public String edit() {
		LOG.debug("edit: " + this.selectedApplication.getName());
		return "edit-application";
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	public String save() throws ApplicationNotFoundException,
			PermissionDeniedException {
		String applicationName = this.selectedApplication.getName();
		String applicationDescription = this.selectedApplication
				.getDescription();
		boolean deviceRestriction = this.selectedApplication
				.isDeviceRestriction();
		LOG.debug("save: " + applicationName);
		LOG.debug("description: " + applicationDescription);

		List<AllowedDeviceEntity> allowedDeviceList = new ArrayList<AllowedDeviceEntity>();
		for (DeviceEntry deviceEntry : this.allowedDevices) {
			if (deviceEntry.isAllowed() == true) {
				AllowedDeviceEntity device = new AllowedDeviceEntity(
						this.selectedApplication, deviceEntry.getDevice(),
						deviceEntry.getWeight());
				allowedDeviceList.add(device);
			}
		}

		this.applicationService.setApplicationDescription(applicationName,
				applicationDescription);
		this.applicationService.setApplicationDeviceRestriction(
				applicationName, deviceRestriction);
		this.deviceService.setAllowedDevices(this.selectedApplication,
				allowedDeviceList);
		return "saved";
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	public String viewStats() {
		return "viewstats";
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	public String viewUsageAgreement() {
		LOG.debug("view usage agreement for application: "
				+ this.selectedApplication.getName() + ", version="
				+ this.selectedUsageAgreement.getUsageAgreementVersion());
		return "view-usage-agreement";
	}

	@RolesAllowed(OwnerConstants.OWNER_ROLE)
	public String editUsageAgreement() throws ApplicationNotFoundException,
			PermissionDeniedException {
		LOG.debug("edit usage agreement for application: "
				+ this.selectedApplication.getName());
		this.draftUsageAgreement = this.usageAgreementService
				.getDraftUsageAgreement(this.selectedApplication.getName());
		this.currentUsageAgreement = this.usageAgreementService
				.getCurrentUsageAgreement(this.selectedApplication.getName());
		return "edit-usage-agreement";
	}
}
