/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service.bean;

import java.security.cert.X509Certificate;
import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.exception.SubscriptionNotFoundException;
import net.link.safeonline.authentication.exception.TrustDomainNotFoundException;
import net.link.safeonline.authentication.service.AuthenticationService;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.HistoryDAO;
import net.link.safeonline.dao.SubjectDAO;
import net.link.safeonline.dao.SubjectIdentifierDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.entity.TrustDomainEntity;
import net.link.safeonline.model.PkiProvider;
import net.link.safeonline.model.PkiProviderManager;
import net.link.safeonline.model.PkiValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of authentication service interface. This component does not
 * live within the SafeOnline core security domain (chicken-egg problem).
 * 
 * @author fcorneli
 * 
 */
@Stateless
public class AuthenticationServiceBean implements AuthenticationService {

	private static final Log LOG = LogFactory
			.getLog(AuthenticationServiceBean.class);

	@EJB
	private SubjectDAO entityDAO;

	@EJB
	private ApplicationDAO applicationDAO;

	@EJB
	private SubscriptionDAO subscriptionDAO;

	@EJB
	private HistoryDAO historyDAO;

	@EJB
	private AttributeDAO attributeDAO;

	@EJB
	private PkiProviderManager pkiProviderManager;

	@EJB
	private PkiValidator pkiValidator;

	@EJB
	private SubjectIdentifierDAO subjectIdentifierDAO;

	public boolean authenticate(String applicationName, String login,
			String password) {
		LOG.debug("authenticate \"" + login + "\" for \"" + applicationName
				+ "\"");

		// TODO: aspectize the input validation
		if (null == login) {
			throw new IllegalArgumentException("login is null");
		}

		if (null == password) {
			throw new IllegalArgumentException("password is null");
		}

		SubjectEntity subject = this.entityDAO.findSubject(login);
		if (null == subject) {
			LOG.debug("subject not found");
			return false;
		}

		AttributeEntity passwordAttribute = this.attributeDAO.findAttribute(
				SafeOnlineConstants.PASSWORD_ATTRIBUTE, login);
		if (null == passwordAttribute) {
			String event = "password attribute not present for subject "
					+ login;
			addHistoryEntry(subject, event);
			return false;
		}

		String actualPassword = passwordAttribute.getStringValue();
		if (null == actualPassword) {
			String event = "password is null for subject " + login;
			addHistoryEntry(subject, event);
			return false;
		}

		if (!actualPassword.equals(password)) {
			String event = "incorrect password for application: "
					+ applicationName;
			addHistoryEntry(subject, event);
			return false;
		}

		ApplicationEntity application = this.applicationDAO
				.findApplication(applicationName);
		if (null == application) {
			String event = "application not found: " + applicationName;
			addHistoryEntry(subject, event);
			return false;
		}

		SubscriptionEntity subscription = this.subscriptionDAO
				.findSubscription(subject, application);
		if (null == subscription) {
			String event = "subscription not found for application: "
					+ applicationName;
			addHistoryEntry(subject, event);
			return false;
		}

		addHistoryEntry(subject, "authenticated for application "
				+ applicationName);

		return true;
	}

	private void addHistoryEntry(SubjectEntity subject, String event) {
		Date now = new Date();
		this.historyDAO.addHistoryEntry(now, subject, event);
		LOG.debug(event);
	}

	public boolean authenticate(String login, String password) {
		LOG.debug("authenticate \"" + login + "\"");

		// TODO: aspectize the input validation
		if (null == login) {
			throw new IllegalArgumentException("login is null");
		}

		if (null == password) {
			throw new IllegalArgumentException("password is null");
		}

		SubjectEntity subject = this.entityDAO.findSubject(login);
		if (null == subject) {
			LOG.debug("subject not found");
			return false;
		}

		AttributeEntity passwordAttribute = this.attributeDAO.findAttribute(
				SafeOnlineConstants.PASSWORD_ATTRIBUTE, login);
		if (null == passwordAttribute) {
			String event = "incorrect password";
			addHistoryEntry(subject, event);
			return false;
		}

		String actualPassword = passwordAttribute.getStringValue();
		if (null == actualPassword) {
			addHistoryEntry(subject, "actual password is null for subject: "
					+ login);
			return false;
		}

		if (!actualPassword.equals(password)) {
			Date now = new Date();
			String event = "incorrect password";
			this.historyDAO.addHistoryEntry(now, subject, event);
			LOG.debug(event);
			return false;
		}

		return true;
	}

	public String authenticate(String sessionId,
			byte[] authenticationStatementData)
			throws ArgumentIntegrityException, TrustDomainNotFoundException,
			SubjectNotFoundException, SubscriptionNotFoundException,
			ApplicationNotFoundException {
		LOG.debug("authenticate session: " + sessionId);
		AuthenticationStatement authenticationStatement = new AuthenticationStatement(
				authenticationStatementData);

		X509Certificate certificate = authenticationStatement.verifyIntegrity();
		if (null == certificate) {
			throw new ArgumentIntegrityException();
		}

		PkiProvider pkiProvider = this.pkiProviderManager
				.findPkiProvider(certificate);
		if (null == pkiProvider) {
			throw new ArgumentIntegrityException();
		}
		TrustDomainEntity trustDomain = pkiProvider.getTrustDomain();
		boolean validationResult = this.pkiValidator.validateCertificate(
				trustDomain, certificate);
		if (false == validationResult) {
			throw new ArgumentIntegrityException();
		}

		if (false == sessionId.equals(authenticationStatement.getSessionId())) {
			throw new ArgumentIntegrityException();
		}

		String identifierDomainName = pkiProvider.getIdentifierDomainName();
		String identifier = pkiProvider.getSubjectIdentifier(certificate);
		SubjectEntity subject = this.subjectIdentifierDAO.findSubject(
				identifierDomainName, identifier);
		if (null == subject) {
			LOG.warn("no subject was found for the given certificate");
			throw new SubjectNotFoundException();
		}
		LOG.debug("subject: " + subject);

		String applicationId = authenticationStatement.getApplicationId();
		ApplicationEntity application = this.applicationDAO
				.findApplication(applicationId);
		if (null == application) {
			String event = "application not found: " + applicationId;
			addHistoryEntry(subject, event);
			throw new ApplicationNotFoundException();
		}

		SubscriptionEntity subscription = this.subscriptionDAO
				.findSubscription(subject, application);
		if (null == subscription) {
			String event = "subscription not found for application: "
					+ applicationId;
			addHistoryEntry(subject, event);
			throw new SubscriptionNotFoundException();
		}

		addHistoryEntry(subject, "authenticated subject " + subject
				+ " for application " + applicationId);

		return subject.getLogin();
	}

	public String authenticate(X509Certificate certificate)
			throws ApplicationNotFoundException {
		ApplicationEntity application = this.applicationDAO
				.getApplication(certificate);
		String applicationName = application.getName();
		LOG.debug("authenticated application: " + applicationName);
		return applicationName;
	}
}
