/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.demo.wicket.tools;

import org.apache.wicket.AbstractRestartResponseException;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;


/**
 * <h2>{@link RedirectResponseException}<br>
 * <sub>[in short] (TODO).</sub></h2>
 * 
 * <p>
 * [description / usage].
 * </p>
 * 
 * <p>
 * <i>Nov 18, 2008</i>
 * </p>
 * 
 * @author lhunath
 */
public class RedirectResponseException extends AbstractRestartResponseException {

    private static final long serialVersionUID = 1L;


    public RedirectResponseException(IRequestTarget target) {

        RequestCycle.get().setRequestTarget(target);
    }
}
