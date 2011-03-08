/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.idmapping.ws;

import static org.junit.Assert.assertNotNull;

import net.lin_k.safe_online.idmapping.NameIdentifierMappingService;
import net.link.safeonline.idmapping.ws.NameIdentifierMappingServiceFactory;
import org.junit.Test;


public class NameIdentifierMappingServiceFactoryTest {

    @Test
    public void testNewInstance()
            throws Exception {

        // Test
        NameIdentifierMappingService service = NameIdentifierMappingServiceFactory.newInstance();

        // Verify
        assertNotNull( service );
    }
}
