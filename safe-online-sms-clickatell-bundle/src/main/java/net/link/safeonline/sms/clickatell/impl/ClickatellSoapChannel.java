/*
 * SafeOnline project.
 * 
 * Copyright 2006-2009 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.sms.clickatell.impl;

import java.net.URL;
import java.rmi.RemoteException;

import net.link.safeonline.sms.clickatell.ClickatellChannel;
import net.link.safeonline.sms.clickatell.exception.ClickatellException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Service;

import com.clickatell.api.soap.webservice.PushServerWSBindingStub;


/**
 * <h2>{@link ClickatellSoapChannel}<br>
 * <sub>The SOAP implementation of Clickatell</sub></h2>
 * 
 * <p>
 * This class interacts with the Clickatell SOAP interfaces.
 * </p>
 * 
 * <p>
 * <i>Feb 20, 2009</i>
 * </p>
 * 
 * @author dhouthoo
 */
public class ClickatellSoapChannel implements ClickatellChannel {

    private URL    url;
    private int    apiId;
    private String username;
    private String password;


    public ClickatellSoapChannel(URL url, int apiId, String username, String password) {

        this.url = url;
        this.apiId = apiId;
        this.username = username;
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    public void send(String mobile, String message)
            throws ClickatellException {

        try {
            String[] to = { mobile };
            PushServerWSBindingStub stub = new PushServerWSBindingStub(url, new Service());
            String[] response = stub.sendmsg(apiId, username, password, to, message);
            if (response.length != 0)
                throw new ClickatellException("Error codes were received while calling the clickatell sms ws");
        } catch (AxisFault e) {
            throw new ClickatellException("Error while calling clickatell sms ws", e);
        } catch (RemoteException e) {
            throw new ClickatellException("Error while calling clickatell sms ws", e);
        }
    }
}