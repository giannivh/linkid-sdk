/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.user.webapp;

import net.link.safeonline.user.webapp.UserApplication;
import net.link.safeonline.wicket.test.TestStringResourceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class UserTestApplication extends UserApplication {

    static final Log LOG = LogFactory.getLog(UserTestApplication.class);


    @Override
    protected void init() {

        super.init();

        getResourceSettings().addStringResourceLoader(new TestStringResourceLoader());

    }
}