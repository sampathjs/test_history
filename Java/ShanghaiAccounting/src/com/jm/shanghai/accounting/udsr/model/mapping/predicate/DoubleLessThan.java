package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

/*
 * History:
 * 2018-11-24		V1.0	jwaechter		- Initial Version
 */

/**
 * Predicate implementing a comparison for < on doubles.
 * @author jwaechter
 * @version 1.0
 */
public class DoubleLessThan extends AbstractPredicate<Double>{
	private double threshold;
	
	public DoubleLessThan(String unparsedPredicate,
			int weight) {
		super(Double.class, unparsedPredicate, true, weight);
		parseDoublePredicate(unparsedPredicate);
	}

	@Override
	public boolean evaluate(String input) {
		double parsedInput = parseDoubleInput(input);
		return parsedInput < threshold;
	}
	
	private double parseDoublePredicate(String unparsedPredicate) {
		String trimmedPredicate = unparsedPredicate.trim();
		String unparsedRightOperand = trimmedPredicate.substring(1).trim();
		String formattedRightOperand = unparsedRightOperand.replaceAll(",", "");
		try {
			return Double.parseDouble(formattedRightOperand);
		} catch (NumberFormatException ex) {
			throw new RuntimeException ("Could not parse predicate '" + unparsedPredicate 
					+ "'. The right operand '" + formattedRightOperand 
					+ "' could not be parsed as a double");
		}
	}
	
	private double parseDoubleInput(String input) {
		String trimmedInput = input.trim();
		String formattedLeftOperand = trimmedInput.replaceAll(",", "");
		try {
			return Double.parseDouble(formattedLeftOperand);
		} catch (NumberFormatException ex) {
			throw new RuntimeException ("Could not parse input '" + input 
					+ "' while applying predicate '" + this.toString() 
					+ "'. The left operand '" + formattedLeftOperand
					+ "' could not be parsed as a double");
		}
	}

	@Override
	public String toString() {
		return "DoubleLessThan [threshold=" + threshold + ", getWeight()="
				+ getWeight() + ", getUnparsedPredicate()="
				+ getUnparsedPredicate() + ", getTypeClass()=" + getTypeClass()
				+ "]";
	}

	public Double getThreshold() {
		return threshold;
	}
}
