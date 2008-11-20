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

import net.link.safeonline.SafeOnlineService;
import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.AttributeNotFoundException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.DeviceDisabledException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.DeviceRegistrationNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.data.AttributeDO;


@Local
public interface DigipassDeviceService extends SafeOnlineService {

    public static final String JNDI_BINDING = SafeOnlineService.JNDI_PREFIX + "DigipassDeviceServiceBean/local";


    String authenticate(String userId, String token)
            throws SubjectNotFoundException, PermissionDeniedException, DeviceNotFoundException, DeviceDisabledException;

    String register(String userId, String serialNumber)
            throws ArgumentIntegrityException, SubjectNotFoundException, PermissionDeniedException, AttributeTypeNotFoundException;

    void remove(String serialNumber)
            throws DigipassException, AttributeTypeNotFoundException, PermissionDeniedException, AttributeNotFoundException;

    List<AttributeDO> getDigipasses(String userId, Locale locale)
            throws SubjectNotFoundException, PermissionDeniedException, DeviceNotFoundException;

    void disable(String userId, String serialNumber)
            throws SubjectNotFoundException, DeviceNotFoundException, DeviceRegistrationNotFoundException;
}