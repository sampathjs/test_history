package com.jm.rbreports.BalanceSheet.model.formula;

import java.util.Set;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial version
 */

/**
 * Abstract base class for meta model of formulas. It ensure that every subclass 
 * has access to the datasource and provides the three methods
 * {@link #canEvaluate()}, {@link #evaluate(int)} and {@link #getReferencedRows()}
 * @author WaechJ01
 * @version 1.0
 */
public abstract class AbstractExpression {
	/**
	 * Report data. 
	 */
	protected final Table dataSource;	
	
	/**
	 * 
	 * @param dataSource must have the column balance_line_id of type integer
	 */
	public AbstractExpression (final Table dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 
	 * @return
	 * @throws OException
	 */
	public abstract boolean canEvaluate () throws OException;
	
	public abstract double evaluate (int col, int row) throws OException;
	
	public abstract Set<Integer> getReferencedRows();
}
