/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.service;

import javax.ejb.Local;

import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.entity.DeviceMappingEntity;

/**
 * <h2>{@link DeviceMappingService} - Service for device mapping registration.</h2>
 * 
 * <p>
 * Creates device mappings for subject-device issuer pair. These mappings
 * contain a UUID that is used by the device provider to map the identity
 * provided by their device to an OLAS identity.
 * </p>
 * 
 * <p>
 * <i>Jan 29, 2008</i>
 * </p>
 * 
 * @author mbillemo
 */
@Local
public interface DeviceMappingService {

	public DeviceMappingEntity getDeviceMapping(String userId, String deviceName)
			throws SubjectNotFoundException, DeviceNotFoundException;

	public DeviceMappingEntity getDeviceMapping(String id);
}