package com.jm.shanghai.accounting.udsr.model.retrieval;

/*
 * History:
 * 2019-07-19	V1.0		jwaechter		- Initial Version
 */

/**
 * Denotes how a column in the retrieval configuration table is used.
 * @author jwaechter
 * @version 1.0
 */
public enum ColumnSemantics {
	PRIORITY, RETRIEVAL_LOGIC, MAPPER_COLUMN, RUNTIME_COLUMN, OUTPUT_COLUMN
}
