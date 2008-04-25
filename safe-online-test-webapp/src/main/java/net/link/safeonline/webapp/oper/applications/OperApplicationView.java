/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.webapp.oper.applications;

import net.link.safeonline.webapp.oper.OperTemplate;

public class OperApplicationView extends OperTemplate {

	public static final String PAGE_NAME = SAFE_ONLINE_OPER_WEBAPP_PREFIX
			+ "/applications/application-view.seam";

	public OperApplicationView() {
		super(PAGE_NAME);
	}

	public OperApplications back() {
		clickButtonAndWait("back");
		return new OperApplications();
	}

	public OperApplicationEdit edit() {
		clickButtonAndWait("edit");
		return new OperApplicationEdit();
	}
}