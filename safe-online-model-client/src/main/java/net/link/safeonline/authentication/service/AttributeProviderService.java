/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.authentication.service;

import java.util.Map;

import javax.ejb.Local;

import net.link.safeonline.SafeOnlineService;
import net.link.safeonline.authentication.exception.AttributeNotFoundException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.AttributeUnavailableException;
import net.link.safeonline.authentication.exception.DatatypeMismatchException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;


/**
 * Interface for attribute provider service component. The application using this component must be registered as an attribute provider by
 * the operator.
 * 
 * @author fcorneli
 * 
 */
@Local
public interface AttributeProviderService extends SafeOnlineService {

    public static final String JNDI_BINDING = SafeOnlineService.JNDI_PREFIX + "AttributeProviderServiceBean/local";


    /**
     * Gives back the values of an attribute of a certain subject. This application must be an attribute provider of the attribute in order
     * to read the attribute's values.
     * 
     * <p>
     * For single-valued attributes the returned list will of course contain at maximum one entry.
     * </p>
     * 
     * <p>
     * For compounded attributes the members field will be filled in correctly.
     * </p>
     * 
     * @param subjectLogin
     * @param attributeName
     * @return the list of attribute entries.
     * @throws AttributeTypeNotFoundException
     * @throws PermissionDeniedException
     *             if the caller application is not an attribute provider for the given attribute.
     * @throws SubjectNotFoundException
     * @throws AttributeUnavailableException
     */
    Object getAttributes(String subjectLogin, String attributeName)
            throws AttributeTypeNotFoundException, PermissionDeniedException, SubjectNotFoundException, AttributeUnavailableException;

    /**
     * Create a new attribute for the given user.
     * 
     * <p>
     * The optional attribute value can be a {@link String} or {@link Integer} or an array in case of a multivalued attribute. In case of a
     * compounded attribute record the attribute value is a Map.
     * </p>
     * 
     * @param subjectLogin
     * @param attributeName
     * @param attributeValue
     *            the optional attribute value.
     * @throws AttributeTypeNotFoundException
     * @throws PermissionDeniedException
     * @throws SubjectNotFoundException
     * @throws DatatypeMismatchException
     * @throws NodeNotFoundException
     */
    void createAttribute(String subjectLogin, String attributeName, Object attributeValue)
            throws AttributeTypeNotFoundException, PermissionDeniedException, SubjectNotFoundException, DatatypeMismatchException,
            NodeNotFoundException;

    /**
     * Sets an attribute for the given user. For attribute value we accept {@link String} and {@link Boolean}. A <code>null</code> attribute
     * value is also allowed.
     * 
     * @param subjectLogin
     * @param attributeName
     * @param attributeValue
     * @throws PermissionDeniedException
     * @throws AttributeTypeNotFoundException
     * @throws SubjectNotFoundException
     * @throws AttributeNotFoundException
     * @throws DatatypeMismatchException
     * @throws NodeNotFoundException
     */
    void setAttribute(String subjectLogin, String attributeName, Object attributeValue)
            throws AttributeTypeNotFoundException, PermissionDeniedException, SubjectNotFoundException, AttributeNotFoundException,
            DatatypeMismatchException, NodeNotFoundException;

    /**
     * Sets the member values of a compound attribute record. Editing a compound multivalued attribute is somehow different from editing a
     * regular multivalued attribute. This because we explicitly address the records of a multivalued compounded attribute via the attribute
     * Id.
     * 
     * @param subjectLogin
     * @param attributeName
     * @param attributeId
     * @param memberValues
     * @throws PermissionDeniedException
     * @throws AttributeTypeNotFoundException
     * @throws SubjectNotFoundException
     * @throws DatatypeMismatchException
     * @throws AttributeNotFoundException
     * @throws NodeNotFoundException
     */
    void setCompoundAttributeRecord(String subjectLogin, String attributeName, String attributeId, Map<String, Object> memberValues)
            throws AttributeTypeNotFoundException, PermissionDeniedException, SubjectNotFoundException, DatatypeMismatchException,
            AttributeNotFoundException, NodeNotFoundException;

    /**
     * Removes the attribute for the given subject.
     * 
     * @param subjectLogin
     *            the login of the subject from whom to remove an attribute.
     * @param attributeName
     *            the name of the attribute to be removed.
     * @throws PermissionDeniedException
     * @throws AttributeTypeNotFoundException
     * @throws SubjectNotFoundException
     * @throws AttributeNotFoundException
     * @throws NodeNotFoundException
     */
    void removeAttribute(String subjectLogin, String attributeName)
            throws AttributeTypeNotFoundException, PermissionDeniedException, SubjectNotFoundException, AttributeNotFoundException,
            NodeNotFoundException;

    /**
     * Removes a record of a compounded attribute. The record is identified via the attribute Id.
     * 
     * @param subjectLogin
     * @param attributeName
     * @param attributeId
     * @throws PermissionDeniedException
     * @throws AttributeTypeNotFoundException
     * @throws SubjectNotFoundException
     * @throws AttributeNotFoundException
     * @throws NodeNotFoundException
     */
    void removeCompoundAttributeRecord(String subjectLogin, String attributeName, String attributeId)
            throws AttributeTypeNotFoundException, PermissionDeniedException, SubjectNotFoundException, AttributeNotFoundException,
            NodeNotFoundException;
}
