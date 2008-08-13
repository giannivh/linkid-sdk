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
package net.link.safeonline.performance.scenario.charts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import net.link.safeonline.performance.entity.DriverExceptionEntity;
import net.link.safeonline.performance.entity.ProfileDataEntity;
import net.link.safeonline.performance.entity.ScenarioTimingEntity;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;


/**
 * <h2>{@link AbstractMovingAverageChart}<br>
 * <sub>A chart that renders the speed at which the agent was able to execute scenarios.</sub></h2>
 *
 * <p>
 * The chart generated by this module is a line chart depicting the moving average of the scenario execution speed. This
 * basically means the amount of scenarios that were executed in a time frame of one period expressed in
 * <code>scenarios/second</code>.<br>
 * <br>
 * The period is defined in the constructor ( {@link #AbstractMovingAverageChart(String, String, long)}).
 * </p>
 *
 * <p>
 * <i>Feb 22, 2008</i>
 * </p>
 *
 * @author mbillemo
 */
public abstract class AbstractMovingAverageChart extends AbstractChart {

    private String                             rangeAxisName;

    protected LinkedList<Long>                 averageTimes;
    protected TimeSeries                       averageSeries;
    protected long                             period;

    protected Map<Long, ScenarioTimingEntity>  averageTimings;
    protected Map<Long, ProfileDataEntity>     averageData;
    protected Map<Long, DriverExceptionEntity> averageErrors;

    private boolean                            enoughData = false;


    /**
     * Create a new {@link AbstractMovingAverageChart} instance.
     */
    public AbstractMovingAverageChart(String title, String rangeAxisName, long period) {

        super(title);

        this.period = period;
        this.rangeAxisName = rangeAxisName;
        this.averageTimes = new LinkedList<Long>();
        this.averageSeries = new TimeSeries("Period: " + period + "ms", FixedMillisecond.class);

        this.averageTimings = new HashMap<Long, ScenarioTimingEntity>();
        this.averageData = new HashMap<Long, ProfileDataEntity>();
        this.averageErrors = new HashMap<Long, DriverExceptionEntity>();
    }

    /**
     * @return The value of the moving average for the current set of {@link ScenarioTimingEntity}s.
     */
    protected abstract Number getMovingAverage();

    /**
     * {@inheritDoc}
     */
    @Override
    public void processTiming(ScenarioTimingEntity timing) {

        long startTime = timing.getStart();

        this.averageTimings.put(startTime, timing);
        Set<Long> polled = doAverage(startTime);

        for (Long polledTime : polled) {
            this.averageTimings.remove(polledTime);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processData(ProfileDataEntity data) {

        long startTime = data.getScenarioTiming().getStart();

        this.averageData.put(startTime, data);
        Set<Long> polled = doAverage(startTime);

        for (Long polledTime : polled) {
            this.averageData.remove(polledTime);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processError(DriverExceptionEntity error) {

        long startTime = error.getOccurredTime();

        this.averageErrors.put(startTime, error);
        Set<Long> polled = doAverage(startTime);

        for (Long polledTime : polled) {
            this.averageErrors.remove(polledTime);
        }
    }

    /**
     * Calculate the moving average using {@link #getMovingAverage()} adding data at the given time.
     *
     * @return A set of times that were polled off the averages list. The objects for this time have dropped out of the
     *         moving average period and need no longer be remembered.
     */
    protected Set<Long> doAverage(Long currentTime) {

        Set<Long> polled = new HashSet<Long>();
        this.averageTimes.offer(currentTime);

        // Poll off outdated data (more than a period old).
        Long baseTime;
        while (true) {
            baseTime = this.averageTimes.peek();
            if (currentTime - this.period <= baseTime || baseTime > currentTime) {
                break;
            }

            this.enoughData = true;
            polled.add(this.averageTimes.poll());
        }

        // Multiply hits by 1000 and divide by period to obtain hits/s.
        if (this.enoughData) {
            this.averageSeries.addOrUpdate(new FixedMillisecond(currentTime), getMovingAverage());
        }

        return polled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XYPlot getPlot() {

        if (this.averageSeries.isEmpty())
            return null;

        ValueAxis domainAxis = new DateAxis("Time");

        TimeSeriesCollection speedSet;
        speedSet = new TimeSeriesCollection(this.averageSeries);

        return new XYPlot(speedSet, domainAxis, new NumberAxis(this.rangeAxisName), new XYLineAndShapeRenderer(true,
                false));
    }
}
