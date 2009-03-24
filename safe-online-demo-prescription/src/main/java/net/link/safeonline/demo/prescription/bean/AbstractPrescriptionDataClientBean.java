/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.prescription.bean;

import javax.annotation.PostConstruct;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;

import net.link.safeonline.demo.prescription.AbstractPrescriptionDataClient;
import net.link.safeonline.demo.prescription.keystore.DemoPrescriptionKeyStore;
import net.link.safeonline.model.demo.DemoConstants;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.AttributeUnavailableException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.OlasServiceFactory;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.data.DataClient;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;
import net.link.safeonline.sdk.ws.idmapping.NameIdentifierMappingClient;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;


/**
 * Abstract class for data client beans. Inherit from this class if you need a {@link DataClient} component.
 * 
 * @author fcorneli
 * 
 */
public abstract class AbstractPrescriptionDataClientBean implements AbstractPrescriptionDataClient {

    @Logger
    private Log   log;

    @In(create = true)
    FacesMessages facesMessages;


    /**
     * {@inheritDoc}
     */
    @PostConstruct
    public void postConstructCallback() {

    }

    /**
     * {@inheritDoc}
     */
    @PostActivate
    public void postActivateCallback() {

    }

    /**
     * {@inheritDoc}
     */
    @PrePassivate
    public void prePassivateCallback() {

    }

    /**
     * {@inheritDoc}
     */
    @Remove
    @Destroy
    public void destroyCallback() {

    }

    protected DataClient getDataClient() {

        return OlasServiceFactory.getDataService(DemoPrescriptionKeyStore.getPrivateKeyEntry());
    }

    protected AttributeClient getAttributeClient() {

        return OlasServiceFactory.getAttributeService(DemoPrescriptionKeyStore.getPrivateKeyEntry());
    }

    protected NameIdentifierMappingClient getNameIdentifierMappingClient() {

        return OlasServiceFactory.getIdMappingService(DemoPrescriptionKeyStore.getPrivateKeyEntry());
    }

    /**
     * Returns the username for this user Id. Sets {@link FacesMessages} in case something goes wrong.
     * 
     * @param userId
     */
    protected String getUsername(String userId) {

        String username = null;
        AttributeClient tempAttributeClient = getAttributeClient();
        try {
            username = tempAttributeClient.getAttributeValue(userId, DemoConstants.DEMO_LOGIN_ATTRIBUTE_NAME, String.class);
        } catch (WSClientTransportException e) {
            facesMessages.add("connection error: " + e.getMessage());
            return null;
        } catch (RequestDeniedException e) {
            facesMessages.add("request denied");
            return null;
        } catch (AttributeNotFoundException e) {
            facesMessages.add("login attribute not found");
            return null;
        } catch (AttributeUnavailableException e) {
            facesMessages.add("login attribute unavailable");
            return null;
        }

        log.debug("username = " + username);
        return username;
    }
}
