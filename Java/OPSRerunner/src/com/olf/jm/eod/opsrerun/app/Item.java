package com.olf.jm.eod.opsrerun.app;


/**
 * Class containing a triple of the name, type and the associated additional
 * query to run.
 * @author jwaechter
 */
public final class Item {
	private final String opsName;
	private final String additionalQuery;

	public Item (final String opsName, 
			final String additionalQuery) {
		this.opsName = opsName;
		this.additionalQuery = additionalQuery;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((additionalQuery == null) ? 0 : additionalQuery
						.hashCode());
		result = prime * result
				+ ((opsName == null) ? 0 : opsName.hashCode());
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
		Item other = (Item) obj;
		if (additionalQuery == null) {
			if (other.additionalQuery != null)
				return false;
		} else if (!additionalQuery.equals(other.additionalQuery))
			return false;
		if (opsName == null) {
			if (other.opsName != null)
				return false;
		} else if (!opsName.equals(other.opsName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Item [opsName=" + opsName + ", additionalQuery="
				+ additionalQuery + "]";
	}

	public String getOpsName() {
		return opsName;
	}

	public String getAdditionalQuery() {
		return additionalQuery;
	}
}
