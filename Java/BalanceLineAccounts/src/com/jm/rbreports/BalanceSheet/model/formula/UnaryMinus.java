package com.jm.rbreports.BalanceSheet.model.formula;

import java.util.HashSet;
import java.util.Set;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 */


/**
 * A simple negative of a given expression.
 * @author jwaechter
 * @version 1.0
 */
public class UnaryMinus extends AbstractExpression {
	final AbstractExpression expr;
	
	public UnaryMinus (final Table dataSources, final AbstractExpression expr) {
		super (dataSources);
		this.expr = expr;
	}
	
	@Override
	public boolean canEvaluate() throws OException {
		return expr.canEvaluate();
	}

	@Override
	public double evaluate(int col, int row) throws OException {
		return -expr.evaluate(col, row);
	}
	
	@Override
	public String toString() {
		return "-" + expr.toString();
	}
	
	public Set<Integer> getReferencedRows() {
		return expr.getReferencedRows();
	}

}
