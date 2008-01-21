/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline;

/**
 * Defines various SafeOnline constants.
 * 
 * @author fcorneli
 * 
 */
public class SafeOnlineConstants {

	/**
	 * The SafeOnline JPA entity manager unit name.
	 */
	public static final String SAFE_ONLINE_ENTITY_MANAGER = "SafeOnlineEntityManager";

	/**
	 * The JBoss AS security domain for the SafeOnline components that need to
	 * be accessible by users.
	 */
	public static final String SAFE_ONLINE_SECURITY_DOMAIN = "safe-online";

	/**
	 * The JBoss AS security domain for the SafeOnline components that need to
	 * be accessible by applications.
	 */
	public static final String SAFE_ONLINE_APPLICATION_SECURITY_DOMAIN = "safe-online-application";

	/**
	 * The PKI trust domain name for the SafeOnline application owner
	 * applications.
	 */
	public static final String SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN = "applications";

	public static final String SAFE_ONLINE_USER_APPLICATION_NAME = "olas-user";

	public static final String SAFE_ONLINE_OPERATOR_APPLICATION_NAME = "olas-oper";

	public static final String SAFE_ONLINE_OWNER_APPLICATION_NAME = "olas-owner";

	public static final String SAFE_ONLINE_HELPDESK_APPLICATION_NAME = "olas-helpdesk";

	public static final String NAME_ATTRIBUTE = "urn:net:lin-k:safe-online:attribute:name";

	public static final String PASSWORD_HASH_ATTRIBUTE = "urn:net:lin-k:safe-online:attribute:password:hash";

	public static final String PASSWORD_SEED_ATTRIBUTE = "urn:net:lin-k:safe-online:attribute:password:seed";

	public static final String PASSWORD_ALGORITHM_ATTRIBUTE = "urn:net:lin-k:safe-online:attribute:password:algorithm";

	public static final String USERNAME_PASSWORD_AUTH_DEVICE = "password";

	/**
	 * The PKI trust domain name for the SafeOnline devices.
	 */
	public static final String SAFE_ONLINE_DEVICES_TRUST_DOMAIN = "devices";

	public static final String PASSWORD_DEVICE_CLASS = "Password";

	public static final String MOBILE_DEVICE_CLASS = "Mobile";

	public static final String PKI_DEVICE_CLASS = "PKI";

	/**
	 * The login attribute name and login service provider domain
	 */
	public static final String LOGIN_ATTRIBTUE = "urn:net:lin-k:safe-online:attribute:login";

	public static final String LOGIN_IDENTIFIER_DOMAIN = "login";

	/**
	 * Mobile device attributes
	 */
	public static final String WEAK_MOBILE_IDENTIFIER_DOMAIN = "weak-mobile";

	public static final String STRONG_MOBILE_IDENTIFIER_DOMAIN = "strong-mobile";

	public static final String MOBILE_ENCAP_ATTRIBUTE = "urn:net:lin-k:safe-online:attribute:mobile:encap";

	public static final String STRONG_MOBILE_ATTRIBUTE = "urn:net:lin-k:safe-online:attribute:mobile:strong";

	private SafeOnlineConstants() {
		// empty
	}
}
