/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.logging.exception;

/**
 * Thrown in case a webservice was not found.
 *
 * @author wvdhaute
 */
public class WSClientTransportException extends Exception {

    private String location;

    public WSClientTransportException(String location) {

        super( "Failed to contact webservice: " + location );

        this.location = location;
    }

    public WSClientTransportException(String location, Throwable cause) {

        super( "Failed to contact webservice: " + location, cause );

        this.location = location;
    }

    public String getLocation() {

        return location;
    }
}
