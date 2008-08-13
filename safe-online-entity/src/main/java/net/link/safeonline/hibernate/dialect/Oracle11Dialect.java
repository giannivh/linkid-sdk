/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.OracleDialect;


/**
 * Custom Oracle Dialect to resolve the issue with Double mapping to Oracle's Double precision
 * 
 * @author wvdhaute
 * 
 */
public class Oracle11Dialect extends OracleDialect {

    public Oracle11Dialect() {

        super();
        registerColumnType(Types.DOUBLE, "binary_double");
    }

}
