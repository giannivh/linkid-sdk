/*
 * SafeOnline project.
 * 
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.device.sdk.exception;

public class DeviceInitializationException extends Exception {

    private static final long serialVersionUID = 1L;

    private String            message;


    public DeviceInitializationException(String message) {

        this.message = message;
    }

    @Override
    public String getMessage() {

        return this.message;
    }

}
