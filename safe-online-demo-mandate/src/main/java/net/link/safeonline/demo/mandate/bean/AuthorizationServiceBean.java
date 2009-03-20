/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.mandate.bean;

import javax.ejb.Stateless;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import net.link.safeonline.demo.mandate.AuthorizationService;
import net.link.safeonline.demo.mandate.MandateConstants;
import net.link.safeonline.demo.mandate.entity.UserEntity;
import net.link.safeonline.demo.mandate.keystore.DemoMandateKeyStore;
import net.link.safeonline.model.demo.DemoConstants;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.AttributeUnavailableException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.OlasServiceFactory;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;


@Stateless
@LocalBinding(jndiBinding = AuthorizationService.JNDI_BINDING)
public class AuthorizationServiceBean implements AuthorizationService {

    private static final Log LOG = LogFactory.getLog(AuthorizationServiceBean.class);

    @PersistenceContext(unitName = MandateConstants.ENTITY_MANAGER_NAME)
    private EntityManager    entityManager;


    private String getUsername(String userId) {

        String username;
        AttributeClient attributeClient = getAttributeClient();
        try {
            username = attributeClient.getAttributeValue(userId, DemoConstants.DEMO_LOGIN_ATTRIBUTE_NAME, String.class);
        } catch (WSClientTransportException e) {
            LOG.debug("connection error: " + e.getMessage());
            return null;
        } catch (RequestDeniedException e) {
            LOG.debug("request denied");
            return null;
        } catch (AttributeNotFoundException e) {
            LOG.debug("login attribute not found");
            return null;
        } catch (AttributeUnavailableException e) {
            LOG.debug("login attribute unavailable");
            return null;
        }

        LOG.debug("username = " + username);
        return username;

    }

    private AttributeClient getAttributeClient() {

        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

        return OlasServiceFactory.getAttributeService(request, DemoMandateKeyStore.getPrivateKeyEntry());
    }

    public boolean isAdmin(String userId) {

        String username = getUsername(userId);
        LOG.debug("isAdmin: " + username);

        UserEntity user = entityManager.find(UserEntity.class, username);
        if (null == user)
            return false;

        return user.isAdmin();
    }

    public void bootstrap() {

        LOG.debug("bootstrapping...");
        UserEntity defaultAdminUser = entityManager.find(UserEntity.class, AuthorizationService.DEFAULT_ADMIN_USER);
        if (null == defaultAdminUser) {
            LOG.debug("adding default admin user: " + AuthorizationService.DEFAULT_ADMIN_USER);
            defaultAdminUser = new UserEntity(AuthorizationService.DEFAULT_ADMIN_USER);
            entityManager.persist(defaultAdminUser);
        }
        if (false == defaultAdminUser.isAdmin()) {
            LOG.debug("resetting default admin user to admin privilege: " + defaultAdminUser.getName());
            defaultAdminUser.setAdmin(true);
        }
    }
}
