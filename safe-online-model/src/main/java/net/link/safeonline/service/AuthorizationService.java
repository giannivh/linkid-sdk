package net.link.safeonline.service;

import java.util.Set;

import javax.ejb.Local;

/**
 * Authorization service interface. This component is used by the SafeOnline
 * core JAAS login module to assign roles to an authenticated user.
 * 
 * @author fcorneli
 * 
 */
@Local
public interface AuthorizationService {

	/**
	 * Gives back a set of roles for a given login. The assignment of these
	 * roles to a certain principal depends on the security measures against
	 * attacks of the SafeOnline core.
	 * 
	 * @param login
	 * @return
	 */
	Set<String> getRoles(String login);
}
