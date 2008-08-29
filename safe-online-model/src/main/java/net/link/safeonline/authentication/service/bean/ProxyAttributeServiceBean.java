package net.link.safeonline.authentication.service.bean;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import net.link.safeonline.audit.ResourceAuditLogger;
import net.link.safeonline.audit.SecurityAuditLogger;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.AttributeUnavailableException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SafeOnlineResourceException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.ProxyAttributeService;
import net.link.safeonline.authentication.service.ProxyAttributeServiceRemote;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.CompoundedAttributeTypeMemberEntity;
import net.link.safeonline.entity.DatatypeType;
import net.link.safeonline.entity.NodeMappingEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.audit.ResourceLevelType;
import net.link.safeonline.entity.audit.ResourceNameType;
import net.link.safeonline.entity.audit.SecurityThreatType;
import net.link.safeonline.osgi.OSGIStartable;
import net.link.safeonline.osgi.plugin.Attribute;
import net.link.safeonline.osgi.plugin.PluginAttributeService;
import net.link.safeonline.osgi.plugin.exception.AttributeNotFoundException;
import net.link.safeonline.osgi.plugin.exception.InvalidDataException;
import net.link.safeonline.osgi.plugin.exception.UnsupportedDataTypeException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.attrib.AttributeClientImpl;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;
import net.link.safeonline.service.NodeMappingService;
import net.link.safeonline.service.SubjectService;
import net.link.safeonline.util.ee.AuthIdentityServiceClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@Stateless
public class ProxyAttributeServiceBean implements ProxyAttributeService, ProxyAttributeServiceRemote {

    private static final Log    LOG = LogFactory.getLog(ProxyAttributeServiceBean.class);

    @EJB
    private AttributeTypeDAO    attributeTypeDAO;

    @EJB
    private AttributeDAO        attributeDAO;

    @EJB
    private SubjectService      subjectService;

    @EJB
    private OSGIStartable       osgiStartable;

    @EJB
    private ResourceAuditLogger resourceAuditLogger;

    @EJB
    private SecurityAuditLogger securityAuditLogger;

    @EJB
    private NodeMappingService  nodeMappingService;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Object findAttributeValue(String userId, String attributeName) throws PermissionDeniedException,
            AttributeTypeNotFoundException, AttributeUnavailableException, SubjectNotFoundException {

        LOG.debug("find attribute " + attributeName + " for " + userId);

        AttributeTypeEntity attributeType = this.attributeTypeDAO.getAttributeType(attributeName);

        SubjectEntity subject = this.subjectService.getSubject(userId);

        if (attributeType.isExternal())
            return findExternalAttributeValue(subject, attributeType);

        if (attributeType.isLocal())
            return findLocalAttribute(subject, attributeType);

        try {
            return findRemoteAttribute(subject, attributeType);
        } catch (SubjectNotFoundException e) {
            LOG.debug("subject not found");
            return null;
        } catch (NodeNotFoundException e) {
            String message = "node " + attributeType.getName() + " not found attribute " + attributeName;
            this.securityAuditLogger.addSecurityAudit(SecurityThreatType.DECEPTION, message);
            throw new PermissionDeniedException(message);
        }
    }

    /**
     * Find an external attribute value using OSGi plugin specified in the attribute type.
     * 
     * @throws AttributeUnavailableException
     * @throws AttributeTypeNotFoundException
     * @throws PermissionDeniedException
     * @throws SubjectNotFoundException
     */
    private Object findExternalAttributeValue(SubjectEntity subject, AttributeTypeEntity attributeType)
            throws AttributeUnavailableException, AttributeTypeNotFoundException, PermissionDeniedException,
            SubjectNotFoundException {

        LOG.debug("find external attribute " + attributeType.getName() + " for " + subject.getUserId());
        try {
            PluginAttributeService pluginAttributeService = this.osgiStartable.getPluginService(attributeType
                    .getPluginName());
            List<Attribute> attributeView = pluginAttributeService.getAttribute(subject.getUserId(), attributeType
                    .getName(), attributeType.getPluginConfiguration());
            return convertAttribute(attributeView, attributeType);
        } catch (UnsupportedDataTypeException e) {
            throw new PermissionDeniedException("Unsupported data type");
        } catch (AttributeNotFoundException e) {
            LOG.debug("external attribute " + attributeType.getName() + " not found for " + subject.getUserId()
                    + " ( plugin=" + attributeType.getPluginName() + " )");
            return null;
        } catch (net.link.safeonline.osgi.plugin.exception.AttributeTypeNotFoundException e) {
            throw new AttributeTypeNotFoundException();
        } catch (InvalidDataException e) {
            throw new PermissionDeniedException("Invalid data");
        } catch (SafeOnlineResourceException e) {
            throw new AttributeUnavailableException();
        } catch (net.link.safeonline.osgi.plugin.exception.AttributeUnavailableException e) {
            throw new AttributeUnavailableException();
        } catch (net.link.safeonline.osgi.plugin.exception.SubjectNotFoundException e) {
            throw new SubjectNotFoundException();
        }
    }

    /**
     * Convert attribute view as used by the OSGi plugins
     * 
     * @throws InvalidDataException
     * @throws UnsupportedDataTypeException
     */
    @SuppressWarnings("unchecked")
    private Object convertAttribute(List<Attribute> attributeView, AttributeTypeEntity attributeType)
            throws InvalidDataException, UnsupportedDataTypeException {

        if (null == attributeView || attributeView.isEmpty())
            return null;

        Attribute parent = attributeView.get(0);
        net.link.safeonline.osgi.plugin.DatatypeType datatype = parent.getType();
        switch (datatype) {
            case STRING: {
                if (attributeView.size() == 1)
                    return parent.getStringValue();
                String[] values = new String[attributeView.size()];
                for (int idx = 0; idx < values.length; idx++) {
                    Attribute attribute = attributeView.get(idx);
                    if (!attribute.getType().equals(datatype)) {
                        throw new InvalidDataException("datatype " + attribute.getType()
                                + " not matching expected datatype " + datatype);
                    }
                    if (!attribute.getName().equals(attributeType.getName())) {
                        throw new InvalidDataException("attribute name " + attribute.getName()
                                + " not matching expected " + attributeType.getName());
                    }
                    values[idx] = attributeView.get(idx).getStringValue();
                }
                return values;
            }
            case BOOLEAN: {
                if (attributeView.size() == 1)
                    return parent.getBooleanValue();
                Boolean[] values = new Boolean[attributeView.size()];
                for (int idx = 0; idx < values.length; idx++) {
                    Attribute attribute = attributeView.get(idx);
                    if (!attribute.getType().equals(datatype)) {
                        throw new InvalidDataException("datatype " + attribute.getType()
                                + " not matching expected datatype " + datatype);
                    }
                    if (!attribute.getName().equals(attributeType.getName())) {
                        throw new InvalidDataException("attribute name " + attribute.getName()
                                + " not matching expected " + attributeType.getName());
                    }
                    values[idx] = attributeView.get(idx).getBooleanValue();
                }
                return values;
            }
            case INTEGER: {
                if (attributeView.size() == 1)
                    return parent.getIntegerValue();
                Integer[] values = new Integer[attributeView.size()];
                for (int idx = 0; idx < values.length; idx++) {
                    Attribute attribute = attributeView.get(idx);
                    if (!attribute.getType().equals(datatype)) {
                        throw new InvalidDataException("datatype " + attribute.getType()
                                + " not matching expected datatype " + datatype);
                    }
                    if (!attribute.getName().equals(attributeType.getName())) {
                        throw new InvalidDataException("attribute name " + attribute.getName()
                                + " not matching expected " + attributeType.getName());
                    }
                    values[idx] = attributeView.get(idx).getIntegerValue();
                }
                return values;
            }
            case DOUBLE: {
                if (attributeView.size() == 1)
                    return parent.getDoubleValue();
                Double[] values = new Double[attributeView.size()];
                for (int idx = 0; idx < values.length; idx++) {
                    Attribute attribute = attributeView.get(idx);
                    if (!attribute.getType().equals(datatype)) {
                        throw new InvalidDataException("datatype " + attribute.getType()
                                + " not matching expected datatype " + datatype);
                    }
                    if (!attribute.getName().equals(attributeType.getName())) {
                        throw new InvalidDataException("attribute name " + attribute.getName()
                                + " not matching expected " + attributeType.getName());
                    }
                    values[idx] = attributeView.get(idx).getDoubleValue();
                }
                return values;
            }
            case DATE: {
                if (attributeView.size() == 1)
                    return parent.getDateValue();
                Date[] values = new Date[attributeView.size()];
                for (int idx = 0; idx < values.length; idx++) {
                    Attribute attribute = attributeView.get(idx);
                    if (!attribute.getType().equals(datatype)) {
                        throw new InvalidDataException("datatype " + attribute.getType()
                                + " not matching expected datatype " + datatype);
                    }
                    if (!attribute.getName().equals(attributeType.getName())) {
                        throw new InvalidDataException("attribute name " + attribute.getName()
                                + " not matching expected " + attributeType.getName());
                    }
                    values[idx] = attributeView.get(idx).getDateValue();
                }
                return values;
            }
            case COMPOUNDED: {
                if (attributeView.size() % (1 + attributeType.getMembers().size()) != 0)
                    throw new InvalidDataException("invalid data for compounded attribute  " + attributeType.getName());

                int size = attributeView.size() / (1 + attributeType.getMembers().size());
                Map[] values = new Map[size];
                int memberIdx = 0;
                for (int idx = 0; idx < size; idx++) {
                    Attribute attribute = attributeView.get(memberIdx);
                    if (!attribute.getType().equals(datatype)) {
                        throw new InvalidDataException("datatype " + attribute.getType()
                                + " not matching expected datatype " + datatype);
                    }
                    if (!attribute.getName().equals(attributeType.getName()))
                        throw new InvalidDataException("attribute name " + attribute.getName()
                                + " not matching expected " + attributeType.getName());
                    Map<String, Object> memberMap = new HashMap<String, Object>();
                    values[idx] = memberMap;
                    memberIdx++;
                    for (CompoundedAttributeTypeMemberEntity member : attributeType.getMembers()) {
                        AttributeTypeEntity memberAttributeType = member.getMember();
                        Attribute memberAttribute = attributeView.get(memberIdx);
                        if (!sameType(memberAttribute.getType(), memberAttributeType.getType())) {
                            throw new InvalidDataException("datatype " + memberAttribute.getType()
                                    + " not matching expected datatype " + memberAttributeType.getType());
                        }
                        if (!memberAttribute.getName().equals(memberAttributeType.getName()))
                            throw new InvalidDataException("attribute name " + memberAttribute.getName()
                                    + " not matching expected " + memberAttributeType.getName());
                        memberMap.put(memberAttributeType.getName(), memberAttribute.getValue());
                        memberIdx++;
                    }
                }
                return values;
            }
            default:
                throw new UnsupportedDataTypeException("datatype " + datatype + " not supported");
        }
    }

    private boolean sameType(net.link.safeonline.osgi.plugin.DatatypeType osgiDatatype, DatatypeType olasDatatypeType) {

        switch (osgiDatatype) {
            case BOOLEAN: {
                return olasDatatypeType.equals(DatatypeType.BOOLEAN);
            }
            case STRING: {
                return olasDatatypeType.equals(DatatypeType.STRING);
            }
            case DOUBLE: {
                return olasDatatypeType.equals(DatatypeType.DOUBLE);
            }
            case INTEGER: {
                return olasDatatypeType.equals(DatatypeType.INTEGER);
            }
            case DATE: {
                return olasDatatypeType.equals(DatatypeType.DATE);
            }
            case COMPOUNDED: {
                return olasDatatypeType.equals(DatatypeType.COMPOUNDED);
            }
            default: {
                return false;
            }
        }
    }

    /**
     * Find local attribute.
     * 
     * @param subjectId
     * @param attributeType
     * @return attribute value
     */
    private Object findLocalAttribute(SubjectEntity subject, AttributeTypeEntity attributeType) {

        LOG.debug("find local attribute " + attributeType.getName() + " for " + subject.getUserId());

        // filter out the empty attributes
        List<AttributeEntity> attributes = this.attributeDAO.listAttributes(subject, attributeType);
        List<AttributeEntity> nonEmptyAttributes = new LinkedList<AttributeEntity>();
        for (AttributeEntity attribute : attributes)
            if (attribute.getAttributeType().isCompounded()) {
                nonEmptyAttributes.add(attribute);
            } else if (!attribute.isEmpty()) {
                nonEmptyAttributes.add(attribute);
            }

        LOG.debug("found " + nonEmptyAttributes.size());

        if (nonEmptyAttributes.isEmpty())
            return null;

        return getValue(nonEmptyAttributes, attributeType, subject);
    }

    /**
     * Find remote attribute.
     * 
     * @param subjectId
     * @param attributeType
     * @return attribute value
     * @throws NodeNotFoundException
     * @throws SubjectNotFoundException
     * @throws SafeOnlineResourceException
     */
    private Object findRemoteAttribute(SubjectEntity subject, AttributeTypeEntity attributeType)
            throws PermissionDeniedException, AttributeUnavailableException, SubjectNotFoundException,
            NodeNotFoundException {

        LOG.debug("find remote attribute " + attributeType.getName() + " for " + subject.getUserId());

        NodeMappingEntity nodeMapping = this.nodeMappingService.getNodeMapping(subject.getUserId(), attributeType
                .getLocation().getName());

        AuthIdentityServiceClient authIdentityServiceClient = new AuthIdentityServiceClient();
        AttributeClient attributeClient = new AttributeClientImpl(attributeType.getLocation().getLocation(),
                authIdentityServiceClient.getCertificate(), authIdentityServiceClient.getPrivateKey());

        DatatypeType datatype = attributeType.getType();
        Class<?> attributeClass;

        if (attributeType.isMultivalued()) {
            switch (datatype) {
                case STRING:
                case LOGIN:
                    attributeClass = String[].class;
                break;
                case BOOLEAN:
                    attributeClass = Boolean[].class;
                break;
                case INTEGER:
                    attributeClass = Integer[].class;
                break;
                case DOUBLE:
                    attributeClass = Double[].class;
                break;
                case DATE:
                    attributeClass = Date[].class;
                break;
                case COMPOUNDED:
                    attributeClass = Map[].class;
                break;
                default:
                    throw new EJBException("datatype not supported: " + datatype);
            }
        } else {
            switch (datatype) {
                case STRING:
                case LOGIN:
                    attributeClass = String.class;
                break;
                case BOOLEAN:
                    attributeClass = Boolean.class;
                break;
                case INTEGER:
                    attributeClass = Integer.class;
                break;
                case DOUBLE:
                    attributeClass = Double.class;
                break;
                case DATE:
                    attributeClass = Date.class;
                break;
                case COMPOUNDED:
                    attributeClass = Map.class;
                break;
                default:
                    throw new EJBException("datatype not supported: " + datatype);
            }
        }

        try {
            return attributeClient.getAttributeValue(nodeMapping.getId(), attributeType.getName(), attributeClass);
        }

        catch (WSClientTransportException e) {
            this.resourceAuditLogger.addResourceAudit(ResourceNameType.WS, ResourceLevelType.RESOURCE_UNAVAILABLE, e
                    .getLocation(), "Failed to get attribute value of type " + attributeType.getName()
                    + " for subject " + subject.getUserId());
            throw new PermissionDeniedException(e.getMessage());
        } catch (RequestDeniedException e) {
            throw new PermissionDeniedException(e.getMessage());
        } catch (net.link.safeonline.sdk.exception.AttributeNotFoundException e) {
            return null;
        } catch (net.link.safeonline.sdk.exception.AttributeUnavailableException e) {
            throw new AttributeUnavailableException();
        }
    }

    @SuppressWarnings("unchecked")
    private Object getValue(List<AttributeEntity> attributes, AttributeTypeEntity attributeType, SubjectEntity subject) {

        DatatypeType datatype = attributeType.getType();
        if (attributeType.isMultivalued()) {
            switch (datatype) {
                case STRING:
                case LOGIN: {
                    String[] values = new String[attributes.size()];
                    for (int idx = 0; idx < values.length; idx++) {
                        values[idx] = attributes.get(idx).getStringValue();
                    }
                    return values;
                }
                case BOOLEAN: {
                    Boolean[] values = new Boolean[attributes.size()];
                    for (int idx = 0; idx < values.length; idx++) {
                        values[idx] = attributes.get(idx).getBooleanValue();
                    }
                    return values;
                }
                case INTEGER: {
                    Integer[] values = new Integer[attributes.size()];
                    for (int idx = 0; idx < values.length; idx++) {
                        values[idx] = attributes.get(idx).getIntegerValue();
                    }
                    return values;
                }
                case DOUBLE: {
                    Double[] values = new Double[attributes.size()];
                    for (int idx = 0; idx < values.length; idx++) {
                        values[idx] = attributes.get(idx).getDoubleValue();
                    }
                    return values;
                }
                case DATE: {
                    Date[] values = new Date[attributes.size()];
                    for (int idx = 0; idx < values.length; idx++) {
                        values[idx] = attributes.get(idx).getDateValue();
                    }
                    return values;
                }
                case COMPOUNDED: {
                    Map[] values = new Map[attributes.size()];
                    for (CompoundedAttributeTypeMemberEntity member : attributeType.getMembers()) {
                        AttributeTypeEntity memberAttributeType = member.getMember();
                        for (int idx = 0; idx < attributes.size(); idx++) {
                            AttributeEntity attribute = this.attributeDAO.findAttribute(subject, memberAttributeType,
                                    idx);
                            Map<String, Object> memberMap = values[idx];
                            if (null == memberMap) {
                                memberMap = new HashMap<String, Object>();
                                values[idx] = memberMap;
                            }
                            Object memberValue;
                            if (null != attribute) {
                                memberValue = attribute.getValue();
                            } else {
                                memberValue = null;
                            }
                            memberMap.put(memberAttributeType.getName(), memberValue);
                        }
                    }
                    return values;
                }
                default:
                    throw new EJBException("datatype not supported: " + datatype);
            }
        }

        /*
         * Single-valued attribute.
         */
        if (attributes.isEmpty())
            return null;

        AttributeEntity attribute = attributes.get(0);
        return attribute.getValue();
    }
}
