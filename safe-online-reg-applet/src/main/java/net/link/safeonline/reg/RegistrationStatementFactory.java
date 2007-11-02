/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.reg;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import net.link.safeonline.p11sc.SmartCard;
import net.link.safeonline.shared.statement.RegistrationStatement;

public class RegistrationStatementFactory {

	private RegistrationStatementFactory() {
		// empty
	}

	public static byte[] createRegistrationStatement(String user,
			String sessionId, String applicationId, SmartCard smartCard) {

		X509Certificate authCert = smartCard.getAuthenticationCertificate();
		PrivateKey authPrivateKey = smartCard.getAuthenticationPrivateKey();
		RegistrationStatement registrationStatement = new RegistrationStatement(
				user, sessionId, applicationId, authCert, authPrivateKey);
		byte[] registrationStatementData = registrationStatement
				.generateStatement();
		return registrationStatementData;
	}
}