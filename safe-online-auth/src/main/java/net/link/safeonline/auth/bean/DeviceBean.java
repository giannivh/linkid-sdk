/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.bean;

import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import net.link.safeonline.auth.AuthenticationConstants;
import net.link.safeonline.auth.Device;
import net.link.safeonline.auth.LoginManager;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.EmptyDevicePolicyException;
import net.link.safeonline.authentication.service.AuthenticationDevice;
import net.link.safeonline.authentication.service.DevicePolicyService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.core.ResourceBundle;

@Stateful
@Name("device")
@LocalBinding(jndiBinding = AuthenticationConstants.JNDI_PREFIX
		+ "DeviceBean/local")
public class DeviceBean implements Device {

	private static final Log LOG = LogFactory.getLog(DeviceBean.class);

	@In(create = true)
	FacesMessages facesMessages;

	@EJB
	private DevicePolicyService devicePolicyService;

	@In(value = LoginManager.APPLICATION_ID_ATTRIBUTE, required = true)
	private String application;

	@In(value = LoginManager.REQUIRED_DEVICES_ATTRIBUTE, required = false)
	private Set<AuthenticationDevice> requiredDevicePolicy;

	@Out(required = false, scope = ScopeType.SESSION)
	private AuthenticationDevice deviceSelection;

	@Remove
	@Destroy
	public void destroyCallback() {
	}

	public String getSelection() {
		if (null == this.deviceSelection)
			return null;
		return this.deviceSelection.getDeviceName();
	}

	public void setSelection(String deviceSelection) {
		this.deviceSelection = AuthenticationDevice
				.getAuthenticationDevice(deviceSelection);
	}

	public String next() {
		LOG.debug("next: " + this.deviceSelection.getDeviceName());
		if (null == this.deviceSelection) {
			LOG.debug("Please make a selection.");
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorMakeSelection");
			return null;
		}
		return this.deviceSelection.getDeviceName();
	}

	@Factory("applicationDevices")
	public List<SelectItem> applicationDevicesFactory() {
		LOG.debug("application devices factory");
		List<SelectItem> applicationDevices = new LinkedList<SelectItem>();
		try {
			Set<AuthenticationDevice> devicePolicy = this.devicePolicyService
					.getDevicePolicy(this.application,
							this.requiredDevicePolicy);
			for (AuthenticationDevice device : devicePolicy) {
				String deviceName = device.getDeviceName();
				SelectItem applicationDevice = new SelectItem(deviceName);
				applicationDevices.add(applicationDevice);
			}
		} catch (ApplicationNotFoundException e) {
			LOG.error("application not found: " + this.application);
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorApplicationNotFound");
		} catch (EmptyDevicePolicyException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorEmptyDevicePolicy");
			LOG.error("empty device policy");
		}
		deviceNameDecoration(applicationDevices);
		return applicationDevices;
	}

	@Factory("allDevices")
	public List<SelectItem> allDevicesFactory() {
		LOG.debug("all devices factory");
		List<SelectItem> allDevices = new LinkedList<SelectItem>();
		Set<AuthenticationDevice> devices = this.devicePolicyService
				.getDevices();
		for (AuthenticationDevice device : devices) {
			String deviceName = device.getDeviceName();
			SelectItem allDevice = new SelectItem(deviceName);
			allDevices.add(allDevice);
		}
		deviceNameDecoration(allDevices);
		return allDevices;
	}

	private void deviceNameDecoration(List<SelectItem> selectItems) {
		for (SelectItem selectItem : selectItems) {
			String deviceId = (String) selectItem.getValue();
			try {
				String deviceName = ResourceBundle.instance().getString(
						deviceId);
				if (null == deviceName) {
					deviceName = deviceId;
				}
				selectItem.setLabel(deviceName);

			} catch (MissingResourceException e) {
				LOG.debug("resource not found: " + deviceId);
			}
		}
	}
}
