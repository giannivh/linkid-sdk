/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.applet;

import java.io.IOException;

import net.link.safeonline.shared.helpdesk.LogLevelType;

/**
 * Applet helpdesk interface. Some methods which send out helpdesk events to a
 * servlet specified in ServletPath from the applet runtime context. These are
 * picked up by the HelpdeskServlet and forwarded to the HelpdeskLogger.
 * 
 * @author wvdhaute
 * 
 */
public interface AppletHelpdesk {

	public boolean addHelpdeskEvent(String message, LogLevelType logLevel)
			throws IOException;

	public boolean clearHelpdesk() throws IOException;

	public Long persistHelpdesk() throws IOException;
}