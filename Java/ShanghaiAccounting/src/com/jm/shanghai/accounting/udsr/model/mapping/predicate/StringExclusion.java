package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

import java.util.HashSet;
import java.util.Set;

/*
 * History:
 * 2018-11-24	V1.0	jwaechter	- Initial Version
 */

/**
 * Class containing a list of excluded string values.
 */
public class StringExclusion extends AbstractPredicate<String>{
	private Set<String> listOfExcludedValues;
	
	public StringExclusion(String unparsedPredicate, int weight) {
		super(String.class, unparsedPredicate, weight);
		String[] unparsedAlternatives = unparsedPredicate.split(";");
		listOfExcludedValues = new HashSet<String>(unparsedAlternatives.length*3);
		for (String alternative : unparsedAlternatives) {
		    String trimmedAlternative = alternative.trim();
		    if (!trimmedAlternative.startsWith("!")) {
		    	throw new RuntimeException ("Error parsing predicate '" + unparsedPredicate 
		    			+ "': List supports exclusions only and alternative '" 
		    			+ trimmedAlternative + "' is not excluded");
		    }
			listOfExcludedValues.add(trimmedAlternative.substring(1).trim());
		}	
	}

	@Override
	public boolean evaluate(String input) {
		return !listOfExcludedValues.contains(input);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((listOfExcludedValues == null) ? 0 : listOfExcludedValues
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringExclusion other = (StringExclusion) obj;
		if (listOfExcludedValues == null) {
			if (other.listOfExcludedValues != null)
				return false;
		} else if (!listOfExcludedValues.equals(other.listOfExcludedValues))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StringExclusion [listOfExcludedValues=" + listOfExcludedValues
				+ ", getWeight()=" + getWeight() + ", getUnparsedPredicate()="
				+ getUnparsedPredicate() + ", getTypeClass()=" + getTypeClass()
				+ "]";
	}
}
