/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.bean;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.data.AttributeDO;
import net.link.safeonline.entity.ApplicationIdentityAttributeEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionPK;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.DatatypeType;
import net.link.safeonline.model.AttributeTypeDescriptionDecorator;


@Stateless
@LocalBinding(jndiBinding = AttributeTypeDescriptionDecorator.JNDI_BINDING)
public class AttributeTypeDescriptionDecoratorBean implements AttributeTypeDescriptionDecorator {

    @EJB
    private AttributeTypeDAO attributeTypeDAO;


    public List<AttributeDO> addDescriptionFromIdentityAttributes(Collection<ApplicationIdentityAttributeEntity> identityAttributes,
                                                                  Locale locale) {

        List<AttributeDO> attributes = new LinkedList<AttributeDO>();
        String language = null;
        if (null != locale) {
            language = locale.getLanguage();
        }
        for (ApplicationIdentityAttributeEntity identityAttribute : identityAttributes) {
            String name = identityAttribute.getAttributeTypeName();
            AttributeTypeEntity attributeType = identityAttribute.getAttributeType();
            DatatypeType datatype = attributeType.getType();
            String humanReadableName = null;
            String description = null;
            if (null != language) {
                AttributeTypeDescriptionEntity attributeTypeDescription = this.attributeTypeDAO
                                                                                               .findDescription(new AttributeTypeDescriptionPK(
                                                                                                       name, language));
                if (null != attributeTypeDescription) {
                    humanReadableName = attributeTypeDescription.getName();
                    description = attributeTypeDescription.getDescription();
                }
            }
            AttributeDO attribute = new AttributeDO(name, datatype, false, 0, humanReadableName, description,
                    identityAttribute.getAttributeType().isUserEditable(), identityAttribute.isDataMining(), null, null);
            attributes.add(attribute);
        }
        return attributes;
    }
}
