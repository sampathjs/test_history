package com.jm.shanghai.accounting.udsr.gui;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.Display;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory({ EnumScriptCategory.Generic })
public class AccountingOperatorsGui extends AbstractGenericScript {
	private MainDialog mainDialog;
	private volatile Simulation simToRun=null;
	private volatile QueryResult queryToRun=null;
	
    @Override
	public Table execute(final Session session, final ConstTable table) {
		Context context = (Context) session;
		final Display display = context.getDisplay();
		if (display == null) {
			throw new RuntimeException ("Display not available");
		}

		mainDialog = new MainDialog(this, display, session);
		// Start blocking the normal GUI display
//		if (display != null) {
//			display.block();
//		}
		try {
			while (!mainDialog.isFinished()) {
				try {
					Thread.sleep(100);
					if (simToRun != null && queryToRun != null) {
						SimResults simResults = simToRun.runLocally(queryToRun);
						simToRun = null;
						queryToRun = null;
					}
				} catch (InterruptedException e) {
					throw new RuntimeException (e);
				}			
			};			
		} catch (Throwable t) {
			simToRun = null;
			queryToRun = null;
			mainDialog.dispose();
			throw t;
		}
		return context.getTableFactory().createTable();
    }

	public Simulation getSimToRun() {
		return simToRun;
	}

	public void setSimToRun(Simulation simToRun) {
		this.simToRun = simToRun;
	}

	public QueryResult getQueryToRun() {
		return queryToRun;
	}

	public void setQueryToRun(QueryResult queryToRun) {
		this.queryToRun = queryToRun;
	}
}
