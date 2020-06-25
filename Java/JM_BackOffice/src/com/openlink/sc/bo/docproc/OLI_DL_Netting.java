/********************************************************************************
Status:         testing

Revision History:
1.0 - 2010-10-18 - jbonetzk - initial version
1.1 - 2011-01-28 - jbonetzk - additional columns; fixed retrieval via event nums
1.2 - 2011-06-01 - jbonetzk - additional columns(ext_doc_id_*, pymt_due_date_*, invoice_date_*
                              removed doc type specific columns for the same as in argt
1.3 - 2011-06-22 - jbonetzk - fixed 'External Reference' retrieval
1.4 - 2013-05-13 - jbonetzk - fixed getSingleDocumentsStatuses for empty tblProcessConfig
1.5 - 2013-10-16 - jbonetzk - revised Invoice Date retrieval (allow ISO formatted date strings)
1.6 - 2014-03-10 - jbonetzk - full review; improved performance
1.7 - 2014-03-11 - jbonetzk - format datetime columns to show date only
1.8 - 2014-03-13 - jbonetzk - added container for collecting tables to be viewed
1.9 - 2014-07-18 - jbonetzk - prepared for different data types of Event Numbers
1.10 - 2014-11-11 - jbonetzk - full review; improved performance
1.11 - 2014-12-11 - jbonetzk - 'This Doc Num' is type of string (long integer)
1.12   2020-03-25   YadavP03 - memory leaks, remove console prints & formatting changes

Description:
 DataLoad script to populate a Settlement Desktop view.
 This script is intended for use with the Document Type 'Netting statement';
 assuming the Document Type was set up, it's id doesn't matter.
 This script collects and provides data only for related Invoices, if exist.
 This script also collects and provides optional Stldoc Info values from 
 related Invoices; such info fields' names are passed thru from ConstRepo.

Dependencies:
 - 
********************************************************************************/

package com.openlink.sc.bo.docproc;

import java.util.ArrayList;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class OLI_DL_Netting implements IScript {
	private static DATE_FORMAT _dateFormat = DATE_FORMAT.DATE_FORMAT_DEFAULT;//.DATE_FORMAT_DMLY_NOSLASH;
	private static DATE_LOCALE _dateLocale = DATE_LOCALE.DATE_LOCALE_DEFAULT;
	private final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	private final static int STLDOC_DOCUMENT_TYPE_INVOICE = 1;//Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, "Invoice"); // '1' should be corp's id for 'Invoice'

	private static ConstRepository _constRepo;
	private static boolean _viewTables = false;
	private Container _container = null;

	// optional: Stldoc Info fields - names provided thru ConstRepo
	private final String INVOICE_DATE = "Invoice Date";
	private String _stldoc_info_type_invoice_date_name = INVOICE_DATE;//""; //< ConstRepo
	private int    _stldoc_info_type_invoice_date_id   = -1; //< DataBase
	private final String THIS_DOC_NUM = "This Doc Num";
	private String _stldoc_info_type_this_doc_num_name = THIS_DOC_NUM;//""; //< ConstRepo
	private int    _stldoc_info_type_this_doc_num_id   = -1; //< DataBase

	// used for what/where clauses for optional Stldoc Info values stored for Invoices
	private ArrayList<NamedStldocInfoTypeHandling> namedStldocInfoTypeHandling = new ArrayList<NamedStldocInfoTypeHandling>();

	public void execute(IContainerContext context) throws OException {
		_constRepo = new ConstRepository("BackOffice", "OLI-Netting");
		_container = new Container();

		initLogging();
		retrieveSettingsFromConstRep();

		try { 
			process(context); 
		} catch (Exception e) { 
			Logging.error("Exception: " + e.getMessage()); 
		} finally { 
			_container.view("debug - "+getClass().getSimpleName(), _viewTables);
			Logging.close();
			_container.destroy(); 
		}
	}

	private void initLogging() {

		try {
			Logging.init( this.getClass(), _constRepo.getContext(), _constRepo.getSubcontext());
		} catch (Exception e) {
			// do something
		}

		try {
			_viewTables = _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		} catch (Exception e) {
			// do something
		}
	}

	private void process(IContainerContext context) throws OException {
		Table tblQuery = Util.NULL_TABLE;
		int intQueryId = 0;
		try{			
			// measure execution time
			long start = System.currentTimeMillis(), total;// should also mind ConstRepo and Logging initialization
			long startSub, totalSub;

			Table argt = context.getArgumentsTable();
			_container.addCopy("argt - initial", argt, _viewTables);

			int intDocType, intQueryRet;

			String strDocType, strDocTypeDb, strColNameStatus, 
				   strColNameOurRef, strColNameCptRef, strColNameExtRef, 
				   strColNameStlAmount, strColNamePymtDueDate;
			String strResultTable;
			// optional: named Stldoc Info values
			String strColNameInvoiceDate = null, strColNameThisDocNum = null; // initialize to prevent from compiler issues

//			Logging.memory();
			intDocType = STLDOC_DOCUMENT_TYPE_INVOICE;
			strDocType = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, intDocType);
			if (strDocType == null || strDocType.trim().length() == 0){
				Logging.warn("No Document Type found for id: "+intDocType);
			} else{
				Logging.debug("Doc type is '" + strDocType + "'");
			}
			strDocTypeDb = strDocType.replaceAll(" ", "_");//.toLowerCase();

			// add columns for Document Type 'Invoice' (including formatting)
			{
				Logging.debug("Adding columns for '"+strDocType+"' ...");
				startSub = System.currentTimeMillis();

				strColNameStatus      = "doc_status_"    + strDocTypeDb; // sh.doc_status
				strColNameOurRef      = "our_ref_"       + strDocTypeDb; // sh.document_num
				strColNameCptRef      = "cpt_ref_"       + strDocTypeDb; // sd.ext_doc_id
				strColNameExtRef      = "ext_ref_"       + strDocTypeDb; // sh.doc_external_ref
				strColNameStlAmount   = "settle_amount_" + strDocTypeDb; // sd.settle_amount (ie Saved Settle Amount)
				strColNamePymtDueDate = "pymt_due_date_" + strDocTypeDb; // sh.pymt_due_date

				argt.addCol(strColNameStatus, "Current Status\n["+ strDocType + "]", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
				argt.addCol(strColNameOurRef,      COL_TYPE_ENUM.COL_INT,       "Document Number\n[" + strDocType + "]");
				argt.addCol(strColNameCptRef,      COL_TYPE_ENUM.COL_STRING,    "External Doc ID\n[" + strDocType + "]");
				argt.addCol(strColNameExtRef,      COL_TYPE_ENUM.COL_STRING,    "External Reference\n[" + strDocType + "]");
				argt.addCol(strColNameStlAmount,   COL_TYPE_ENUM.COL_DOUBLE,    "Saved Settle Amount\n[" + strDocType + "]");
				argt.addCol(strColNamePymtDueDate, COL_TYPE_ENUM.COL_DATE_TIME, "Pymt Due Date\n[" + strDocType + "]");

				argt.setColFormatAsDate(strColNamePymtDueDate, _dateFormat, _dateLocale);
			//	argt.copyColFormat("curr_doc_status",     argt, strColNameStatus);
			//	argt.copyColFormat("saved_settle_amount", argt, strColNameStlAmount);
			//	argt.copyColFormat("pymt_due_date",       argt, strColNamePymtDueDate);

				// named optional stldoc info types
				if (_stldoc_info_type_invoice_date_id >= 0) {
					strColNameInvoiceDate = "invoice_date_" + strDocTypeDb;
					argt.addCol(strColNameInvoiceDate, COL_TYPE_ENUM.COL_DATE_TIME, _stldoc_info_type_invoice_date_name + "\n[" + strDocType + "]");
					argt.setColFormatAsDate(strColNameInvoiceDate, _dateFormat, _dateLocale);
					argt.formatSetJustifyLeft(strColNameInvoiceDate);
					// prepare what/where clauses for later stldoc info retrieval
					namedStldocInfoTypeHandling.add(new NamedStldocInfoTypeHandling(strDocType+" - "+_stldoc_info_type_invoice_date_name, 
							"date_value("+strColNameInvoiceDate+")",  "document_num EQ $"+strColNameOurRef+" AND doc_type EQ "+intDocType+" AND type_id EQ "+_stldoc_info_type_invoice_date_id));
				}
				
				if (_stldoc_info_type_this_doc_num_id >= 0){
					strColNameThisDocNum  = "this_doc_num_" + strDocTypeDb;
				//	argt.addCol(strColNameThisDocNum, COL_TYPE_ENUM.COL_INT, _stldoc_info_type_this_doc_num_name + "\n[" + strDocType + "]");
					argt.addCol(strColNameThisDocNum, COL_TYPE_ENUM.COL_STRING, _stldoc_info_type_this_doc_num_name + "\n[" + strDocType + "]");
					// prepare what/where clauses for later stldoc info retrieval
					namedStldocInfoTypeHandling.add(new NamedStldocInfoTypeHandling(strDocType+" - "+_stldoc_info_type_this_doc_num_name, 
							"string_value("+strColNameThisDocNum+")", "document_num EQ $"+strColNameOurRef+" AND doc_type EQ "+intDocType+" AND type_id EQ "+_stldoc_info_type_this_doc_num_id));
				}

				totalSub = System.currentTimeMillis() - startSub;
				Logging.info("Adding columns for '"+strDocType+"' done in "+totalSub+" millis");
			}

			int numRowsArgt = argt.getNumRows();
			if (numRowsArgt <= 0){
				Logging.info("No events available to retrieve data for");
			} else {
				Logging.info("Handling "+numRowsArgt+(numRowsArgt>1?" rows/events":" row/event")+" ...");

				Logging.debug("Storing current event numbers ...");
				startSub = System.currentTimeMillis();

				// handle current events only
				strResultTable = Query.getResultTableForId(intQueryId = Query.tableQueryInsert(argt, "event_num"));
				Logging.debug("Stored current event numbers in "+strResultTable+" for unique id "+intQueryId);

				totalSub = System.currentTimeMillis() - startSub;
				Logging.info("Storing current event numbers done in "+totalSub+" millis");

				// --------------------

				Logging.debug("Loading '"+strDocType+"' data limited to current events ...");
				startSub = System.currentTimeMillis();

				String sql  = "SELECT sd.event_num,sd.tran_num,sh.doc_type" // for merging/information purposes
							+ ",sd.document_num" // "Document Number"
							+ ",sh.doc_status curr_doc_status" // "Current Status"
							+ ",sd.ext_doc_id" // "External Doc ID"
							+ ",sh.doc_external_ref" // "External Reference"
							+ ",sd.settle_amount saved_settle_amount" // "Saved Settle Amt"
							+ ",sh.pymt_due_date" // "Pymt Due Date"
							+ " FROM stldoc_details sd,stldoc_header sh,"+ strResultTable + " qr"
							+ " WHERE qr.query_result=sd.event_num AND qr.unique_id="+intQueryId
							+ " AND sd.document_num=sh.document_num AND sh.doc_type="+intDocType
							;
				tblQuery = Table.tableNew("Invoice data");
				_container.add("relevant results", tblQuery);
				intQueryRet = DBaseTable.execISql(tblQuery, sql);
				if (intQueryRet != OLF_RETURN_SUCCEED){
					Logging.warn("Loading '"+strDocType+"' data limited to current events failed when executing SQL statement:\n"+sql);
				} else {
					if (_viewTables) {
						
						// format columns for proper displaying
						tblQuery.setColFormatAsRef("doc_status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
						tblQuery.setColFormatAsRef("doc_type", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE);
						tblQuery.setColFormatAsDate("pymt_due_date", _dateFormat, _dateLocale);
					}

					int intNumInvoiceEvents = tblQuery.getNumRows();
					totalSub = System.currentTimeMillis() - startSub;
					if (intNumInvoiceEvents <= 0){
						Logging.info("Loading '"+strDocType+"' data limited to current events returned zero events when executing SQL statement:\n"+sql);
					} else{
						
						Logging.info("Loading '"+strDocType+"' data limited to current events done in "+totalSub+" millis");// retrieved data for "+intNumInvoiceEvents+" events");

						// --------------------

						Logging.debug("Populating standard columns ...");
						startSub = System.currentTimeMillis();
						String what = "curr_doc_status(" + strColNameStatus + ")"
									+ ",document_num(" + strColNameOurRef + ")"
									+ ",ext_doc_id(" + strColNameCptRef + ")"
									+ ",doc_external_ref(" + strColNameExtRef + ")"
									+ ",saved_settle_amount(" + strColNameStlAmount + ")"
									+ ",pymt_due_date(" + strColNamePymtDueDate + ")"
									;
						argt.select(tblQuery, what, "event_num EQ $event_num AND doc_type EQ " + intDocType);
						totalSub = System.currentTimeMillis() - startSub;
						Logging.info("Populating standard columns done in "+totalSub+" millis");

						// above query for Invoice related events returned events related to already generated documents
						// assuming there are any already generated documents, populate named Stldoc Info Type columns' values
						if (_stldoc_info_type_invoice_date_id != -1 || _stldoc_info_type_this_doc_num_id != -1) {
							
							// clear for re-use
							if(intQueryId > 0)
							Query.clear(intQueryId);

							// handle Stldoc Info values
							Logging.debug("Storing retrieved doc numbers ...");
							startSub = System.currentTimeMillis();
							strResultTable = Query.getResultTableForId(intQueryId = Query.tableQueryInsert(tblQuery, "document_num"));
							Logging.debug("Stored retrieved doc numbers in "+strResultTable+" for unique id "+intQueryId);
							if(intQueryId > 0)
							Query.delete(intQueryId, 0); // to be on the safe side
							totalSub = System.currentTimeMillis() - startSub;
							Logging.info("Storing retrieved doc numbers done in "+totalSub+" millis");

							// --------------------

							Logging.debug("Retrieving Stldoc Info values ...");
							startSub = System.currentTimeMillis();
							Table tbl = retrieveStldocInfoValues(intQueryId, strResultTable, _stldoc_info_type_invoice_date_id, _stldoc_info_type_this_doc_num_id);
							_container.add("stldoc_info", tbl);
							totalSub = System.currentTimeMillis() - startSub;
							Logging.info("Retrieving Stldoc Info values done in "+totalSub+" millis");

							// --------------------

							Logging.debug("Populating Stldoc Info columns ...");
							startSub = System.currentTimeMillis();
							for (NamedStldocInfoTypeHandling n : namedStldocInfoTypeHandling) {
								Logging.debug("Handling Stldoc Info '"+n.name+"' ...");
								argt.select(tbl, n.what, n.where);
								Logging.debug("Handling Stldoc Info '"+n.name+"' done");
							}

							totalSub = System.currentTimeMillis() - startSub;
							Logging.info("Populating Stldoc Info columns done in "+totalSub+" millis");
						}
					}
				}

				
			}
//			Logging.memory();

			if (_viewTables){
				_container.addCopy("argt - final", argt, _viewTables);
			}

			total = System.currentTimeMillis() - start;
			Logging.info("Handling "+numRowsArgt+(numRowsArgt>1?" rows/events":" row/event")+" done in "+total+" millis");
		
		}finally{
			if(Table.isTableValid(tblQuery) == 1){
				tblQuery.destroy();
			}
			if(intQueryId > 0){
				Query.clear(intQueryId);	
			}
			
		}
	}

	private int getStlDocInfoTypeId(String name) throws OException {
		
		String sql = "SELECT type_id FROM stldoc_info_types WHERE type_name='" + name + "'";
		Table tbl = Table.tableNew();
		try {
			
			if (DBaseTable.execISql(tbl, sql) == OLF_RETURN_SUCCEED){
				switch (tbl.getNumRows()) {
					case 1:
						return tbl.getInt(1, 1);
					case 2:
						Logging.warn("Ambigious setup of Back Office Info Type: "+name+" - Also check via SQL statement:\n"+sql);
					default:
						return -1;
				}
			}
			Logging.warn("Failed when executing SQL statement:\n"+sql);
			return -1;
		} finally { 
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();	
			}
			 
		}
	}

	private Table retrieveStldocInfoValues(int query_id, String query_result, int... type_id) throws OException {
		
		Table tbl = Table.tableNew("stldoc_info");
		int type_id_num;
		if (type_id == null || (type_id_num=type_id.length) == 0) {
			tbl.addCols("I(document_num)I(doc_type)I(type_id)I(data_type)S(string_value)I(int_value)F(double_value)T(date_value)");
			return tbl; // no ids provided
		}

		String type_id_csv;
		if (type_id_num > 1) {
			type_id_csv = "";
			while (--type_id_num >= 0){
				type_id_csv += ","+type_id[type_id_num];
			}
			type_id_csv = type_id_csv.substring(1);
		} else{ 
			type_id_csv = ""+type_id[0];
		}
		type_id_csv = type_id_csv.replaceAll("\\-1,", "").replaceAll(",\\-1", "").replaceAll("\\-1", "").trim();// 'kiss' principle, of course ;-)

		if (type_id_csv.length() == 0) {
			tbl.addCols("I(document_num)I(doc_type)I(type_id)I(data_type)S(string_value)I(int_value)F(double_value)T(date_value)");
			return tbl; // invalid ids provided
		}

		// 'data_type' - see 'tran_info_data_type' table
		String sql	= "SELECT DISTINCT sh.document_num, sh.doc_type, si.type_id, sit.data_type, si.value string_value"
					+ ",(CASE WHEN data_type=0 THEN value ELSE '' END) AS int_value"
					+ ",(CASE WHEN data_type=1 THEN value ELSE '' END) AS double_value"
					+ ",(CASE WHEN data_type=3 THEN value ELSE '' END) AS date_value"
					+ "  FROM stldoc_header sh, stldoc_info si, stldoc_info_types sit, "+query_result+" qr"
					+ " WHERE sh.document_num=si.document_num AND si.type_id=sit.type_id AND sit.doc_type in (0,sh.doc_type)"
					+ "   AND si.document_num>0 AND si.type_id in ("+type_id_csv+")"
					+ "   AND sh.document_num=qr.query_result AND qr.unique_id in ("+query_id+")"
					;
		tbl.addCols("I(document_num)I(doc_type)I(type_id)I(data_type)S(string_value)S(int_value)S(double_value)S(date_value)");

		int ret = DBaseTable.execISql(tbl, sql);
		if (ret != OLF_RETURN_SUCCEED){
			Logging.warn("Failed when executing SQL statement:\n"+sql);
		}

		tbl.convertStringCol(tbl.getColNum("int_value"), COL_TYPE_ENUM.COL_INT.toInt(), -1);
		tbl.convertStringCol(tbl.getColNum("double_value"), COL_TYPE_ENUM.COL_DOUBLE.toInt(), -1);
		tbl.convertStringCol(tbl.getColNum("date_value"), COL_TYPE_ENUM.COL_DATE_TIME.toInt(), -1);

		if (_viewTables){
			tbl.setColFormatAsRef("data_type", SHM_USR_TABLES_ENUM.TRAN_INFO_DATA_TYPE_TABLE);
			tbl.setColFormatAsRef("doc_type", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE);
			tbl.setColFormatAsDate("date_value", _dateFormat, _dateLocale);
			tbl.formatSetJustifyLeft("date_value");
		}

		return tbl;
	}

	private void retrieveSettingsFromConstRep() throws OException {
		String name; int id;

		id = getStlDocInfoTypeId(name = tryRetrieveSettingFromConstRep(INVOICE_DATE, _stldoc_info_type_invoice_date_name));
		if (id >= 0) { 
			_stldoc_info_type_invoice_date_name = name; 
			_stldoc_info_type_invoice_date_id = id; 
		}  else if (name.trim().length()>0) {
			Logging.debug("StlDoc Info Field for feature '"+INVOICE_DATE+"' not found - invalid name: '"+name+"'");
		}

		id = getStlDocInfoTypeId(name = tryRetrieveSettingFromConstRep(THIS_DOC_NUM, _stldoc_info_type_this_doc_num_name));
		if (id >= 0) { 
			_stldoc_info_type_this_doc_num_name = name; 
			_stldoc_info_type_this_doc_num_id = id; 
		} else if (name.trim().length()>0) {
			Logging.debug("StlDoc Info Field for feature '"+THIS_DOC_NUM+"' not found - invalid name: '"+name+"'");
		}
	}

	private String tryRetrieveSettingFromConstRep(String variable_name, String default_value) {
		try { 
			default_value = _constRepo.getStringValue(variable_name, default_value); 
		} catch (Exception e) { 
			Logging.warn("Couldn't solve setting for: " + variable_name + " - " + e.getMessage()); 
		} finally{ 
			if (default_value.trim().length()>0) {
				Logging.debug(variable_name + ": " + default_value); 
			}
		}
		return default_value;
	}

	class NamedStldocInfoTypeHandling {
		final String name, what, where;

		public NamedStldocInfoTypeHandling(String name, String what, String where) {
			this.name=name; this.what=what; this.where=where;
		}

		// prevent from invalid instantiation
		@SuppressWarnings("unused")
		private NamedStldocInfoTypeHandling() {
			this(null, null, null);
		}

		public String toString() {
			return "name:  "+name + "\nwhat:  " + what + "\nwhere: " + where;
		}
	}
}
