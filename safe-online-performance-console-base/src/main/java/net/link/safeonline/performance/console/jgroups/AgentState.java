/*
 *   Copyright 2007, Maarten Billemont
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
package net.link.safeonline.performance.console.jgroups;

/**
 * <h2>{@link AgentState} - [in short] (TODO).</h2>
 * <p>
 * [description / usage].
 * </p>
 * <p>
 * <i>Dec 19, 2007</i>
 * </p>
 * 
 * @author mbillemo
 */
public enum AgentState {
	RESET("Ready", "Idle"), UPLOAD("Scenario Uploaded", "Receiving"), DEPLOY(
			"Scenario Deployed", "Deploying"), EXECUTE("Charts Available",
			"Executing");

	private String state;
	private String transitioning;

	private AgentState(String state, String transitioning) {

		this.state = state;
		this.transitioning = transitioning;
	}

	public String getState() {

		return this.state;
	}

	public String getTransitioning() {

		return this.transitioning;
	}
}