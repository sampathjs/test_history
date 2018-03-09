package com.olf.migration.pricing_fixing.model;

/*
 * History:
 * 2016-05-24	V1.0	jwaechter	 - Initial Version
 */

/**
 * Possible grouping criterias. Influences the data chunks being processed before in memory log is dumped and cleared.
 * @author jwaechter
 */
public enum GroupingCriteria {
	NONE, EOY, EOM, PORTFOLIO, CLUSTER_SIZE;
	
	/**
	 * Chunksize for resets. Just an estimate, as it always processed all deals.
	 */
	public static final int MAX_NUMBER_OF_RESETS = 20000;
}
