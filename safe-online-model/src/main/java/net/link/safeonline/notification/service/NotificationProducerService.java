/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.notification.service;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import net.link.safeonline.authentication.exception.EndpointReferenceNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubscriptionNotFoundException;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.notification.exception.MessageHandlerNotFoundException;


@Local
public interface NotificationProducerService {

    public void sendNotification(String topic, List<String> message) throws SubscriptionNotFoundException,
            MessageHandlerNotFoundException;

    public void subscribe(String topic, String address, X509Certificate certificate) throws PermissionDeniedException;

    public void unsubscribe(String topic, String address, X509Certificate certificate)
            throws SubscriptionNotFoundException, PermissionDeniedException, EndpointReferenceNotFoundException;

    public void subscribe(String topic, String address, DeviceEntity device);

    public void subscribe(String topic, String address, ApplicationEntity application);

    public void unsubscribe(String topic, String address, DeviceEntity device) throws SubscriptionNotFoundException,
            EndpointReferenceNotFoundException;

    public void unsubscribe(String topic, String address, ApplicationEntity application)
            throws SubscriptionNotFoundException, EndpointReferenceNotFoundException;

}
