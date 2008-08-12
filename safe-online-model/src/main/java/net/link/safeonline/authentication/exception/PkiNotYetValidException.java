/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.exception;

import javax.ejb.ApplicationException;

import net.link.safeonline.shared.SharedConstants;

@ApplicationException(rollback = true)
public class PkiNotYetValidException extends SafeOnlineException {

    private static final long serialVersionUID = 1L;


    public PkiNotYetValidException() {

        super(null, SharedConstants.PKI_NOT_YET_VALID_ERROR);
    }

}
