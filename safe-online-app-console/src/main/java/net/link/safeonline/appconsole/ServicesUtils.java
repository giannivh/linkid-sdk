/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.appconsole;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ExecutionException;

import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.exception.SubjectNotFoundException;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.attrib.AttributeClientImpl;
import net.link.safeonline.sdk.ws.data.DataClient;
import net.link.safeonline.sdk.ws.data.DataClientImpl;
import net.link.safeonline.sdk.ws.idmapping.NameIdentifierMappingClient;
import net.link.safeonline.sdk.ws.idmapping.NameIdentifierMappingClientImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingworker.SwingWorker;

/**
 * SafeOnline services util class
 * 
 * <p>
 * Used to access to the attribute and data SafeOnline web services
 * </p>
 * 
 * @author wvdhaute
 * 
 */
public class ServicesUtils extends Observable {

	static final Log LOG = LogFactory.getLog(ServicesUtils.class);

	private static ServicesUtils servicesUtilsInstance = null;

	private ApplicationConsoleManager consoleManager = ApplicationConsoleManager
			.getInstance();

	private NameIdentifierMappingClient nameClient = null;
	private AttributeClient attributeClient = null;
	private DataClient dataClient = null;

	private ServicesUtils() {
	}

	public static ServicesUtils getInstance() {
		if (null == servicesUtilsInstance)
			servicesUtilsInstance = new ServicesUtils();
		return servicesUtilsInstance;
	}

	/*
	 * 
	 * Name Identifier web service methods
	 * 
	 */
	NameIdentifierMappingClient getNameIdentifierMappingClient() {
		if (null == this.nameClient)
			this.nameClient = new NameIdentifierMappingClientImpl(
					this.consoleManager.getLocation(),
					(X509Certificate) this.consoleManager.getIdentity()
							.getCertificate(), this.consoleManager
							.getIdentity().getPrivateKey());
		return this.nameClient;
	}

	public String getUserId(String username) throws SubjectNotFoundException,
			RequestDeniedException {
		return getNameIdentifierMappingClient().getUserId(username);
	}

	/*
	 * 
	 * Attribute web service methods
	 * 
	 */
	AttributeClient getAttributeClient() {
		if (null == this.attributeClient)
			this.attributeClient = new AttributeClientImpl(this.consoleManager
					.getLocation(), (X509Certificate) this.consoleManager
					.getIdentity().getCertificate(), this.consoleManager
					.getIdentity().getPrivateKey());

		this.consoleManager.setMessageAccessor(this.attributeClient);
		return this.attributeClient;
	}

	public void getAttributes(final String user) {
		SwingWorker<Map<String, Object>, Object> worker = new SwingWorker<Map<String, Object>, Object>() {

			@Override
			protected Map<String, Object> doInBackground() throws Exception {
				Map<String, Object> attributes = null;
				attributes = getAttributeClient().getAttributeValues(
						getUserId(user));
				return attributes;
			}

			@SuppressWarnings("synthetic-access")
			@Override
			protected void done() {
				String errorMessage = null;
				setChanged();
				try {
					notifyObservers(get());
				} catch (InterruptedException e) {
					errorMessage = "Retrieving attributes interrupted ...";
					LOG.error(errorMessage, e);
				} catch (ExecutionException e) {
					errorMessage = "Retrieving attributes failed to execute ...";
					LOG.error(errorMessage, e);
				}
				if (null != errorMessage) {
					setChanged();
					notifyObservers(errorMessage);
				}
			}

		};
		worker.execute();
	}

	/*
	 * 
	 * Data web service methods
	 * 
	 */
	DataClient getDataClient() {
		if (null == this.dataClient)
			this.dataClient = new DataClientImpl(this.consoleManager
					.getLocation(), (X509Certificate) this.consoleManager
					.getIdentity().getCertificate(), this.consoleManager
					.getIdentity().getPrivateKey());

		this.consoleManager.setMessageAccessor(this.dataClient);
		return this.dataClient;
	}

	public void setAttributeValue(final String userName,
			final String attributeName, final Object attributeValue) {
		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {

			@Override
			protected Boolean doInBackground() throws Exception {

				getDataClient().setAttributeValue(getUserId(userName),
						attributeName, attributeValue);
				return Boolean.TRUE;
			}

			@SuppressWarnings("synthetic-access")
			@Override
			protected void done() {
				String errorMessage = null;
				setChanged();
				try {
					notifyObservers(get());
				} catch (InterruptedException e) {
					errorMessage = "Set attribute interrupted ...";
					LOG.error(errorMessage, e);
				} catch (ExecutionException e) {
					errorMessage = "Set attribute failed to execute ...";
					e.printStackTrace();
					LOG.error(errorMessage, e);
				}
				if (null != errorMessage) {
					setChanged();
					notifyObservers(errorMessage);
				}
			}
		};
		worker.execute();
	}

	public void removeAttribute(final String userName,
			final String attributeName, final String attributeId) {
		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				getDataClient().removeAttribute(getUserId(userName),
						attributeName, attributeId);
				return Boolean.TRUE;
			}

			@SuppressWarnings("synthetic-access")
			@Override
			protected void done() {
				String errorMessage = null;
				setChanged();
				try {
					notifyObservers(get());
				} catch (InterruptedException e) {
					errorMessage = "Remove attribute interrupted ...";
					LOG.error(errorMessage, e);
				} catch (ExecutionException e) {
					errorMessage = "Remove attribute failed to execute ...";
					e.printStackTrace();
					LOG.error(errorMessage, e);
				}
				if (null != errorMessage) {
					setChanged();
					notifyObservers(errorMessage);
				}

			}
		};
		worker.execute();
	}

	public void createAttribute(final String userName,
			final String attributeName, final Object attributeValue) {
		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				getDataClient().createAttribute(getUserId(userName),
						attributeName, attributeValue);
				return Boolean.TRUE;
			}

			@SuppressWarnings("synthetic-access")
			@Override
			protected void done() {
				String errorMessage = null;
				setChanged();
				try {
					notifyObservers(get());
				} catch (InterruptedException e) {
					errorMessage = "Create attribute interrupted ...";
					LOG.error(errorMessage, e);
				} catch (ExecutionException e) {
					errorMessage = "Create attribute failed to execute ...";
					e.printStackTrace();
					LOG.error(errorMessage, e);
				}
				if (null != errorMessage) {
					setChanged();
					notifyObservers(errorMessage);
				}

			}
		};
		worker.execute();
	}

}
