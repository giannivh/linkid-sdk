/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.ticket.bean;

import java.net.ConnectException;
import java.security.PrivateKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import net.link.safeonline.demo.ticket.AbstractTicketDataClient;
import net.link.safeonline.demo.ticket.keystore.DemoTicketKeyStoreUtils;
import net.link.safeonline.model.demo.DemoConstants;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.attrib.AttributeClientImpl;
import net.link.safeonline.sdk.ws.data.DataClient;
import net.link.safeonline.sdk.ws.data.DataClientImpl;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.log.Log;

/**
 * Abstract class for data client beans. Inherit from this class if you need a
 * {@link DataClient} component.
 * 
 * @author wvdhaute
 * 
 */
public abstract class AbstractTicketDataClientBean implements
		AbstractTicketDataClient {

	@Logger
	private Log log;

	@In(create = true)
	FacesMessages facesMessages;

	private transient DataClient dataClient;

	private transient AttributeClient attributeClient;

	protected String wsHostName;
	protected String wsHostPort;

	protected String demoHostName;
	protected String demoHostPort;

	private X509Certificate certificate;

	private PrivateKey privateKey;

	@PostConstruct
	public void postConstructCallback() {
		log.debug("postConstruct");
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		this.demoHostName = externalContext.getInitParameter("DemoHostName");
		this.demoHostPort = externalContext.getInitParameter("DemoHostPort");
		this.wsHostName = externalContext.getInitParameter("WsHostName");
		this.wsHostPort = externalContext.getInitParameter("WsHostPort");
		PrivateKeyEntry privateKeyEntry = DemoTicketKeyStoreUtils
				.getPrivateKeyEntry();
		this.certificate = (X509Certificate) privateKeyEntry.getCertificate();
		this.privateKey = privateKeyEntry.getPrivateKey();
		postActivateCallback();
	}

	@PostActivate
	public void postActivateCallback() {
		log.debug("postActivate");
		this.dataClient = new DataClientImpl(this.wsHostName + ":"
				+ this.wsHostPort, this.certificate, this.privateKey);
		this.attributeClient = new AttributeClientImpl(this.wsHostName + ":"
				+ this.wsHostPort, this.certificate, this.privateKey);
	}

	@PrePassivate
	public void prePassivateCallback() {
		log.debug("prePassivate");
		this.dataClient = null;
		this.attributeClient = null;
	}

	@Remove
	@Destroy
	public void destroyCallback() {
		log.debug("destroy");
		this.dataClient = null;
		this.attributeClient = null;
		this.wsHostName = null;
		this.wsHostPort = null;
		this.certificate = null;
		this.privateKey = null;
	}

	protected DataClient getDataClient() {
		if (null == this.dataClient) {
			throw new EJBException("data client not yet initialized");
		}
		return this.dataClient;
	}

	protected AttributeClient getAttributeClient() {
		if (null == this.attributeClient) {
			throw new EJBException("attribute client not yet initialized");
		}
		return this.attributeClient;
	}

	/**
	 * Returns the username for this user Id. Sets {@link FacesMessages} in case
	 * something goes wrong.
	 * 
	 * @param userId
	 */
	protected String getUsername(String userId) {
		String username = null;
		AttributeClient attributeClient = getAttributeClient();
		try {
			username = attributeClient.getAttributeValue(userId,
					DemoConstants.DEMO_LOGIN_ATTRIBUTE_NAME, String.class);
		} catch (ConnectException e) {
			this.facesMessages.add("connection error: " + e.getMessage());
			return null;
		} catch (RequestDeniedException e) {
			this.facesMessages.add("request denied");
			return null;
		} catch (AttributeNotFoundException e) {
			this.facesMessages.add("login attribute not found");
			return null;
		}

		log.debug("username = " + username);
		return username;
	}
}