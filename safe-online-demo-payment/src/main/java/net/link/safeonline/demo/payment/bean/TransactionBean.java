/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.payment.bean;

import java.security.Principal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.link.safeonline.demo.payment.PaymentConstants;
import net.link.safeonline.demo.payment.Transaction;
import net.link.safeonline.demo.payment.entity.PaymentEntity;
import net.link.safeonline.demo.payment.entity.UserEntity;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.AttributeUnavailableException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;

import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;


@Stateful
@Name("transactionBean")
@Scope(ScopeType.CONVERSATION)
@LocalBinding(jndiBinding = Transaction.JNDI_BINDING)
@SecurityDomain("demo-payment")
public class TransactionBean extends AbstractPaymentDataClientBean implements Transaction {

    private static final org.apache.commons.logging.Log LOG              = LogFactory.getLog(TransactionBean.class);

    @Logger
    private Log                                         log;

    @Resource
    private SessionContext                              sessionContext;

    @PersistenceContext(unitName = "DemoPaymentEntityManager")
    private EntityManager                               entityManager;

    public static final String                          NEW_PAYMENT_NAME = "newPayment";

    @In(value = NEW_PAYMENT_NAME, required = false)
    private PaymentEntity                               newPayment;


    private String getUserId() {

        Principal principal = sessionContext.getCallerPrincipal();
        return principal.getName();
    }

    private String getUsername() {

        String username = getUsername(getUserId());
        log.debug("username #0", username);
        return username;
    }

    @RolesAllowed(PaymentConstants.AUTHENTICATED_ROLE)
    public String confirm() {

        log.debug("confirm");
        LOG.debug("confirm");
        UserEntity user = entityManager.find(UserEntity.class, getUserId());
        if (user == null) {
            user = new UserEntity(getUserId(), getUsername());
            entityManager.persist(user);
        }

        Date paymentDate = new Date();
        newPayment.setPaymentDate(paymentDate);
        newPayment.setOwner(user);

        entityManager.persist(newPayment);

        return "confirmed";
    }

    @Factory(NEW_PAYMENT_NAME)
    @RolesAllowed(PaymentConstants.AUTHENTICATED_ROLE)
    public PaymentEntity newPaymentEntityFactory() {

        return new PaymentEntity();
    }

    @Factory("visas")
    @RolesAllowed(PaymentConstants.AUTHENTICATED_ROLE)
    public List<SelectItem> visasFactory() {

        log.debug("visas factory");
        String userId = getUserId();
        String[] values;
        try {
            values = getAttributeClient().getAttributeValue(userId, "urn:net:lin-k:safe-online:attribute:visaCardNumber", String[].class);
        } catch (AttributeNotFoundException e) {
            String msg = "attribute not found: " + e.getMessage();
            log.debug(msg);
            facesMessages.add(msg);
            return new LinkedList<SelectItem>();
        } catch (RequestDeniedException e) {
            String msg = "request denied";
            log.debug(msg);
            facesMessages.add(msg);
            return new LinkedList<SelectItem>();
        } catch (WSClientTransportException e) {
            String msg = "Connection error. Check your SSL setup.";
            log.debug(msg);
            facesMessages.add(msg);
            return new LinkedList<SelectItem>();
        } catch (AttributeUnavailableException e) {
            String msg = "Visa Attribute unavailable error.";
            log.debug(msg);
            facesMessages.add(msg);
            return new LinkedList<SelectItem>();
        }
        List<SelectItem> visas = new LinkedList<SelectItem>();
        for (Object value : values) {
            visas.add(new SelectItem(value));
        }
        return visas;
    }
}
