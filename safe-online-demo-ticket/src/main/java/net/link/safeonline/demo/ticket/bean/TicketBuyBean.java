/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.ticket.bean;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.link.safeonline.demo.ticket.TicketBuy;
import net.link.safeonline.demo.ticket.entity.Ticket;
import net.link.safeonline.demo.ticket.entity.User;
import net.link.safeonline.demo.ticket.entity.Ticket.Site;
import net.link.safeonline.model.demo.DemoConstants;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;
import net.link.safeonline.util.servlet.SafeOnlineConfig;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.joda.time.DateTime;
import org.joda.time.Period;


@Stateful
@Name("ticketBuy")
@Scope(ScopeType.CONVERSATION)
@LocalBinding(jndiBinding = TicketBuy.JNDI_BINDING)
@SecurityDomain("demo-ticket")
public class TicketBuyBean extends AbstractTicketDataClientBean implements TicketBuy {

    @Logger
    private Log            log;

    @Resource
    private SessionContext sessionContext;

    @PersistenceContext(unitName = "DemoTicketEntityManager")
    private EntityManager  entityManager;

    @SuppressWarnings("unused")
    @Out(required = false)
    private double         ticketPrice;

    @SuppressWarnings("unused")
    @Out(required = false)
    private Date           startDate;

    @SuppressWarnings("unused")
    @Out(required = false)
    private Date           endDate;


    public enum TicketPeriod {
        DAY("one day", Period.days(1)),
        WEEK("one week", Period.weeks(1)),
        MONTH("one month", Period.months(1));

        private final String name;

        private final Period period;


        TicketPeriod(String name, Period period) {

            this.name = name;
            this.period = period;
        }

        public String getName() {

            return name;
        }

        public Date getEndDate(Date beginDate) {

            DateTime begin = new DateTime(beginDate);
            DateTime endDate = begin.plus(period);
            return endDate.toDate();
        }
    }


    private String  from;

    private String  to;

    private String  validUntil;

    private boolean returnTicket;

    private String  nrn;

    @SuppressWarnings("unused")
    @Out(required = false)
    private int     juniorReduction;


    public String getFrom() {

        return from;
    }

    public void setFrom(String from) {

        this.from = from;
    }

    public String getTo() {

        return to;
    }

    public void setTo(String to) {

        this.to = to;
    }

    public String getValidUntil() {

        return validUntil;
    }

    public void setValidUntil(String validUntil) {

        this.validUntil = validUntil;
    }

    public boolean getReturnTicket() {

        return returnTicket;
    }

    public void setReturnTicket(boolean returnTicket) {

        this.returnTicket = returnTicket;
    }

    private String getUsername() {

        String username = getUsername(getUserId());
        log.debug("username #0", username);
        return username;
    }

    private String getUserId() {

        Principal principal = sessionContext.getCallerPrincipal();
        return principal.getName();
    }

    @Factory("siteList")
    public List<SelectItem> siteListFactory() {

        List<SelectItem> result = new ArrayList<SelectItem>();
        for (Ticket.Site site : Ticket.Site.values()) {
            result.add(new SelectItem(site.toString(), site.getName()));
        }
        return result;
    }

    @Factory("dateList")
    public List<SelectItem> dateListFactory() {

        List<SelectItem> result = new ArrayList<SelectItem>();
        for (TicketPeriod period : TicketPeriod.values()) {
            result.add(new SelectItem(period.toString(), period.getName()));
        }
        return result;
    }

    @RolesAllowed("user")
    public String checkOut() {

        ticketPrice = 100;
        juniorReduction = 0;
        String userId = getUserId();
        try {
            nrn = getAttributeClient().getAttributeValue(userId, "urn:net:lin-k:safe-online:attribute:beid:nrn", String[].class)[0];
            Boolean juniorValue = getAttributeClient()
                                                      .getAttributeValue(userId, DemoConstants.PAYMENT_JUNIOR_ATTRIBUTE_NAME, Boolean.class);
            if (juniorValue != null && juniorValue.booleanValue() == true) {
                juniorReduction = 10;
            }
        } catch (AttributeNotFoundException e) {
            String msg = "attribute not found: " + e.getMessage();
            log.debug(msg);
            facesMessages.add(msg);
            return null;
        } catch (RequestDeniedException e) {
            String msg = "request denied";
            log.debug(msg);
            facesMessages.add(msg);
            return null;
        } catch (WSClientTransportException e) {
            String msg = "Connection error. Check your SSL setup.";
            log.debug(msg);
            facesMessages.add(msg);
            return null;
        } catch (Exception e) {
            String msg = "Error occurred: " + e.getMessage();
            log.debug(msg, e);
            log.debug("exception type: " + e.getClass().getName());
            facesMessages.add(msg);
            return null;
        }
        TicketPeriod valid = TicketPeriod.valueOf(validUntil);
        startDate = new Date();
        endDate = valid.getEndDate(startDate);
        return "checkout";
    }

    @RolesAllowed("user")
    @End
    // conversation begin via pages.xml
    public String confirm() {

        User user = entityManager.find(User.class, getUserId());
        if (user == null) {
            user = new User(getUserId(), this.getUsername(), nrn);
            entityManager.persist(user);
        }
        user.setNrn(nrn);
        Ticket ticket = new Ticket(user, Site.valueOf(from), Site.valueOf(to), startDate, endDate, returnTicket);
        user.getTickets().add(ticket);
        entityManager.persist(ticket);

        redirectToPaymentService(ticket);

        return "list";
    }

    private void redirectToPaymentService(Ticket ticket) {

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        SafeOnlineConfig safeOnlineConfig = SafeOnlineConfig.load(request);

        String user = getUsername();
        String recipient = "De Lijn";
        String message = "Ticket " + ticket.getId();
        String target = safeOnlineConfig.absoluteUrlFromPath(request, "list.seam");
        target = response.encodeRedirectURL(target);

        String redirectUrl;
        try {
            redirectUrl = safeOnlineConfig.absoluteUrlFromPath(request, "../entry.seam?user=" + URLEncoder.encode(user, "UTF-8")
                    + "&recipient=" + URLEncoder.encode(recipient, "UTF-8") + "&amount="
                    + URLEncoder.encode(Double.toString(ticketPrice - juniorReduction), "UTF-8") + "&message="
                    + URLEncoder.encode(message, "UTF-8") + "&target=" + URLEncoder.encode(target, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            String msg = "URL encoding error";
            log.debug(msg);
            facesMessages.add(msg);
            return;
        }

        try {
            externalContext.redirect(redirectUrl);
        } catch (IOException e) {
            String msg = "IO redirect error";
            log.debug(msg);
            facesMessages.add(msg);
            return;
        }
    }
}
