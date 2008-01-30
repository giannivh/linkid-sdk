/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.service;

import javax.ejb.Local;

import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.RegisteredDeviceEntity;
import net.link.safeonline.entity.SubjectEntity;

/**
 * <h2>{@link RegisteredDeviceService} - Service for device registration.</h2>
 *
 * <p>
 * Creates device registrations for a subject-device pair. These registrations
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
public interface RegisteredDeviceService {

	public RegisteredDeviceEntity getDeviceRegistration(SubjectEntity subject,
			DeviceEntity device);
}
