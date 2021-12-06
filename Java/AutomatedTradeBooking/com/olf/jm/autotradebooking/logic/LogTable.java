package com.olf.jm.autotradebooking.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

public class LogTable {
	private static final String COL_MESSAGE = "Message";
	private static final String COL_STATUS = "Status";
	private static final String COL_LINE_TEXT = "LineText";
	private static final String COL_LINE_NUMBER = "LineNumber";
	private final Table logTable;
	private final Session session;

	public LogTable (final Session session, final String fullPath) {
		this.session = session;
		this.logTable = session.getTableFactory().createTable("Trade Booking Tool Log Table");
		logTable.addColumn(COL_LINE_NUMBER, EnumColType.Int);
		logTable.addColumn(COL_LINE_TEXT, EnumColType.String);
		logTable.addColumn(COL_STATUS, EnumColType.String);
		logTable.addColumn(COL_MESSAGE, EnumColType.String);
		try (Stream<String> stream = Files.lines(Paths.get(fullPath))) {
			stream.forEach(this::createLogLine);
		} catch (IOException e) {
			Logging.error("Error while reading file '" + fullPath + "': " + e.toString());
    		for (StackTraceElement ste : e.getStackTrace()) {
    			Logging.error(ste.toString());
    		} 
		}
	}
	
	private void createLogLine (String logLineRaw) {
		TableRow row = logTable.addRow();
		logTable.setInt(COL_LINE_NUMBER, row.getNumber(), row.getNumber()+1);
		logTable.setString(COL_LINE_TEXT, row.getNumber(), logLineRaw);
		logTable.setString(COL_STATUS, row.getNumber(), "Unprocessed");
		logTable.setString(COL_MESSAGE, row.getNumber(), "");
	}
	
	public void addLogEntry (int lineNum, boolean succeeded, String message) {
		logTable.setString(COL_STATUS, lineNum, succeeded?"Processed":"Failed");
		logTable.setString(COL_MESSAGE, lineNum, message);
	}
	
	public void showLogTableToUser () {
		session.getDebug().viewTable(logTable);
	}
	

}
