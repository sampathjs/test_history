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
 * An operator having n expressions as arguments with n >= 1
 * @author jwaechter
 * @version 1.0
 */
public abstract class NAryOperator extends AbstractExpression {
	final protected AbstractExpression[] operands;
	
	public NAryOperator (final Table dataSources, final AbstractExpression...operands) {
		super (dataSources);
		this.operands = operands;
	}
	
	@Override
	public boolean canEvaluate() throws OException {
		boolean canEvaluate = true;
		for (AbstractExpression expr : operands) {
			canEvaluate &= expr.canEvaluate();
		}
		return canEvaluate;
	}
	
	public Set<Integer> getReferencedRows() {
		Set<Integer> refRows = new HashSet<Integer>();
		for (AbstractExpression operand : operands) {
			refRows.addAll(operand.getReferencedRows());
		}
		return refRows;
	}
}
