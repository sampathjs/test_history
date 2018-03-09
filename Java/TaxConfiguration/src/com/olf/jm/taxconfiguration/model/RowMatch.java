package com.olf.jm.taxconfiguration.model;

/*
 * History:
 * 2015-10-10	V1.0	jwaechter	- initial version
 */

/**
 * Helper class to capture return values of method {@link #checkRuleColumn}
 * @author jwaechter
 * @version 1.o
 */
public class RowMatch {
	private final boolean matchRow;
	private final int weight;
	
	public RowMatch (boolean matchRow, int weight) {
		this.matchRow = matchRow;
		this.weight = weight;
	}
	
	

	public boolean isMatchRow() {
		return matchRow;
	}

	public int getWeight() {
		return weight;
	}


	@Override
	public String toString() {
		return "matchRow=" + matchRow + ", weight=" + weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (matchRow ? 1231 : 1237);
		result = prime * result + weight;
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
		RowMatch other = (RowMatch) obj;
		if (matchRow != other.matchRow)
			return false;
		if (weight != other.weight)
			return false;
		return true;
	}
}