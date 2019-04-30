package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

import java.util.HashSet;
import java.util.Set;

/*
 * History:
 * 2018-11-24	V1.0	jwaechter		- Initial Version
 */

/**
 * Implements the base predicate for a list of alternatives regardless of type.
 * @author jwaechter
 * @version 1.0
 */
public class StringAlternatives extends AbstractPredicate<String>{
	private Set<String> listOfAlternatives;
	
	public StringAlternatives(
			String unparsedPredicate, int weight) {
		super(String.class, unparsedPredicate, weight);
		String[] unparsedAlternatives = unparsedPredicate.split(";");
		listOfAlternatives = new HashSet<String>(unparsedAlternatives.length*3);
		for (String alternative : unparsedAlternatives) {
			listOfAlternatives.add(alternative.trim());
		}
	}
	
	public Set<String> getListOfAlternatives() {
		return listOfAlternatives;
	}
	
	@Override
	public int getWeight() {
		return listOfAlternatives.size();
	}

	@Override
	public boolean evaluate(String input) {
		return listOfAlternatives.contains(input.trim());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((listOfAlternatives == null) ? 0 : listOfAlternatives
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
		StringAlternatives other = (StringAlternatives) obj;
		if (listOfAlternatives == null) {
			if (other.listOfAlternatives != null)
				return false;
		} else if (!listOfAlternatives.equals(other.listOfAlternatives))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StringAlternatives [listOfAlternatives=" + listOfAlternatives
				+ ", getWeight()=" + getWeight() + ", getUnparsedPredicate()="
				+ getUnparsedPredicate() + ", getTypeClass()=" + getTypeClass()
				+ "]";
	}	
}
