/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.service.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.authentication.exception.NodeMappingNotFoundException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.dao.NodeDAO;
import net.link.safeonline.dao.NodeMappingDAO;
import net.link.safeonline.entity.NodeEntity;
import net.link.safeonline.entity.NodeMappingEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.service.NodeMappingService;
import net.link.safeonline.service.SubjectService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;


/**
 * <h2>{@link NodeMappingServiceBean} - Service bean for node mappings.</h2>
 * 
 * <p>
 * <i>Aug 27, 2008</i>
 * </p>
 * 
 * @author wvdhaute
 */
@Stateless
@LocalBinding(jndiBinding = NodeMappingService.JNDI_BINDING)
public class NodeMappingServiceBean implements NodeMappingService {

    private final static Log LOG = LogFactory.getLog(NodeMappingServiceBean.class);

    @EJB(mappedName = NodeMappingDAO.JNDI_BINDING)
    private NodeMappingDAO   nodeMappingDAO;

    @EJB(mappedName = SubjectService.JNDI_BINDING)
    private SubjectService   subjectService;

    @EJB(mappedName = NodeDAO.JNDI_BINDING)
    private NodeDAO          nodeDAO;


    /**
     * {@inheritDoc}
     */
    public NodeMappingEntity getNodeMapping(String userId, String nodeName)
            throws SubjectNotFoundException, NodeNotFoundException {

        SubjectEntity subject = subjectService.getSubject(userId);
        NodeEntity node = nodeDAO.getNode(nodeName);

        NodeMappingEntity nodeMapping = nodeMappingDAO.findNodeMapping(subject, node);
        if (null == nodeMapping) {
            nodeMapping = nodeMappingDAO.addNodeMapping(subject, node);
        }
        return nodeMapping;
    }

    /**
     * {@inheritDoc}
     * 
     */
    public NodeMappingEntity getNodeMapping(String id)
            throws NodeMappingNotFoundException {

        LOG.debug("get node mapping: " + id);
        return nodeMappingDAO.getNodeMapping(id);
    }

    /**
     * {@inheritDoc}
     */
    public List<NodeMappingEntity> listNodeMappings(SubjectEntity subject) {

        return nodeMappingDAO.listNodeMappings(subject);
    }

}
