/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.prescription;

import javax.ejb.Local;


@Local
public interface PrescriptionLogon extends AbstractPrescriptionDataClient {

    /*
     * Actions.
     */
    String login();

    String logout();

    String getUsername();

    String activateAdminRole();

    String activateCareProviderRole();

    String activatePharmacistRole();
}
