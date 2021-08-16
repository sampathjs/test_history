package com.olf.cxplugins.adapter.fixgateway;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2020-04-16	V1.0	jwaechter	- Initial Version
 */

/**
 * Interface enhancing IFIXCustomProcessFIXInclude with methods to check if implementing instances
 * are going to process an incoming message.
 * @author jwaechter
 * @version 1.0
 */
public interface GuardedCustomFixProcessFIXInclude extends
		IFIXCustomProcessFIXInclude {
	/**
	 * Checks if instances of this class can process in incoming FIX message represented as an
	 * Endur table.
	 * 
	 * @param argTbl The format of this table is the same as the one being used in the methods declared in
	 * {@link IFIXCustomProcessFIXInclude}.
	 * @return
	 * @throws OException 
	 */
	public boolean canProcess(Table argTbl) throws OException;
}
