package com.jm.shanghai.accounting.udsr;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;

/*
 * History:
 * 2019-01-02	jwaechter	- Initial Version
 */

/**
 * Interface used to provide a single method on the style of java 8 lambda expressions
 * to retrieve a column name from a {@link RetrievalConfiguration}.
 * This is used to create an abstraction to allow multiple mapping tables being processed 
 * by the same methods.
 * @author jwaechter
 * @version 1.0
 */
public interface ColNameProvider {
	public String getColName(RetrievalConfiguration rc);
}
