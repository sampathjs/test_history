package com.jm.rbreports.BalanceSheet;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * History:
 * 2017-03-27	- V1.0	- jwaechter	- Initial Version
 */

/**
 * Cache containing the information if a certain row literal has already been included in 
 * a sum of another balance line.
 * @author jwaechter
 * @version 1.0
 */
public class RowLiteralSumTrack {	
	private static RowLiteralSumTrack singleton=null;
	
	private Map<Integer, Set<Integer>> finalizedRows; 
	private Set<Integer> currentRowData;
	private int currentBalanceLineId=-1;
	
	public boolean isRowLiteralAlreadyInSumOfBalanceLine (int balanceLineId,
			int rowLiteralBalanceLineId) {
		Set<Integer> finRowLits = finalizedRows.get(balanceLineId);
		if (finRowLits == null) {
			return false;
		}
		return finRowLits.contains(rowLiteralBalanceLineId);
	}
	
	public void newBalanceLine (int balanceLineId) {
		finalizedRows.put(currentBalanceLineId  , currentRowData);
		if (finalizedRows.containsKey(balanceLineId)) {
			currentRowData = finalizedRows.get(balanceLineId);
		} else {
			currentRowData = new TreeSet<>();			
		}
		currentBalanceLineId = balanceLineId;
	}
	
	public void addRowLiteralInSumContext (int rowLiteralBalanceLineId) {
		currentRowData.add(rowLiteralBalanceLineId);
	}
	
	/**
	 * Clears all existing data in the cache and the current row staging area.
	 */
	public void clear () {
		finalizedRows.clear();
		currentRowData.clear();
		currentBalanceLineId = -1;
	}
	
	/**
	 * Retrieves the one and only instance of the cache.
	 * @return
	 */
	public static RowLiteralSumTrack getInstance () {
		if (singleton == null) {
			singleton = new RowLiteralSumTrack();
		}
		return singleton;
	}
	
	private RowLiteralSumTrack() {
		finalizedRows = new TreeMap<> (); 
		currentRowData = new TreeSet<Integer>();
	}

}
