/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.sms.ra.impl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAllocationException;
import javax.security.auth.Subject;

import net.link.safeonline.sms.ra.SMSManagedConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SMSManagedConnectionFactoryImpl implements SMSManagedConnectionFactory {

    private static final Log       LOG              = LogFactory.getLog(SMSManagedConnectionFactoryImpl.class);

    private static final long      serialVersionUID = 1L;

    private transient List<String> serialPorts;


    public SMSManagedConnectionFactoryImpl() {

        LOG.debug("Factory created");
        this.serialPorts = new ArrayList<String>();
    }

    public Object createConnectionFactory() {

        throw new UnsupportedOperationException("Cannot be used in unmanaged env");
    }

    public Object createConnectionFactory(ConnectionManager connectionManager) {

        LOG.debug("Creating a ConnectionFactory");
        return new SMSConnectionFactoryImpl(connectionManager, this);
    }

    public ManagedConnection createManagedConnection(@SuppressWarnings("unused") Subject arg0,
                                                     @SuppressWarnings("unused") ConnectionRequestInfo arg1) throws ResourceException {

        LOG.debug("Creating a managed connection");
        if (this.serialPorts.size() == 0) {
            LOG.debug("Failed");
            throw new ResourceAllocationException();
        }
        LOG.debug("Success");
        ManagedConnection managedConnection = new SMSManagedConnectionImpl(this.serialPorts.get(0));
        this.serialPorts.remove(0);
        return managedConnection;
    }

    public PrintWriter getLogWriter() {

        return null;
    }

    @SuppressWarnings("unused")
    public void setLogWriter(PrintWriter arg0) {

        // empty
    }

    @SuppressWarnings("unchecked")
    public ManagedConnection matchManagedConnections(Set set, @SuppressWarnings("unused") Subject subject,
                                                     @SuppressWarnings("unused") ConnectionRequestInfo connectionRequestInfo) {

        LOG.debug("Matching connections");
        return (ManagedConnection) set.iterator().next();
    }

    public void addSerialPort(String serialPort) {

        LOG.debug("Adding a serial port");
        this.serialPorts.add(serialPort);
    }

}
