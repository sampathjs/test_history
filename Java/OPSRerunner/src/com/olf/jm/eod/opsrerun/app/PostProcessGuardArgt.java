package com.olf.jm.eod.opsrerun.app;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/*
 * History:
 * 2016-10-07	V1.0	jwaechter	- Initial Version
 * 2016-10-10	V1.1	jwaechter	- added retryCountIgnore
 *
 */

/**
 * Class containing the arguments exchanged between param and main script
 * of the post process guard.
 * @author jwaechter
 * @version 1.0
 */
public class PostProcessGuardArgt {
	public static final String ARGT_PARAM_CANCELLED = "Cancelled";
	public static final String ARGT_PARAM_ITEM_LIST = "OpsDefList";
	public static final String ITEM_LIST_OPS_DEF_NAME = "OpsName";
	public static final String ITEM_LIST_ADDITIONAL_QUERY = "AdditionalQuery";
	public static final String ARGT_PARAM_RETRY_COUNT = "RetryCount";
	public static final String ARGT_PARAM_TIMESPAN = "Timespan";
	public static final String ARGT_PARAM_RETRY_COUNT_IGNORE = "RetryCountIgnore";

	private final boolean cancelled;
	private final List<Item> items = new ArrayList<>();
	private final int retryCount;
	private final int retryCountIgnore;
	private final int timespan;
	
	public PostProcessGuardArgt (final boolean cancelled, 
				final int retryCount, final int timespan, final int retryCountIgnore) {
		this.cancelled = cancelled;
		this.retryCount = retryCount;
		this.timespan = timespan;
		this.retryCountIgnore = retryCountIgnore;
	}
	
	/**
	 * Generates an argt object out of the argument table.
	 * 
	 * @param argt had been produced by {@link #toTable()}
	 * @throws OException 
	 */
	public PostProcessGuardArgt (final Table argt) throws OException {
		if (argt.getNumRows() != 1) {
			throw new OException ("Argt for PostProcessGuard has invalid format: more or less than 1 row");
		}
		if (argt.getColNum(ARGT_PARAM_CANCELLED) < 1) {
			throw new OException ("Argt for PostProcessGuard has invalid format: missing column " 
					+ ARGT_PARAM_CANCELLED);
		}
		if (argt.getColNum(ARGT_PARAM_ITEM_LIST) < 1) {
			throw new OException ("Argt for PostProcessGuard has invalid format: missing column " 
					+ ARGT_PARAM_ITEM_LIST);
		}
		if (argt.getColNum(ARGT_PARAM_RETRY_COUNT) < 1) {
			throw new OException ("Argt for PostProcessGuard has invalid format: missing column " 
					+ ARGT_PARAM_RETRY_COUNT);
		}
		this.cancelled = argt.getInt(ARGT_PARAM_CANCELLED, 1) == 1;
		this.retryCount = argt.getInt(ARGT_PARAM_RETRY_COUNT, 1);
		this.retryCountIgnore = argt.getInt(ARGT_PARAM_RETRY_COUNT_IGNORE, 1);
		this.timespan = argt.getInt(ARGT_PARAM_TIMESPAN, 1);
		Table itemTable = argt.getTable(ARGT_PARAM_ITEM_LIST, 1);
		if (itemTable.getColNum(ITEM_LIST_OPS_DEF_NAME) < 1) {
			throw new OException ("Argt for PostProcessGuard has invalid format: missing column " 
					+ ITEM_LIST_OPS_DEF_NAME + " within item list table");
		}
		if (itemTable.getColNum(ITEM_LIST_ADDITIONAL_QUERY) < 1) {
			throw new OException ("Argt for PostProcessGuard has invalid format: missing column " 
					+ ITEM_LIST_ADDITIONAL_QUERY  + " within item list table");
		}
		for (int row = itemTable.getNumRows(); row >= 1; row--) {
			String opsName = itemTable.getString(ITEM_LIST_OPS_DEF_NAME, row);
			String queryName = itemTable.getString(ITEM_LIST_ADDITIONAL_QUERY, row);
			Item item = new Item (opsName, queryName);
			items.add(item);
		}
	}
	
	
	public void addItem (final String opsName,
			final String queryName) {
		items.add(new Item(opsName, queryName));
	}
	
	/**
	 * Generates a table containing the data of this instance.
	 * @return
	 * @throws OException
	 */
	public Table toTable () throws OException {
		Table argt = Table.tableNew("Argt for PostProcessGuard");
		argt.addCol(ARGT_PARAM_CANCELLED, COL_TYPE_ENUM.COL_INT);
		argt.addCol(ARGT_PARAM_ITEM_LIST, COL_TYPE_ENUM.COL_TABLE);
		argt.addCol(ARGT_PARAM_RETRY_COUNT, COL_TYPE_ENUM.COL_INT);
		argt.addCol(ARGT_PARAM_TIMESPAN, COL_TYPE_ENUM.COL_INT);
		argt.addCol(ARGT_PARAM_RETRY_COUNT_IGNORE, COL_TYPE_ENUM.COL_INT);
		argt.addRow();
		argt.setInt(ARGT_PARAM_CANCELLED, 1, cancelled?1:0);
		argt.setInt(ARGT_PARAM_RETRY_COUNT, 1, retryCount);
		argt.setInt(ARGT_PARAM_TIMESPAN, 1, timespan);
		argt.setInt(ARGT_PARAM_RETRY_COUNT_IGNORE, 1, retryCountIgnore);
		Table itemList = Table.tableNew("Item List of Argt for PostProcessGuard");
		itemList.addCol(ITEM_LIST_OPS_DEF_NAME, COL_TYPE_ENUM.COL_STRING);
		itemList.addCol(ITEM_LIST_ADDITIONAL_QUERY, COL_TYPE_ENUM.COL_STRING);
		for (Item item : items) {
			int row = itemList.addRow();
			itemList.setString(ITEM_LIST_OPS_DEF_NAME, row, item.getOpsName());
			itemList.setString(ITEM_LIST_ADDITIONAL_QUERY, row, item.getAdditionalQuery());
		}
		argt.setTable(ARGT_PARAM_ITEM_LIST, 1, itemList);
		return argt;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public List<Item> getItems() {
		return items;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public int getRetryCountIgnore() {
		return retryCountIgnore;
	}
}
