package com.olf.jm.tranfieldutil.model;

public class AdditionalFilterCriterium {
	private final TranFieldIdentificator identificator;
	private final String value;
	private final Operator operator;
	
	public AdditionalFilterCriterium (final TranFieldIdentificator identificator,
			final Operator operator, final String value) {
		this.identificator = identificator;
		this.operator = operator;
		this.value = value;
	}

	public TranFieldIdentificator getIdentificator() {
		return identificator;
	}

	public String getValue() {
		return value;
	}

	public Operator getOperator() {
		return operator;
	}

	@Override
	public String toString() {
		return "AdditionalFilterCriterium [identificator=" + identificator
				+ ", value=" + value + ", operator=" + operator + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((identificator == null) ? 0 : identificator.hashCode());
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		AdditionalFilterCriterium other = (AdditionalFilterCriterium) obj;
		if (identificator == null) {
			if (other.identificator != null)
				return false;
		} else if (!identificator.equals(other.identificator))
			return false;
		if (operator != other.operator)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
