package net.link.safeonline.demo.lawyer.webapp;

import java.io.Serializable;

public class Lawyer implements Serializable {

	private static final long serialVersionUID = 1L;

	private String safeonlineId;

	private String name;

	private boolean isLawyer;

	private boolean isSuspended;

	private String bar;

	private boolean isAdmin;

	public String getBar() {
		return bar;
	}

	public void setBar(String bar) {
		this.bar = bar;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	public boolean isLawyer() {
		return isLawyer;
	}

	public void setLawyer(boolean isLawyer) {
		this.isLawyer = isLawyer;
	}

	public boolean isSuspended() {
		return isSuspended;
	}

	public void setSuspended(boolean isSuspended) {
		this.isSuspended = isSuspended;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSafeonlineId() {
		return safeonlineId;
	}

	public void setSafeonlineId(String safeonlineId) {
		this.safeonlineId = safeonlineId;
	}

}
