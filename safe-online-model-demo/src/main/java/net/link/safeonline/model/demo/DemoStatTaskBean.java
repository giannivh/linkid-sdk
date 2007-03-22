/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.demo;

import java.util.Random;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import net.link.safeonline.Task;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.StatisticDAO;
import net.link.safeonline.dao.StatisticDataPointDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.StatisticEntity;

import org.jboss.annotation.ejb.LocalBinding;

@Stateless
@Local(Task.class)
@LocalBinding(jndiBinding = Task.JNDI_PREFIX + "/" + "DemoStatTaskBean")
public class DemoStatTaskBean implements Task {

	@EJB
	private StatisticDAO statisticDAO;

	@EJB
	private StatisticDataPointDAO statisticDataPointDAO;

	@EJB
	private ApplicationDAO applicationDAO;

	private final static String STAT_NAME = "demo stat";

	public String getName() {
		return "Demo statistic generator";
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void perform() {
		ApplicationEntity application = applicationDAO
				.findApplication(DemoStartableBean.DEMO_APPLICATION_NAME);
		if (application == null) {
			return;
		}
		StatisticEntity statistic = statisticDAO
				.findStatisticByNameAndApplication(STAT_NAME, application);
		if (statistic == null) {
			statistic = this.statisticDAO.addStatistic(STAT_NAME, application);
		}
		Random generator = new Random();
		statisticDataPointDAO.cleanStatisticDataPoints(statistic);
		this.statisticDataPointDAO.addStatisticDataPoint("cat A", statistic,
				generator.nextInt(), 0, 0);
		this.statisticDataPointDAO.addStatisticDataPoint("cat B", statistic,
				generator.nextInt(), 0, 0);
		this.statisticDataPointDAO.addStatisticDataPoint("cat C", statistic,
				generator.nextInt(), 0, 0);
		this.statisticDataPointDAO.addStatisticDataPoint("cat D", statistic,
				generator.nextInt(), 0, 0);
	}
}
