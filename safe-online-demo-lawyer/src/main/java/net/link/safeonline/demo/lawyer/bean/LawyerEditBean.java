/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.demo.lawyer.bean;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;

import net.link.safeonline.demo.lawyer.LawyerConstants;
import net.link.safeonline.demo.lawyer.LawyerEdit;
import net.link.safeonline.demo.lawyer.LawyerStatus;
import net.link.safeonline.model.demo.DemoConstants;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.exception.SubjectNotFoundException;
import net.link.safeonline.sdk.ws.data.DataClient;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.log.Log;


@Stateful
@Name("lawyerEdit")
@LocalBinding(jndiBinding = "SafeOnlineLawyerDemo/LawyerEditBean/local")
@SecurityDomain(LawyerConstants.SECURITY_DOMAIN)
public class LawyerEditBean extends AbstractLawyerDataClientBean implements LawyerEdit {

    @Logger
    private Log          log;

    @In("name")
    @Out("name")
    private String       name;

    @SuppressWarnings("unused")
    @In("lawyerEditableStatus")
    @Out("lawyerEditableStatus")
    private LawyerStatus lawyerStatus;


    @RolesAllowed(LawyerConstants.ADMIN_ROLE)
    public String persist() {

        this.log.debug("---------------------------------------- save #0 -----------------------------", this.name);

        try {
            createOrUpdateAttribute(DemoConstants.LAWYER_ATTRIBUTE_NAME, Boolean.valueOf(this.lawyerStatus.isLawyer()));
            createOrUpdateAttribute(DemoConstants.LAWYER_SUSPENDED_ATTRIBUTE_NAME, Boolean.valueOf(this.lawyerStatus
                    .isSuspended()));
            createOrUpdateAttribute(DemoConstants.LAWYER_BAR_ATTRIBUTE_NAME, this.lawyerStatus.getBar());
            createOrUpdateAttribute(DemoConstants.LAWYER_BAR_ADMIN_ATTRIBUTE_NAME, Boolean.valueOf(this.lawyerStatus
                    .isBarAdmin()));
        } catch (WSClientTransportException e) {
            this.facesMessages.add("connection error");
            return null;
        } catch (RequestDeniedException e) {
            this.facesMessages.add("request denied");
            return null;
        } catch (SubjectNotFoundException e) {
            this.facesMessages.add("subject not found: " + this.name);
            return null;
        } catch (AttributeNotFoundException e) {
            this.facesMessages.add("attribute not found");
            return null;
        }
        return "success";
    }

    private void createOrUpdateAttribute(String attributeName, Object attributeValue)
            throws WSClientTransportException, RequestDeniedException, SubjectNotFoundException,
            AttributeNotFoundException {

        String userId = getNameIdentifierMappingClient().getUserId(this.name);

        DataClient dataClient = getDataClient();
        if (null == dataClient.getAttributeValue(userId, attributeName, attributeValue.getClass())) {
            this.log.debug("create attribute #0 for #1", attributeName, this.name);
            dataClient.createAttribute(userId, attributeName, attributeValue);
        } else {
            this.log.debug("set attribute #0 for #1", attributeName, this.name);
            dataClient.setAttributeValue(userId, attributeName, attributeValue);
        }
    }
}
