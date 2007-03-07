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

import net.link.safeonline.entity.SchedulingEntity;
import net.link.safeonline.entity.TaskEntity;
import net.link.safeonline.entity.TaskHistoryEntity;

@Local
@Remote
public interface SchedulingService {

	List<TaskEntity> getTaskList();

	List<SchedulingEntity> getSchedulingList();

	List<TaskHistoryEntity> getTaskHistoryList(TaskEntity task);

	void performTask(TaskEntity task);

	void performScheduling(SchedulingEntity scheduling);

	void clearTaskHistory(TaskEntity task);

	void clearAllTasksHistory();

}
