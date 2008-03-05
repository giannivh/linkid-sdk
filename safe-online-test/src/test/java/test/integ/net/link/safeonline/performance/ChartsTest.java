/*
 *   Copyright 2008, Maarten Billemont
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package test.integ.net.link.safeonline.performance;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import net.link.safeonline.performance.entity.DriverProfileEntity;
import net.link.safeonline.performance.entity.ExecutionEntity;
import net.link.safeonline.performance.entity.ProfileDataEntity;
import net.link.safeonline.performance.entity.ScenarioTimingEntity;
import net.link.safeonline.performance.scenario.Scenario;
import net.link.safeonline.performance.scenario.charts.Chart;
import net.link.safeonline.performance.scenario.charts.ScenarioSpeedCorrelationChart;

/**
 * <h2>{@link ChartsTest}<br>
 * <sub>[in short] (TODO).</sub></h2>
 *
 * <p>
 * [description / usage].
 * </p>
 *
 * <p>
 * <i>Mar 3, 2008</i>
 * </p>
 *
 * @author mbillemo
 */
public class ChartsTest extends AbstractDataTest {

	private static final int DATA_POINTS = 800;
	private final Integer datalimit = 1000;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {

		this.SHOW_SQL = false;
	}

	/**
	 * Create a new {@link ChartsTest} instance.
	 */
	public ChartsTest() throws Exception {

		// Execution to chart.
		ExecutionEntity execution = getLatestExecution();

		// Chart modules to render.
		ArrayList<Chart> charts = new ArrayList<Chart>();
		charts.add(new ScenarioSpeedCorrelationChart(5 * 60 * 1000));

		// Render and display the charts.
		// displayCharts(getCharts(execution, charts.toArray(new Chart[0])));
		displayCharts(getAllCharts(execution));
	}

	/**
	 * Get the most recent execution.
	 */
	private ExecutionEntity getLatestExecution() {

		Date executionId = new TreeSet<Date>(this.executionService
				.getExecutions()).last();

		return this.executionService.getExecution(executionId);
	}

	/**
	 * Instantiate a scenario object of the class used in the given execution.
	 */
	private Scenario createScenario(ExecutionEntity execution)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {

		String scenarioName = execution.getScenarioName();
		return (Scenario) Thread.currentThread().getContextClassLoader()
				.loadClass(scenarioName).newInstance();
	}

	/**
	 * Render all charts registered with the scenario used in the given
	 * execution.
	 */
	@SuppressWarnings("unused")
	private Map<String, byte[][]> getAllCharts(ExecutionEntity execution)
			throws Exception {

		List<Chart> charts = createScenario(execution).getCharts();
		return getCharts(execution, charts.toArray(new Chart[charts.size()]));
	}

	/**
	 * Render given charts with data from the given execution.
	 */
	private Map<String, byte[][]> getCharts(ExecutionEntity execution,
			Chart... charts) throws Exception {

		// Retrieve scenario timings recorded during the execution.
		List<ScenarioTimingEntity> scenarioTimings = this.scenarioTimingService
				.getExecutionTimings(execution);

		int i = 0, t = scenarioTimings.size();

		// Let the charts process the timings.
		for (ScenarioTimingEntity timing : scenarioTimings) {
			for (Chart chart : charts)
				try {
					chart.processTiming(timing);
				} catch (Exception e) {
					this.LOG.error("Charting Timing Failed:", e);
				}

			// Show timing completion percentage.
			if (++i % Math.max(1, t / 100) == 0)
				this.LOG.debug(100 * i / t + "% ..");
			if (this.datalimit != null && i > this.datalimit)
				break;
		}

		// Retrieve driver profiles created in the execution.
		Set<DriverProfileEntity> profiles = execution.getProfiles();

		i = 0;
		t = profiles.size();

		for (DriverProfileEntity profile : profiles) {

			// Retrieve data for current profile, paged or not.
			List<ProfileDataEntity> profileData;
			if (this.datalimit == null)
				profileData = this.profileDataService.getProfileData(profile,
						DATA_POINTS);
			else
				profileData = this.profileDataService
						.getAllProfileData(profile);

			int j = 0, u = profileData.size();

			// Let the charts process the data.
			for (ProfileDataEntity data : profileData) {
				for (Chart chart : charts)
					try {
						chart.processData(data);
					} catch (Exception e) {
						this.LOG.error("Charting Data Failed:", e);
					}

				// Show data completion percentage.
				if (++j % Math.max(1, u / 100) == 0)
					this.LOG.debug(100 * i / t + "%, " + 100 * j / u + "% ..");
				if (this.datalimit != null && j > this.datalimit)
					break;
			}

			// Show profile completion percentage.
			if (++i % Math.max(1, t / 100) == 0)
				this.LOG.debug(100 * i / t + "%, 100% ..");
			if (this.datalimit != null && i > this.datalimit)
				break;
		}

		// Render all charts to images.
		Map<String, byte[][]> images = new LinkedHashMap<String, byte[][]>();
		for (Chart chart : charts)
			images.put(chart.getTitle(), chart.render(DATA_POINTS));

		return images;
	}

	/**
	 * Display the given charts in a dialog.
	 */
	private void displayCharts(Map<String, byte[][]> charts) {

		// Create the dialog.
		JPanel contentPane = new JPanel();
		JFrame dialog = new JFrame(getClass().getCanonicalName());
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setContentPane(new JScrollPane(contentPane));

		// Add the images.
		for (Map.Entry<String, byte[][]> chart : charts.entrySet()) {
			JLabel image = new JLabel(chart.getKey(), new ImageIcon(chart
					.getValue()[0]), SwingConstants.CENTER);
			image.setVerticalTextPosition(SwingConstants.TOP);
			image.setHorizontalTextPosition(SwingConstants.CENTER);
			contentPane.add(image);
		}

		// Size & position the dialog.
		dialog.pack();
		dialog.setExtendedState(Frame.MAXIMIZED_BOTH);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	public static void main(String[] args) throws Exception {

		new ChartsTest();
	}
}