/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.siemens.acceptance.attributepull.wsclient;

import java.util.Map;

import net.link.safeonline.siemens.acceptance.attributepull.wsclient.exception.AttributeNotFoundException;
import net.link.safeonline.siemens.acceptance.attributepull.wsclient.exception.AttributeUnavailableException;
import net.link.safeonline.siemens.acceptance.attributepull.wsclient.exception.RequestDeniedException;
import net.link.safeonline.siemens.acceptance.attributepull.wsclient.exception.WSClientTransportException;


/**
 * Interface for attribute client. Via components implementing this interface applications can retrieve attributes for subjects.
 * Applications can only retrieve attribute values for which the user confirmed the corresponding application identity.
 * 
 * <p>
 * The attribute value can be of type String, Boolean or an array of Object[] in case of a multivalued attribute.
 * </p>
 * 
 * @author fcorneli
 * 
 */
public interface AttributeClient extends MessageAccessor {

    /**
     * Gives back the attribute value of a single attribute of the given subject. The type of the value returned depends on the datatype of
     * the corresponding attribute type.
     * 
     * <p>
     * Compounded attributes are handled via annotated java classes. The annotations to be used for this are
     * {@link net.link.safeonline.sdk.ws.annotation.Compound} and {@link net.link.safeonline.sdk.ws.annotation.CompoundMember}.
     * </p>
     * 
     * @param <T>
     * @param userId
     * @param attributeName
     * @param valueClass
     * @throws AttributeNotFoundException
     * @throws RequestDeniedException
     * @throws WSClientTransportException
     *             in case the service could not be contacted. Can happen if the SSL was not setup correctly.
     * @throws AttributeUnavailableException
     */
    <T> T getAttributeValue(String userId, String attributeName, Class<T> valueClass)
            throws AttributeNotFoundException, RequestDeniedException, WSClientTransportException, AttributeUnavailableException;

    /**
     * Gives back attribute values via the map of attributes. The map should hold the requested attribute names as keys. The method will
     * fill in the corresponding values.
     * 
     * @param userId
     * @param attributes
     * @throws AttributeNotFoundException
     * @throws RequestDeniedException
     * @throws WSClientTransportException
     * @throws AttributeUnavailableException
     */
    void getAttributeValues(String userId, Map<String, Object> attributes)
            throws AttributeNotFoundException, RequestDeniedException, WSClientTransportException, AttributeUnavailableException;

    /**
     * Gives back a map of attributes for the given subject that this application is allowed to read.
     * 
     * @param userId
     * @throws RequestDeniedException
     * @throws WSClientTransportException
     * @throws AttributeNotFoundException
     * @throws AttributeUnavailableException
     */
    Map<String, Object> getAttributeValues(String userId)
            throws RequestDeniedException, WSClientTransportException, AttributeNotFoundException, AttributeUnavailableException;

    /**
     * Gives back the application identity for the given subject.
     * 
     * The identity card class is a POJO annotated with {@link net.link.safeonline.sdk.ws.attrib.annotation.IdentityCard}. It's properties
     * should be annotated with {@link net.link.safeonline.sdk.ws.attrib.annotation.IdentityAttribute}.
     * 
     * @param <T>
     * @param userId
     * @throws AttributeNotFoundException
     * @throws RequestDeniedException
     * @throws WSClientTransportException
     * @throws AttributeUnavailableException
     */
    <T> T getIdentity(String userId, Class<T> identityCardClass)
            throws AttributeNotFoundException, RequestDeniedException, WSClientTransportException, AttributeUnavailableException;
}