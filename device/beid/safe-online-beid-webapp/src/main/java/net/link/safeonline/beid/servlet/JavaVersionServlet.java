/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.beid.servlet;

import static net.link.safeonline.beid.webapp.BeIdMountPoints.ErrorType.BAD_JAVA_VERSION;
import static net.link.safeonline.beid.webapp.BeIdMountPoints.ErrorType.BAD_PLATFORM;
import static net.link.safeonline.beid.webapp.BeIdMountPoints.ErrorType.NO_JAVA;
import static net.link.safeonline.beid.webapp.BeIdMountPoints.ErrorType.PROTOCOL_VIOLATION;
import static net.link.safeonline.beid.webapp.BeIdMountPoints.MountPoint.ERROR;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.model.node.util.AbstractNodeInjectionServlet;
import net.link.safeonline.util.servlet.annotation.Out;
import net.link.safeonline.util.servlet.annotation.RequestParameter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Servlet that receives the java version data from the JavaVersionApplet applet and processes it. Depending on target session attributes
 * being set this servlet will redirect to different locations.
 * 
 * @author fcorneli
 * 
 */
public class JavaVersionServlet extends AbstractNodeInjectionServlet {

    private static final long   serialVersionUID                = 1L;

    private static final Log    LOG                             = LogFactory.getLog(JavaVersionServlet.class);

    private static final String TARGET15_SESSION_ATTRIBUTE      = JavaVersionServlet.class.getName() + ".target15";

    private static final String TARGET16_SESSION_ATTRIBUTE      = JavaVersionServlet.class.getName() + ".target16";

    private static final String PKCS11_TARGET_SESSION_ATTRIBUTE = JavaVersionServlet.class.getName() + ".pkcs11target";

    public static final String  JAVA_VERSION_REG_EXPR           = "^1\\.(5|6).*";

    private static final String JAVA_1_5_VERSION_REG_EXPR       = "^1\\.5.*";


    /**
     * Sets the target in case no PKCS#11 drivers were detected but Java 1.5 runtime is present.
     * 
     * @param target
     * @param session
     */
    public static void setJava15NoPkcs11Target(String target, HttpSession session) {

        session.setAttribute(TARGET15_SESSION_ATTRIBUTE, target);
    }

    /**
     * Sets the target in case no PKCS#11 drivers were detected but Java 1.6 runtime is present.
     * 
     * @param target
     * @param session
     */
    public static void setJava16NoPkcs11Target(String target, HttpSession session) {

        session.setAttribute(TARGET16_SESSION_ATTRIBUTE, target);
    }

    /**
     * Sets the target in case PKCS#11 drivers were detected.
     * 
     * @param target
     * @param session
     */
    public static void setPkcs11Target(String target, HttpSession session) {

        session.setAttribute(PKCS11_TARGET_SESSION_ATTRIBUTE, target);
    }


    @RequestParameter("appName")
    private String   appName;

    @RequestParameter("appVersion")
    private String   appVersion;

    @RequestParameter("appMinorVersion")
    private String   appMinorVersion;

    @RequestParameter("appCodeName")
    private String   appCodeName;

    @RequestParameter("platform")
    private String   platformRequestParameter;

    @RequestParameter("userAgent")
    private String   userAgent;

    @RequestParameter("vendor")
    private String   vendor;

    @RequestParameter("cpuClass")
    private String   cpuClass;

    @RequestParameter("javaEnabled")
    private String   javaEnabled;

    @RequestParameter("javaVersion")
    private String   javaVersion;

    @RequestParameter("javaVendor")
    private String   javaVendor;

    @RequestParameter("hasPkcs11")
    private String   hasPkcs11;

    @SuppressWarnings("unused")
    @Out("platform")
    private PLATFORM platform;


    public static enum PLATFORM {
        WINDOWS,
        LINUX,
        MAC,
        UNSUPPORTED
    }

    public static enum JAVA_VERSION {
        JAVA_1_5,
        JAVA_1_6
    }


    public static final String JAVA_VERSION_NAME = "javaVersion";

    @SuppressWarnings("unused")
    @Out(JAVA_VERSION_NAME)
    private JAVA_VERSION       sessionJavaVersion;


    @Override
    protected void invokeGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        invoke(request, response);
    }

    @Override
    protected void invokePost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        invoke(request, response);
    }

    private void invoke(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        LOG.debug("doPost");
        LOG.debug("platform: " + platformRequestParameter);
        LOG.debug("java enabled: " + javaEnabled);
        LOG.debug("java version: " + javaVersion);
        LOG.debug("java vendor: " + javaVendor);
        LOG.debug("cpu class: " + cpuClass);
        LOG.debug("user agent: " + userAgent);
        LOG.debug("vendor: " + vendor);
        LOG.debug("app name: " + appName);
        LOG.debug("app version: " + appVersion);
        LOG.debug("app minor version: " + appMinorVersion);
        LOG.debug("app code name: " + appCodeName);
        LOG.debug("has PKCS11: " + hasPkcs11);

        boolean checkPlatform = checkPlatform();
        boolean checkJavaEnabled = checkJavaEnabled();
        boolean checkJavaVersion = checkJavaVersion();

        if (false == checkPlatform) {
            response.sendRedirect(ERROR.linkFor(BAD_PLATFORM));
            return;
        }
        if (false == checkJavaEnabled) {
            response.sendRedirect(ERROR.linkFor(NO_JAVA));
            return;
        }
        if (false == checkJavaVersion) {
            response.sendRedirect(ERROR.linkFor(BAD_JAVA_VERSION));
            return;
        }

        HttpSession session = request.getSession();
        if (Boolean.parseBoolean(hasPkcs11)) {
            String pkcs11target = (String) session.getAttribute(PKCS11_TARGET_SESSION_ATTRIBUTE);
            if (null != pkcs11target) {
                LOG.debug("redirect to target: " + pkcs11target);
                response.sendRedirect(pkcs11target);
                return;
            }
        }

        /*
         * Else no PKCS#11 driver available.
         */

        switch (sessionJavaVersion) {
            case JAVA_1_5:
                String target15 = (String) session.getAttribute(TARGET15_SESSION_ATTRIBUTE);
                if (null != target15) {
                    LOG.debug("redirecting to target: " + target15);
                    response.sendRedirect(target15);
                    return;
                }
            break;
            case JAVA_1_6:
                String target16 = (String) session.getAttribute(TARGET16_SESSION_ATTRIBUTE);
                if (null != target16) {
                    LOG.debug("redirecting to target: " + target16);
                    response.sendRedirect(target16);
                    return;
                }
            break;
            default:
        }

        LOG.error("Java Version Servlet has no redirection target set.");
        response.sendRedirect(ERROR.linkFor(PROTOCOL_VIOLATION));
    }

    private boolean checkJavaVersion()
            throws ServletException {

        if (null == javaVersion)
            throw new ServletException("javaVersion request parameter is required");
        boolean result = Pattern.matches(JAVA_VERSION_REG_EXPR, javaVersion);
        LOG.debug("java version check result: " + result);
        boolean java15 = Pattern.matches(JAVA_1_5_VERSION_REG_EXPR, javaVersion);
        if (java15) {
            sessionJavaVersion = JAVA_VERSION.JAVA_1_5;
        } else {
            sessionJavaVersion = JAVA_VERSION.JAVA_1_6;
        }
        return result;
    }

    private boolean checkJavaEnabled()
            throws ServletException {

        if (null == javaEnabled)
            throw new ServletException("javaEnabled request parameter required");
        if (false == Boolean.TRUE.toString().equals(javaEnabled))
            return false;
        return true;
    }

    private boolean checkPlatform()
            throws ServletException {

        if (null == platformRequestParameter)
            throw new ServletException("platform request parameter required");
        String platformStr = platformRequestParameter.toLowerCase();
        if (platformStr.indexOf("win") != -1) {
            platform = PLATFORM.WINDOWS;
        } else if (platformStr.indexOf("linux") != -1) {
            platform = PLATFORM.LINUX;
        } else if (platformStr.indexOf("mac") != -1) {
            platform = PLATFORM.MAC;
        } else {
            platform = PLATFORM.UNSUPPORTED;
            return false;
        }
        return true;
    }
}