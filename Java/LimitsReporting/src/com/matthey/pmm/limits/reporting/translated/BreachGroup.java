package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.List;

public class BreachGroup {
	private String breachType;
	private List<RunResult> breaches;

	public BreachGroup(final String breachType, final List<RunResult> breaches) {
		this.breachType = breachType;
		this.breaches = new ArrayList<>(breaches);
	}

	public String getBreachType() {
		return breachType;
	}

	public void setBreachType(String breachType) {
		this.breachType = breachType;
	}

	public List<RunResult> getBreaches() {
		return breaches;
	}

	public void setBreaches(List<RunResult> breaches) {
		this.breaches = breaches;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((breachType == null) ? 0 : breachType.hashCode());
		result = prime * result + ((breaches == null) ? 0 : breaches.hashCode());
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
		BreachGroup other = (BreachGroup) obj;
		if (breachType == null) {
			if (other.breachType != null)
				return false;
		} else if (!breachType.equals(other.breachType))
			return false;
		if (breaches == null) {
			if (other.breaches != null)
				return false;
		} else if (!breaches.equals(other.breaches))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BreachGroup [breachType=" + breachType + ", breaches=" + breaches + "]";
	}
}
