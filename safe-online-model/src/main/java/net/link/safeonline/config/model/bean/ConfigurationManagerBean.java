/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.config.model.bean;

import static net.link.safeonline.common.Configurable.defaultGroup;

import java.lang.reflect.Field;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;

import net.link.safeonline.common.Configurable;
import net.link.safeonline.config.dao.ConfigGroupDAO;
import net.link.safeonline.config.dao.ConfigItemDAO;
import net.link.safeonline.config.dao.ConfigItemValueDAO;
import net.link.safeonline.config.model.ConfigurationManager;
import net.link.safeonline.entity.config.ConfigGroupEntity;
import net.link.safeonline.entity.config.ConfigItemEntity;
import net.link.safeonline.entity.config.ConfigItemValueEntity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;


@Stateless
@LocalBinding(jndiBinding = ConfigurationManager.JNDI_BINDING)
public class ConfigurationManagerBean implements ConfigurationManager {

    private static final Log   LOG = LogFactory.getLog(ConfigurationManagerBean.class);

    @EJB(mappedName = ConfigGroupDAO.JNDI_BINDING)
    private ConfigGroupDAO     configGroupDAO;

    @EJB(mappedName = ConfigItemDAO.JNDI_BINDING)
    private ConfigItemDAO      configItemDAO;

    @EJB(mappedName = ConfigItemValueDAO.JNDI_BINDING)
    private ConfigItemValueDAO configItemValueDAO;


    public void addConfigurationValue(String group, String name, boolean multipleChoice, Object value) {

        String valueType = value.getClass().getName();

        ConfigGroupEntity configGroup = this.configGroupDAO.findConfigGroup(group);
        if (configGroup == null) {
            LOG.debug("Adding configuration group: " + group);
            configGroup = this.configGroupDAO.addConfigGroup(group);
        }

        ConfigItemEntity configItem = this.configItemDAO.findConfigItem(configGroup.getName(), name);
        if (configItem == null) {
            LOG.debug("Adding configuration item: " + name);
            configItem = this.configItemDAO.addConfigItem(name, valueType, multipleChoice, configGroup);
        }
        String stringValue = value.toString();
        LOG.debug("add item value: " + stringValue);
        this.configItemValueDAO.addConfigItemValue(configItem, stringValue);

    }

    public void removeConfigurationValue(String group, String name, Object value) {

        ConfigGroupEntity configGroup = this.configGroupDAO.findConfigGroup(group);
        if (null == configGroup)
            return;

        ConfigItemEntity configItem = this.configItemDAO.findConfigItem(configGroup.getName(), name);
        if (null == configItem)
            return;

        String stringValue = value.toString();
        for (ConfigItemValueEntity configItemValue : configItem.getValues()) {
            if (configItemValue.getValue().equals(stringValue)) {
                LOG.debug("remove item value: " + stringValue);
                this.configItemValueDAO.removeConfigItemValue(configItemValue);
            }
        }
    }

    public void configure(Object object) {

        LOG.debug("Configuring: " + object.getClass().getName());

        try {
            Configurable generalConfigurable = object.getClass().getAnnotation(Configurable.class);
            String group = generalConfigurable.group();

            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (null == configurable) {
                    continue;
                }

                if (!configurable.group().equals(defaultGroup)) {
                    group = configurable.group();
                }
                ConfigGroupEntity configGroup = this.configGroupDAO.findConfigGroup(group);
                if (configGroup == null) {
                    LOG.debug("Adding configuration group: " + group);
                    configGroup = this.configGroupDAO.addConfigGroup(group);
                }

                String name = configurable.name();
                if (name == null || name == "") {
                    name = field.getName();
                }

                boolean multipleChoice = configurable.multipleChoice();

                ConfigItemEntity configItem = this.configItemDAO.findConfigItem(configGroup.getName(), name);
                field.setAccessible(true);
                if (configItem == null) {
                    LOG.debug("Adding configuration item: " + name);
                    String valueType = object.getClass().getName();
                    Object value = field.get(object);
                    configItem = this.configItemDAO.addConfigItem(name, valueType, multipleChoice, configGroup);
                    if (null != value) {
                        String stringValue = value.toString();
                        LOG.debug("add item value: " + stringValue);
                        this.configItemValueDAO.addConfigItemValue(configItem, stringValue);
                    }
                } else {
                    configItem.setConfigGroup(configGroup);
                    setValue(configItem, field, object);
                }
            }
        } catch (Exception e) {
            throw new EJBException("Failed to configure bean", e);
        }
    }

    private void setValue(ConfigItemEntity configItem, Field field, Object object)
            throws IllegalArgumentException, IllegalAccessException {

        Class<?> fieldType = field.getType();
        Object value;
        if (null == configItem.getValue()) {
            LOG.debug("Failed to configure field " + field.getName() + ": configuration item value is null");
            return;
        }
        if (String.class.equals(fieldType)) {
            value = configItem.getValue().getValue();
        } else if (Integer.class.equals(fieldType)) {
            try {
                value = Integer.parseInt(configItem.getValue().getValue());
            } catch (NumberFormatException e) {
                LOG.error("invalid integer value for config item: " + configItem.getName());
                /*
                 * In case the value is not OK, we continue and let the bean use its initial value as is.
                 */
                return;
            }
        } else if (Double.class.equals(fieldType)) {
            try {
                value = Double.parseDouble(configItem.getValue().getValue());
            } catch (NumberFormatException e) {
                LOG.error("invalid double value for config item: " + configItem.getName());
                return;
            }
        } else if (Long.class.equals(fieldType)) {
            try {
                value = Long.parseLong(configItem.getValue().getValue());
            } catch (NumberFormatException e) {
                LOG.error("invalid long value for config item: " + configItem.getName());
                return;
            }
        } else if (Boolean.class.equals(fieldType)) {
            value = Boolean.parseBoolean(configItem.getValue().getValue());
        } else {
            LOG.error("unsupported field type: " + fieldType.getName());
            return;
        }
        LOG.debug("Configuring field: " + field.getName() + "; value: " + value);
        field.setAccessible(true);
        field.set(object, value);
    }

}
