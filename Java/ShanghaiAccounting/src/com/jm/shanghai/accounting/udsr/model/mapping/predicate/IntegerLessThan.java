package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

/*
 * History:
 * 2018-11-24		V1.0	jwaechter		- Initial Version
 */

/**
 * Predicate implementing a comparison for < on integers.
 * @author jwaechter
 * @version 1.0
 */
public class IntegerLessThan extends AbstractPredicate<Integer>{
	private double threshold;
	
	public IntegerLessThan(String unparsedPredicate,
			int weight) {
		super(Integer.class, unparsedPredicate, true, weight);
		parseIntegerPredicate(unparsedPredicate);
	}

	@Override
	public boolean evaluate(String input) {
		double parsedInput = parseIntegerInput(input);
		return parsedInput < threshold;
	}
	
	private int parseIntegerPredicate(String unparsedPredicate) {
		String trimmedPredicate = unparsedPredicate.trim();
		String unparsedRightOperand = trimmedPredicate.substring(1).trim();
		try {
			return Integer.parseInt(unparsedRightOperand);
		} catch (NumberFormatException ex) {
			throw new RuntimeException ("Could not parse predicate '" + unparsedPredicate 
					+ "'. The right operand '" + unparsedRightOperand 
					+ "' could not be parsed as an integer");
		}
	}
	
	private int parseIntegerInput(String input) {
		String trimmedInput = input.trim();
		try {
			return Integer.parseInt(trimmedInput);
		} catch (NumberFormatException ex) {
			throw new RuntimeException ("Could not parse input '" + input 
					+ "' while applying predicate '" + this.toString() 
					+ "'. The left operand '" + trimmedInput
					+ "' could not be parsed as an integer");
		}
	}

	@Override
	public String toString() {
		return "IntegerLessThan [threshold=" + threshold + ", getWeight()="
				+ getWeight() + ", getUnparsedPredicate()="
				+ getUnparsedPredicate() + ", getTypeClass()=" + getTypeClass()
				+ "]";
	}

	public Double getThreshold() {
		return threshold;
	}
}
