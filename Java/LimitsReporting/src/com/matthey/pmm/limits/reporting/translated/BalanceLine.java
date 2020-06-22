package com.matthey.pmm.limits.reporting.translated;

public class BalanceLine {
	private String lineTitle;
	private String purpose;
	
	public BalanceLine (final String lineTitle, final String purpose) {
		this.lineTitle = lineTitle;
		this.purpose = purpose;
	}

	public String getLineTitle() {
		return lineTitle;
	}

	public void setLineTitle(String lineTitle) {
		this.lineTitle = lineTitle;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lineTitle == null) ? 0 : lineTitle.hashCode());
		result = prime * result + ((purpose == null) ? 0 : purpose.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BalanceLine other = (BalanceLine) obj;
		if (lineTitle == null) {
			if (other.lineTitle != null)
				return false;
		} else if (!lineTitle.equals(other.lineTitle))
			return false;
		if (purpose == null) {
			if (other.purpose != null)
				return false;
		} else if (!purpose.equals(other.purpose))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BalanceLine [lineTitle=" + lineTitle + ", purpose=" + purpose + "]";
	}
}
