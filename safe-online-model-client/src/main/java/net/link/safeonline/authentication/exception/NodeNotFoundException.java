/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.exception;

import javax.ejb.ApplicationException;


@ApplicationException(rollback = true)
public class NodeNotFoundException extends SafeOnlineException {

    private static final long serialVersionUID = 1L;


    public NodeNotFoundException() {

    }

    public NodeNotFoundException(String message) {

        super(message);
    }

    public NodeNotFoundException(String message, Throwable cause) {

        super(message, cause);
    }
}
