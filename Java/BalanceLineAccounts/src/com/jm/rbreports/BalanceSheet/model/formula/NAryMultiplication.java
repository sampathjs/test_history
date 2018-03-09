package com.jm.rbreports.BalanceSheet.model.formula;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 */


/**
 * Represents a multiplication of an n expressions, with n >=1
 * @author jwaechter
 * @version 1.0
 */
public class NAryMultiplication extends NAryOperator{
	public NAryMultiplication (final Table dataSources, final AbstractExpression... operands) {
		super (dataSources, operands);
	}
	
	@Override
	public double evaluate(int col, int row) throws OException {
		double product = 1.0d;
		for (AbstractExpression operand : operands) {
			product *= operand.evaluate(col, row);
		}
		return product;
	}
	
	@Override
	public String toString() {
		String str = "+(";
		boolean isFirst=true;
		for (AbstractExpression expr : operands ) {
			if (!isFirst) {
				str += ", ";
			}
			str += expr.toString();
			isFirst=false;
		}
		str += ")";
		return str;
	}	
}
