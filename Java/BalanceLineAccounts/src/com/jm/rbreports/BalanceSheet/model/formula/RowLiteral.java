package com.jm.rbreports.BalanceSheet.model.formula;

import java.util.HashSet;
import java.util.Set;

import com.jm.rbreports.BalanceSheet.RowLiteralSumTrack;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 * 2017-03-27	V1.1	jwaechter	- Added usedWithinBalanceLine
 */


/**
 * A row literal, representing a reference to a row having a certain row id as idenficator. 
 * Note it's possible to have multiple rows having row id in the data source table. 
 * If the row literal is executed in summation mode, it sums up the value of all rows
 * having the given row id, if it's not executed in summation mode it takes the value of
 * the row provided by the caller to the evaluate method.
 * @author jwaechter
 * @version 1.1
 */
public class RowLiteral extends AbstractExpression {
	/**
	 * the row id identificator
	 */
	final int balanceLineId;
	
	/**
	 * The balance line the formula is used within.
	 */
	final int usedWithinBalanceLine;
	final boolean useSummation;
	
	/**
	 * States if this formula is used within the context of an addition
	 * with respect of the complete line formula.
	 */
	final boolean isSummationContext;
	
	
	
	public RowLiteral (final Table dataSource, final int balanceLineId, 
			final int usedWithinBalanceLine,
			final boolean useSummation,
			final boolean isSummationContext) {
		super (dataSource);
		this.balanceLineId = balanceLineId;
		this.useSummation = useSummation;
		this.usedWithinBalanceLine = usedWithinBalanceLine;
		this.isSummationContext = isSummationContext;
	}

	@Override
	public boolean canEvaluate() throws OException {
		int row = findBalanceLineRow();
		
		String formula = dataSource.getString("formula", row);
		if (formula == null || formula.trim().length() == 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * If in summation mode returns the sum of the value of col for all rows
	 * having the row id provided in the constructor, 
	 * if not in summation mode, returns the value of col in row "srcRow"
	 */
	@Override
	public double evaluate(int col, int srcRow) throws OException {
		double sum = 0.0;
		
		if (balanceLineId == 63) {
			PluginLog.info("match");			
		}
		
		if (isSummationContext && RowLiteralSumTrack.getInstance().
				isRowLiteralAlreadyInSumOfBalanceLine(usedWithinBalanceLine,
				balanceLineId) &&
				balanceLineId != usedWithinBalanceLine) {
			return 0.0;
		}
		int row = findBalanceLineRow();
		if (!useSummation) {
			row = srcRow;
		}
		while (dataSource.getInt("balance_line_id", row) ==  balanceLineId) {
			sum += dataSource.getDouble(col, row);		
			if (!useSummation) {
				break;
			}
			row--;
		}
		if (isSummationContext) {
			RowLiteralSumTrack.getInstance().addRowLiteralInSumContext(balanceLineId);
		}
		return sum;
	}


	private int findBalanceLineRow() throws OException {
		int row = dataSource.findInt("balance_line_id", balanceLineId, SEARCH_ENUM.LAST_IN_GROUP);
		if (row <= 0) {
			throw new OException("Balance Line ID " + balanceLineId + " not found in table " +
					dataSource.getTableName());
		}
		return row;
	}
	
	@Override
	public String toString() {
		return "R" + balanceLineId;
	}	
	
	public Set<Integer> getReferencedRows() {
		Set<Integer> refRows = new HashSet<Integer>();
		refRows.add(balanceLineId);
		return refRows;
	}

}
