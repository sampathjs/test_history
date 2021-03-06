package com.openlink.esp.confandinv.taxes.app.bridge; 

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * This class has been branched out of standard com.openlink.sc.bo.docproc.OLI_MOD_Taxes because most of the methods were not overridable
 * @author BorisI01
 *
 */
/*
 * History:
 * 
 * 2020-02-13   V1.1   agrawa01    - Changes related to memory leaks & formatting 
 *
 */

public class JM_MOD_Taxes extends com.openlink.sc.bo.docproc.OLI_MOD_Taxes {

	protected ConstRepository _constRepo;
	protected static boolean _viewTables;

	public void execute(IContainerContext context) throws OException {
		_constRepo = new ConstRepository("BackOffice", "OLI-Taxes");

		initLogging ();

		try
		{
			Table argt = context.getArgumentsTable();

			if (argt.getInt("GetItemList", 1) == 1) // if mode 1
			{
				//Generates user selectable item list
				Logging.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			}
			else //if mode 2
			{
				//Gets generation data
				Logging.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}
		}
		catch (Exception e)
		{
			Logging.error("Exception: " + e.getMessage());
		}finally{
			Logging.close();
		}
	}

	private void initLogging()
	{
		String logLevel = "Error", 
			   logFile  = getClass().getSimpleName() + ".log", 
			   logDir   = null;

		try
		{
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), _constRepo.getContext(),_constRepo.getSubcontext());
		}
		catch (Exception e)
		{
			// do something
		}

		try
		{
			_viewTables = _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		}
		catch (Exception e)
		{
			// do something
		}
	}
	
	/*
	 * Add items to selection list
	 */
	private void createItemsForSelection(Table itemListTable) throws OException
	{
		createTaxesItems(itemListTable);

		if (_viewTables)
			itemListTable.viewTable();
	}

	/**
	 * Provides additional columns as compared to standard content version
	 * @param itemListTable
	 * @throws OException
	 */
	private void createTaxesItems(Table itemListTable) throws OException
	{
		String groupName = null;

		groupName = "Tax Data";
		ItemList.add(itemListTable, groupName, "Tax Type", "olfTaxType", 1);
		ItemList.add(itemListTable, groupName, "Tax Subtype", "olfTaxSubtype", 1);
		ItemList.add(itemListTable, groupName, "Tax Jurisdiction", "olfTaxJurisdiction", 1);
		
		// ==> JM Additional fields
		ItemList.add(itemListTable, groupName, "Tax Rate Id", "olfTaxRateId", 1);
		ItemList.add(itemListTable, groupName, "Tax Effective Rate", "olfTaxEffectiveRate", 1);
		ItemList.add(itemListTable, groupName, "Taxable Amount", "olfTaxableAmount", 1);
		ItemList.add(itemListTable, groupName, "Tax Payment", "olfTaxPayment", 1);
		ItemList.add(itemListTable, groupName, "Gross Amount", "olfGrossAmount", 1);
	}

	/**
	 * Retrieve data and add to GenData table for output
	 * Provides additional columns as compared to standard content version
	 */
	private void retrieveGenerationData() throws OException
	{
		int tranNum, insNum, numRows, row;
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
		Transaction tran;

		if (gendataTable.getNumRows() == 0)
			gendataTable.addRow();

		tranNum = eventTable.getInt("tran_num", 1);
		//tran = Transaction.retrieve(tranNum);
		tran = retrieveTransactionObjectFromArgt(tranNum);
		if (Transaction.isNull(tran) == 1) {
			Logging.error ("Unable to retrieve transaction info due to invalid transaction object found. Tran#" + tranNum);
		}
		else
		{
			//Add the required fields to the GenData table
			//Only fields that are checked in the item list will be added
			numRows = itemlistTable.getNumRows();

			String olfTaxJurisdictionField = null; // used as a flag
			String internal_field_name = null;
			String output_field_name   = null;
			int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
			int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

			// Retrieve additional tax data from the database
			int ins_para_seq_num = eventTable.getInt("ins_para_seq_num", 1);
			int ins_seq_num = eventTable.getInt("ins_seq_num", 1);
			Table tranTaxData = Util.NULL_TABLE;
			
			try {
				tranTaxData = retrieveTranTaxData(tranNum, ins_para_seq_num, ins_seq_num);
				
				for (row = 1; row <= numRows; row++) {
					internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
					output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

					if (internal_field_name == null || internal_field_name.trim().length() == 0)
						continue;

					// ==> "Tax Data";
					// "Tax Type"
					else if (internal_field_name.equalsIgnoreCase("olfTaxType")) {
						String strValue = tran.getField(TRANF_FIELD.TRANF_TAX_TRAN_TYPE.toInt());
						GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
					}

					// "Tax Subtype"
					else if (internal_field_name.equalsIgnoreCase("olfTaxSubtype")) {
						String strValue = tran.getField(TRANF_FIELD.TRANF_TAX_TRAN_SUBTYPE.toInt());
						GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
					}

					// "Tax Jurisdiction"
					else if (internal_field_name.equalsIgnoreCase("olfTaxJurisdiction")) {
						olfTaxJurisdictionField = output_field_name;
					}

					// ==> JM Additional fields;
					// "Tax Rate Id"
					else if (internal_field_name.equalsIgnoreCase("olfTaxRateId")
							|| internal_field_name.equalsIgnoreCase("olfTaxEffectiveRate")
							|| internal_field_name.equalsIgnoreCase("olfTaxableAmount")
							|| internal_field_name.equalsIgnoreCase("olfTaxPayment")
							|| internal_field_name.equalsIgnoreCase("olfGrossAmount")) {
						GenData.setField(gendataTable, output_field_name, tranTaxData.copyTable(), internal_field_name, 1);
					}
				}

				if (olfTaxJurisdictionField != null) {
					String strValue = getTaxJurisdiction();
					GenData.setField(gendataTable, olfTaxJurisdictionField, strValue);
				}
				
			} finally {
				if (Table.isTableValid(tranTaxData) == 1) {
					tranTaxData.destroy();
				}
			}
		}
	}

	// *************************************************************************************************************

	/**
	 * Creates and populates the table with transaction taxes
	 * @param tran_num
	 * @param ins_para_seq_num
	 * @param ins_seq_num
	 * @return
	 * @throws OException 
	 */
	private Table retrieveTranTaxData(int tran_num, int ins_para_seq_num, int ins_seq_num) throws OException {
		Table output = Table.tableNew("Transaction taxes");

		output.addCol("olfTaxRateId",        COL_TYPE_ENUM.COL_INT);
		output.addCol("olfTaxEffectiveRate", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("olfTaxableAmount",    COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("olfTaxPayment",       COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("olfGrossAmount",      COL_TYPE_ENUM.COL_DOUBLE);
		
		if (tran_num < 0 || ins_para_seq_num < 0 || ins_seq_num < 0)
			return output;
		
		String what = "MAX(t.tax_rate_id) as olfTaxRateId"
				+ ", MAX(t.effective_rate) as olfTaxEffectiveRate"
				+ ", SUM(t.taxable_amount) as ohd_olfTaxableAmount"
				+ ", SUM(s.settle_amount) ohd_olfTaxPayment"
				+ ", SUM(t.taxable_amount + s.settle_amount) as ohd_olfGrossAmount";

		String from = "ins_tax t"
				+ " INNER JOIN ab_tran_event e ON t.tran_num = e.tran_num AND t.param_seq_num = e.ins_para_seq_num AND t.tax_seq_num = e.ins_seq_num AND e.event_type = 98"
				+ " INNER JOIN ab_tran_event_settle s ON e.event_num = s.event_num";
		
		String where = "t.tran_num = " + tran_num + " GROUP BY t.tran_num";
		
		DBaseTable.loadFromDbWithSQL(output, what, from, where);
		
		if (output.getNumRows() <= 0)
			output.addRow();
		
		output.setColFormatAsRef("olfTaxRateId", SHM_USR_TABLES_ENUM.TAX_RATE_TABLE);
		
		int numCols = output.getNumCols();
		for (int i = 1; i <= numCols; i++)
			output.convertColToString(i);
		
		return output;
	}

	private String getTaxJurisdiction() {
		return "";
	}

	
}
