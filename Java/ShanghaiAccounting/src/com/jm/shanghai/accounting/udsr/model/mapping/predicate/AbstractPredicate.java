package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

/*
 * History:
 * 2018-11-22	V1.0		jwaechter		- Initial Version
 */

/**
 * Abstract base class for all other predicates used in the mapping table to check whether a 
 * row in the runtime data table passes through the filter defined by the mapping row.
 * @author WaetcJ01
 * @version 1.0
 */
public abstract class AbstractPredicate <T> {
	/**
	 * The weight applied to the overall weight of all cells of the mapping table row even if it matches.
	 */
	private final int weight;
	
	private final String unparsedPredicate;
	
	private final Class<T> typeClass;
	
	protected AbstractPredicate (final Class<T> typeClass, 
			final String unparsedPredicate,
			final int weight) {
		this.unparsedPredicate = unparsedPredicate;
		this.weight = weight;
		this.typeClass = typeClass;
	}
	
	public abstract boolean evaluate (String input);
	
	public int getWeight() {
		return weight;
	}

	public String getUnparsedPredicate() {
		return unparsedPredicate;
	}

	public Class<T> getTypeClass() {
		return typeClass;
	}

	@Override
	public String toString() {
		return "Predicate for " + typeClass.getName() + " with definition '" + unparsedPredicate + "'";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((typeClass == null) ? 0 : typeClass.hashCode());
		result = prime
				* result
				+ ((unparsedPredicate == null) ? 0 : unparsedPredicate
						.hashCode());
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
		AbstractPredicate other = (AbstractPredicate) obj;
		if (typeClass == null) {
			if (other.typeClass != null)
				return false;
		} else if (!typeClass.equals(other.typeClass))
			return false;
		if (unparsedPredicate == null) {
			if (other.unparsedPredicate != null)
				return false;
		} else if (!unparsedPredicate.equals(other.unparsedPredicate))
			return false;
		if (weight != other.weight)
			return false;
		return true;
	}	
}
