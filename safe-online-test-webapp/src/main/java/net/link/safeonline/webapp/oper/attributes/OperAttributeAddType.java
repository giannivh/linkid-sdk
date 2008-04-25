/*
 * SafeOnline project.
 * 
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.webapp.oper.attributes;

import net.link.safeonline.webapp.oper.OperTemplate;

public class OperAttributeAddType extends OperTemplate {

	public static final String PAGE_NAME = SAFE_ONLINE_OPER_WEBAPP_PREFIX
			+ "/attributes/attribute-add-type.seam";

	public OperAttributeAddType() {
		super(PAGE_NAME);
	}

	public OperAttributeAdd previous() {
		clickButtonAndWait("previous");
		return new OperAttributeAdd();
	}

	public OperAttributes cancel() {
		clickButtonAndWait("cancel");
		return new OperAttributes();
	}

	public OperAttributeAddAc next() {
		clickButtonAndWait("next");
		return new OperAttributeAddAc();
	}
}