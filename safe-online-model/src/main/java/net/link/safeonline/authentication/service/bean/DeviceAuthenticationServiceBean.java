/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service.bean;

import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.service.DeviceAuthenticationService;
import net.link.safeonline.dao.DeviceDAO;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.pkix.TrustDomainEntity;
import net.link.safeonline.entity.pkix.TrustPointEntity;
import net.link.safeonline.pkix.dao.TrustDomainDAO;
import net.link.safeonline.pkix.dao.TrustPointDAO;
import net.link.safeonline.pkix.exception.TrustDomainNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of device authentication service.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class DeviceAuthenticationServiceBean implements
		DeviceAuthenticationService {

	private static final Log LOG = LogFactory
			.getLog(DeviceAuthenticationServiceBean.class);

	@EJB
	private DeviceDAO deviceDAO;

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private TrustPointDAO trustPointDAO;

	public String authenticate(X509Certificate certificate)
			throws DeviceNotFoundException {
		DeviceEntity device = this.deviceDAO.getDevice(certificate);
		LOG.debug("authenticated device: " + device.getName());
		return device.getName();
	}

	public X509Certificate getCertificate(String deviceName)
			throws DeviceNotFoundException {
		LOG.debug("get certificate for device: " + deviceName);
		DeviceEntity device = this.deviceDAO.getDevice(deviceName);
		X509Certificate certificate = device.getCertificate();
		return certificate;
	}

	public TrustPointEntity findTrustPoint(String domainName,
			X509Certificate certificate) throws TrustDomainNotFoundException {
		LOG.debug("find trust point: domain=" + domainName + " cert="
				+ certificate);
		TrustDomainEntity trustDomain = this.trustDomainDAO
				.getTrustDomain(domainName);
		return this.trustPointDAO.findTrustPoint(trustDomain, certificate);
	}

}