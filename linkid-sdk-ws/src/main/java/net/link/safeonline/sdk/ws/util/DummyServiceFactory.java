/*
 * SafeOnline project.
 *
 * Copyright 2006-2009 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.sdk.ws.util;

import java.lang.reflect.Field;
import java.security.cert.X509Certificate;
import net.link.safeonline.sdk.api.ws.attrib.client.AttributeClient;
import net.link.safeonline.sdk.api.ws.data.client.DataClient;
import net.link.safeonline.sdk.api.ws.idmapping.client.NameIdentifierMappingClient;
import net.link.safeonline.sdk.api.ws.notification.consumer.client.NotificationConsumerClient;
import net.link.safeonline.sdk.api.ws.notification.producer.client.NotificationProducerClient;
import net.link.safeonline.sdk.api.ws.notification.subscription.client.NotificationSubscriptionManagerClient;
import net.link.safeonline.sdk.api.ws.payment.PaymentServiceClient;
import net.link.safeonline.sdk.api.ws.session.client.SessionTrackingClient;
import net.link.safeonline.sdk.api.ws.sts.client.SecurityTokenServiceClient;
import net.link.safeonline.sdk.api.ws.xkms2.client.Xkms2Client;
import net.link.safeonline.sdk.ws.LinkIDServiceFactory;
import net.link.safeonline.sdk.ws.ServiceFactory;
import net.link.util.ws.security.WSSecurityConfiguration;


/**
 * <h2>{@link DummyServiceFactory}</h2>
 * <p/>
 * <p> [description / usage]. </p>
 * <p/>
 * <p> <i>Mar 3, 2009</i> </p>
 *
 * @author lhunath
 */
public class DummyServiceFactory extends ServiceFactory {

    private static final DummyServiceFactory instance = new DummyServiceFactory();

    protected DummyServiceFactory() {

    }

    private static DummyServiceFactory getInstance() {

        return instance;
    }

    public static void install()
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Field linkidInstance = LinkIDServiceFactory.class.getDeclaredField( "instance" );
        linkidInstance.setAccessible( true );
        linkidInstance.set( null, getInstance() );
    }

    @Override
    protected AttributeClient _getAttributeService(final WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        return new DummyAttributeClient();
    }

    @Override
    protected DataClient _getDataService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected NameIdentifierMappingClient _getIdMappingService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        return new DummyNameIdentifierMappingClient();
    }

    @Override
    protected NotificationConsumerClient _getNotificationConsumerService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected NotificationProducerClient _getNotificationProducerService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected NotificationSubscriptionManagerClient _getNotificationSubscriptionService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected SecurityTokenServiceClient _getStsService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected SessionTrackingClient _getSessionTrackingService(WSSecurityConfiguration configuration, X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected PaymentServiceClient _getPaymentService(final X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }

    @Override
    protected Xkms2Client _getXkms2Client(X509Certificate sslCertificate) {

        throw new UnsupportedOperationException( "Not yet implemented" );
    }
}
