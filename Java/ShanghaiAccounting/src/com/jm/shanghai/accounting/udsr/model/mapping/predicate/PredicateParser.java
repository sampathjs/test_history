package com.jm.shanghai.accounting.udsr.model.mapping.predicate;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2019-01-02	V1.0	jwaechter		- Initial Version
 */

/**
 * Provides the necessary methods to parse predicates.
 * @author jwaechter
 * @version 1.0
 */
public class PredicateParser {
	public static AbstractPredicate parsePredicate(EnumColType colType,
			String unparsedPredicate) {
		Class<?> classForColType = getClassForColType(colType);
		// KleeneStar
		if (unparsedPredicate.trim().equals("*")) {
			return new KleeneStar(classForColType, unparsedPredicate, 20);
		}
		// insert predicates that contain designators like operators here 
		
		// less than or equals predicates 
		if (unparsedPredicate.contains("<=") && colType == EnumColType.Double) {
			return new DoubleLessThanOrEquals(unparsedPredicate, 8);
		} else if (unparsedPredicate.contains("<=") && colType == EnumColType.Int) {
			return new IntegerLessThanOrEquals(unparsedPredicate, 8);
		}
		
		// less than predicates 
		if (unparsedPredicate.contains("<") && colType == EnumColType.Double) {
			return new DoubleLessThan(unparsedPredicate, 7);
		} else if (unparsedPredicate.contains("<") && colType == EnumColType.Int) {
			return new IntegerLessThan(unparsedPredicate, 7);
		}

		// greater than or equals predicates 
		if (unparsedPredicate.contains(">=") && colType == EnumColType.Double) {
			return new DoubleGreaterThanOrEquals(unparsedPredicate, 8);
		} else if (unparsedPredicate.contains(">=") && colType == EnumColType.Int) {
			return new IntegerGreaterThanOrEquals(unparsedPredicate, 8);
		}
		
		// greater than predicates 
		if (unparsedPredicate.contains(">") && colType == EnumColType.Double) {
			return new DoubleGreaterThan(unparsedPredicate, 7);
		} else if (unparsedPredicate.contains(">") && colType == EnumColType.Int) {
			return new IntegerGreaterThan(unparsedPredicate, 7);
		}
		
		// list of alternatives
		if (!unparsedPredicate.contains("!") 
				&& (colType == EnumColType.String || colType == EnumColType.Int) ) {
			return new StringAlternatives (unparsedPredicate, 2);
		} else if (unparsedPredicate.contains("!") // list of exclusions
				&& (colType == EnumColType.String || colType == EnumColType.Int) ) { 
			return new StringExclusion (unparsedPredicate, 15);
		}
		throw new RuntimeException ("Could not parse predicate '" + unparsedPredicate + "'");
	}

	private static Class getClassForColType(EnumColType colType) {
		Class classForColType;
		switch (colType) {
		case Int:
			classForColType = Integer.class;
			break;
		case Long:
			classForColType = Long.class;
			break;
		case Double:
			classForColType = Double.class;
			break;
		case String:
			classForColType = String.class;
			break;
		default:
			throw new RuntimeException ("The class to EnumColType mapping is not defined for "
					+ colType);
		}
		return classForColType;
	}
}
