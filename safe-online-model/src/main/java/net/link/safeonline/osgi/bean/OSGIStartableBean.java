/*
 * SafeOnline project.
 * 
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.osgi.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.naming.NamingException;

import net.link.safeonline.Startable;
import net.link.safeonline.audit.AuditContextManager;
import net.link.safeonline.audit.ResourceAuditLoggerInterceptor;
import net.link.safeonline.authentication.exception.SafeOnlineResourceException;
import net.link.safeonline.entity.audit.ResourceLevelType;
import net.link.safeonline.entity.audit.ResourceNameType;
import net.link.safeonline.osgi.OSGIHostActivator;
import net.link.safeonline.osgi.OSGIStartable;
import net.link.safeonline.osgi.plugin.PluginAttributeService;
import net.link.safeonline.util.ee.EjbUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.main.AutoActivator;
import org.jboss.annotation.ejb.LocalBinding;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


/**
 * <h2>{@link OSGIStartableBean}<br>
 * <sub>Startable bean for initialization of the embedded Felix OSGI instance.</sub></h2>
 * 
 * <p>
 * This startable starts the embedded Felix OSGI instance.
 * 
 * It also auto starts the Felix FileInstall bundle. This bundle will monitor $JBOSS_HOME/osgi/plugins directory.
 * </p>
 * 
 * <p>
 * <i>Aug 18, 2008</i>
 * </p>
 * 
 * @author wvdhaute
 */
@Stateless
@LocalBinding(jndiBinding = OSGIStartable.JNDI_BINDING)
@Interceptors( { AuditContextManager.class, ResourceAuditLoggerInterceptor.class })
public class OSGIStartableBean implements OSGIStartable {

    private static final Log LOG = LogFactory.getLog(OSGIStartableBean.class);


    /**
     * {@inheritDoc}
     */
    public int getPriority() {

        return Startable.PRIORITY_BOOTSTRAP;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void postStart() {

        LOG.debug("Initializing Felix OSGI container");

        // Fetch jboss dir
        String jbossHome = System.getenv("JBOSS_HOME");
        LOG.debug("JBOSS_HOME = " + jbossHome);

        // Create a case-insensitive configuration property map.
        Map configMap = new StringMap(false);

        // Configure the Felix instance to be embedded.
        configMap.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");

        // Add the attribute service interface package and the core OSGi
        // packages to be exported from the class path via the system bundle.
        String systemPackages = "org.osgi.framework; version=1.3.0, " + "org.osgi.service.packageadmin; version=1.2.0, "
                + "org.osgi.service.startlevel; version=1.0.0, " + "org.osgi.service.url; version=1.0.0, "
                + "org.osgi.util.tracker; version=1.3.1, ";

        // javax.*
        systemPackages += "javax; version=1.0.0, ";
        systemPackages += "javax.accessibility ; version=1.0.0, ";
        systemPackages += "javax.activity ; version=1.0.0, ";
        systemPackages += "javax.crypto ; version=1.0.0, ";
        systemPackages += "javax.crypto.interfaces ; version=1.0.0, ";
        systemPackages += "javax.crypto.spec ; version=1.0.0, ";
        systemPackages += "javax.management ; version=1.0.0, ";
        systemPackages += "javax.management.loading ; version=1.0.0, ";
        systemPackages += "javax.management.modelmbean ; version=1.0.0, ";
        systemPackages += "javax.management.monitor ; version=1.0.0, ";
        systemPackages += "javax.management.openmbean ; version=1.0.0, ";
        systemPackages += "javax.management.relation ; version=1.0.0, ";
        systemPackages += "javax.management.remote ; version=1.0.0, ";
        systemPackages += "javax.management.remote.rmi ; version=1.0.0, ";
        systemPackages += "javax.management.timer ; version=1.0.0, ";
        systemPackages += "javax.naming ; version=1.0.0, ";
        systemPackages += "javax.naming.directory ; version=1.0.0, ";
        systemPackages += "javax.naming.event ; version=1.0.0, ";
        systemPackages += "javax.naming.ldap ; version=1.0.0, ";
        systemPackages += "javax.naming.spi ; version=1.0.0, ";
        systemPackages += "javax.net ; version=1.0.0, ";
        systemPackages += "javax.net.ssl ; version=1.0.0, ";
        systemPackages += "javax.print ; version=1.0.0, ";
        systemPackages += "javax.print.attribute ; version=1.0.0, ";
        systemPackages += "javax.print.attribute.standard ; version=1.0.0, ";
        systemPackages += "javax.print.event ; version=1.0.0, ";
        systemPackages += "javax.rmi ; version=1.0.0, ";
        systemPackages += "javax.rmi.CORBA ; version=1.0.0, ";
        systemPackages += "javax.rmi.ssl ; version=1.0.0, ";
        systemPackages += "javax.security.auth ; version=1.0.0, ";
        systemPackages += "javax.security.auth.callback ; version=1.0.0, ";
        systemPackages += "javax.security.auth.kerberos ; version=1.0.0, ";
        systemPackages += "javax.security.auth.login ; version=1.0.0, ";
        systemPackages += "javax.security.auth.spi ; version=1.0.0, ";
        systemPackages += "javax.security.auth.x500 ; version=1.0.0, ";
        systemPackages += "javax.security.cert ; version=1.0.0, ";
        systemPackages += "javax.security.sasl ; version=1.0.0, ";
        systemPackages += "javax.sql ; version=1.0.0, ";
        systemPackages += "javax.sql.rowset ; version=1.0.0, ";
        systemPackages += "javax.sql.rowset.serial ; version=1.0.0, ";
        systemPackages += "javax.sql.rowset.spi ; version=1.0.0, ";
        systemPackages += "javax.transaction ; version=1.0.0, ";
        systemPackages += "javax.transaction.xa ; version=1.0.0, ";
        systemPackages += "javax.xml ; version=1.0.0, ";
        systemPackages += "javax.xml.datatype; version=1.0.0, ";
        systemPackages += "javax.xml.namespace ; version=1.0.0, ";
        systemPackages += "javax.xml.parsers ; version=1.0.0, ";
        systemPackages += "javax.xml.transform ; version=1.0.0, ";
        systemPackages += "javax.xml.transform.dom ; version=1.0.0, ";
        systemPackages += "javax.xml.transform.sax ; version=1.0.0, ";
        systemPackages += "javax.xml.transform.stream ; version=1.0.0, ";
        systemPackages += "javax.xml.validation ; version=1.0.0, ";
        systemPackages += "javax.xml.xpath ; version=1.0.0, ";

        // org.*
        systemPackages += "org.w3c.dom ; version=1.0.0, ";
        systemPackages += "org.w3c.dom.bootstrap ; version=1.0.0, ";
        systemPackages += "org.w3c.dom.events ; version=1.0.0, ";
        systemPackages += "org.w3c.dom.ls ; version=1.0.0, ";
        systemPackages += "org.xml.sax ; version=1.0.0, ";
        systemPackages += "org.xml.sax.ext ; version=1.0.0, ";
        systemPackages += "org.xml.sax.helpers ; version=1.0.0, ";

        // jsse.jar packages
        systemPackages += "sun; version=1.0.0, ";
        systemPackages += "sun.net; version=1.0.0, ";
        systemPackages += "sun.net.www; version=1.0.0, ";
        systemPackages += "sun.net.www.protocol; version=1.0.0, ";
        systemPackages += "sun.net.www.protocol.https; version=1.0.0, ";
        systemPackages += "com; version=1.0.0, ";
        systemPackages += "com.sun; version=1.0.0, ";
        systemPackages += "com.sun.net; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl.internal; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl.internal.www; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl.internal.www.protocol; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl.internal.www.protocol.https; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl.internal.ssl; version=1.0.0, ";
        systemPackages += "com.sun.net.ssl.internal.pkcs12; version=1.0.0, ";
        systemPackages += "com.sun.security; version=1.0.0, ";
        systemPackages += "com.sun.security.cert; version=1.0.0, ";
        systemPackages += "com.sun.security.cert.internal; version=1.0.0, ";
        systemPackages += "com.sun.security.cert.internal.x509; version=1.0.0, ";

        // plugin service package
        systemPackages += "net.link.safeonline.osgi.plugin; version=1.0.0";
        LOG.debug("systemPackages: " + systemPackages);

        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackages);

        // Autostart the fileinstall bundle, configured with the path to drop our plugin bundles into
        configMap.put(AutoActivator.AUTO_START_PROP + ".1", "file://" + jbossHome + "/osgi/autostart/org.apache.felix.fileinstall.jar");
        configMap.put("felix.fileinstall.dir", jbossHome + "/osgi/plugins");

        // Explicitly specify the directory to use for caching bundles.
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, jbossHome + "/osgi/cache");

        // Add the AutoActivator to the list of initial bundle activators.
        // Add our OSGI Host bundle activator that provides the connection from jboss to osgi and back.
        OSGIHostActivator hostActivator = new OSGIHostActivator();
        List<BundleActivator> baseBundles = new ArrayList<BundleActivator>();
        baseBundles.add(new AutoActivator(configMap));
        baseBundles.add(hostActivator);

        // Bind OSGI Host activator to JNDI
        try {
            EjbUtils.bindComponent(OSGIHostActivator.JNDI_BINDING, hostActivator);
        } catch (NamingException e) {
            throw new EJBException("Unable to bind OSGI Host activator to the JNDI tree: " + OSGIHostActivator.JNDI_BINDING);
        }

        // Now create an instance of the framework.
        Felix felix = new Felix(configMap, baseBundles);

        // Now start Felix instance.
        try {
            LOG.debug("Starting Felix OSGI container");
            felix.start();
        } catch (BundleException e) {
            throw new EJBException("Unable to start Felix: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void preStop() {

        LOG.debug("preStop");

    }

    public Object[] getPluginServices() {

        OSGIHostActivator hostActivator;
        try {
            hostActivator = (OSGIHostActivator) EjbUtils.getComponent(OSGIHostActivator.JNDI_BINDING);
        } catch (NamingException e) {
            throw new EJBException("Unable to find OSGI Host activator in the JNDI tree: " + OSGIHostActivator.JNDI_BINDING);
        }
        return hostActivator.getPluginServices();

    }

    public PluginAttributeService getPluginService(String serviceName)
            throws SafeOnlineResourceException {

        Object[] services = getPluginServices();
        if (null == services)
            throw new SafeOnlineResourceException(ResourceNameType.OSGI, ResourceLevelType.RESOURCE_UNAVAILABLE, serviceName);

        for (Object service : services) {
            if (service.getClass().getName().equals(serviceName))
                return (PluginAttributeService) service;
        }
        throw new SafeOnlineResourceException(ResourceNameType.OSGI, ResourceLevelType.RESOURCE_UNAVAILABLE, serviceName);
    }
}
