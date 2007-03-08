/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.dao.bean;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.dao.ConfigItemDAO;
import net.link.safeonline.entity.ConfigGroupEntity;
import net.link.safeonline.entity.ConfigItemEntity;

@Stateless
public class ConfigItemDAOBean implements ConfigItemDAO {

	@PersistenceContext(unitName = SafeOnlineConstants.SAFE_ONLINE_ENTITY_MANAGER)
	private EntityManager entityManager;

	public ConfigItemEntity addConfigItem(String name, String value,
			ConfigGroupEntity configGroup) {
		ConfigItemEntity configItem = new ConfigItemEntity(name, value,
				configGroup);
		if (configGroup != null) {
			configGroup.getConfigItems().add(configItem);
		}
		this.entityManager.persist(configItem);
		return configItem;
	}

	public void removeConfigItem(ConfigItemEntity configItem) {
		this.entityManager.remove(configItem);
	}

	public void saveConfigItem(ConfigItemEntity configItem) {
		this.entityManager.merge(configItem);
	}

	public ConfigItemEntity findConfigItem(String name) {
		return this.entityManager.find(ConfigItemEntity.class, name);
	}

	@SuppressWarnings("unchecked")
	public List<ConfigItemEntity> listConfigItems() {
		Query query = ConfigItemEntity.createQueryListAll(this.entityManager);
		List<ConfigItemEntity> result = query.getResultList();
		return result;
	}

}
