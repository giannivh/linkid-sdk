/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.performance.console.swing.data;

import java.util.ArrayList;
import java.util.List;

import net.link.safeonline.performance.console.ScenarioExecution;
import net.link.safeonline.performance.console.ScenarioRemoting;
import net.link.safeonline.performance.console.jgroups.Agent;
import net.link.safeonline.performance.console.jgroups.AgentState;
import net.link.safeonline.performance.console.swing.ui.AgentStatusListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;

/**
 * This object keeps the status of communication between the console and the
 * agent it represents. It also features locking such that only one operation
 * would be executed upon it at once.
 *
 * @author mbillemo
 *
 */
public class ConsoleAgent implements Agent {

	private static final long serialVersionUID = 1L;
	static final Log LOG = LogFactory.getLog(ConsoleAgent.class);

	private List<AgentStatusListener> agentStatusListeners;
	private ScenarioRemoting scenarioDeployer;
	private Address agentAddress;
	private boolean healthy;
	private boolean selected;
	private AgentState transit = AgentState.RESET;
	private AgentState state = AgentState.RESET;
	private Exception error;

	/**
	 * Create a new {@link ConsoleAgent} component based off the agent at the
	 * given {@link Address}.
	 */
	public ConsoleAgent(Address agentAddress) {

		this.agentStatusListeners = new ArrayList<AgentStatusListener>();
		this.scenarioDeployer = ConsoleData.getInstance().getRemoting();
		this.agentAddress = agentAddress;
		this.healthy = true;

		new UpdateAgentState().start();
	}

	/**
	 * @return false if JGroups suspects this agent of being unavailable.
	 */
	public boolean isHealthy() {

		return this.healthy;
	}

	/**
	 * Set the JGroups health status of this agent.
	 */
	public void setHealthy(boolean healthy) {

		this.healthy = healthy;
		fireAgentStatus();
	}

	/**
	 * @return <code>true</code> if this agent is to participate in actions.
	 */
	public boolean isSelected() {

		return this.selected;
	}

	/**
	 * @param selected
	 *            <code>true</code> if this agent is selected for actions.
	 */
	public void setSelected(boolean selected) {

		this.selected = selected;
	}

	/**
	 * Define an object that should be notified when this agent changes. This
	 * should be an object that can fire the appropriate events in the UI
	 * required to render the change in this {@link ConsoleAgent}'s status.
	 */
	public void addAgentStatusListener(AgentStatusListener agentStatusListener) {

		if (!this.agentStatusListeners.contains(agentStatusListener))
			this.agentStatusListeners.add(agentStatusListener);
	}

	/**
	 * Manually fire an agent status event forcing the UI to update itself for
	 * this agent.
	 */
	public void fireAgentStatus() {

		for (AgentStatusListener listener : this.agentStatusListeners)
			listener.statusChanged(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {

		String transitStr = AgentState.RESET.getTransitioning();
		String stateStr = AgentState.RESET.getTransitioning();
		if (null != this.transit)
			transitStr = this.transit.getTransitioning();
		if (null != this.state)
			stateStr = this.state.getState();

		String health = this.healthy ? "Healthy" : "Unavailable";
		return String.format("%s: [%s]", health, stateStr, transitStr,
				this.agentAddress);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {

		return this.agentAddress.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == this)
			return true;
		if (obj == null || !(obj instanceof ConsoleAgent))
			return false;

		return this.agentAddress.equals(((ConsoleAgent) obj).agentAddress);
	}

	/**
	 * Retrieve the JGroups address of this {@link ConsoleAgent}.
	 */
	public Address getAddress() {

		return this.agentAddress;
	}

	/**
	 * {@inheritDoc}
	 */
	public AgentState getState() {

		return this.state;
	}

	/**
	 * {@inheritDoc}
	 */
	public void resetTransit() {

		this.scenarioDeployer.resetTransit(this.agentAddress);
		updateState();
	}

	/**
	 * {@inheritDoc}
	 */
	public AgentState getTransit() {

		return this.transit;
	}

	/**
	 * {@inheritDoc}
	 */
	public Exception getError() {

		return this.error;
	}

	/**
	 * {@inheritDoc}
	 */
	public ScenarioExecution getStats() {

		return this.scenarioDeployer.getStats(this.agentAddress);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean actionRequest(AgentState action) {

		try {
			return this.scenarioDeployer.actionRequest(this.agentAddress,
					action);
		} finally {
			updateState();
		}
	}

	public void updateState() {

		AgentState oldTransit = this.transit;
		AgentState oldState = this.state;
		Exception oldError = this.error;

		this.transit = this.scenarioDeployer.getTransit(this.agentAddress);
		this.state = this.scenarioDeployer.getState(this.agentAddress);
		this.error = this.scenarioDeployer.getError(this.agentAddress);

		if (oldTransit != this.transit || oldState != this.state
				|| oldError != this.error)
			fireAgentStatus();
	}

	private class UpdateAgentState extends Thread {

		private static final long INTERVAL = 2000;

		public UpdateAgentState() {

			setDaemon(true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {

			while (true) {
				try {
					updateState();
				} catch (Exception e) {
					LOG.error("Error updating state:", e);
				}

				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}