package com.olf.jm.autosipopulation.persistence;

import java.util.Collection;
import java.util.List;

import com.olf.jm.autosipopulation.model.EnumRunMode;
import com.olf.jm.autosipopulation.model.Pair;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial version
 */
 
/**
 * Class that is doing the same as it's parent, but throwing exceptions instead of asking the user.
 */
public class LogicResultApplicatorComplexCommPhysException extends LogicResultApplicatorComplexCommPhys {
	
	public LogicResultApplicatorComplexCommPhysException (final Transaction tran, final Session session, 
			final Collection<LogicResult> results, final EnumRunMode rmode,
			final Table clientData,	final List<Integer> preciousMetalList ) {
		super (tran, session, results, rmode, clientData, preciousMetalList);
	}
	
	protected boolean displayConfirmationMessage (LogicResult result) {
		throw new RuntimeException (result.getMessageToUser());
	}

	protected Pair<Pair<Integer, String>, Pair<Integer, String>> displaySelectDialog (LogicResult result) {
		throw new RuntimeException (result.getMessageToUser());
	}	
}
