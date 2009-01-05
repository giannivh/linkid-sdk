/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.wicket.web;

import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;


/**
 * <h2>{@link OLASSession}<br>
 * <sub>An abstract wicket session which allows the wicket tools backend to check whether the user was authenticated.</sub></h2>
 * 
 * <p>
 * Currently, you should use this {@link OLASSession} instead of wicket's {@link Session} so that the {@link WicketPage} can properly
 * identify whether the user is authenticated before rendering a page that requires him to be.
 * </p>
 * 
 * <p>
 * <i>Dec 31, 2008</i>
 * </p>
 * 
 * @author lhunath
 */
public abstract class OLASSession extends WebSession {

    private static final long serialVersionUID = 1L;
    private Page              postAuthenticationPage;


    // USER ---------------------------------------------------------

    public abstract String getUserOlasId();

    public abstract boolean isUserSet();

    // POST AUTHENTICATION REDIRECTION ------------------------------

    public Page getPostAuthenticationPage() {

        return postAuthenticationPage;
    }

    public void setPostAuthenticationPage(Page postAuthenticationPage) {

        this.postAuthenticationPage = postAuthenticationPage;
    }

    // GLOBAL -------------------------------------------------------

    public static OLASSession get() {

        return (OLASSession) Session.get();
    }

    public OLASSession(Request request) {

        super(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanupFeedbackMessages() {

    }
}