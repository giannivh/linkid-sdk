/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.api.exception;

/**
 * Thrown in case the attribute was not found.
 *
 * @author fcorneli
 */
public class InvalidPaymentContextException extends Exception {

    public InvalidPaymentContextException(final String message) {

        super( message );
    }
}
