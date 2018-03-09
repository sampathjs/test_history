package com.olf.jm.tranfieldutil.model;

public enum Operator {
	EQUALS, NOTEQUALS, LESS, GREATER, LESSOREQUAL, GREATEROREQUAL;
	
	public boolean apply (String left, String right) {
		switch (this) {
		case EQUALS:
			return left.equals(right);
		case NOTEQUALS:
			return !left.equals(right);
		case GREATER:
			return left.compareTo(right) > 0;
		case LESS:
			return left.compareTo(right) < 0;
		case  GREATEROREQUAL:
			return left.compareTo(right) >= 0;
		case LESSOREQUAL:
			return left.compareTo(right) <= 0;
		}
		throw new RuntimeException ("Check enum " + this.getClass().getName());
	}
}
