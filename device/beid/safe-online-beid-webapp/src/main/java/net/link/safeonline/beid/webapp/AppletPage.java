/*
 * SafeOnline project.
 *
 * Copyright 2006-2009 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.beid.webapp;

import java.applet.Applet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import net.link.safeonline.applet.AppletBase;
import net.link.safeonline.applet.AppletControl;
import net.link.safeonline.applet.RuntimeContext;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.device.sdk.auth.saml2.DeviceManager;
import net.link.safeonline.device.sdk.operation.saml2.DeviceOperationManager;
import net.link.safeonline.sc.pkcs11.auth.AuthenticationApplet;
import net.link.safeonline.webapp.template.TemplatePage;
import net.link.safeonline.wicket.tools.WicketUtil;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplate;


/**
 * <h2>{@link AppletPage}<br>
 * <sub>[in short] (TODO).</sub></h2>
 * 
 * <p>
 * [description / usage].
 * </p>
 * 
 * <p>
 * <i>Jan 5, 2009</i>
 * </p>
 * 
 * @author lhunath
 */
public abstract class AppletPage extends TemplatePage {

    public AppletPage(PageParameters parameters, final Class<? extends Applet> code, final String archive, final int width,
                      final int height, final String smartCardConfig, final String servletPath, final String targetPath,
                      final String helpdeskEventPath, final String helpPath, final String noPkcs11Path) {

        super(parameters);

        Label deployJavaAppletLabel = new Label("deployJavaApplet", new AbstractReadOnlyModel<String>() {

            private static final long serialVersionUID = 1L;


            @Override
            public String getObject() {

                ProtocolContext protocolContext = ProtocolContext.getProtocolContext(WicketUtil.getHttpSession());

                String operation = null;
                String language = getLocale().getLanguage();
                String javaVersion = isPkcs11(getPageParameters())? "1.5": "1.6";
                String sessionId = WicketUtil.getHttpSession().getId();
                String userId = protocolContext == null? null: protocolContext.getSubject();
                Object applicationId = WicketUtil.getHttpSession().getAttribute(DeviceManager.APPLICATION_ID_SESSION_ATTRIBUTE);
                try {
                    operation = DeviceOperationManager.getOperation(WicketUtil.getHttpSession());
                } catch (ServletException e) {
                    // No operation found.
                }

                Map<String, Object> variables = new HashMap<String, Object>();
                variables.put("name", code.getSimpleName());
                variables.put("code", code.getCanonicalName().concat(".class"));
                variables.put("archive", archive);
                variables.put("width", width);
                variables.put("height", height);
                variables.put(RuntimeContext.PARAM_SMARTCARD_CONFIG, smartCardConfig);
                variables.put(RuntimeContext.PARAM_SERVLET_PATH, servletPath);
                variables.put(AppletBase.PARAM_TARGET_PATH, targetPath);
                variables.put(AppletBase.PARAM_HELPDESK_EVENT_PATH, helpdeskEventPath);
                variables.put(AppletBase.PARAM_HELP_PATH, helpPath);
                variables.put(AppletControl.PARAM_NO_PCKS11_PATH, noPkcs11Path);
                variables.put(AuthenticationApplet.PARAM_SESSION_ID, sessionId);
                variables.put(AuthenticationApplet.PARAM_APPLICATION_ID, applicationId);
                variables.put("userId", userId);
                variables.put("operation", operation);
                variables.put(AppletBase.PARAM_LANGUAGE, language);
                variables.put("javaVersion", javaVersion);

                TextTemplate deployJavaApplet = new PackagedTextTemplate(getClass(), "deployJavaApplet.js");
                return deployJavaApplet.asString(variables);
            }
        });

        deployJavaAppletLabel.setEscapeModelStrings(false);
        getContent().add(deployJavaAppletLabel);

        getContent().add(new Link<String>("cancel") {

            private static final long serialVersionUID = 1L;

            {
                setVisible(isCancelVisible());
            }


            @Override
            public void onClick() {

                cancel();
            }
        });
    }

    protected static boolean isPkcs11(PageParameters parameters) {

        return "pkcs11".equalsIgnoreCase(parameters.getString(BeIdMountPoints.MountPoint.TYPE_PARAMETER));
    }

    /**
     * @return <code>true</code> to show the cancel button on this page.
     */
    protected abstract boolean isCancelVisible();

    /**
     * The cancel button was pressed.
     */
    protected abstract void cancel();
}
