package com.jm.rbreports.BalanceSheet.model.formula;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 */


/**
 * An addition of n expressions with n >= 1
 * @author jwaechter
 * @version 1.0
 */
public class NAryPlus extends NAryOperator {
	
	public NAryPlus (final Table dataSources, final AbstractExpression... arguments) {
		super (dataSources, arguments);
	}
	
	@Override
	public double evaluate(int col, int row) throws OException {
		double sum = 0.00d;
		for (AbstractExpression operand : operands) {
			sum += operand.evaluate(col, row);
		}
		return sum;
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
			isFirst = false;
		}
		str += ")";
		return str;
	}	

}
