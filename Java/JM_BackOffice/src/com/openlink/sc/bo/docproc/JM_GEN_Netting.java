package com.openlink.sc.bo.docproc;
/********************************************************************************
Status:         testing

Revision History:
1.0 - 2014-03-13 - jbonetzk  - initial version
1.1 - 2015-10-30 - jbonetzk  - update Doc Issue Date with Business Date if empty
1.2	- 2016-04-20 - jwaechter - now using col "payment_currency_Invoice" instead of "settle_ccy"
                             - now using col "payment_amount_Invoice" instead of "settle_amount_Invoice" 

Description:
Generation script to save amount values to database.
The saved settle amount on Invoices over to the Netting Statement
The total amount based the saved settle amounts is stored.

Dependencies:
 -

TODOs:
 - 
********************************************************************************/



import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_GENERATE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_GEN_Netting implements IScript {
	
	protected final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	private ConstRepository _constRepo;
	private static boolean _viewTables = false;
	private Container _container = null;

	@Override
	public void execute(IContainerContext context) throws OException {
		
		_constRepo = new ConstRepository("BackOffice", "OLI-Netting");
		_container = new Container();

		initLogging();

		try {
			process(context);
		} catch (Exception e) {
			Logging.error("Exception: " + e.getMessage());
			
		} finally {
			Logging.info("done");
			Logging.close();
			_container.view(getClass().getSimpleName(), _viewTables);
			_container.destroy();
		}

	//	PluginLog.exitWithStatus();
		
	}

	private void initLogging() {
		
		String logLevel = "Error", 
			   logFile  = getClass().getSimpleName() + ".log", 
			   logDir   = null;

		try {
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			Logging.init( this.getClass(), _constRepo.getContext(), _constRepo.getSubcontext());
			
		} catch (Exception e) {
			// do something
		}

		try {
			_viewTables =  _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		} catch (Exception e) {
			// do something
		}
	}

	private void process(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		_container.addCopy("argt", argt, _viewTables);

		// checks
		if (isPreview(argt)) {
			Logging.info("Document is previewed - doing nothing");
			return;
		}
		Table eventData = getEventData(argt);
		if (eventData == null) {
			Logging.error("Failed to retrieve Event Data table");
			return;
		}
		if (eventData.getColNum("settle_amount_Invoice") <= 0) {
			Logging.warn("Couldn't find 'settle_amount_Invoice' column - did Dataload script run?");
		//	if (_viewTables) eventData.viewTable();
			return;
		}

		// work
		int numRows = eventData.getNumRows();
		if (numRows < 1) {
			Logging.info("No data rows available");
			return;
		}
		Logging.debug("Preparing data ("+numRows+" row"+(numRows==1?")":"s)"));
		Table tbl = Table.tableNew("Amounts");
		_container.add("Amounts - work data", tbl);

		tbl.addCol("event_num", COL_TYPE_ENUM.fromInt(eventData.getColType("event_num")));
		// JW: two new columns "payment_currency_invoice" and "payment_amount_invoice"
		tbl.addCols("I(document_num)I(settle_ccy)I(settle_unit)F(settle_amount)F(saved_settle_amount)F(settle_amount_Invoice)I(payment_currency_Invoice)F(payment_amount_Invoice)I(amounts_differ)");
		eventData.copyRowAddAllByColName(tbl);
		tbl.setColName("settle_amount", "curr_settle_amount");
		{
			/*
			int document_num = eventData.getInt("document_num", 1);
			Table tblDB = Table.tableNew();
			_container.add("Amounts - db data", tblDB);
			DBaseTable.loadFromDbWithSQL(tblDB, "event_num, settle_amount settle_amount_db", "stldoc_details", "document_num="+document_num);
			tbl.insertCol("settle_amount_db", "curr_settle_amount", COL_TYPE_ENUM.COL_DOUBLE);
			if (tblDB.getNumRows()>0)
				tbl.select(tblDB, "settle_amount_db", "event_num EQ $event_num");
			*/
		}
		
		if (_viewTables) {
			tbl.setColFormatAsRef("settle_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tbl.setColFormatAsRef("settle_unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
			tbl.setColFormatAsRef("amounts_differ", SHM_USR_TABLES_ENUM.NO_YES_TABLE);
		}
		double total_amount = 0D, saved_settle_amount, curr_settle_amount, settle_amount_Invoice;
		if (numRows != tbl.getNumRows()){
			throw new OException("An error occured when copying row data");
		}

		int col_settle_ccy = tbl.getColNum("settle_ccy"),
			col_settle_unit = tbl.getColNum("settle_unit"),			
			col_payment_currency_Invoice = tbl.getColNum("payment_currency_Invoice");		
		col_settle_ccy = col_payment_currency_Invoice; // JW-2016-04-20
		
		if (numRows > 1) {
			tbl.group("settle_ccy");
			if (tbl.getInt(col_settle_ccy, 1) != tbl.getInt(col_settle_ccy, numRows)){
				throw new OException("Settle Currency is not unique - check grouping");
			}
			tbl.group("settle_unit");
			if (tbl.getInt(col_settle_unit, 1) != tbl.getInt(col_settle_unit, numRows)){
				throw new OException("Settle Unit is not unique - check grouping");
			}
			tbl.group("event_num");
		}
		int col_event_num = tbl.getColNum("event_num"),
			col_saved_settle_amount = tbl.getColNum("saved_settle_amount"),
			col_curr_settle_amount = tbl.getColNum("curr_settle_amount"),
			col_settle_amount_Invoice = tbl.getColNum("settle_amount_Invoice"),
			col_amounts_differ = tbl.getColNum("amounts_differ"),
			col_payment_amount_Invoice = tbl.getColNum("payment_amount_Invoice");
		
		col_settle_amount_Invoice = col_payment_amount_Invoice; // JW-2016-04-20
		
		for (int row=numRows; row>0; --row) {
			settle_amount_Invoice = tbl.getDouble(col_settle_amount_Invoice, row);
			saved_settle_amount   = tbl.getDouble(col_saved_settle_amount,   row);
			curr_settle_amount    = tbl.getDouble(col_curr_settle_amount,    row);
			if (java.lang.Math.abs(settle_amount_Invoice-saved_settle_amount)>0.00000009D || java.lang.Math.abs(settle_amount_Invoice-curr_settle_amount)>0.00000009D){
				tbl.setInt(col_amounts_differ, row, 1);
			}
			if (java.lang.Math.abs(settle_amount_Invoice)>0.00000009D){
				total_amount += settle_amount_Invoice;
			}
		}
		_container.addCopy("Amounts - perpared", tbl, _viewTables);
		_container.add("Total Amount: "+total_amount, null);
		int document_num, settle_ccy, settle_unit, ret, retTotal = 1;
		document_num = tbl.getInt("document_num", 1);
		settle_ccy = tbl.getInt(col_settle_ccy, 1);
		settle_unit = tbl.getInt(col_settle_unit, 1);
		tbl.deleteWhereValue(col_amounts_differ, 0);
		numRows = tbl.getNumRows();
		tbl.group("event_num");
		Logging.info("Updating settle amount on "+numRows+" event"+(numRows==1?"":"s")+" for document "+document_num);
		Logging.debug(tbl.exportCSVString()+ " Amount table to handle");
		
		Logging.debug("\tevent\tamount\n");
		
		
		for (int row=0; ++row <= numRows; ) {
			ret = StlDoc.updateDetailSettleAmount(document_num, tbl.getInt64(col_event_num, row), tbl.getDouble(col_settle_amount_Invoice, row));
			Logging.info(ret==OLF_RETURN_SUCCEED?".":"f");
			Logging.debug("\t"+tbl.getInt64(col_event_num, row)+"\t"+tbl.getDouble(col_settle_amount_Invoice, row)+"\t"+(ret==OLF_RETURN_SUCCEED?"done":ret)+"\n");
			retTotal *= ret;
		}
		
			Logging.info("\n");
		
		if (retTotal != OLF_RETURN_SUCCEED){
			Logging.error("Errors occured within updating settle amounts per event for document "+document_num+" - check privileges");
		}
		Logging.info("Updating total amount for document "+document_num);
		
		Logging.info("\tTotal Amount "+total_amount+"\n");
		
		ret = StlDoc.updateHeaderTotalAmount(document_num, total_amount, settle_unit, settle_ccy);
		
		if (ret != OLF_RETURN_SUCCEED) {
			Logging.error("An ("+ret+") error occured within updating the total amount for document "+document_num+" - check privileges");
			Logging.debug("SECURITY: 44334 is required to run function.");
		}

		int doc_issue_date;
		doc_issue_date = eventData.getDate("doc_issue_date", 1);
		if (doc_issue_date <= 0) {
			
			doc_issue_date = Util.getBusinessDate();
			Logging.info("Updating doc issue date for document "+document_num+" ("+OCalendar.formatJd(doc_issue_date)+";"+doc_issue_date+")");
			ret = StlDoc.updateHeaderDocIssueDate(document_num, doc_issue_date);
			if (ret != OLF_RETURN_SUCCEED) {
				Logging.error("An ("+ret+") error occured within updating the doc issue date for document "+document_num+" - check privileges");
				Logging.info("SECURITY: 44335 is required to run function.");
			}
		}
	}

	private boolean isPreview(Table argt) throws OException {
		int row_num = argt.unsortedFindString("col_name", "View/Process", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		return (row_num > 0) ? ("View").equalsIgnoreCase(argt.getString("col_data", row_num)) : false;
	}

	// returns a non-freeable table; either the event table or a null table
	private Table getEventData(Table argt) throws OException {
		int row_num = argt.unsortedFindString("col_name", "*SourceEventData", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		return (row_num > 0) ? argt.getTable("doc_table", row_num) : null/*Util.NULL_TABLE*/;
	}
}
