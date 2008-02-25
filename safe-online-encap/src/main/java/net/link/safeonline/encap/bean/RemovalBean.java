/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.encap.bean;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.MobileException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.DeviceDAO;
import net.link.safeonline.data.AttributeDO;
import net.link.safeonline.device.WeakMobileDeviceService;
import net.link.safeonline.device.sdk.seam.SafeOnlineDeviceUtils;
import net.link.safeonline.encap.EncapConstants;
import net.link.safeonline.encap.Removal;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionPK;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.DatatypeType;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.RegisteredDeviceEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.service.RegisteredDeviceService;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.log.Log;

@Stateful
@Name("removal")
@LocalBinding(jndiBinding = EncapConstants.JNDI_PREFIX + "RemovalBean/local")
public class RemovalBean implements Removal {

	private static final String MOBILE_ATTRIBUTE_LIST_NAME = "mobileAttributes";

	@EJB
	private WeakMobileDeviceService weakMobileDeviceService;

	@EJB
	private DeviceDAO deviceDAO;

	@EJB
	private AttributeTypeDAO attributeTypeDAO;

	@EJB
	private AttributeDAO attributeDAO;

	@EJB
	private RegisteredDeviceService registeredDeviceService;

	@Logger
	private Log log;

	@In(create = true)
	FacesMessages facesMessages;

	@In
	private String userId;

	@DataModel(MOBILE_ATTRIBUTE_LIST_NAME)
	List<AttributeDO> mobileAttributes;

	@SuppressWarnings("unused")
	@DataModelSelection(MOBILE_ATTRIBUTE_LIST_NAME)
	private AttributeDO selectedMobile;

	@Remove
	@Destroy
	public void destroyCallback() {
		this.log.debug("destroy");
	}

	private Locale getViewLocale() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		Locale viewLocale = facesContext.getViewRoot().getLocale();
		return viewLocale;
	}

	@Factory(MOBILE_ATTRIBUTE_LIST_NAME)
	public List<AttributeDO> mobileAttributesFactory() {
		Locale locale = getViewLocale();
		try {
			this.mobileAttributes = listAttributes(
					SafeOnlineConstants.ENCAP_DEVICE_ID, locale);
		} catch (DeviceNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorDeviceNotFound");
			this.log.error("device not found");
			return new LinkedList<AttributeDO>();
		}
		return this.mobileAttributes;
	}

	private List<AttributeDO> listAttributes(String deviceId, Locale locale)
			throws DeviceNotFoundException {
		/**
		 * TODO: external attributes
		 */
		this.log.debug("list attributes for device: " + deviceId);
		DeviceEntity device = this.deviceDAO.getDevice(deviceId);
		List<AttributeTypeEntity> deviceAttributeTypes = device
				.getAttributeTypes();
		List<AttributeDO> attributes = new LinkedList<AttributeDO>();

		RegisteredDeviceEntity registeredDevice = this.registeredDeviceService
				.getRegisteredDevice(this.userId);
		SubjectEntity subject = registeredDevice.getSubject();

		String language;
		if (null == locale) {
			language = null;
		} else {
			language = locale.getLanguage();
		}

		this.log.debug("# device attributes: " + deviceAttributeTypes.size());
		for (AttributeTypeEntity attributeType : deviceAttributeTypes) {
			this.log.debug("attribute type: " + attributeType.getName());
			if (false == attributeType.isUserVisible()) {
				continue;
			}

			boolean multivalued = attributeType.isMultivalued();
			String name = attributeType.getName();
			DatatypeType type = attributeType.getType();
			boolean editable = attributeType.isUserEditable();
			boolean dataMining = false;
			String humanReabableName = null;
			String description = null;
			if (null != language) {
				AttributeTypeDescriptionEntity attributeTypeDescription = this.attributeTypeDAO
						.findDescription(new AttributeTypeDescriptionPK(name,
								language));
				if (null != attributeTypeDescription) {
					humanReabableName = attributeTypeDescription.getName();
					description = attributeTypeDescription.getDescription();
				}
			}
			List<AttributeEntity> attributeList = this.attributeDAO
					.listAttributes(subject, attributeType);
			for (AttributeEntity attribute : attributeList) {
				String stringValue;
				Boolean booleanValue;
				long index;
				if (null != attribute) {
					stringValue = attribute.getStringValue();
					booleanValue = attribute.getBooleanValue();
					index = attribute.getAttributeIndex();
				} else {
					stringValue = null;
					booleanValue = null;
					index = 0;
				}
				AttributeDO attributeView = new AttributeDO(name, type,
						multivalued, index, humanReabableName, description,
						editable, dataMining, stringValue, booleanValue);
				attributes.add(attributeView);
			}
		}
		return attributes;
	}

	public String mobileRemove() {
		try {
			this.weakMobileDeviceService.remove(this.userId,
					this.selectedMobile.getStringValue());
		} catch (MobileException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "mobileRemovalFailed");
			return null;
		} catch (MalformedURLException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "mobileRemovalFailed");
			return null;
		} catch (SubjectNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorSubjectNotFound");
			return null;
		}
		try {
			SafeOnlineDeviceUtils.deviceExit();
		} catch (IOException e) {
			this.facesMessages.addFromResourceBundle(
					FacesMessage.SEVERITY_ERROR, "errorIO");
			return null;
		}
		return "success";
	}

}