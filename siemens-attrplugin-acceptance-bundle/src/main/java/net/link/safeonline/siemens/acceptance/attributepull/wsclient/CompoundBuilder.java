/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.siemens.acceptance.attributepull.wsclient;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Builder for compound attribute instances. A compound attribute instance in build using the compound annotations.
 * 
 * @author fcorneli
 * 
 */
public class CompoundBuilder {

    private static final Log   LOG              = LogFactory.getLog(CompoundBuilder.class);

    public static final String ATTRIBUTE_ID_KEY = "@Id";

    private final Class<?>     compoundClass;

    private final boolean      isMap;

    private final Object       compoundAttribute;


    /**
     * Main constructor. The compound class should be annotated with {@link Compound} and the member properties with {@link CompoundMember}.
     * The compound class can also be a simple {@link Map}. In this case the result map will be filled with name value map entries for every
     * member of the compounded attribute.
     * 
     * @param compoundClass
     */
    @SuppressWarnings("unchecked")
    public CompoundBuilder(Class compoundClass) {

        this.compoundClass = compoundClass;

        Compound compoundAnnotation = (Compound) compoundClass.getAnnotation(Compound.class);
        if (null == compoundAnnotation) {
            if (false == Map.class.isAssignableFrom(compoundClass))
                throw new IllegalArgumentException("valueClass not @Compound annotated or not of type java.util.Map");
            isMap = true;
            compoundAttribute = new HashMap<String, Object>();
        } else {
            isMap = false;
            try {
                compoundAttribute = compoundClass.newInstance();
            } catch (Exception e) {
                LOG.error("error: " + e.getMessage(), e);
                throw new IllegalArgumentException("could not create new instance for " + compoundClass.getName());
            }
        }
    }

    /**
     * Gives back the resulting compound object.
     * 
     */
    public Object getCompound() {

        return compoundAttribute;
    }

    /**
     * Sets a property (i.e. a member attribute) on the compounded object.
     * 
     * @param memberName
     *            the name of the member attribute.
     * @param memberAttributeValue
     *            the value of the member attribute.
     */
    @SuppressWarnings("unchecked")
    public void setCompoundProperty(String memberName, Object memberAttributeValue) {

        if (isMap) {
            /*
             * We also support non-annotated compound results via a simple java.util.Map.
             */
            Map<String, Object> compoundMap = (Map<String, Object>) compoundAttribute;
            if (compoundMap.containsKey(memberName))
                throw new RuntimeException("member already present in result map: " + memberName);
            compoundMap.put(memberName, memberAttributeValue);
            return;
        }
        Method[] methods = compoundClass.getMethods();
        for (Method method : methods) {
            CompoundMember compoundMemberAnnotation = method.getAnnotation(CompoundMember.class);
            if (null == compoundMemberAnnotation) {
                continue;
            }
            if (false == memberName.equals(compoundMemberAnnotation.value())) {
                continue;
            }
            Method setPropertyMethod = CompoundUtil.getSetMethod(compoundClass, method);
            try {
                setPropertyMethod.invoke(compoundAttribute, new Object[] { memberAttributeValue });
            } catch (Exception e) {
                throw new RuntimeException("could not invoke: " + setPropertyMethod.getName());
            }
        }
    }

    /**
     * Sets the compounded attribute Id. Every compounded attribute record has an identifier.
     * 
     * @param attributeId
     */
    @SuppressWarnings("unchecked")
    public void setCompoundId(String attributeId) {

        if (isMap) {
            /*
             * We also support non-annotated compound results via a simple java.util.Map.
             */
            Map<String, Object> compoundMap = (Map<String, Object>) compoundAttribute;
            compoundMap.put(ATTRIBUTE_ID_KEY, attributeId);
            return;
        }
        Method[] methods = compoundClass.getMethods();
        for (Method method : methods) {
            CompoundId compoundIdAnnotation = method.getAnnotation(CompoundId.class);
            if (null == compoundIdAnnotation) {
                continue;
            }
            Method setPropertyMethod = CompoundUtil.getSetMethod(compoundClass, method);
            try {
                setPropertyMethod.invoke(compoundAttribute, new Object[] { attributeId });
            } catch (Exception e) {
                throw new RuntimeException("could not invoke: " + setPropertyMethod.getName());
            }
        }
    }
}