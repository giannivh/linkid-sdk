/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Copyright 2005-2006 Frank Cornelis.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.pkix.model.bean;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.security.auth.x500.X500Principal;

import net.link.safeonline.entity.pkix.TrustDomainEntity;
import net.link.safeonline.entity.pkix.TrustPointEntity;
import net.link.safeonline.entity.pkix.TrustPointPK;
import net.link.safeonline.pkix.dao.TrustDomainDAO;
import net.link.safeonline.pkix.dao.TrustPointDAO;
import net.link.safeonline.pkix.exception.TrustDomainNotFoundException;
import net.link.safeonline.pkix.model.CachedOcspValidator;
import net.link.safeonline.pkix.model.PkiValidator;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

@Stateless
public class PkiValidatorBean implements PkiValidator {

	private static final Log LOG = LogFactory.getLog(PkiValidatorBean.class);

	@EJB
	private TrustPointDAO trustPointDAO;

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private CachedOcspValidator cachedOcspValidator;

	public boolean validateCertificate(TrustDomainEntity trustDomain,
			X509Certificate certificate) {
		/*
		 * We don't use the JDK certificate path builder API here, since it
		 * doesn't bring anything but unnecessary complexity. Keep It Simple,
		 * Stupid.
		 */

		if (null == certificate) {
			throw new IllegalArgumentException("certificate is null");
		}

		LOG.debug("validate certificate "
				+ certificate.getSubjectX500Principal() + " in domain "
				+ trustDomain.getName());

		List<TrustPointEntity> trustPointPath = buildTrustPointPath(
				trustDomain, certificate);

		boolean verificationResult = verifyPath(trustDomain, certificate,
				trustPointPath);
		if (false == verificationResult) {
			return false;
		}
		return true;
	}

	private boolean checkValidity(X509Certificate certificate) {
		try {
			certificate.checkValidity();
			return true;
		} catch (CertificateExpiredException e) {
			LOG.debug("certificate expired");
			return false;
		} catch (CertificateNotYetValidException e) {
			LOG.debug("certificate not yet valid");
			return false;
		}
	}

	/**
	 * Build the trust point path for a given certificate.
	 * 
	 * @param trustDomain
	 * @param certificate
	 * @return the path, or an empty list otherwise.
	 */
	private List<TrustPointEntity> buildTrustPointPath(
			TrustDomainEntity trustDomain, X509Certificate certificate) {

		List<TrustPointEntity> trustPoints = this.trustPointDAO
				.listTrustPoints(trustDomain);
		HashMap<TrustPointPK, TrustPointEntity> trustPointMap = new HashMap<TrustPointPK, TrustPointEntity>();
		for (TrustPointEntity trustPoint : trustPoints) {
			trustPointMap.put(trustPoint.getPk(), trustPoint);
		}

		List<TrustPointEntity> trustPointPath = new LinkedList<TrustPointEntity>();

		LOG.debug("build path for cert: "
				+ certificate.getSubjectX500Principal());

		X509Certificate currentRootCertificate = certificate;
		while (true) {
			byte[] authorityKeyIdentifierData = currentRootCertificate
					.getExtensionValue(X509Extensions.AuthorityKeyIdentifier
							.getId());
			String keyId;
			if (null == authorityKeyIdentifierData) {
				/*
				 * PKIX RFC allows this for the root CA certificate.
				 */
				LOG
						.warn("certificate has no authority key indentifier extension");
				/*
				 * NULL is not allowed for persistence.
				 */
				keyId = "";
			} else {
				AuthorityKeyIdentifierStructure authorityKeyIdentifierStructure;
				try {
					authorityKeyIdentifierStructure = new AuthorityKeyIdentifierStructure(
							authorityKeyIdentifierData);
				} catch (IOException e) {
					LOG
							.error("error parsing authority key identifier structure");
					break;
				}
				keyId = new String(Hex
						.encodeHex(authorityKeyIdentifierStructure
								.getKeyIdentifier()));
			}
			String issuer = currentRootCertificate.getIssuerX500Principal()
					.toString();
			LOG.debug("issuer: " + issuer);
			LOG.debug("keyId: " + keyId);
			TrustPointPK trustPointPK = new TrustPointPK(trustDomain, issuer,
					keyId);
			TrustPointEntity matchingTrustPoint = trustPointMap
					.get(trustPointPK);
			if (null == matchingTrustPoint) {
				LOG.debug("no matching trust point found");
				break;
			}
			LOG.debug("found path node: "
					+ matchingTrustPoint.getCertificate()
							.getSubjectX500Principal());
			trustPointPath.add(0, matchingTrustPoint);
			currentRootCertificate = matchingTrustPoint.getCertificate();
			if (isSelfIssued(currentRootCertificate)) {
				break;
			}
		}

		LOG.debug("path construction completed");
		return trustPointPath;
	}

	private boolean isSelfIssued(X509Certificate certificate) {
		X500Principal issuer = certificate.getIssuerX500Principal();
		X500Principal subject = certificate.getSubjectX500Principal();
		boolean result = subject.equals(issuer);
		return result;
	}

	boolean verifyPath(TrustDomainEntity trustDomain,
			X509Certificate certificate, List<TrustPointEntity> trustPointPath) {
		if (trustPointPath.isEmpty()) {
			LOG.debug("trust point path is empty");
			return false;
		}

		boolean performOcspCheck = trustDomain.isPerformOcspCheck();

		X509Certificate rootCertificate = trustPointPath.get(0)
				.getCertificate();
		X509Certificate issuerCertificate = rootCertificate;
		PublicKey issuerPublicKey = issuerCertificate.getPublicKey();

		for (TrustPointEntity trustPoint : trustPointPath) {
			X509Certificate trustPointCertificate = trustPoint.getCertificate();
			LOG.debug("verifying: "
					+ trustPointCertificate.getSubjectX500Principal());
			if (false == checkValidity(trustPointCertificate)) {
				return false;
			}
			if (false == verifySignature(trustPointCertificate, issuerPublicKey)) {
				return false;
			}
			if (false == verifyConstraints(trustPointCertificate)) {
				LOG.debug("verify constraints did not pass");
				return false;
			}
			issuerCertificate = trustPointCertificate;
			issuerPublicKey = issuerCertificate.getPublicKey();
		}

		if (false == checkValidity(certificate)) {
			return false;
		}
		if (false == verifySignature(certificate, issuerPublicKey)) {
			return false;
		}
		if (true == performOcspCheck) {
			LOG.debug("performing OCSP check");
			if (false == cachedOcspValidator.performCachedOcspCheck(
					trustDomain, certificate, issuerCertificate)) {
				return false;
			}
		}

		return true;
	}

	private boolean verifyConstraints(X509Certificate certificate) {
		byte[] basicConstraintsValue = certificate
				.getExtensionValue(X509Extensions.BasicConstraints.getId());
		if (null == basicConstraintsValue) {
			LOG.debug("no basic contraints extension present");
			/*
			 * A basic constraints extension is optional.
			 */
			return true;
		}
		ASN1Encodable basicConstraintsDecoded;
		try {
			basicConstraintsDecoded = X509ExtensionUtil
					.fromExtensionValue(basicConstraintsValue);
		} catch (IOException e) {
			LOG.error("IO error: " + e.getMessage(), e);
			return false;
		}
		if (false == basicConstraintsDecoded instanceof ASN1Sequence) {
			LOG.debug("basic constraints extension is not an ASN1 sequence");
			return false;
		}
		ASN1Sequence basicConstraintsSequence = (ASN1Sequence) basicConstraintsDecoded;
		BasicConstraints basicConstraints = new BasicConstraints(
				basicConstraintsSequence);
		if (false == basicConstraints.isCA()) {
			LOG.debug("basic contraints says not a CA");
			return false;
		}
		return true;
	}

	private boolean verifySignature(X509Certificate certificate,
			PublicKey issuerPublicKey) {
		try {
			certificate.verify(issuerPublicKey);
		} catch (InvalidKeyException e) {
			LOG.debug("invalid key");
			/*
			 * This can occur if a root certificate was not self-signed.
			 */
			return false;
		} catch (CertificateException e) {
			LOG.debug("cert error: " + e.getMessage());
			return false;
		} catch (NoSuchAlgorithmException e) {
			LOG.debug("algo error");
			return false;
		} catch (NoSuchProviderException e) {
			LOG.debug("provider error");
			return false;
		} catch (SignatureException e) {
			LOG.debug("sign error: " + e.getMessage(), e);
			return false;
		}
		return true;
	}

	public boolean validateCertificate(String trustDomainName,
			X509Certificate certificate) throws TrustDomainNotFoundException {
		TrustDomainEntity trustDomain = this.trustDomainDAO
				.getTrustDomain(trustDomainName);
		boolean result = validateCertificate(trustDomain, certificate);
		return result;
	}
}