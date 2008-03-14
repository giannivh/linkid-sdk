/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.device;

import javax.ejb.Local;

import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.AttributeNotFoundException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.ExistingUserException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.bean.AuthenticationStatement;
import net.link.safeonline.authentication.service.bean.RegistrationStatement;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.pkix.exception.TrustDomainNotFoundException;

@Local
public interface BeIdDeviceService {

	String authenticate(String sessionId,
			AuthenticationStatement authenticationStatement)
			throws ArgumentIntegrityException, TrustDomainNotFoundException,
			SubjectNotFoundException;

	void register(String deviceUserId, byte[] identityStatementData)
			throws PermissionDeniedException, ArgumentIntegrityException,
			TrustDomainNotFoundException, AttributeTypeNotFoundException,
			DeviceNotFoundException, AttributeNotFoundException;

	SubjectEntity registerAndAuthenticate(String sessionId, String username,
			RegistrationStatement registrationStatement)
			throws ArgumentIntegrityException, ExistingUserException,
			AttributeTypeNotFoundException, TrustDomainNotFoundException;

	void remove(String deviceUserId, byte[] identityStatementData)
			throws TrustDomainNotFoundException, PermissionDeniedException,
			ArgumentIntegrityException, AttributeTypeNotFoundException,
			SubjectNotFoundException;

}
