package com.jm.rbreports.BalanceSheet.parser;

import java.util.ArrayList;
import java.util.List;

import com.jm.rbreports.BalanceSheet.model.formula.AbstractExpression;
import com.jm.rbreports.BalanceSheet.model.formula.BinaryDivision;
import com.jm.rbreports.BalanceSheet.model.formula.Constant;
import com.jm.rbreports.BalanceSheet.model.formula.NAryMultiplication;
import com.jm.rbreports.BalanceSheet.model.formula.NAryPlus;
import com.jm.rbreports.BalanceSheet.model.formula.RowLiteral;
import com.jm.rbreports.BalanceSheet.model.formula.UnaryMinus;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 * 2017-03-27	V1.1	jwaechter	- Added usedWithinBalanceLine for
 *                                    RowLiteral
 *                                  - Added tracking if parsing in context
 *                                    of a sum.
 */

/**
 * Class capable of transforming an input string into an in memory model of the expression 
 * utilizing the classes in the com.jm.rpreports.BalanceSheet.model.formula package. 
 * <br/>
 * The grammar described allows arithmetic expressions in prefix notation as well as row literals.
 * <br/> <br/>
 * The grammar of the expression being processed is the following: <br/>
 * Expression 			--> Constant | RowLiteral | UnaryMinus | BinaryDivision | NAryOperator  <br/>
 * Constant   			--> (everything being parsed by {@link Double#parseDouble(String)})  <br/>
 * RowLiteral 			--> RNumber <br/>
 * Number     			--> (everything being parsed by {@link Integer#parseInt(String)}) <br/>
 * UnaryMinus     		--> -Expression  <br/>
 * BinaryDevision 		--> /(Expression, Expression) <br/>
 * NAryOperator			--> NAryMultiplication | NAryPlus <br/>
 * NAryMultiplication	--> *(Expression, Expression, ... ,Expression) <br/>
 * NAryPlus				--> +(Expression, Expression, ..., Expression) <br/>
 * 
 * Examples of valid expressions:
 * <ul>
 *   <li> 3.141 </li>
 *   <li> -3.141 </li>
 *   <li> R23 </li>
 *   <li> +(R23, 3.141) </li>
 *   <li> +(R23, 3.141, R24, R33) </li>
 *   <li> *(R1, 100) </li>
 *   <li> /(R23,R4)</li>
 *   <li> +(/(R34, R35), *(+(R1, 2000), R34, /(1,R33)))</li>
 *  <ul>
 * @author jwaechter
 * @version 1.1
 * 
 * @see AbstractExpression
 * @see BinaryDivision
 * @see Constant
 * @see NAryOperator
 * @see NAryMultiplication
 * @see NAryPlus
 * @see UnaryMinus
 * @see RowLiteral
 * @see Constant
 */
public class Parser {
	public static final char OPENING_BRACKET='(';
	public static final char CLOSING_BRACKET=')';
	public static final char ROW_LITERAL_PREFIX='R';
	
	public static final String OPERATOR_PLUS  = "+";
	public static final String OPERATOR_MINUS = "-";
	public static final String OPERATOR_MULTIPLICATION = "*";
	public static final String OPERATOR_DIVISION = "/";
	
	/**
	 * Helper class containing meta information about the input string as well as some helper
	 * methods to split the input string. This is the base for rapid parsing of input strings.
	 * @author jwaechter
	 * @version 1.0
	 */
	private static class InputSeparator {
		private final String input;
		/**
		 * Array of ints (one to one mapping to the characters of the input string) 
		 * with bracketCountPerChar[i] denoting the number of unclosed open brackets <= i 
		 */
		private final int[] bracketCountPerChar; 
		/**
		 * True if the input string has any brackets
		 */
		private final boolean hasBrackets;
		/**
		 * The highest number on unclosed brackets (basically max over i).
		 */
		private int maxBracketLevel=0;
		/**
		 * true if there is neither a negative count of unclosed brackets and 
		 * if the last character of input has a bracket count of 0
		 */
		private boolean validBracketExpression=true;
		private int firstRowLiteralPrefix=-1;
		private int lastRowLiteralPrefix=-1;
		
		public InputSeparator (final String input) {
			this.input = input;
			bracketCountPerChar = new int[input.length()];
			int bracketCount=0;
			for (int i = 0; i < input.length(); i++) {
				char at = input.charAt(i);
				if (at == OPENING_BRACKET) {
					bracketCount++;
					if (maxBracketLevel < bracketCount) {
						maxBracketLevel = bracketCount;
					}
				} else if (at == CLOSING_BRACKET) {
					bracketCount--;
					if (bracketCount < 0) {
						validBracketExpression = false;
					}
				}
				if (at == ROW_LITERAL_PREFIX && firstRowLiteralPrefix == -1) {
					firstRowLiteralPrefix = i;
				}
				if (at == ROW_LITERAL_PREFIX) {
					lastRowLiteralPrefix = i;
				}
				bracketCountPerChar[i] = bracketCount;
			}
			hasBrackets = maxBracketLevel>0;
			if (bracketCountPerChar[input.length()-1] != 0) {
				validBracketExpression = false;
			}
		}
		
		/**
		 * Retrievies a list of strings that are having the same bracket count.
		 * The list contains more than one element in case there is separation based
		 * on other chars having a different bracket level in between, e.g.
		 * for "+(R1, /(1, 2), R3)" with (1, 2) not being part of bracket level 1
		 * @param bracketLevel
		 * @return
		 */
		public List<String> getBracketLevelTokens(int bracketLevel) {
			List<String> tokens = new ArrayList<>();
			StringBuilder sb = new StringBuilder ();
			int lastCharAtBracketLevel = -1;
			for (int i=0; i<input.length(); i++) {
				if (bracketCountPerChar[i] == bracketLevel) {
					if (lastCharAtBracketLevel == i-1) {
						sb.append(input.charAt(i));
					} 
					lastCharAtBracketLevel = i;
				} else {
					if (lastCharAtBracketLevel == i-1 && lastCharAtBracketLevel != -1) {
						tokens.add(sb.toString());
						sb = new StringBuilder();	
					}
				}
			}
			if (sb.length() > 0) {
				tokens.add(sb.toString());
			}
			return tokens;
		}
		
		/**
		 * Returns a list of lists of strings. The list of lists returns separated blocks of characters
		 * having at least the bracket count stated in bracket level and the sublists contains texts matching
		 * the bracket count but are separated by splitOn 
		 * @param bracketLevel
		 * @param splitOn
		 * @return
		 */
		public List<List<String>> splitBracketLevelTokens(int bracketLevel, char splitOn) {
			List<List<String>> lists = new ArrayList<>();
			List<String> tokens = new ArrayList<>();
			StringBuilder sb = new StringBuilder ();
			int lastCharAtBracketLevel = -1;
			for (int i=0; i<input.length(); i++) {
				if (bracketCountPerChar[i] >= bracketLevel ) {
					if (lastCharAtBracketLevel == i-1) {
						if ((bracketLevel == bracketCountPerChar[i] && input.charAt(i) != splitOn) 
							||bracketCountPerChar[i] > bracketLevel) {
							sb.append(input.charAt(i));							
						}
					}
					lastCharAtBracketLevel = i;
					if (bracketCountPerChar[i] == bracketLevel && input.charAt(i) == splitOn) {
						tokens.add(sb.toString());
						sb = new StringBuilder();
					}
				} else {
					if (lastCharAtBracketLevel == i-1 && lastCharAtBracketLevel != -1) {
						tokens.add(sb.toString());
						lists.add(tokens);
						sb = new StringBuilder();
						tokens = new ArrayList<>();
					}
				}
			}
			if (sb.length() > 0) {
				tokens.add(sb.toString());
				lists.add(tokens);
			}
			return lists;
		}		

	}
	
	/**
	 * The table used to look up the row literals. This is not used
	 * by the parser, but has to be passed on to the expressions to allow the use
	 * of the {@link AbstractExpression#evaluate(int)} and {@link AbstractExpression#canEvaluate()} 
	 * methods.
	 */
	private final Table dataSources;
	
	/**
	 * Initializes the parser. The table dataSources is being passed on to the
	 * expressions being created by the parser to allow evaluation of row literals.
	 * @param dataSources
	 */
	public Parser (final Table dataSources) {
		this.dataSources = dataSources;
	}
	
	/**
	 * Transforms the formula into a meta model of an expression for later manipulation and
	 * evaluation. For evaluation of the row literals, the expressions generated by this instance
	 * are using the table provided in the constructor.
	 * Note this method is being called recursively.
	 * @param formula
	 * @param usedWithinBalanceLine the balance line the formula belongs to.
	 * @param useSummation set to true in case the row literals are considered sums of all rows matching the 
	 * given row ID. Example:
	 * If you have 3 rows having row ID 1, a reference to R1 is evaluated to R1 + R1 + R1 
	 * (for all three R1 rows) in case of true, but to a single R1 only in case of 
	 * @param isSummationContext tracks if the expression is part of a summation context.
	 * When initially triggering the parse it has to be set to true.
	 * @return
	 * @throws OException
	 */
	public AbstractExpression parse(final String formula, final int usedWithinBalanceLine, 
			final boolean useSummation, final boolean isSummationContext) throws OException {
		InputSeparator is = new InputSeparator(formula);
		if (!is.validBracketExpression) { // checks if the brackets in formula confirm to superficial criteria
			throw new OException ("Error parsing '" + formula + "': opening/closing bracket do not match");
		}
		// constant, row literal, negation?
		AbstractExpression parsedExpr = parseBracketLessExpressions(formula, is, 
				usedWithinBalanceLine, useSummation, isSummationContext);
		if (parsedExpr == null) { // if null, it's neither constant, row literal or negation.
			// check if it's division, addition or multiplication
			parsedExpr = parseExpressionWithBrackets(formula, is, usedWithinBalanceLine, useSummation,
					isSummationContext); 
		}
		return parsedExpr;
	}

	private AbstractExpression parseExpressionWithBrackets(String formula,
			InputSeparator is, final int usedWithinBalanceLine,
			boolean useSummation, boolean isSummationContext) throws OException {
		{ // neither literal nor constant nor minus
			List<String> tokens = is.getBracketLevelTokens(0);
			if (tokens.size() > 1) { // this happens if the input is like this: "+(R1, R2)+(R1,R2)"
				throw new OException ("Error parsing '" + formula + "': check for missing commas or missing closing brackets");
			}
			String possibleOperator = tokens.get(0).trim();
			List<List<String>> minLevel1Tokens = is.splitBracketLevelTokens(1, ',');
			if (minLevel1Tokens.size() == 0) {
				throw new OException ("Error parsing '" + formula + "': no operands defined");
			}
			List<String> firstSameLevelGroup = minLevel1Tokens.get(0);
			for (int opNum = 0; opNum < minLevel1Tokens.size(); opNum++) {
				firstSameLevelGroup.set(opNum, firstSameLevelGroup.get(opNum).trim());
			}
			switch (possibleOperator) {
			case OPERATOR_PLUS:
				return processPlus(formula, firstSameLevelGroup, usedWithinBalanceLine, useSummation, isSummationContext);
			case OPERATOR_MULTIPLICATION:
				return processMultiplication(formula, firstSameLevelGroup, usedWithinBalanceLine, useSummation, false);
			case OPERATOR_DIVISION:
				return processDivision(formula, firstSameLevelGroup, usedWithinBalanceLine, useSummation, false);
		default:
				throw new OException ("Error parsing '" + formula + "': " + possibleOperator + " is not "
						+ " a known operator ( one of " + getAllowedOperatorList() + ")" );
			}
		}
	}

	private AbstractExpression processPlus(String formula,
			List<String> minLevel1Tokens, int usedWithinBalanceLine,
			boolean useSummation, boolean isSummationContext) throws OException {
		AbstractExpression[] operands = new AbstractExpression[minLevel1Tokens.size()];
		for (int opNum = 0; opNum < operands.length; opNum++) {
			AbstractExpression operand = null;
			try {
				operand = parse(minLevel1Tokens.get(opNum),usedWithinBalanceLine, useSummation, isSummationContext);
			} catch (Throwable t) {
				throw new OException ("Error parsing '" + formula + "' operand #" + (opNum+1) + " can't be parsed\n " + t.toString());
			}
			operands[opNum] = operand;
		}
		return new NAryPlus(dataSources, operands);
	}
	
	private AbstractExpression processMultiplication(String formula,
			List<String> minLevel1Tokens, 
			int usedWithinBalanceLine, boolean useSummation,
			boolean isSummationContext) throws OException {
		AbstractExpression[] operands = new AbstractExpression[minLevel1Tokens.size()];
		for (int opNum = 0; opNum < operands.length; opNum++) {
			AbstractExpression operand = null;
			try {
				operand = parse(minLevel1Tokens.get(opNum), usedWithinBalanceLine, useSummation, false);
			} catch (Throwable t) {
				throw new OException ("Error parsing '" + formula + "' operand #" + (opNum+1) + " can't be parsed\n " + t.toString());
			}
			operands[opNum] = operand;
		}
		return new NAryMultiplication(dataSources, operands);
	}


	private AbstractExpression processDivision(String formula,
			List<String> minLevel1Tokens, 
			int usedWithinBalanceLine, boolean useSummation,
			boolean isSummationContext) throws OException {
		if (minLevel1Tokens.size() != 2) {
			throw new OException ("Error parsing '" + formula + "': Division has to be a binary operator");
		}
		AbstractExpression dividend = null;
		AbstractExpression divisor = null;
		try {
			dividend = parse(minLevel1Tokens.get(0), usedWithinBalanceLine, useSummation, isSummationContext);
		} catch (Throwable t) {
			throw new OException ("Error parsing '" + formula + "' first operand can't be parsed\n " + t.toString() );
		}
		try {
			divisor = parse(minLevel1Tokens.get(1), usedWithinBalanceLine, useSummation, false);
		} catch (Throwable t) {
			throw new OException ("Error parsing '" + formula + "' second operand can't be parsed\n " + t.toString() );
		}
		return new BinaryDivision(dataSources, dividend, divisor);
	}

	private AbstractExpression parseBracketLessExpressions(String formula, InputSeparator is,
			int usedWithinBalanceLine,
			boolean useSummation, boolean isSummationContext)
			throws OException {
		if (is.maxBracketLevel == 0) { // no brackets, can be either row literal or constant
			if (formula.trim().startsWith(OPERATOR_MINUS)) { // maybe a minus
				String afterMinus = formula.substring(formula.indexOf(OPERATOR_MINUS)+1);
				AbstractExpression expr = null;
				try {
					expr = parse(afterMinus, usedWithinBalanceLine, useSummation, isSummationContext);
				} catch (Throwable ex) {
					throw new OException ("Can't parse '" + formula + "' as no valid expression found after "
							+ OPERATOR_MINUS + ". \nDetails\n" + ex.toString());
				}
				return new UnaryMinus(dataSources, expr);
			}
			if (is.firstRowLiteralPrefix != -1) { // maybe a row literal
				if (is.lastRowLiteralPrefix != is.firstRowLiteralPrefix) {
					throw new OException ("Can't parse '" + formula + "' as it violates the syntax for row literals");
				}
				String rowIdUnparsed = formula.substring(is.firstRowLiteralPrefix+1).trim();
				int rowId = -1;
				try {
					rowId = Integer.parseInt(rowIdUnparsed);
				} catch (NumberFormatException nfe) {
					throw new OException ("Can't parse '" + formula + "' is not a valid integer after the row literal prefix");
				}
				return new RowLiteral(dataSources, rowId, usedWithinBalanceLine, useSummation, isSummationContext);
			} else { // maybe a constant
				double constant = Double.NaN;
				try {
					constant = Double.parseDouble(formula.trim());
				} catch (NumberFormatException nfe) {
					throw new OException ("Can't parse '" + formula + "' as it is not a double");
				}
				return new Constant(dataSources, constant);
			}
		}
		return null;
	}
	
	public static String getAllowedOperatorList() {
		return "(" + OPERATOR_MINUS + "(unary), " + OPERATOR_PLUS + "(n-ary), " + OPERATOR_DIVISION 
				+ "(binary), " + OPERATOR_MULTIPLICATION + " (n-ary))";
	}

	
}
