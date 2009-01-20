/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service.bean;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.SafeOnlineNodeRoles;
import net.link.safeonline.audit.AccessAuditLogger;
import net.link.safeonline.audit.AuditContextManager;
import net.link.safeonline.authentication.service.NodeAccountService;
import net.link.safeonline.authentication.service.NodeAccountServiceRemote;
import net.link.safeonline.dao.ApplicationScopeIdDAO;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.HistoryDAO;
import net.link.safeonline.dao.NodeMappingDAO;
import net.link.safeonline.dao.SubjectDAO;
import net.link.safeonline.dao.SubjectIdentifierDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.entity.SubjectEntity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.RemoteBinding;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.security.SecurityDomain;


@Stateless
@SecurityDomain(SafeOnlineConstants.SAFE_ONLINE_NODE_SECURITY_DOMAIN)
@LocalBinding(jndiBinding = NodeAccountService.JNDI_BINDING)
@RemoteBinding(jndiBinding = NodeAccountServiceRemote.JNDI_BINDING)
@Interceptors( { AuditContextManager.class, AccessAuditLogger.class })
public class NodeAccountServiceBean implements NodeAccountService, NodeAccountServiceRemote {

    private static final Log      LOG = LogFactory.getLog(NodeAccountServiceBean.class);

    @EJB(mappedName = HistoryDAO.JNDI_BINDING)
    private HistoryDAO            historyDAO;

    @EJB(mappedName = AttributeDAO.JNDI_BINDING)
    private AttributeDAO          attributeDAO;

    @EJB(mappedName = SubscriptionDAO.JNDI_BINDING)
    private SubscriptionDAO       subscriptionDAO;

    @EJB(mappedName = ApplicationScopeIdDAO.JNDI_BINDING)
    private ApplicationScopeIdDAO applicationScopeIdDAO;

    @EJB(mappedName = SubjectDAO.JNDI_BINDING)
    private SubjectDAO            subjectDAO;

    @EJB(mappedName = SubjectIdentifierDAO.JNDI_BINDING)
    private SubjectIdentifierDAO  subjectIdentifierDAO;

    @EJB(mappedName = NodeMappingDAO.JNDI_BINDING)
    private NodeMappingDAO        nodeMappingDAO;


    @RolesAllowed(SafeOnlineNodeRoles.NODE_ROLE)
    public void removeAccount(SubjectEntity subject) {

        LOG.debug("remove account: " + subject.getUserId());

        historyDAO.clearAllHistory(subject);
        subscriptionDAO.removeAllSubscriptions(subject);
        applicationScopeIdDAO.removeApplicationScopeIds(subject);
        attributeDAO.removeAttributes(subject);
        subjectIdentifierDAO.removeSubjectIdentifiers(subject);
        nodeMappingDAO.removeNodeMappings(subject);
        subjectDAO.removeSubject(subject);
    }
}
