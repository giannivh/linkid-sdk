/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.device.bean;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.audit.SecurityAuditLogger;
import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.MobileAuthenticationException;
import net.link.safeonline.authentication.exception.MobileException;
import net.link.safeonline.authentication.exception.MobileRegistrationException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.HistoryDAO;
import net.link.safeonline.dao.SubjectIdentifierDAO;
import net.link.safeonline.device.WeakMobileDeviceService;
import net.link.safeonline.device.WeakMobileDeviceServiceRemote;
import net.link.safeonline.device.backend.MobileManager;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.HistoryEventType;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.audit.SecurityThreatType;
import net.link.safeonline.service.SubjectService;

@Stateless
public class WeakMobileDeviceServiceBean implements WeakMobileDeviceService,
		WeakMobileDeviceServiceRemote {

	@EJB
	private SubjectService subjectService;

	@EJB
	private SubjectIdentifierDAO subjectIdentifierDAO;

	@EJB
	private MobileManager mobileManager;

	@EJB
	private AttributeDAO attributeDAO;

	@EJB
	private AttributeTypeDAO attributeTypeDAO;

	@EJB
	private HistoryDAO historyDAO;

	@EJB
	private SecurityAuditLogger securityAuditLogger;

	public SubjectEntity authenticate(String mobile, String challengeId,
			String mobileOTP) throws MalformedURLException, MobileException,
			SubjectNotFoundException, MobileAuthenticationException {
		SubjectEntity subject = this.subjectIdentifierDAO.findSubject(
				SafeOnlineConstants.WEAK_MOBILE_IDENTIFIER_DOMAIN, mobile);

		boolean result = this.mobileManager.verifyOTP(challengeId, mobileOTP);
		if (false == result) {
			this.historyDAO.addHistoryEntry(new Date(), subject,
					HistoryEventType.LOGIN_INCORRECT_MOBILE_TOKEN, null, null);
			this.securityAuditLogger.addSecurityAudit(
					SecurityThreatType.DECEPTION, subject.getUserId(),
					"incorrect mobile token");
			throw new MobileAuthenticationException();
		}
		return subject;
	}

	public String register(SubjectEntity subject, String mobile)
			throws MobileException, MalformedURLException,
			MobileRegistrationException, ArgumentIntegrityException {
		SubjectEntity existingMappedSubject = this.subjectIdentifierDAO
				.findSubject(SafeOnlineConstants.WEAK_MOBILE_IDENTIFIER_DOMAIN,
						mobile);
		if (null != existingMappedSubject) {
			throw new ArgumentIntegrityException();
		}
		String activationCode = this.mobileManager.activate(mobile, subject);
		if (null == activationCode)
			throw new MobileRegistrationException();
		setMobile(subject, mobile);
		this.subjectIdentifierDAO.addSubjectIdentifier(
				SafeOnlineConstants.WEAK_MOBILE_IDENTIFIER_DOMAIN, mobile,
				subject);
		return activationCode;
	}

	private void setMobile(SubjectEntity subject, String mobile) {
		AttributeTypeEntity mobileAttributeType;
		try {
			mobileAttributeType = this.attributeTypeDAO
					.getAttributeType(SafeOnlineConstants.WEAK_MOBILE_ATTRIBUTE);
		} catch (AttributeTypeNotFoundException e) {
			throw new EJBException("weak mobile attribute type not found");
		}
		List<AttributeEntity> mobileAttributes = this.attributeDAO
				.listAttributes(subject, mobileAttributeType);
		AttributeEntity mobileAttribute = this.attributeDAO.addAttribute(
				mobileAttributeType, subject, mobileAttributes.size());
		mobileAttribute.setStringValue(mobile);
	}

	public void remove(SubjectEntity subject, String mobile)
			throws MobileException, MalformedURLException {
		AttributeTypeEntity mobileAttributeType;
		try {
			mobileAttributeType = this.attributeTypeDAO
					.getAttributeType(SafeOnlineConstants.WEAK_MOBILE_ATTRIBUTE);
		} catch (AttributeTypeNotFoundException e) {
			throw new EJBException("weak mobile attribute type not found");
		}
		this.subjectIdentifierDAO.removeOtherSubjectIdentifiers(
				SafeOnlineConstants.WEAK_MOBILE_IDENTIFIER_DOMAIN, mobile,
				subject);
		List<AttributeEntity> mobileAttributes = this.attributeDAO
				.listAttributes(subject, mobileAttributeType);
		for (AttributeEntity mobileAttribute : mobileAttributes) {
			if (mobileAttribute.getStringValue().equals(mobile))
				this.attributeDAO.removeAttribute(mobileAttribute);
		}
		this.mobileManager.remove(mobile);
	}

	public void update(SubjectEntity subject, String oldMobile, String newMobile) {
		// TODO Auto-generated method stub
	}

	public String requestOTP(String mobile) throws MalformedURLException,
			MobileException {
		return this.mobileManager.requestOTP(mobile);
	}

	public List<String> getMobiles(String login) {
		AttributeTypeEntity mobileAttributeType;
		try {
			mobileAttributeType = this.attributeTypeDAO
					.getAttributeType(SafeOnlineConstants.WEAK_MOBILE_ATTRIBUTE);
		} catch (AttributeTypeNotFoundException e) {
			throw new EJBException("weak mobile attribute type not found");
		}
		List<String> mobileList = new LinkedList<String>();
		SubjectEntity subject = this.subjectService
				.findSubjectFromUserName(login);
		List<AttributeEntity> mobileAttributes = this.attributeDAO
				.listAttributes(subject, mobileAttributeType);
		for (AttributeEntity mobileAttribute : mobileAttributes)
			mobileList.add(mobileAttribute.getStringValue());
		return mobileList;
	}
}
