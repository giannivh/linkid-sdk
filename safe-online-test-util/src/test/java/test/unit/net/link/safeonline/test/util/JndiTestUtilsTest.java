/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.test.util;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import junit.framework.TestCase;
import net.link.safeonline.test.util.JndiTestUtils;


/**
 * Who will guard the guards.
 *
 * @author fcorneli
 *
 */
public class JndiTestUtilsTest extends TestCase {

    public void testBindComponent() throws Exception {

        // setup
        Object testComponent = new Object();
        String testJndiName = "test/jndi/name";

        // operate
        JndiTestUtils testedInstance = new JndiTestUtils();
        testedInstance.setUp();
        testedInstance.bindComponent(testJndiName, testComponent);

        // verify
        InitialContext initialContext = new InitialContext();
        Object result = initialContext.lookup(testJndiName);
        assertEquals(testComponent, result);

        // operate & verify
        testedInstance.tearDown();
        try {
            initialContext.lookup(testJndiName);
            fail();
        } catch (NameNotFoundException e) {
            // expected
        }
    }

    public void testBindComponentWithSimpleName() throws Exception {

        // setup
        Object testComponent = new Object();
        String testSimpleJndiName = "simpleName";

        // operate
        JndiTestUtils testedInstance = new JndiTestUtils();
        testedInstance.setUp();
        testedInstance.bindComponent(testSimpleJndiName, testComponent);

        // verify
        InitialContext initialContext = new InitialContext();
        Object result = initialContext.lookup(testSimpleJndiName);
        assertEquals(testComponent, result);

        // operate & verify
        testedInstance.tearDown();
        try {
            initialContext.lookup(testSimpleJndiName);
            fail();
        } catch (NameNotFoundException e) {
            // expected
        }
    }
}
