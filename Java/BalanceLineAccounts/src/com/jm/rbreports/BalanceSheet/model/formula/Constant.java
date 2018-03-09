package com.jm.rbreports.BalanceSheet.model.formula;

import java.util.HashSet;
import java.util.Set;

import com.olf.openjvs.Table;

/*
 * History:
 * 2016-03-09	V1.0	jwaechter	- Initial Version
 */


/**
 * Represents  a constant withing a formula - a concrete number that can be represented as double.
 * @author jwaechter
 * @version 1.0
 */
public class Constant extends AbstractExpression {
	final double constant;
	
	public Constant (final Table dataSource, final double constant) {
		super(dataSource);
		this.constant = constant;
	}
	
	@Override
	public boolean canEvaluate() {
		return true;
	}

	@Override
	public double evaluate(int col, int row) {
		return constant;
	}

	@Override
	public String toString() {
		return ""+constant;
	}

	@Override
	public Set<Integer> getReferencedRows() {
		return new HashSet<Integer>();
	}	
}
