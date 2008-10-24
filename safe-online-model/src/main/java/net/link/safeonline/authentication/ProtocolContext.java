/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication;

import java.util.Set;

import net.link.safeonline.entity.DeviceEntity;


/**
 * Protocol Context class. Protocol Context objects should be generated by protocol handlers after they have successfully processed an
 * authentication request.
 * 
 * @author fcorneli
 * 
 */
public class ProtocolContext {

    private final String            applicationId;

    private final String            applicationFriendlyName;

    private final String            target;

    private final String            language;

    private final Set<DeviceEntity> requiredDevices;


    /**
     * Main constructor.
     * 
     * @param applicationId
     *            the application Id of the application that the authentication protocol handler has determined that issued the
     *            authentication request.
     * @param applicationFriendlyName
     *            the application friendly name, can be used by the remote device issuer to display what the user is authenticating for
     * @param target
     *            the target URL to which to send the authentication response.
     * @param language
     *            the (optional) language to be used by the authentication webapp
     * @param requiredDevices
     *            the optional set of required devices for this authentication session.
     */
    public ProtocolContext(String applicationId, String applicationFriendlyName, String target, String language,
                           Set<DeviceEntity> requiredDevices) {

        this.applicationId = applicationId;
        this.applicationFriendlyName = applicationFriendlyName;
        this.target = target;
        this.language = language;
        this.requiredDevices = requiredDevices;
    }

    public String getApplicationId() {

        return this.applicationId;
    }

    public String getApplicationFriendlyName() {

        return this.applicationFriendlyName;
    }

    public String getTarget() {

        return this.target;
    }

    public Set<DeviceEntity> getRequiredDevices() {

        return this.requiredDevices;
    }

    public String getLanguage() {

        return this.language;
    }
}
