package com.jm.rbreports.BalanceSheet.model.formula;

import java.util.Set;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 */

/**
 * Represents a division (dividend/divisor)
 * @author jwaechter
 * @version 1.0
 */
public class BinaryDivision extends AbstractExpression {
	private final AbstractExpression dividend;
	private final AbstractExpression divisor;
	
	public BinaryDivision(final Table dataSources, 
			final AbstractExpression dividend, final AbstractExpression divisor) {
		super (dataSources);
		this.dividend = dividend;
		this.divisor = divisor;
	}
	
	@Override
	public boolean canEvaluate() throws OException {
		return dividend.canEvaluate() && divisor.canEvaluate();
	}

	@Override
	public double evaluate(int col, int row) throws OException {
		double divid=dividend.evaluate(col, row);
		double divis=divisor.evaluate(col, row);
		if (divis != 0) {
			return divid/divis;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "(" + dividend + "/" + divisor
				+ ")";
	}

	@Override
	public Set<Integer> getReferencedRows() {
		Set<Integer> refRows =  dividend.getReferencedRows();
		refRows.addAll(divisor.getReferencedRows());
		return refRows;
	}	
	
	
}
