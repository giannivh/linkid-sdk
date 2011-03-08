/*
 * SafeOnline project.
 *
 * Copyright 2006-2009 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.sdk.ws.util;

import java.lang.reflect.Field;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import net.link.safeonline.sdk.ws.LinkIDServiceFactory;
import net.link.safeonline.sdk.ws.ServiceFactory;
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
 * <h2>{@link DummyServiceFactory}</h2>
 *
 * <p> [description / usage]. </p>
 *
 * <p> <i>Mar 3, 2009</i> </p>
 *
 * @author lhunath
 */
public class DummyServiceFactory extends ServiceFactory {

    private static DummyServiceFactory instance;

    protected DummyServiceFactory() {

    }

    private static DummyServiceFactory getInstance() {

        if (instance == null)
            instance = new DummyServiceFactory();

        return instance;
    }

    public static void install()
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Field linkidInstance = LinkIDServiceFactory.class.getDeclaredField( "instance" );
        linkidInstance.setAccessible( true );
        linkidInstance.set( null, getInstance() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AttributeClient _getAttributeService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate, Long maxTimestampOffset,
                                                   X509Certificate sslCertificate) {

        return new DummyAttributeClient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataClient _getDataService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate, Long maxTimestampOffset,
                                         X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NameIdentifierMappingClient _getIdMappingService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                               Long maxTimestampOffset, X509Certificate sslCertificate) {

        return new DummyNameIdentifierMappingClient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotificationConsumerClient _getNotificationConsumerService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                                         Long maxTimestampOffset, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotificationProducerClient _getNotificationProducerService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                                         Long maxTimestampOffset, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotificationSubscriptionManagerClient _getNotificationSubscriptionService(PrivateKeyEntry privateKeyEntry,
                                                                                        X509Certificate serverCertificate, Long maxTimestampOffset,
                                                                                        X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SecurityTokenServiceClient _getStsService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate, Long maxTimestampOffset,
                                                        X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionTrackingClient _getSessionTrackingService(PrivateKeyEntry privateKeyEntry, X509Certificate serverCertificate,
                                                               Long maxTimestampOffset, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Xkms2Client _getXkms2Client(X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }
}
