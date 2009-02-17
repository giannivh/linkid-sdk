/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.model.digipass;

import java.util.List;
import java.util.Locale;

import javax.ejb.Local;

import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.DeviceDisabledException;
import net.link.safeonline.authentication.exception.DeviceRegistrationNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.data.AttributeDO;


@Local
public interface DigipassDeviceService extends DigipassService {

    public static final String JNDI_BINDING = DigipassService.JNDI_PREFIX + "DigipassDeviceServiceBean/local";


    String authenticate(String userId, String token)
            throws SubjectNotFoundException, PermissionDeniedException, DeviceDisabledException;

    String register(String userId, String serialNumber)
            throws ArgumentIntegrityException, SubjectNotFoundException;

    void remove(String serialNumber)
            throws DigipassException;

    List<AttributeDO> getDigipasses(String userId, Locale locale)
            throws SubjectNotFoundException;

    void disable(String userId, String serialNumber)
            throws SubjectNotFoundException, DeviceRegistrationNotFoundException;

    String enable(String userId, String serialNumber, String token)
            throws SubjectNotFoundException, DeviceRegistrationNotFoundException;
}
