/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sdk.ws.ping;

import java.net.ConnectException;

import javax.xml.ws.BindingProvider;

import com.sun.xml.ws.client.ClientTransportException;

import net.lin_k.safe_online.ping.PingPort;
import net.lin_k.safe_online.ping.PingService;
import net.lin_k.safe_online.ping.Request;
import net.link.safeonline.ping.ws.PingServiceFactory;
import net.link.safeonline.sdk.ws.AbstractMessageAccessor;

public class PingClientImpl extends AbstractMessageAccessor implements
		PingClient {

	private final PingPort pingPort;

	/**
	 * Main constructor.
	 * 
	 * @param location
	 */
	public PingClientImpl(String location) {
		PingService pingService = PingServiceFactory.newInstance();
		this.pingPort = pingService.getPingPort();

		BindingProvider bindingProvider = (BindingProvider) this.pingPort;
		bindingProvider.getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				"http://" + location + "/safe-online-ws/ping");
	}

	public void ping() throws ConnectException {
		Request request = new Request();
		try {
			this.pingPort.pingOperation(request);
		} catch (ClientTransportException e) {
			throw new ConnectException(e.getMessage());
		}
	}
}