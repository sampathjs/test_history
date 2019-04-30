package com.matthey.openlink.jde_extract;

import java.util.Vector;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;

public interface IJdeDataManager {

	/**
	 * Process the deals passed in, generate "JDE Extract Data" UDSR output for them, and send it to staging area table
	 * @param argt - table that has a "tran_num" column, and a list of transaction numbers, one per each row
	 * @throws OException
	 */
	public abstract void processDeals(Table argt) throws OException;

	/**
	 * Indicates if a given transaction will contribute to JDE extract, and hence needs to be processed
	 * @param trn
	 * @return - Does the transaction need to be processed by JDE Extract Data Manager
	 * @throws OException
	 */
	public abstract boolean needToProcessTransaction(Transaction trn)
			throws OException;

	/**
	 * Process the deals passed in, generate "JDE Extract Data" UDSR output for them, and send it to staging area table
	 * @param tranNums - Vector of tran numbers
	 * @throws OException
	 */
	public abstract void processDeals(Vector<Integer> tranNums)
			throws OException;

}