package com.matthey.pmm.tradebooking.processors;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.transaction.items.TransactionItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

public class LogTable {
    private static final String USER_TABLE_TRADE_LOG = "USER_jm_builder_trade_log";
    
	private static final String COL_MESSAGE = "message";
	private static final String COL_STATUS = "status";
	private static final String COL_LINE_TEXT = "command";
	private static final String COL_LINE_NUMBER = "step_no";
	private static final String COL_RUN_ID = "run_id";
	private static final String COL_DEAL_COUNTER = "deal_counter";
	private static final String COL_LAST_UPDATE = "last_update";
	
	private static Logger logger = null;
	private final Table logTable;
	private final Session session;
	
	private final int runId;
	private final int dealCounter;
	
	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(LogTable.class);
		}
		return logger;
	}
	
	public LogTable (final Session session, final int runId, final int dealCounter) {
		this.session = session;
		this.logTable = session.getTableFactory().createTable("Trade Booking Tool Log Table");
		this.runId = runId;
		this.dealCounter = dealCounter;
		logTable.addColumn(COL_RUN_ID, EnumColType.Int);
		logTable.addColumn(COL_DEAL_COUNTER, EnumColType.Int);
		logTable.addColumn(COL_LINE_NUMBER, EnumColType.Int);
		logTable.addColumn(COL_LINE_TEXT, EnumColType.String);
		logTable.addColumn(COL_STATUS, EnumColType.String);
		logTable.addColumn(COL_MESSAGE, EnumColType.String);
		logTable.addColumn(COL_LAST_UPDATE, EnumColType.DateTime);
	}
	
	public void init (List<TransactionItem<?, ?, ?, ?>>  actionPlan) {
		actionPlan.forEach(x -> createLogLine(x.toString()));
	}
	
	private void createLogLine (String logLineRaw) {
		TableRow row = logTable.addRow();
		logTable.setInt(COL_RUN_ID, row.getNumber(), runId);
		logTable.setInt(COL_DEAL_COUNTER, row.getNumber(), dealCounter);
		logTable.setInt(COL_LINE_NUMBER, row.getNumber(), row.getNumber()+1);
		logTable.setString(COL_LINE_TEXT, row.getNumber(), logLineRaw);
		logTable.setString(COL_STATUS, row.getNumber(), "Unprocessed");
		logTable.setString(COL_MESSAGE, row.getNumber(), "");
		
	}
	
	public void addLogEntry (int lineNum, boolean succeeded, String message) {
		logTable.setString(COL_STATUS, lineNum, succeeded?"Processed":"Failed");
		logTable.setString(COL_MESSAGE, lineNum, message);
		logTable.setDate(COL_LAST_UPDATE, lineNum, new Date());
	}
	
	public void showLogTableToUser () {
		session.getDebug().viewTable(logTable);
	}
	
	public void persistToDatabase () {
		try (UserTable table = session.getIOFactory().getUserTable(USER_TABLE_TRADE_LOG)) {
			table.insertRows(logTable);
		}
	}
}
