/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.service;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;

import net.link.safeonline.entity.ConfigGroupEntity;
import net.link.safeonline.entity.ConfigItemEntity;

@Local
@Remote
public interface ConfigurationService {

	List<ConfigGroupEntity> getConfigGroups();

	void saveConfiguration(List<ConfigGroupEntity> configGroupList);

	ConfigItemEntity getConfigItem(String name);

	void saveConfigItem(ConfigItemEntity configItem);

}
