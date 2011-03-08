/*
 * SafeOnline project.
 *
 * Copyright 2006-2009 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.sdk.ws;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.data.DataClient;
import net.link.safeonline.sdk.ws.idmapping.NameIdentifierMappingClient;
import net.link.safeonline.sdk.ws.notification.consumer.NotificationConsumerClient;
import net.link.safeonline.sdk.ws.notification.producer.NotificationProducerClient;
import net.link.safeonline.sdk.ws.notification.subscription.NotificationSubscriptionManagerClient;
import net.link.safeonline.sdk.ws.session.SessionTrackingClient;
import net.link.safeonline.sdk.ws.sts.SecurityTokenServiceClient;
import net.link.safeonline.sdk.ws.xkms2.Xkms2Client;


/**
 * <h2>{@link ServiceFactory}</h2>
 *
 * <p> [description / usage]. </p>
 *
 * <p> <i>Jan 15, 2009</i> </p>
 *
 * @author lhunath
 */
public abstract class ServiceFactory {

    protected abstract AttributeClient _getAttributeService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                            Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract DataClient _getDataService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                  Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract NameIdentifierMappingClient _getIdMappingService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                                        Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract SecurityTokenServiceClient _getStsService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                                 Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract NotificationConsumerClient _getNotificationConsumerService(PrivateKeyEntry privateKeyEntry,
                                                                                  X509Certificate serverCertificate,
                                                                                  Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract NotificationProducerClient _getNotificationProducerService(PrivateKeyEntry privateKeyEntry,
                                                                                  X509Certificate serverCertificate,
                                                                                  Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract NotificationSubscriptionManagerClient _getNotificationSubscriptionService(PrivateKeyEntry privateKeyEntry,
                                                                                                 X509Certificate serverCertificate,
                                                                                                 Long maxTimestampOffset,
                                                                                                 X509Certificate sslCertificate);

    protected abstract SessionTrackingClient _getSessionTrackingService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                                        Long maxTimestampOffset, X509Certificate sslCertificate);

    protected abstract Xkms2Client _getXkms2Client(X509Certificate sslCertificate);
}
