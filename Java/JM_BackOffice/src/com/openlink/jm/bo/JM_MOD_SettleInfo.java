/**
 * Status: under construction
 * Additional Confirmation/Invoice Data based on European Standards
 * @author jbonetzky
 * @version 0.1
 */
/*
 * History:
 * 2016-11-21	V1.0	jwaechter	- Created as copy of V0.11 of OLI_MOD_SettleInfo
 * 2016-11-23	V1.1	jwaechter	- settlement instructions are now retrieved by matching
 *                                    currency. If currency can't be matched the SI is not used
 *                                    any more.
 * 2019-09-20   V1.2   Pramod Garg  - Additional account info(account num, sort code) fields has been added to populate on invoices.                                   
 */

package com.openlink.jm.bo;

import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.sc.bo.docproc.OLI_MOD_ModuleBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

//@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_MODULE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_MOD_SettleInfo extends OLI_MOD_ModuleBase implements IScript
{
	protected ConstRepository _constRepo;
	protected static boolean _viewTables;
	private Table _tableContainer = null;
	
	public void execute(IContainerContext context) throws OException
	{
		_constRepo = new ConstRepository("BackOffice", "OLI-SettleInfo");

		initPluginLog ();
		
		_tableContainer = Table.tableNew();
		_tableContainer.addCols("I(row)S(info_msg)I(data_rows)A(data_table)");
		try
		{
			Table argt = context.getArgumentsTable();
			String module = getModuleName(argt);

			if (argt.getInt("GetItemList", 1) == 1) // if mode 1
			{
				//Generates user selectable item list
				PluginLog.info("Generating item list"+(module==null?"":" for module '"+module+"'")+" ...");
				long total, start = System.currentTimeMillis();
				createItemsForSelection(argt.getTable("ItemList", 1));
				total = System.currentTimeMillis() - start;
				PluginLog.info("Generating item list"+(module==null?"":" for module '"+module+"'")+" took "+total+" millis");
			}
			else //if mode 2
			{
				//Gets generation data
				PluginLog.info("Retrieving gen data"+(module==null?"":" for module '"+module+"'")+" ...");
				long total, start = System.currentTimeMillis();
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
				total = System.currentTimeMillis() - start;
				PluginLog.info("Retrieving gen data"+(module==null?"":" for module '"+module+"'")+" took "+total+" millis");
			}
		}
		catch (Exception e)
		{
			PluginLog.error("Exception: " + e.getMessage());
		}
		finally
		{
			if (_viewTables)
				_tableContainer.viewTable();
			_tableContainer.destroy();
		}

		PluginLog.exitWithStatus();
	}

	/**
	 * !!! This adds a passed table's COPY only !!!
	 * @param str
	 * @param tbl
	 * @throws OException
	 */
	private final void addToContainer(String str, Table tbl) throws OException
	{
		if (_tableContainer.getNumRows() > 0)
			_tableContainer.insertRowBefore(1);
		else
			_tableContainer.addRow();

		_tableContainer.setInt("row", 1, _tableContainer.getNumRows());
		_tableContainer.setString("info_msg", 1, str!=null ? str.trim() : "");
		if (tbl != null)
		{
			_tableContainer.setInt("data_rows", 1, tbl.getNumRows());
			_tableContainer.setTable("data_table", 1, tbl.copyTable());
		}
	}

	private void initPluginLog()
	{
		String logLevel = "Error", 
			   logFile  = getClass().getSimpleName() + ".log", 
			   logDir   = null;

		try
		{
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			if (logDir == null)
				PluginLog.init(logLevel);
			else
				PluginLog.init(logLevel, logDir, logFile);
		}
		catch (Exception e)
		{
			// do something
		}

		try
		{
			_viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && 
							_constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
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
		createSettleInstrItems(itemListTable);
		createSettleAccountItems(itemListTable);
		addToContainer("Item List", itemListTable);
	}

	private void createSettleInstrItems(Table itemListTable) throws OException
	{
		String groupName = null;

/*
		groupName = "Settlement Instruction, Buyer DO_NOT_USE";
		createSettleInstrItemsTrad(groupName, "Buy", itemListTable);
		groupName = "Settlement Instruction, Seller DO_NOT_USE";
		createSettleInstrItemsTrad(groupName, "Sel", itemListTable);
*/

		groupName = "Settlement Instruction, Tables";
		ItemList.add(itemListTable, groupName, "Ext & Int Table (Flag)", "olfSetAllSIFlag", 1);
		ItemList.add(itemListTable, groupName, "External Table (Flag)", "olfSetExtSIFlag", 1);
		ItemList.add(itemListTable, groupName, "Internal Table (Flag)", "olfSetIntSIFlag", 1);
		ItemList.add(itemListTable, groupName, "Num of All SIs", "olfSetNumAllSIs", 1);
		ItemList.add(itemListTable, groupName, "Num of Ext SIs", "olfSetNumExtSIs", 1);
		ItemList.add(itemListTable, groupName, "Num of Int SIs", "olfSetNumIntSIs", 1);

		groupName = "Settlement Instruction, Internal, Non Clfd Account";
		createSettleInstrItems(groupName, "Int" + "None", itemListTable);
		groupName = "Settlement Instruction, External, Non Clfd Account";
		createSettleInstrItems(groupName, "Ext" + "None", itemListTable);

		Table tblAccountClasses = getAccountClassData();
		for (int row = tblAccountClasses.getNumRows(); row > 0; --row)
		{
			groupName = "Settlement Instruction, Internal, " + tblAccountClasses.getString("account_class_name", row);
			createSettleInstrItems(groupName, "Int" + tblAccountClasses.getString("acc4_class", row), itemListTable);
			groupName = "Settlement Instruction, External, " + tblAccountClasses.getString("account_class_name", row);
			createSettleInstrItems(groupName, "Ext" + tblAccountClasses.getString("acc4_class", row), itemListTable);
		}
		tblAccountClasses.destroy();
	}

	private void createSettleInstrItems(String groupName, String strSettlePrefix, Table itemListTable) throws OException
	{
		ItemList.add(itemListTable, groupName, "Deal Delivery Type",      "olfSet" + strSettlePrefix + "DealDelType", 1);
		ItemList.add(itemListTable, groupName, "Account Id",              "olfSet" + strSettlePrefix + "Acct", 1);
		ItemList.add(itemListTable, groupName, "Account Number",          "olfSet" + strSettlePrefix + "AcctNum", 1);
		ItemList.add(itemListTable, groupName, "Account Name",            "olfSet" + strSettlePrefix + "AcctName", 1);
		ItemList.add(itemListTable, groupName, "Account IBAN",            "olfSet" + strSettlePrefix + "AcctIBAN", 1);
		ItemList.add(itemListTable, groupName, "Currency",                "olfSet" + strSettlePrefix + "Ccy", 1);
		ItemList.add(itemListTable, groupName, "Long Name",               "olfSet" + strSettlePrefix + "LongName", 1);
		ItemList.add(itemListTable, groupName, "Short Name",              "olfSet" + strSettlePrefix + "BankName", 1);
		ItemList.add(itemListTable, groupName, "Address 1",               "olfSet" + strSettlePrefix + "Addr1", 1);
		ItemList.add(itemListTable, groupName, "Address 2",               "olfSet" + strSettlePrefix + "Addr2", 1);
		ItemList.add(itemListTable, groupName, "City",                    "olfSet" + strSettlePrefix + "City", 1);
		ItemList.add(itemListTable, groupName, "State",                   "olfSet" + strSettlePrefix + "State", 1);
		ItemList.add(itemListTable, groupName, "Country",                 "olfSet" + strSettlePrefix + "Country", 1);
		ItemList.add(itemListTable, groupName, "Mail Code",               "olfSet" + strSettlePrefix + "Zip", 1);
		ItemList.add(itemListTable, groupName, "Phone",                   "olfSet" + strSettlePrefix + "Phone", 1);
		ItemList.add(itemListTable, groupName, "Fax",                     "olfSet" + strSettlePrefix + "Fax", 1);
		ItemList.add(itemListTable, groupName, "Party Delivery Type",     "olfSet" + strSettlePrefix + "ParDelType", 1);
		ItemList.add(itemListTable, groupName, "Party Delivery Code",     "olfSet" + strSettlePrefix + "ParDelCode", 1);
		ItemList.add(itemListTable, groupName, "Party Delivery Add Code", "olfSet" + strSettlePrefix + "ParDelAddCode", 1);
		ItemList.add(itemListTable, groupName, "Hold Delivery Type",      "olfSet" + strSettlePrefix + "DeliType", 1);
		ItemList.add(itemListTable, groupName, "Hold Delivery Code",      "olfSet" + strSettlePrefix + "DeliCode", 1);
		ItemList.add(itemListTable, groupName, "Hold Delivery Add Code",  "olfSet" + strSettlePrefix + "AddCode", 1);
		ItemList.add(itemListTable, groupName, "Description",             "olfSet" + strSettlePrefix + "Des", 1);
		ItemList.add(itemListTable, groupName, "Holder Id",               "olfSet" + strSettlePrefix + "HolderId", 1);
		ItemList.add(itemListTable, groupName, "Holder Name",             "olfSet" + strSettlePrefix + "HolderName", 1);
		ItemList.add(itemListTable, groupName, "Holder Long Name",        "olfSet" + strSettlePrefix + "HolderLongName", 1);
		ItemList.add(itemListTable, groupName, "Bic Code",                "olfSet" + strSettlePrefix + "Bic", 1);
		ItemList.add(itemListTable, groupName, "Sort Code",               "olfSet" + strSettlePrefix + "SortCode", 1);
		ItemList.add(itemListTable, groupName, "Account Num",             "olfSet" + strSettlePrefix + "AccountNum", 1);

		ItemList.add(itemListTable, groupName + ", Third Party", "1st Int. Long Name",      "olfSet" + strSettlePrefix + "3rdPty1stName", 1);
		ItemList.add(itemListTable, groupName + ", Third Party", "1st Int. Account Number", "olfSet" + strSettlePrefix + "3rdPty1stAcctNum", 1);
		ItemList.add(itemListTable, groupName + ", Third Party", "1st Int. Delivery Code",  "olfSet" + strSettlePrefix + "3rdPty1stDeliCode", 1);
		ItemList.add(itemListTable, groupName + ", Third Party", "2nd Int. Long Name",      "olfSet" + strSettlePrefix + "3rdPty2ndName", 1);
		ItemList.add(itemListTable, groupName + ", Third Party", "2nd Int. Account Number", "olfSet" + strSettlePrefix + "3rdPty2ndAcctNum", 1);
		ItemList.add(itemListTable, groupName + ", Third Party", "2nd Int. Delivery Code",  "olfSet" + strSettlePrefix + "3rdPty2ndDeliCode", 1);

		ItemList.add(itemListTable, groupName, "Holder Delivery Codes",   "Table_Set" + strSettlePrefix + "HolderDeliCodes", 1);
		ItemList.add(itemListTable, groupName, "Holder Delivery Codes#",   "olfSet" + strSettlePrefix + "HolderNumDeliCodes", 1);
	}

	private void createSettleAccountItems(Table itemListTable) throws OException
	{
		String groupName = null;

		/* // do not provide until business related review by SME
		groupName = "Settlement Account, Buyer, Non Clfd Account";
		createSettleAccountItems(groupName, "AccBuy" + "None", itemListTable);
		groupName = "Settlement Account, Seller, Non Clfd Account";
		createSettleAccountItems(groupName, "AccSel" + "None", itemListTable);
		*/

		Table tblAccountClasses = getAccountClassData();
		for (int row = tblAccountClasses.getNumRows(); row > 0; --row)
		{
			groupName = "Settlement Account, Buyer, " + tblAccountClasses.getString("account_class_name", row);
			createSettleAccountItems(groupName, "AccBuy" + tblAccountClasses.getString("acc4_class", row), itemListTable);
			groupName = "Settlement Account, Seller, " + tblAccountClasses.getString("account_class_name", row);
			createSettleAccountItems(groupName, "AccSel" + tblAccountClasses.getString("acc4_class", row), itemListTable);
		}
		tblAccountClasses.destroy();
	}

	private void createSettleAccountItems(String groupName, String strSettlePrefix, Table itemListTable) throws OException
	{
		ItemList.add(itemListTable, groupName, "Account Number",         "olfSet" + strSettlePrefix + "AN", 1);
		ItemList.add(itemListTable, groupName, "Account Name",           "olfSet" + strSettlePrefix + "NA", 1);
		ItemList.add(itemListTable, groupName, "Currency",               "olfSet" + strSettlePrefix + "CY", 1);
		ItemList.add(itemListTable, groupName, "Settle Name",            "olfSet" + strSettlePrefix + "SN", 1);
		ItemList.add(itemListTable, groupName, "Holding Bank",           "olfSet" + strSettlePrefix + "HB", 1);
		ItemList.add(itemListTable, groupName, "Holding Bank Full Name", "olfSet" + strSettlePrefix + "HF", 1);
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException
	{
		int tranNum, numRows, row, ccy_id;//, query_id;
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
//		Transaction tran;
//		String sql;

		String internal_field_name = null;
		String output_field_name   = null;
		int internal_field_name_col_num = 0;
		int output_field_name_col_num   = 0;

		if (gendataTable.getNumRows() == 0)
			gendataTable.addRow();

		tranNum = eventTable.getInt("tran_num", 1);
		ccy_id  = eventTable.getInt("settle_ccy", 1);
		if (ccy_id < 0)
			ccy_id  = eventTable.getInt("tran_currency", 1);

//		tran = retrieveTransactionObjectFromArgt(tranNum);
//		if (Transaction.isNull(tran) == 1)
//		{
//			PluginLog.error ("Unable to retrieve transaction info due to invalid transaction object found. Tran#" + tranNum);
//		}
//		else
		{
			Table tblTranDataTable, 
				  tblIntSettle, 
				  tblExtSettle, 
				  tblAbTran, 
				  tblEvent;

			// Get necessary values
			tblTranDataTable = getExtendedTranDataTable();
			tblIntSettle = tblTranDataTable.getTable("internal_settle_table", 1).copyTable();
			tblExtSettle = tblTranDataTable.getTable("external_settle_table", 1).copyTable();
			tblAbTran = tblTranDataTable.getTable("ab_tran", 1);
			tblEvent = tblTranDataTable.getTable("event_table", 1);

			if (tblIntSettle.getColNum("account_iban") <= 0)
			{
				Table tblIBAN = Table.tableNew("account");
				tblIBAN.addCols("I(account_id)S(account_iban)");
				DBaseTable.loadFromDb(tblIBAN, "account");
				tblIntSettle.select(tblIBAN, "account_iban", "account_id EQ $account_id");
				tblExtSettle.select(tblIBAN, "account_iban", "account_id EQ $account_id");
				tblIBAN.destroy();
			}

			// work column for further data retrieval
			tblEvent.addCols("I(pay_rec)S(int_le_long)S(ext_le_long)S(int_bu_long)S(ext_bu_long)");

			// retrieve pay/rec into event data table
			/*
			query_id = Query.tableQueryInsert(tblEvent, "ins_num");
			sql = "select ins_num, param_seq_num, pay_rec from ins_parameter where ins_num in "
				+ "(select query_result from query_result where unique_id="+query_id+")";
			Table tblInsParam = Table.tableNew("ins_parameter");
			if (DBaseTable.execISql(tblInsParam, sql) != OLF_RETURN_SUCCEED)
				PluginLog.error("SQL failed:\n"+sql);
			else
				tblEvent.select(tblInsParam, "pay_rec", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
			tblInsParam.destroy();
			Query.clear(query_id);
			*/
			for (int r = tblEvent.getNumRows() + 1; --r > 0; )
				tblEvent.setInt("pay_rec", r, tblEvent.getDouble("settle_amount", r) < 0D ? 1 : 0);// pay:rec

			// retrieve long party names into event table
			for (int r = tblEvent.getNumRows() + 1; --r > 0; )
			{
				tblEvent.setString("int_le_long", r, Ref.getPartyLongName(tblEvent.getInt("internal_lentity", r)));
				tblEvent.setString("ext_le_long", r, Ref.getPartyLongName(tblEvent.getInt("external_lentity", r)));
				tblEvent.setString("int_bu_long", r, Ref.getPartyLongName(tblEvent.getInt("internal_bunit", r)));
				tblEvent.setString("ext_bu_long", r, Ref.getPartyLongName(tblEvent.getInt("external_bunit", r)));
			}

			final COL_TYPE_ENUM COL_TYPE_EVENT_NUM = COL_TYPE_ENUM.fromInt(tblEvent.getColType("event_num"));
			tblIntSettle.addCol("event_num", COL_TYPE_EVENT_NUM);
			tblExtSettle.addCol("event_num", COL_TYPE_EVENT_NUM);
			tblIntSettle.addCols("I(tran_num)I(ins_num)");
			tblExtSettle.addCols("I(tran_num)I(ins_num)");
			tblIntSettle.addCols("I(Pay_Rec)S(IntLE)S(ExtLE)S(IntBU)S(ExtBU)");
			tblExtSettle.addCols("I(Pay_Rec)S(IntLE)S(ExtLE)S(IntBU)S(ExtBU)");

			if (tblEvent.unsortedFindInt("event_type", EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt())> 0 ||
				tblEvent.unsortedFindInt("event_type", EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()) > 0)
			{
				// for invoices determine the event related accounts and remove all others
				tblIntSettle.select(tblEvent, "event_num", "settle_ccy EQ $currency_id "/*AND int_account_id EQ $account_id" */);
				tblIntSettle.deleteWhereValueInt64("event_num", 0L);
				tblExtSettle.select(tblEvent, "event_num", "settle_ccy EQ $currency_id "/*AND ext_account_id EQ $account_id" */);
				tblExtSettle.deleteWhereValueInt64("event_num", 0L);

				tblIntSettle.select(tblEvent, "pay_rec(Pay_Rec), int_le_long(IntLE), ext_le_long(ExtLE), int_bu_long(IntBU), ext_bu_long(ExtBU)", "event_num EQ $event_num");
				tblExtSettle.select(tblEvent, "pay_rec(Pay_Rec), int_le_long(IntLE), ext_le_long(ExtLE), int_bu_long(IntBU), ext_bu_long(ExtBU)", "event_num EQ $event_num");
			}
			else
			{
			}

			// delete work columns
			tblIntSettle.delCol("event_num"); tblIntSettle.delCol("tran_num"); tblIntSettle.delCol("ins_num"); tblIntSettle.makeTableUnique();
			tblExtSettle.delCol("event_num"); tblExtSettle.delCol("tran_num"); tblExtSettle.delCol("ins_num"); tblExtSettle.makeTableUnique();

		//	tblIntSettle.setColFormatAsRef("account_id", SHM_USR_TABLES_ENUM.ACCOUNT_TABLE);
		//	tblExtSettle.setColFormatAsRef("account_id", SHM_USR_TABLES_ENUM.ACCOUNT_TABLE);
			tblIntSettle.setColFormatAsRef("currency_id", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblExtSettle.setColFormatAsRef("currency_id", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblIntSettle.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
			tblExtSettle.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
			tblIntSettle.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
			tblExtSettle.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
			tblIntSettle.setColFormatAsRef("hold_delivery_type", SHM_USR_TABLES_ENUM.DELIVERY_MECHANISM_TABLE);
			tblExtSettle.setColFormatAsRef("hold_delivery_type", SHM_USR_TABLES_ENUM.DELIVERY_MECHANISM_TABLE);
			tblIntSettle.setColFormatAsRef("delivery_type", SHM_USR_TABLES_ENUM.DELIVERY_TYPE_TABLE);
			tblExtSettle.setColFormatAsRef("delivery_type", SHM_USR_TABLES_ENUM.DELIVERY_TYPE_TABLE);
			tblIntSettle.setColFormatAsRef("Pay_Rec", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);
			tblExtSettle.setColFormatAsRef("Pay_Rec", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);

			// account class
			Table tblAccClass = getAccountData();
			tblIntSettle.select(tblAccClass, "account_class,acc4_class", "account_id EQ $account_id");
			tblExtSettle.select(tblAccClass, "account_class,acc4_class", "account_id EQ $account_id");
			tblIntSettle.setColFormatAsRef("account_class", SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE);
			tblExtSettle.setColFormatAsRef("account_class", SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE);
			tblAccClass.destroy();

			for (int i=0, I=tblIntSettle.getNumRows(); ++i <= I;)
				if (tblIntSettle.getInt("account_class", i) <= 0)
					PluginLog.error("No Account Class set for account " + tblIntSettle.getString("account_name", i));
			for (int i=0, I=tblExtSettle.getNumRows(); ++i <= I;)
				if (tblExtSettle.getInt("account_class", i) <= 0)
					PluginLog.error("No Account Class set for account " + tblExtSettle.getString("account_name", i));

			tblIntSettle.group("account_class");
			tblExtSettle.group("account_class");

			// bic code values
			Table tblBicCode = Table.tableNew();
			tblBicCode.addCols("I(party_id)S(bic_code)");
			DBaseTable.loadFromDb(tblBicCode, "business_unit");
			tblIntSettle.select(tblBicCode, "bic_code", "party_id EQ $holder_id");
			tblExtSettle.select(tblBicCode, "bic_code", "party_id EQ $holder_id");
			tblBicCode.destroy();

			// additional holder info
			Table tblHolder = Table.tableNew();
			tblHolder.addCols("I(party_id)S(short_name)S(long_name)");
			DBaseTable.loadFromDb(tblHolder, "party");
			tblIntSettle.select(tblHolder, "short_name(holder_name),long_name(holder_long)", "party_id EQ $holder_id");
			tblExtSettle.select(tblHolder, "short_name(holder_name),long_name(holder_long)", "party_id EQ $holder_id");
			tblHolder.destroy();
						
			//additional Account Info field Sort code and account num to populate on invoice
			
			Table tblAccountInfo = Util.NULL_TABLE;
			try {
				tblAccountInfo = getAccountInfoData(tblIntSettle);
				tblIntSettle.select(tblAccountInfo, "sort_code, account_num", "account_id EQ $account_id");
				
			} finally {
				if(Table.isTableValid(tblAccountInfo) == 1){
					tblAccountInfo.destroy();
				}
			}
			
			tblIntSettle.addCols("I(holder_num_deli_codes)A(holder_deli_codes)");
			tblExtSettle.addCols("I(holder_num_deli_codes)A(holder_deli_codes)");
			for (int r=tblIntSettle.getNumRows(), holder_id; r>0;--r)
			{
				holder_id = tblIntSettle.getInt("holder_id", r);
				Table tblHolderDeliCodes = Table.tableNew("holder_deli_codes");
				tblIntSettle.setTable("holder_deli_codes", r, tblHolderDeliCodes);
				String sql = "select * from party_delivery_code where party_id="+holder_id;
				DBaseTable.execISql(tblHolderDeliCodes, sql);
				tblHolderDeliCodes.setColFormatAsRef("delivery_type", SHM_USR_TABLES_ENUM.DELIVERY_CODE_TABLE);
				tblHolderDeliCodes.convertColToString(tblHolderDeliCodes.getColNum("delivery_type"));
				tblHolderDeliCodes.colHide("party_id");
				tblIntSettle.setInt("holder_num_deli_codes", r, tblHolderDeliCodes.getNumRows());
			}
			for (int r=tblExtSettle.getNumRows(), holder_id; r>0;--r)
			{
				holder_id = tblExtSettle.getInt("holder_id", r);
				Table tblHolderDeliCodes = Table.tableNew("holder_deli_codes");
				tblExtSettle.setTable("holder_deli_codes", r, tblHolderDeliCodes);
				String sql = "select * from party_delivery_code where party_id="+holder_id;
				DBaseTable.execISql(tblHolderDeliCodes, sql);
				tblHolderDeliCodes.setColFormatAsRef("delivery_type", SHM_USR_TABLES_ENUM.DELIVERY_CODE_TABLE);
				tblHolderDeliCodes.convertColToString(tblHolderDeliCodes.getColNum("delivery_type"));
				tblHolderDeliCodes.colHide("party_id");
				tblExtSettle.setInt("holder_num_deli_codes", r, tblHolderDeliCodes.getNumRows());
			}

			tblIntSettle.convertColToString(tblIntSettle.getColNum("holder_id")); // req'?
			tblExtSettle.convertColToString(tblIntSettle.getColNum("holder_id")); // req'?

			tblIntSettle.makeTableUnique();
			tblExtSettle.makeTableUnique();

			HashMap<String,Integer> accountClassIntRow = new HashMap<String, Integer>();
			HashMap<String,Integer> accountClassExtRow = new HashMap<String, Integer>();
			Table tblAccountClasses = getAccountClassData();
			String acc4 = "None";
			accountClassExtRow.put(acc4, tblExtSettle.unsortedFindInt("account_class", 0));
			accountClassIntRow.put(acc4, tblIntSettle.unsortedFindInt("account_class", 0));
			for (int r = tblAccountClasses.getNumRows(); r > 0; --r)
			{
				acc4 = tblAccountClasses.getString("acc4_class", r);
				accountClassExtRow.put(acc4, tblExtSettle.unsortedFindString("acc4_class", acc4, SEARCH_CASE_ENUM.CASE_SENSITIVE));
				accountClassIntRow.put(acc4, tblIntSettle.unsortedFindString("acc4_class", acc4, SEARCH_CASE_ENUM.CASE_SENSITIVE));
			}
			tblAccountClasses.destroy();

			tblIntSettle.delCol("acc4_class"); tblExtSettle.delCol("acc4_class");

			addToContainer("External SIs (raw)", tblExtSettle);
			addToContainer("Internal SIs (raw)", tblIntSettle);

		//	boolean isBuy = tblAbTran.getInt("buy_sell", 1) == BUY_SELL_ENUM.BUY.jvsValue();
			Map<String, String> settleAccountFields = new HashMap<String, String>();

			Map<String, String> fieldToColumn = new HashMap<String, String>();
			fieldToColumn.put("Acct", "account_id");
			fieldToColumn.put("AcctName", "account_name");
			fieldToColumn.put("AcctNum", "account_number");
			fieldToColumn.put("AcctIBAN", "account_iban");
			fieldToColumn.put("AddCode", "hold_delivery_add_code");
			fieldToColumn.put("Addr1", "addr1");
			fieldToColumn.put("Addr2", "addr2");
			fieldToColumn.put("BankName", "short_name");
			fieldToColumn.put("Bic", "bic_code");
			fieldToColumn.put("Ccy", "currency_id");
			fieldToColumn.put("City", "city");
			fieldToColumn.put("Country", "country");
			fieldToColumn.put("DealDelType", "delivery_type");
			fieldToColumn.put("DeliCode", "hold_delivery_code");
			fieldToColumn.put("DeliType", "hold_delivery_type");
			fieldToColumn.put("Des", "description");
			fieldToColumn.put("Fax", "fax");
			fieldToColumn.put("HolderId", "holder_id");
			fieldToColumn.put("HolderName", "holder_name");
			fieldToColumn.put("HolderNumDeliCodes", "holder_num_deli_codes");
			fieldToColumn.put("HolderLongName", "holder_long");
			fieldToColumn.put("LongName", "long_name");
			fieldToColumn.put("ParDelAddCode", "party_delivery_add_code");
			fieldToColumn.put("ParDelCode", "party_delivery_code");
			fieldToColumn.put("ParDelType", "party_delivery_type");
			fieldToColumn.put("Phone", "phone");
			fieldToColumn.put("State", "state_id");
			fieldToColumn.put("Zip", "mail_code");
			fieldToColumn.put("SortCode", "sort_code");
			fieldToColumn.put("AccountNum", "account_num");


			//Add the required fields to the GenData table
			//Only fields that are checked in the item list will be added
			numRows = itemlistTable.getNumRows();
			// strings used as flags
			String olfSetAllSI = null, olfSetNumAllSIs = null;
			String olfSetExtSI = null, olfSetNumExtSIs = null;
			String olfSetIntSI = null, olfSetNumIntSIs = null;

			internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
			output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

			for (row = 1; row <= numRows; row++)
			{
			//	internal_field_name = itemlistTable.getString("internal_field_name", row);
				internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
			//	output_field_name   = itemlistTable.getString("output_field_name", row);
				output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

				if (internal_field_name == null || internal_field_name.trim().length() == 0)
					continue;

		// ==> "Settlement Instruction, Tables > Ext & Int Table (Flag)"
				else if (internal_field_name.equalsIgnoreCase("olfSetAllSIFlag"))
					olfSetAllSI = output_field_name;

		// ==> "Settlement Instruction, Tables > External Table (Flag)"
				else if (internal_field_name.equalsIgnoreCase("olfSetExtSIFlag"))
					olfSetExtSI = output_field_name;

		// ==> "Settlement Instruction, Tables > Internal Table (Flag)"
				else if (internal_field_name.equalsIgnoreCase("olfSetIntSIFlag"))
					olfSetIntSI = output_field_name;

		// ==> "Settlement Instruction, Tables > Num of All SIs"
				else if (internal_field_name.equalsIgnoreCase("olfSetNumAllSIs"))
					olfSetNumAllSIs = output_field_name;

		// ==> "Settlement Instruction, Tables > Num of Ext SIs"
				else if (internal_field_name.equalsIgnoreCase("olfSetNumExtSIs"))
					olfSetNumExtSIs = output_field_name;

		// ==> "Settlement Instruction, Tables > Num of Int SIs"
				else if (internal_field_name.equalsIgnoreCase("olfSetNumIntSIs"))
					olfSetNumIntSIs = output_field_name;

		// ==> "Settlement Instruction, ..., ..., Third Party";
				else if (internal_field_name.startsWith("3rdPty", 13))
				{
					// "1st" / "2nd"
					int thirdparty_type = internal_field_name.contains("1st") ? 0 : /* "2nd" */ 1, 
							int_ext = internal_field_name.startsWith("Int", 6) ? 0 : /* "Ext" */1;

					// "Int. Long Name"
					if(internal_field_name.endsWith("Name"))
					{
						String strValue = retrieveSettleInstrThirdPartyInfo(tranNum, int_ext, ccy_id, thirdparty_type, true);
						GenData.setField(gendataTable, output_field_name, strValue);
					}

					// "Int. Account Number"
					else if(internal_field_name.endsWith("AcctNum"))
					{
						String strValue = retrieveSettleInstrThirdPartyInfo(tranNum, int_ext, ccy_id, thirdparty_type, "account_num");
						GenData.setField(gendataTable, output_field_name, strValue);
					}

					// "Int. Delivery Code"
					else if(internal_field_name.endsWith("DeliCode"))
					{
						String strValue = retrieveSettleInstrThirdPartyInfo(tranNum, int_ext, ccy_id, thirdparty_type, "delivery_code");
						GenData.setField(gendataTable, output_field_name, strValue);
					}
				}

		// ==> "Settlement Instruction, Internal, (...)"
				else if (internal_field_name.startsWith("olfSetInt"))
				{
					String acc = internal_field_name.substring(9, 13); int rowSetInt = accountClassIntRow.get(acc);
					GenData.setField(gendataTable, output_field_name, tblIntSettle, fieldToColumn.get(internal_field_name.substring(13)), rowSetInt);
				}
				else if (internal_field_name.startsWith("Table_SetInt"))
				{
					String acc = internal_field_name.substring(12, 16); int rowSetInt = accountClassIntRow.get(acc);
					Table tbl;
					if (rowSetInt > 0)
						tbl = tblIntSettle.getTable("holder_deli_codes", rowSetInt).copyTable();
					else
					{
						tbl = Table.tableNew(output_field_name);
						tbl.addCols("S(delivery_type)S(code)S(additional_code)");
					}
					tbl.setTableName(output_field_name);
					GenData.setField(gendataTable, tbl);
				}

		// ==> "Settlement Instruction, External, (...)"
				else if (internal_field_name.startsWith("olfSetExt"))
				{
					String acc = internal_field_name.substring(9, 13); int rowSetExt = accountClassExtRow.get(acc);
					GenData.setField(gendataTable, output_field_name, tblExtSettle, fieldToColumn.get(internal_field_name.substring(13)), rowSetExt);
				}
				else if (internal_field_name.startsWith("Table_SetExt"))
				{
					String acc = internal_field_name.substring(12, 16); int rowSetExt = accountClassExtRow.get(acc);
					Table tbl;
					if (rowSetExt > 0)
						tbl = tblExtSettle.getTable("holder_deli_codes", rowSetExt).copyTable();
					else
					{
						tbl = Table.tableNew(output_field_name);
						tbl.addCols("S(delivery_type)S(code)S(additional_code)");
					}
					tbl.setTableName(output_field_name);
					GenData.setField(gendataTable, tbl);
				}


		// ==> "Settlement Account, Buyer, (...)"
				else if (internal_field_name.length() == 18 &&
						internal_field_name.startsWith("olfSetAccBuy"))
					settleAccountFields.put(internal_field_name, output_field_name);


		// ==> "Settlement Account, Seller, (...)"
				else if (internal_field_name.length() == 18 &&
						internal_field_name.startsWith("olfSetAccSel"))
					settleAccountFields.put(internal_field_name, output_field_name);

			}

			if (settleAccountFields.size() > 0)
			{
				Table tblAccountData = getSettleAccountDataTable(tblEvent, tblAbTran);
				tblAccountData.setTableName("oliAccountData");
				int colNum = tblAccountData.getNumCols();
				String colName;
				while (colNum > 0)
				{
					colName = tblAccountData.getColName(colNum);
					if (settleAccountFields.containsKey(colName))
						tblAccountData.setColName(colNum, settleAccountFields.get(colName));
					else
						tblAccountData.delCol(colNum);
					--colNum;
				}
				GenData.setField(gendataTable, tblAccountData);
			}

			int numIntSIs = tblIntSettle.getNumRows();
			int numExtSIs = tblExtSettle.getNumRows();

			if (olfSetAllSI != null)
			{
				Table tbl = tblIntSettle.copyTable();
				tbl.insertCol("int_ext", 1, COL_TYPE_ENUM.COL_STRING);
				if (numIntSIs > 0)
					tbl.setColValString(1, "int");

				Table tbl2 = tblExtSettle.copyTable();
				tbl2.insertCol("int_ext", 1, COL_TYPE_ENUM.COL_STRING);
				if (numExtSIs > 0)
					tbl2.setColValString(1, "ext");

				tbl2.copyRowAddAll(tbl);
				if (numIntSIs+numExtSIs < 1)
					tbl.addRow();
				tbl2.destroy();

				tbl.setTableName(olfSetAllSI);
				convertAllColsToString(tbl);
				adjustColNames(tbl);
				GenData.setField(gendataTable, tbl);
			}
			if (olfSetExtSI != null)
			{
				Table tbl = tblExtSettle.copyTable();
				tbl.insertCol("int_ext", 1, COL_TYPE_ENUM.COL_STRING);
				if (numExtSIs > 0)
					tbl.setColValString(1, "ext");
				else
					tbl.addRow();
				tbl.setTableName(olfSetExtSI);
				convertAllColsToString(tbl);
				adjustColNames(tbl);
				addToContainer("External SIs", tbl);
				GenData.setField(gendataTable, tbl);
			}
			if (olfSetIntSI != null)
			{
				Table tbl = tblIntSettle.copyTable();
				tbl.insertCol("int_ext", 1, COL_TYPE_ENUM.COL_STRING);
				if (numIntSIs > 0)
					tbl.setColValString(1, "int");
				else
					tbl.addRow();
				tbl.setTableName(olfSetIntSI);
				convertAllColsToString(tbl);
				adjustColNames(tbl);
				addToContainer("Internal SIs", tbl);
				GenData.setField(gendataTable, tbl);
			}

			if (olfSetNumAllSIs != null)
				GenData.setField(gendataTable, olfSetNumAllSIs, numIntSIs + numExtSIs);
			if (olfSetNumExtSIs != null)
				GenData.setField(gendataTable, olfSetNumExtSIs, numExtSIs);
			if (olfSetNumIntSIs != null)
				GenData.setField(gendataTable, olfSetNumIntSIs, numIntSIs);

			tblIntSettle.destroy();
			tblExtSettle.destroy();
		}
	}

	private Table getAccountInfoData(Table tblIntSettle) throws OException {
		
		Table tblDetail = Util.NULL_TABLE;
		int accountQueryID = 0;

		try {
			String queryTbl = "query_result";
			if(tblIntSettle.getNumRows() > 0) {
				accountQueryID = Query.tableQueryInsert(tblIntSettle, "account_id");
				queryTbl = Query.getResultTableForId(accountQueryID);
			}

			String info_sql = " SELECT Account_Id.account_id, Sort_Code.sort_code, Account_Num.account_num FROM account Account_Id \n"
					+ " JOIN " + queryTbl + " q ON q.query_result= Account_Id.account_id AND q.unique_id=" + accountQueryID + "\n"
					+ " LEFT JOIN ( SELECT account_id, info_value as sort_code FROM account_info ai \n"
					+ "             JOIN account_info_type ait ON ai.info_type_id = ait.type_id \n"
					+ " 			 AND ait.type_name = 'Sort Code') Sort_Code \n"
					+ " 			 ON Sort_Code.account_id = Account_Id.account_id \n"
					+ " LEFT JOIN ( SELECT account_id, info_value as account_num FROM account_info ai \n"
					+ "				JOIN account_info_type ait ON ai.info_type_id = ait.type_id \n"
					+ " 			AND ait.type_name = 'Account Num') Account_Num \n"
					+ "			   ON Account_Num.account_id = Account_Id.account_id \n";
			
			tblDetail = Table.tableNew();
			DBaseTable.execISql(tblDetail, info_sql);
			
		} catch (OException e) {

			String errorMessage = "Failed to Populate Account Info(Sort Code and Account Num) values, Please Check";
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		}
		
		finally {
			if (accountQueryID > 0) {
				Query.clear(accountQueryID);

			}
		}
		return tblDetail;
	}

	private void adjustColNames(Table tbl) throws OException
	{
		tbl.setColName("int_ext", "IntOrExt");
		tbl.setColName("account_id", "Acct");
		tbl.setColName("account_name", "AcctName");
		tbl.setColName("account_number", "AcctNum");
		tbl.setColName("account_iban", "AcctIBAN");
		tbl.setColName("account_class", "AcctClass");
		tbl.setColName("hold_delivery_add_code", "AddCode");
		tbl.setColName("addr1", "Addr1");
		tbl.setColName("addr2", "Addr2");
		tbl.setColName("short_name", "BankName");
		tbl.setColName("bic_code", "Bic");
		tbl.setColName("currency_id", "Ccy");
		tbl.setColName("city", "City");
		tbl.setColName("country", "Country");
		tbl.setColName("delivery_type", "DealDelType");
		tbl.setColName("hold_delivery_code", "DeliCode");
		tbl.setColName("hold_delivery_type", "DeliType");
		tbl.setColName("description", "Description");
		tbl.setColName("fax", "Fax");
		tbl.setColName("holder_id", "HolderId");
		tbl.setColName("holder_long", "HolderLongName");
		tbl.setColName("holder_name", "HolderName");
		tbl.setColName("long_name", "LongName");
		tbl.setColName("party_delivery_add_code", "ParDelAddCode");
		tbl.setColName("party_delivery_code", "ParDelCode");
		tbl.setColName("party_delivery_type", "ParDelType");
		tbl.setColName("phone", "Phone");
		tbl.setColName("state_id", "State");
		tbl.setColName("mail_code", "Zip");
		tbl.setColName("sort_code","SortCode");
		tbl.setColName("account_num","AccountNum");
	}

	private Table getExtendedTranDataTable() throws OException
	{
		Table tblTranDataTable, 
			  tblAbTranCopy, 
			  tblEventTable, 
			  tblTranNum;

		int intDocType;

		tblTranDataTable = OutboundDoc.getTranDataTable();

		tblEventTable = tblTranDataTable.getTable("event_table", 1);
		tblTranNum = Table.tableNew();
		tblTranNum.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		tblEventTable.copyCol("tran_num", tblTranNum, "tran_num");
		tblAbTranCopy = Table.tableNew();
		DBaseTable.loadFromDbWithWhatWhere(tblAbTranCopy, "ab_tran", tblTranNum);
		tblTranNum.destroy();

		tblTranDataTable.addCol("ab_tran", COL_TYPE_ENUM.COL_TABLE);
		tblTranDataTable.addCol("doc_type", COL_TYPE_ENUM.COL_INT);

		intDocType = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, "Confirm");
		tblTranDataTable.setTable("ab_tran", 1, tblAbTranCopy);
		tblTranDataTable.setInt("doc_type", 1, intDocType);

		return tblTranDataTable;
	}

	// *************************************************************************************************************

	private Table getAccountData() throws OException
	{
		Table tbl = Table.tableNew();
		tbl.addCols("I(account_id)I(account_class)I(account_type)");
		DBaseTable.loadFromDb(tbl, "account");
//		tbl.setColFormatAsRef(1, SHM_USR_TABLES_ENUM.ACCOUNT_TABLE);
//		tbl.setColFormatAsRef(2, SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE);
//		tbl.setColFormatAsRef(3, SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE);
		tbl.addCols("S(acc4_class)");
		tbl.copyColFromRef("account_class", "acc4_class", SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE);
		String acc;
		for (int row = tbl.getNumRows(); row > 0; --row)
		{
			acc = tbl.getString(4, row);
			if (acc != null && (acc = acc.trim()).length() > 0)
				if (acc.length() >= 4)
					tbl.setString(4, row, acc.substring(0, 4));
		}
		return tbl;
	}

	private Table getAccountClassData() throws OException
	{
		Table tbl = Table.tableNew();
		tbl.addCols("S(account_class_name)I(account_class_id)");
		DBaseTable.loadFromDb(tbl, "account_class");
		tbl.addCols("S(acc4_class)");
		tbl.copyColFromRef("account_class_id", "acc4_class", SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE);
		String acc;
		for (int row = tbl.getNumRows(); row > 0; --row)
		{
			acc = tbl.getString(3, row);
			if (acc != null && (acc = acc.trim()).length() > 0)
				if (acc.length() >= 4)
					tbl.setString(3, row, acc.substring(0, 4));
		}
		return tbl;
	}

	Table getSettleAccountDataTable(Table tblEventTable, Table tblAbTran) throws OException
	{
		Table 	tblData, 
			  	tblSQL;

		String 	strSQL, 
				strAccountClass, 
				strColName, 
				strValue;

		int 	intOverAllData, 
				intNumRows, 
				intCounter, 
				intNettingRefNum, 
				intTranNum, 
				intDataRows, 
				intSQLNumRows, 
				intSQLCounter, 
				intInternExtern;
		boolean isAccountOk;

		tblData = Table.tableNew();
		tblData.addCol("netting_refnum", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("tran_num", COL_TYPE_ENUM.COL_INT);

		String acc4;
		Table tblAccountClasses = getAccountClassData();
		for (int row = tblAccountClasses.getNumRows(); row > 0; --row)
		{
			acc4 = tblAccountClasses.getString("acc4_class", row);

			tblData.addCols(
					"S(olfSetAccBuy" + acc4 + "AN)" +
					"S(olfSetAccBuy" + acc4 + "NA)" +
					"S(olfSetAccBuy" + acc4 + "CY)" +
					"S(olfSetAccBuy" + acc4 + "SN)" +
					"S(olfSetAccBuy" + acc4 + "HB)" +
					"S(olfSetAccBuy" + acc4 + "HF)" );

			tblData.addCols(
					"S(olfSetAccSel" + acc4 + "AN)" +
					"S(olfSetAccSel" + acc4 + "NA)" +
					"S(olfSetAccSel" + acc4 + "CY)" +
					"S(olfSetAccSel" + acc4 + "SN)" +
					"S(olfSetAccSel" + acc4 + "HB)" +
					"S(olfSetAccSel" + acc4 + "HF)" );
		}
		tblAccountClasses.destroy();

		intOverAllData = 0;

		final int BUY = BUY_SELL_ENUM.BUY.jvsValue();
		final int INTERNAL = CONF_INT_EXT.INTERNAL.jvsValue();

		boolean isBuy;

		tblSQL = Table.tableNew();
		intNumRows = tblEventTable.getNumRows();
		for (intCounter = 1; intCounter <= intNumRows; intCounter++)
		{
			intNettingRefNum = tblEventTable.getInt("netting_refnum", intCounter);
			intTranNum = tblEventTable.getInt("tran_num", intCounter);
			isBuy = tblAbTran.getInt("buy_sell", intCounter) == BUY;

			intDataRows = tblData.addRow();
			tblData.setInt("netting_refnum", intDataRows, intNettingRefNum);
			tblData.setInt("tran_num", intDataRows, intTranNum);

			// 1.1 Begin
		//	strSQL	= "SELECT siv.account_number, " 
		//			+ "       siv.account_name, " 
		//			+ "       siv.currency_id, " 
		//			+ "       siv.int_ext, " 
		//			+ "       si.settle_name, " 
		//			+ "       p.short_name, " 
		//			+ "       p.long_name, " 
		//			+ "       acccl.account_class_name " 
		//			+ "FROM settle_info_view siv, " 
		//			+ "     settle_instructions si, " 
		//			+ "     party p, " 
		//			+ "     account acc, " 
		//			+ "     account_class acccl " 
		//			+ "WHERE siv.tran_num = " + Str.intToStr(intTranNum) 
		//			+ "  AND siv.settle_id = si.settle_id " 
		//			+ "  AND siv.holder_id = p.party_id " 
		//			+ "  AND si.account_id = acc.account_id " 
		//			+ "  AND acc.account_class = acccl.account_class_id";

			strSQL 	= "SELECT " 
					+ "acc.account_number " 
					+ ",acc.account_name " 
					+ ",absi.currency_id " 
					+ ",absi.int_ext " 
					+ ",si.settle_name " 
					+ ",p.short_name " 
					+ ",p.long_name " 
					+ ",acccl.account_class_name "
					+ "FROM " 
					+ "settle_instructions si " 
					+ ",party p " 
					+ ",account acc " 
					+ ",account_class acccl " 
					+ ",ab_tran_sttl_inst absi " 
					+ "WHERE " 
					+ "    absi.tran_num            = " + Str.intToStr(intTranNum) + " " 
					+ "AND absi.settle_instructions = si.settle_id " 
					+ "AND acc.holder_id            = p.party_id " 
					+ "AND si.account_id            = acc.account_id " 
				//	+ "AND acc.account_class        = acccl.account_class_id(+) ";
					+ "AND acc.account_class        = acccl.account_class_id ";
			// 1.1 End

			tblSQL.clearRows();
			DBaseTable.execISql(tblSQL, strSQL);
			intSQLNumRows = tblSQL.getNumRows();
			for (intSQLCounter = 1; intSQLCounter <= intSQLNumRows; intSQLCounter++)
			{
				intInternExtern = tblSQL.getInt("int_ext", intSQLCounter);
				strAccountClass = tblSQL.getString("account_class_name", intSQLCounter);

				strColName = "olfSetAcc";

				if (intInternExtern == INTERNAL)
					strColName += isBuy ? "Buy" : "Sel";
				else // intInternExtern: EXTERNAL
					strColName += isBuy ? "Sel" : "Buy";

				isAccountOk = false;
				String acc = strAccountClass;
				if (acc != null && (acc = acc.trim()).length() > 0)
					if (acc.length() >= 4)
					{
						strColName += acc.substring(0, 4);
						isAccountOk = true;
					}

				if (isAccountOk)
				{
					intOverAllData = 1;

					strValue = tblSQL.getString("account_number", intSQLCounter);
					tblData.setString(strColName + "AN", intDataRows, strValue);

					strValue = tblSQL.getString("account_name", intSQLCounter);
					tblData.setString(strColName + "NA", intDataRows, strValue);

					strValue = Table.formatRefInt(tblSQL.getInt("currency_id", intSQLCounter), SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
					tblData.setString(strColName + "CY", intDataRows, strValue);

					strValue = tblSQL.getString("settle_name", intSQLCounter);
					tblData.setString(strColName + "SN", intDataRows, strValue);

					strValue = tblSQL.getString("short_name", intSQLCounter);
					tblData.setString(strColName + "HB", intDataRows, strValue);

					strValue = tblSQL.getString("long_name", intSQLCounter);
					tblData.setString(strColName + "HF", intDataRows, strValue);
				}
			}
		}
		tblSQL.destroy();

		if (intOverAllData != 1)
		{
			PluginLog.warn("No data found.");
		}

		tblData.group("netting_refnum, tran_num");

		tblData.delCol("netting_refnum");
		tblData.delCol("tran_num");

		return tblData;
	}

/*
	// DO NOT USE
	private Table retrieveSettleInstrThirdPartyInfo(int tran_num) throws OException
	{
		Table tbl = Table.tableNew();
		String strSql = "select settle_instructions, int_ext, currency_id from ab_tran_sttl_inst where delivery_type = " + DELIVERY_TYPE_ENUM.DELIVERY_TYPE_CASH.toInt() + " and tran_num = " + tran_num;
		if (DBaseTable.execISql (tbl, strSql) == OLF_RETURN_SUCCEED && tbl.getNumRows() > 0);
		Table tblsettleIdList = Table.tableNew();
		tblsettleIdList.addCols("I(settle_id)");
		tbl.copyCol("settle_instructions", tblsettleIdList, "settle_id");
		Table tblThirdPartyInfo = Table.tableNew();
		tblThirdPartyInfo.addCols("I(settle_id)I(thirdparty_type)I(id)S(account_num)S(delivery_code)");
		DBaseTable.loadFromDbWithWhere(tblThirdPartyInfo, "settle_instruction_thirdparty", tblsettleIdList);
		tblsettleIdList.destroy();
		tbl.select(tblThirdPartyInfo, "*", "settle_id EQ $settle_instructions");
		tblThirdPartyInfo.destroy();

		tbl.addCols("S(name)");
		for (int row = tbl.getNumRows(); row>0;--row)
			tbl.setString("name", row, Ref.getPartyLongName(tbl.getInt("id", row)));
			
		return tbl;
	}
*/

	/**
	 * SQLs about 'ab_tran_sttl_inst' and 'settle_instruction_thirdparty'
	 * @param tran_num
	 * @param int_ext
	 * @param currency_id
	 * @param thirdparty_type
	 * @param getPartyLongName The Party is stored as integer, use the flag for conversion. This is ignored for other fields. 
	 * @return 
	 * @throws OException 
	 */
	String retrieveSettleInstrThirdPartyInfo(int tran_num, int int_ext, int currency_id, int thirdparty_type, boolean getPartyLongName) throws OException
	{
		String ret = null, sql = null;
		Table tbl = Table.tableNew();

		sql = "select tp.id from ab_tran_sttl_inst si, settle_instruction_thirdparty tp" 
			+ " where si.settle_instructions = tp.settle_id and si.delivery_type = " + DELIVERY_TYPE_ENUM.DELIVERY_TYPE_CASH.toInt()
			+ " and tp.thirdparty_type = " + thirdparty_type
			+ " and si.currency_id = " + currency_id
			+ " and si.int_ext = " + int_ext
			+ " and si.tran_num = " + tran_num;
		if (DBaseTable.execISql (tbl, sql) == OLF_RETURN_SUCCEED)
		{
			if (tbl.getNumRows() > 0)
			{
				int id = tbl.getInt("id", 1);
				ret = getPartyLongName ? Ref.getPartyLongName(id) : Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, id);
				if (getPartyLongName && ret.trim().length() == 0)
					PluginLog.warn("No long name found for party# " + id + " - " + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, id));
			}
			else
				ret = "";
		}
		else
		{
			PluginLog.error("Exec SQL failed: " + sql);
			ret = "";
		}

		tbl.destroy();
		return ret;
	}
	String retrieveSettleInstrThirdPartyInfo(int tran_num, int int_ext, int currency_id, int thirdparty_type, String what) throws OException
	{
		String ret = null, sql = null;
		Table tbl = Table.tableNew();

		sql = "select tp."+ what +" from ab_tran_sttl_inst si, settle_instruction_thirdparty tp" 
		+ " where si.settle_instructions = tp.settle_id and si.delivery_type = " + DELIVERY_TYPE_ENUM.DELIVERY_TYPE_CASH.toInt()
			+ " and tp.thirdparty_type = " + thirdparty_type
			+ " and si.currency_id = " + currency_id
			+ " and si.int_ext = " + int_ext
			+ " and si.tran_num = " + tran_num;
		if (DBaseTable.execISql (tbl, sql) == OLF_RETURN_SUCCEED)
			ret = tbl.getNumRows() > 0 ? tbl.getString(1, 1) : "";
		else
		{
			PluginLog.error("Exec SQL failed: " + sql);
			ret = "";
		}

		tbl.destroy();
		return ret;
	}
}
