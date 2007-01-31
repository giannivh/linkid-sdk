/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.model.bean;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.TestCase;
import net.link.safeonline.model.PkiProvider;
import net.link.safeonline.model.bean.PkiProviderManagerBean;
import net.link.safeonline.test.util.EJBTestUtils;
import net.link.safeonline.test.util.PkiTestUtils;

public class PkiProviderManagerBeanTest extends TestCase {

	private PkiProviderManagerBean testedInstance;

	private PkiProvider mockPkiProvider;

	protected void setUp() throws Exception {
		super.setUp();

		System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
				"org.shiftone.ooc.InitialContextFactoryImpl");

		InitialContext initialContext = new InitialContext();
		Context safeOnlineContext = initialContext
				.createSubcontext("SafeOnline");
		Context pkixContext = safeOnlineContext.createSubcontext("pkix");
		this.mockPkiProvider = createMock(PkiProvider.class);
		pkixContext.bind("test-pkix-provider", this.mockPkiProvider);

		this.testedInstance = new PkiProviderManagerBean();

		EJBTestUtils.init(this.testedInstance);
	}

	public void testFindTrustDomain() throws Exception {
		// setup
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		X509Certificate certificate = PkiTestUtils
				.generateSelfSignedCertificate(keyPair, "CN=Test");

		// stubs
		expect(this.mockPkiProvider.accept(certificate)).andStubReturn(true);
		expect(this.mockPkiProvider.getReference()).andStubReturn(
				this.mockPkiProvider);

		// prepare
		replay(this.mockPkiProvider);

		// operate
		PkiProvider resultPkiProvider = this.testedInstance
				.findPkiProvider(certificate);

		// verify
		verify(this.mockPkiProvider);
		assertEquals(this.mockPkiProvider, resultPkiProvider);
	}
}
