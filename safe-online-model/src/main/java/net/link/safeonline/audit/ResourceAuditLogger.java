/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.audit;

import javax.ejb.Local;

import net.link.safeonline.entity.audit.ResourceLevelType;
import net.link.safeonline.entity.audit.ResourceNameType;


@Local
public interface ResourceAuditLogger {

    void addResourceAudit(ResourceNameType resourceName, ResourceLevelType resourceLevel, String sourceComponent,
            String message);

}
