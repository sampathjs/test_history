/**
 * Status: under construction
 * Additional Party Data
 * @author jbonetzky
 * @version 0.1
 */
/*
 * Version history:
 * 0.1  - initial
 * 0.2  - added Geographic Zone; revised data retrieval
 * 0.3  - revised 0.2 (more flexible sql using left join)
 * 0.4  - added County
 * 0.5  - added Mail Groups
 * 0.6  - added BU's Long/Short names
 * 0.7  - BU is now retrieved from strategy and not from account -> BU 
 * 0.8  - In the folder OLI MetalTransfer/AccountData/To Account a new item has to be added called 'Preferred UOM Quantity'. 
 * 			This new item should be populated according to the following logic.
 * 			1.	IF Strategy Unit (olfMtlTfStratInfo_Unit) NOT EQUAL to To Account Reporting Unit (olfMtlTfAcctInfoToAcct_20003), 
 * 				convert Strategy Quantity (olfMtlTfStratInfo_Qty) to the account reporting unit.
 * 			2.	Save that value in the field.
 * 			3.	If Strategy Unit and Account Reporting Unit are equal, use the value from Strategy Quantity.
 */

package com.openlink.sc.bo.docproc;

import java.util.HashMap;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

//@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_MODULE)
//@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class OLI_MOD_MetalTransfer extends OLI_MOD_ModuleBase implements IScript
{
	private String _tranInfo_StrategyDealNum  = "Strategy Num";
	private String _tranInfo_StrategyFromAcct = "From A/C";
	private String _tranInfo_StrategyToAcct   = "To A/C";
	private String _tranInfo_StrategyFromAcctBu = "From A/C BU";
	private String _tranInfo_StrategyToAcctBu   = "To A/C BU";

	private int
		 _tranInfo_StrategyDealNum_id  = 20054
		,_tranInfo_StrategyFromAcct_id = 20043
		,_tranInfo_StrategyToAcct_id   = 20044
		,_tranInfo_StrategyFromAcctBu_id = 20048
		,_tranInfo_StrategyToAcctBu_id   = 20055
		;


	protected ConstRepository _constRepo;
	protected static boolean _viewTables = false;

	public void execute(IContainerContext context) throws OException
	{
		_constRepo = new ConstRepository("BackOffice", "OLI-MetalTransfer");

		initPluginLog ();

		try
		{
			Table argt = context.getArgumentsTable();

			if (argt.getInt("GetItemList", 1) == 1) // if mode 1
			{
				//Generates user selectable item list
				PluginLog.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			}
			else //if mode 2
			{
				//Gets generation data
				PluginLog.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}
		}
		catch (Exception e)
		{
			PluginLog.error("Exception: " + e.getMessage());
		}

		PluginLog.exitWithStatus();
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
		String groupName = null;

		groupName = "Account Data";
		createAccountItems(itemListTable, groupName);

		groupName = "Strategy Data";
		createStrategyItems(itemListTable, groupName);

		groupName = "Party Addresses";
		createAddressesItems(itemListTable, groupName);

		groupName = "VAT Details";
		createVatDetailsItems(itemListTable, groupName);

		if (_viewTables)
			itemListTable.viewTable();
	}

	private void createAccountItems(Table itemListTable, String groupName) throws OException
	{
		String fieldPrefix = "olfMtlTf";

		ItemList.add(itemListTable, groupName+", From Account", "BU Long Name",  fieldPrefix + "FromAcct" + "BULongName",  1);
		ItemList.add(itemListTable, groupName+", To Account",   "BU Long Name",  fieldPrefix + "ToAcct"   + "BULongName",  1);
		ItemList.add(itemListTable, groupName+", From Account", "BU Short Name", fieldPrefix + "FromAcct" + "BUShortName", 1);
		ItemList.add(itemListTable, groupName+", To Account",   "BU Short Name", fieldPrefix + "ToAcct"   + "BUShortName", 1);

		ItemList.add(itemListTable, groupName+", From Account", "Account Name",   fieldPrefix + "FromAcct" + "Name", 1);
		ItemList.add(itemListTable, groupName+", To Account",   "Account Name",   fieldPrefix + "ToAcct"   + "Name", 1);
		ItemList.add(itemListTable, groupName+", From Account", "Account Number", fieldPrefix + "FromAcct" + "Num",  1);
		ItemList.add(itemListTable, groupName+", To Account",   "Account Number", fieldPrefix + "ToAcct"   + "Num",  1);
		ItemList.add(itemListTable, groupName+", From Account", "Account Type",   fieldPrefix + "FromAcct" + "Type", 1);
		ItemList.add(itemListTable, groupName+", To Account",   "Account Type",   fieldPrefix + "ToAcct"   + "Type", 1);

		ItemList.add(itemListTable, groupName+", From Account", "VAT Number",   fieldPrefix + "FromAcct" + "VatNum", 1);
		ItemList.add(itemListTable, groupName+", To Account",   "VAT Number",   fieldPrefix + "ToAcct"   + "VatNum", 1);
		
		//In the folder OLI MetalTransfer/AccountData/To Account a new item has to be added called 'Preferred UOM Quantity'.  
		ItemList.add(itemListTable, groupName+", To Account",   "Preferred UOM Quantity",   fieldPrefix + "ToAcct"   + "PreferredUOMQty", 1);


		createAccountInfoItems(itemListTable, groupName, fieldPrefix+"AcctInfo");
	}

	private void createAccountInfoItems(Table itemListTable, String groupName, String fieldPrefix) throws OException
	{
		Table tbl = Table.tableNew("account_info_type");
		try
		{
			tbl.addCols("I(type_id)S(type_name)");
			DBaseTable.loadFromDb(tbl, tbl.getTableName());
			String type_name;
			for (int row=tbl.getNumRows(), type_id;row>0; --row)
			{
				type_name = tbl.getString(2, row);
				type_id   = tbl.getInt(1, row);
				ItemList.add(itemListTable, groupName+", From Account"+", Info", type_name, fieldPrefix + "FromAcct" + "_" + type_id, 1);
				ItemList.add(itemListTable, groupName+", To Account"  +", Info", type_name, fieldPrefix + "ToAcct"   + "_" + type_id, 1);
			}
		}
		finally { tbl.destroy(); }
	}

	private void createStrategyItems(Table itemListTable, String groupName) throws OException
	{
		String fieldPrefix = "olfMtlTf";
		ItemList.add(itemListTable, groupName, "Strategy Name",   fieldPrefix + "StratName", 1);
		ItemList.add(itemListTable, groupName, "Strategy Number", fieldPrefix + "StratNum",  1);
		createStrategyInfoItems(itemListTable, groupName+", Info", fieldPrefix+"StratInfo");
	}

	private void createStrategyInfoItems(Table itemListTable, String groupName, String fieldPrefix) throws OException
	{
		Table tbl = Table.tableNew("strategy_info_types_view");
		try
		{
			tbl.addCols("I(type_id)S(type_name)");
			DBaseTable.loadFromDb(tbl, tbl.getTableName());
			for (int row=tbl.getNumRows();row>0; --row)
				ItemList.add(itemListTable, groupName, tbl.getString(2, row), fieldPrefix + "_" + tbl.getInt(1, row), 1);
		}
		finally { tbl.destroy(); }
	}

	private void createAddressesItems(Table itemListTable, String groupName) throws OException
	{
		Table tblAddressTypes = getAddressTypesTable("AddressTypes");

		createAddressItemGroup(itemListTable, groupName + ", From Business Unit", "olfMtlTfFromBU", tblAddressTypes);
		createAddressItemGroup(itemListTable, groupName + ", To Business Unit",   "olfMtlTfToBU",   tblAddressTypes);

		tblAddressTypes.destroy();
	}

	private void createAddressItemGroup(Table itemListTable, String groupName, String fieldPrefix, Table addressTypes) throws OException
	{
		String groupNameFull = null, fieldPrefixFull = null;

		for (int row=addressTypes.getNumRows(); row > 0; --row)
		{
			groupNameFull = groupName + ", " + addressTypes.getString("name", row);
			fieldPrefixFull = fieldPrefix + addressTypes.getString("short", row);

			ItemList.add(itemListTable, groupNameFull, "Default",          fieldPrefixFull + "DefFlag",  1);
			ItemList.add(itemListTable, groupNameFull, "Address 1",        fieldPrefixFull + "Addr1",    1);
			ItemList.add(itemListTable, groupNameFull, "Address 2",        fieldPrefixFull + "Addr2",    1);
			ItemList.add(itemListTable, groupNameFull, "City",             fieldPrefixFull + "City",     1);
			ItemList.add(itemListTable, groupNameFull, "State",            fieldPrefixFull + "State",    1);
			ItemList.add(itemListTable, groupNameFull, "Country",          fieldPrefixFull + "Country",  1);
			ItemList.add(itemListTable, groupNameFull, "Mail Code",        fieldPrefixFull + "MailCode", 1);
			ItemList.add(itemListTable, groupNameFull, "Phone",            fieldPrefixFull + "Phone",    1);
			ItemList.add(itemListTable, groupNameFull, "Fax",              fieldPrefixFull + "Fax",      1);
			ItemList.add(itemListTable, groupNameFull, "Description",      fieldPrefixFull + "Desc",     1);
			createAddressItemContact(itemListTable, groupNameFull + ", Contact", fieldPrefixFull + "Cont");
			ItemList.add(itemListTable, groupNameFull, "Reference Name",   fieldPrefixFull + "RefName",  1);
			ItemList.add(itemListTable, groupNameFull, "County",           fieldPrefixFull + "County",   1);
			ItemList.add(itemListTable, groupNameFull, "IRS Terminal Num", fieldPrefixFull + "IRSTN",    1);
			ItemList.add(itemListTable, groupNameFull, "Geog Zone",        fieldPrefixFull + "GeogZone", 1);
		}
	}

	private void createAddressItemContact(Table itemListTable, String groupName, String fieldPrefix) throws OException
	{
		ItemList.add(itemListTable, groupName, "First Name",  fieldPrefix + "FirstName", 1);
		ItemList.add(itemListTable, groupName, "Last Name",   fieldPrefix + "LastName",  1);
		ItemList.add(itemListTable, groupName, "Name",        fieldPrefix + "Name",      1);
		ItemList.add(itemListTable, groupName, "Employee Id", fieldPrefix + "EmpId",     1);
		ItemList.add(itemListTable, groupName, "Title",       fieldPrefix + "Title",     1);
		ItemList.add(itemListTable, groupName, "Address 1",   fieldPrefix + "Addr1",     1);
		ItemList.add(itemListTable, groupName, "Address 2",   fieldPrefix + "Addr2",     1);
		ItemList.add(itemListTable, groupName, "City",        fieldPrefix + "City",      1);
		ItemList.add(itemListTable, groupName, "State",       fieldPrefix + "State",     1);
		ItemList.add(itemListTable, groupName, "Country",     fieldPrefix + "Country",   1);
		ItemList.add(itemListTable, groupName, "Geog Zone",   fieldPrefix + "GeogZone",  1);
		ItemList.add(itemListTable, groupName, "Mail Code",   fieldPrefix + "MailCode",  1);
		ItemList.add(itemListTable, groupName, "Phone",       fieldPrefix + "Phone",     1);
		ItemList.add(itemListTable, groupName, "Fax",         fieldPrefix + "Fax",       1);
		ItemList.add(itemListTable, groupName, "Email",       fieldPrefix + "Email",     1);
	//	ItemList.add(itemListTable, groupName, "Zip",         fieldPrefix + "Zip",       1);
	}

	private void createVatDetailsItems(Table itemListTable, String groupName) throws OException
	{
		String fieldPrefix = "olfMtlTf";

		ItemList.add(itemListTable, groupName, "VAT Amount (Deal Ccy)",  fieldPrefix + "VatDet" + "Amount",  1);
		ItemList.add(itemListTable, groupName, "VAT Amount (VAT Ccy)",  fieldPrefix + "VatDet" + "ConvAmount",  1);
		ItemList.add(itemListTable, groupName, "Deal Currency",  fieldPrefix + "VatDet" + "Ccy",  1);
		ItemList.add(itemListTable, groupName, "VAT Currency",  fieldPrefix + "VatDet" + "ConvCcy",  1);
		ItemList.add(itemListTable, groupName, "FX Rate",  fieldPrefix + "VatDet" + "FXRate",  1);
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException
	{
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
		itemlistTable.group("output_field_name");

		Table container = Table.tableNew(getClass().getSimpleName());
		container.addCol("tables", COL_TYPE_ENUM.COL_TABLE);
		if (_viewTables)
			container.setTable(1, container.addRow(), itemlistTable.copyTable());

		if (gendataTable.getNumRows() == 0)
			gendataTable.addRow();

		Table tblTranInfoTypes = Table.tableNew("tran_info_types");
		try
		{
			tblTranInfoTypes.addCols("I(type_id)S(type_name)");
			DBaseTable.loadFromDb(tblTranInfoTypes, tblTranInfoTypes.getTableName());

			int typeRow = tblTranInfoTypes.unsortedFindString(2, _tranInfo_StrategyDealNum, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			_tranInfo_StrategyDealNum_id = typeRow <= 0 ? -1 : tblTranInfoTypes.getInt(1, typeRow);
			typeRow = tblTranInfoTypes.unsortedFindString(2, _tranInfo_StrategyFromAcct, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			_tranInfo_StrategyFromAcct_id = typeRow <= 0 ? -1 : tblTranInfoTypes.getInt(1, typeRow);
			typeRow = tblTranInfoTypes.unsortedFindString(2, _tranInfo_StrategyToAcct, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			_tranInfo_StrategyToAcct_id = typeRow <= 0 ? -1 : tblTranInfoTypes.getInt(1, typeRow);
			typeRow = tblTranInfoTypes.unsortedFindString(2, _tranInfo_StrategyFromAcctBu, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			_tranInfo_StrategyFromAcctBu_id = typeRow <= 0 ? -1 : tblTranInfoTypes.getInt(1, typeRow);
			typeRow = tblTranInfoTypes.unsortedFindString(2, _tranInfo_StrategyToAcctBu, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			_tranInfo_StrategyToAcctBu_id = typeRow <= 0 ? -1 : tblTranInfoTypes.getInt(1, typeRow);
		}
		finally { tblTranInfoTypes.destroy(); }

		int tranNum = eventTable.getInt("tran_num", 1);
		String sqlStrategy
			= "select h.tran_num, h.linked_deal, h_from.value from_account, h_to.value to_account, "
			+ "     h_from_bu.value from_bu_name, h_to_bu.value to_bu_name"
			+ " from (select tran_num, cast(value as int) linked_deal from ab_tran_info where type_id="+_tranInfo_StrategyDealNum_id+") h"
			+ " left join ab_tran_info h_from on h.linked_deal=h_from.tran_num and h_from.type_id="+_tranInfo_StrategyFromAcct_id
			+ " left join ab_tran_info h_to on h.linked_deal=h_to.tran_num and h_to.type_id="+_tranInfo_StrategyToAcct_id
			+ " left join ab_tran_info h_from_bu on h.linked_deal=h_from_bu.tran_num and h_from_bu.type_id="+_tranInfo_StrategyFromAcctBu_id
			+ " left join ab_tran_info h_to_bu on h.linked_deal=h_to_bu.tran_num and h_to_bu.type_id="+_tranInfo_StrategyToAcctBu_id
			+ " where h.tran_num="+tranNum
			;
		Table tblStrategy = Table.tableNew("Strategy for tran# "+tranNum);
		container.setTable(1, container.addRow(), tblStrategy);
		DBaseTable.execISql(tblStrategy, sqlStrategy);
		int COL_TYPE_INT = COL_TYPE_ENUM.COL_INT.toInt();
		int ACCOUNT_TABLE = SHM_USR_TABLES_ENUM.ACCOUNT_TABLE.toInt();
		tblStrategy.convertStringCol(3, COL_TYPE_INT, ACCOUNT_TABLE);
		tblStrategy.convertStringCol(4, COL_TYPE_INT, ACCOUNT_TABLE);
		tblStrategy.addCols("I(from_bu)I(to_bu)");
		tblStrategy.setColFormatAsRef(5, SHM_USR_TABLES_ENUM.PARTY_TABLE);
		tblStrategy.setColFormatAsRef(6, SHM_USR_TABLES_ENUM.PARTY_TABLE);

		int intStrategy = tblStrategy.getInt(2, 1);
		String sqlStrategyInfo
			= "select ati.tran_num,sit.type_id,sit.type_name,ati.value,sit.default_value"
			+ " ,case when ati.tran_num>0 then ati.value else sit.default_value end as value2"
			+ " from strategy_info_types_view sit"
			+ " left join ab_tran_info ati on ati.type_id=sit.type_id"
			+ " and ati.tran_num="+intStrategy
			;
		Table tblStrategyInfo = Table.tableNew("Info for strategy# "+intStrategy);
		container.setTable(1, container.addRow(), tblStrategyInfo);
		DBaseTable.execISql(tblStrategyInfo, sqlStrategyInfo);

		int intFromAcct = tblStrategy.getInt(3, 1);
		int intToAcct   = tblStrategy.getInt(4, 1);
//		String sqlPartyAccount
//			= "select party_id, account_id"
//			+ " from party_account"
//			+ " where account_id in ("+intFromAcct+","+intToAcct+")"
//			;
//		Table tblPartyAccount = Table.tableNew("PartyAccount");
//		container.setTable(1, container.addRow(), tblPartyAccount);
//		DBaseTable.execISql(tblPartyAccount, sqlPartyAccount);
		String fromBuName = tblStrategy.getString("from_bu_name", 1);
 		String toBuName = tblStrategy.getString("to_bu_name", 1);
		int fromBuId = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, fromBuName);
		int toBuId = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, toBuName);
		tblStrategy.setInt("from_bu", 1, fromBuId);
		tblStrategy.setInt("to_bu", 1, toBuId);
		
		String sqlAccount
			= "select account_id, account_name, account_type, account_number"
			+ " from account where account_id in ("+intFromAcct+","+intToAcct+")"
			;
		Table tblAccount = Table.tableNew("Account");
		container.setTable(1, container.addRow(), tblAccount);
		DBaseTable.execISql(tblAccount, sqlAccount);
		tblAccount.setColFormatAsRef(3, SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE);
		tblAccount.convertColToString(3);

		String sqlAccountInfo
			= "select ai.account_id,ait.type_id,ait.type_name"
			+ " ,case when ai.account_id>0 then ai.info_value else ait.default_value end as value"
			+ " from account_info_type ait"
			+ " left join account_info ai on ait.type_id=ai.info_type_id"
			+ " and ai.account_id in ("+intFromAcct+","+intToAcct+")"
			;
		Table tblAcctInfoFrom = Table.tableNew("Info for (from) account id "+intFromAcct);
		container.setTable(1, container.addRow(), tblAcctInfoFrom);
		DBaseTable.execISql(tblAcctInfoFrom, sqlAccountInfo);
		Table tblAcctInfoTo   = tblAcctInfoFrom.copyTable();
		container.setTable(1, container.addRow(), tblAcctInfoTo);
		tblAcctInfoTo.setTableName("Info for (to) account id "+intToAcct);
		tblAcctInfoFrom.deleteWhereValue(1, intToAcct);
		tblAcctInfoTo.deleteWhereValue(1, intFromAcct);


		int intFromBUnit   = tblStrategy.getInt("from_bu", 1);
		int intToBUnit     = tblStrategy.getInt("to_bu", 1);

		Table tblPartyNames = getPartyNamesTable("AddressItems", intFromBUnit, intToBUnit);
		container.setTable(1, container.addRow(), tblPartyNames);

		Table tblAddressTypes = getAddressTypesTable("AddressTypes");
		container.setTable(1, container.addRow(), tblAddressTypes);
		Table tblAddressItems = getAddressItemsTable("AddressItems", intFromBUnit, intToBUnit, OCalendar.today());
		container.setTable(1, container.addRow(), tblAddressItems);
//		long total, start;
/*
start = System.currentTimeMillis();
Table tblContactItemsBak = getContactItemsTable_bak("ContactItems_bak", tblAddressItems);
total = System.currentTimeMillis() - start;
container.setTable(1, container.addRow(), tblContactItemsBak);
OConsole.print("\n >>>>>>>> "+"contact retrieval took "+total+" ms\n");
*/
//		start = System.currentTimeMillis();
		Table tblContactItems = getContactItemsTable("ContactItems", tblAddressItems);
//		total = System.currentTimeMillis() - start;
		container.setTable(1, container.addRow(), tblContactItems);
//OConsole.print("\n >>>>>>>> "+"contact retrieval took "+total+" ms\n");


		int intCashDeal = -1;
		Table tblCashDeal = Table.tableNew("Cash deal for strategy");
		String sqlCashDeal
			= "select at.tran_num"
			+ " from ab_tran at"
			+ " join ab_tran_info ati on ati.tran_num=at.tran_num"
			+ " where at.tran_status=3 and at.ins_sub_type="+INS_SUB_TYPE.cash_transaction.toInt()+" and at.cflow_type="+Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, "VAT")
			+ " and ati.type_id="+_tranInfo_StrategyDealNum_id+" and ati.value='"+intStrategy+"'"
			;
		DBaseTable.execISql(tblCashDeal, sqlCashDeal);
		switch (tblCashDeal.getNumRows())
		{
			case 0:
				break;
			case 1:
				intCashDeal = tblCashDeal.getInt(1,1);
				break;
			default:
				intCashDeal = tblCashDeal.getInt(1,1);
				PluginLog.warn("Multiple Cash deals found");
				PluginLog.debug(tblCashDeal);
				break;
		}
		tblCashDeal.destroy();

		Transaction tran = null;
		if (intCashDeal > 0)
		{
			tran = Transaction.retrieve(intCashDeal);
			if (Transaction.isNull(tran) == 1)
			{
				PluginLog.error("Unable to retrieve transaction. Tran#" + tranNum);
				tran = null;
			}
		}

		HashMap<String,String> addressFieldToColumn = getAddressFieldToColumnMap();
		HashMap<String,String> contactFieldToColumn = getContactFieldToColumnMap();

		//Add the required fields to the GenData table
		//Only fields that are checked in the item list will be added
		itemlistTable.group("output_field_name");

		String internal_field_name = null;
		String output_field_name   = null;
		int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
		int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

		PluginLog.debug("Prepared data");
		for (int row = 0, numRows = itemlistTable.getNumRows(); ++row <= numRows; )
		{
			internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
			output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

			if (internal_field_name == null || internal_field_name.trim().length() == 0)
				continue;

			// Strategy, Strategy Name
			else if (internal_field_name.equals("olfMtlTf"+"StratName"))
			{
				String strValue = Ref.getName(SHM_USR_TABLES_ENUM.STRATEGY_LISTING_TABLE, intStrategy);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Strategy, Strategy Number
			else if (internal_field_name.equals("olfMtlTf"+"StratNum"))
			{
				String strValue = "" + intStrategy;
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, From, BU Long Name
			else if (internal_field_name.equals("olfMtlTf"+"FromAcct"+"BULongName"))
			{
				int buRow = tblPartyNames.unsortedFindInt("party_id", intFromBUnit);
				String strValue = buRow > 0 ? tblPartyNames.getString("long_name", buRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}
			// Account, From, BU Short Name
			else if (internal_field_name.equals("olfMtlTf"+"FromAcct"+"BUShortName"))
			{
				int buRow = tblPartyNames.unsortedFindInt("party_id", intFromBUnit);
				String strValue = buRow > 0 ? tblPartyNames.getString("short_name", buRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, From, Account Name
			else if (internal_field_name.equals("olfMtlTf"+"FromAcct"+"Name"))
			{
				int acctRow = tblAccount.unsortedFindInt("account_id", intFromAcct);
				String strValue = acctRow > 0 ? tblAccount.getString("account_name", acctRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}
			// Account, From, Account Num
			else if (internal_field_name.equals("olfMtlTf"+"FromAcct"+"Num"))
			{
				int acctRow = tblAccount.unsortedFindInt("account_id", intFromAcct);
				String strValue = acctRow > 0 ? tblAccount.getString("account_number", acctRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}
			// Account, From, Account Type
			else if (internal_field_name.equals("olfMtlTf"+"FromAcct"+"Type"))
			{
				int acctRow = tblAccount.unsortedFindInt("account_id", intFromAcct);
				String strValue = acctRow > 0 ? tblAccount.getString("account_type", acctRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, From, VAT Number
			else if (internal_field_name.equals("olfMtlTf"+"FromAcct"+"VatNum"))
			{
				String strValue = retrieveTaxId(intFromBUnit);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, To, BU Long Name
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"BULongName"))
			{
				int buRow = tblPartyNames.unsortedFindInt("party_id", intToBUnit);
				String strValue = buRow > 0 ? tblPartyNames.getString("long_name", buRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}
			// Account, To, BU Short Name
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"BUShortName"))
			{
				int buRow = tblPartyNames.unsortedFindInt("party_id", intToBUnit);
				String strValue = buRow > 0 ? tblPartyNames.getString("short_name", buRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, To, Account Name
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"Name"))
			{
				int acctRow = tblAccount.unsortedFindInt("account_id", intToAcct);
				String strValue = acctRow > 0 ? tblAccount.getString("account_name", acctRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}
			// Account, To, Account Num
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"Num"))
			{
				int acctRow = tblAccount.unsortedFindInt("account_id", intToAcct);
				String strValue = acctRow > 0 ? tblAccount.getString("account_number", acctRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}
			// Account, To, Account Type
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"Type"))
			{
				int acctRow = tblAccount.unsortedFindInt("account_id", intToAcct);
				String strValue = acctRow > 0 ? tblAccount.getString("account_type", acctRow).trim() : "";
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, To, VAT Number
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"VatNum"))
			{
				String strValue = retrieveTaxId(intToBUnit);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, From, Info
			else if (internal_field_name.startsWith("olfMtlTf"+"AcctInfo"+"FromAcct"))
			{
				String strValue = internal_field_name.substring(("olfMtlTf"+"AcctInfo"+"FromAcct").length()).replaceAll("[^0-9]", "");
				if (strValue.length() == 0)
				{
					PluginLog.error("Couldn't extract id from '" + internal_field_name + "'");
					continue;
				}
				int typeId = Str.strToInt(strValue);
				int infoRow = tblAcctInfoFrom.unsortedFindInt("type_id", typeId);
				strValue = infoRow > 0 ? tblAcctInfoFrom.getString("value", infoRow).trim() : "";

				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Account, To, Info
			else if (internal_field_name.startsWith("olfMtlTf"+"AcctInfo"+"ToAcct"))
			{
				String strValue = internal_field_name.substring(("olfMtlTf"+"AcctInfo"+"ToAcct").length()).replaceAll("[^0-9]", "");
				if (strValue.length() == 0)
				{
					PluginLog.error("Couldn't extract id from '" + internal_field_name + "'");
					continue;
				}
				int typeId = Str.strToInt(strValue);
				int infoRow = tblAcctInfoTo.unsortedFindInt("type_id", typeId);
				strValue = infoRow > 0 ? tblAcctInfoTo.getString("value", infoRow).trim() : "";

				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Strategy, Info
			else if (internal_field_name.startsWith("olfMtlTf"+"StratInfo"))
			{
				String strValue = internal_field_name.substring(("olfMtlTf"+"StratInfo").length()).replaceAll("[^0-9]", "");
				if (strValue.length() == 0)
				{
					PluginLog.error("Couldn't extract id from '" + internal_field_name + "'");
					continue;
				}
				int typeId = Str.strToInt(strValue);
				int infoRow = tblStrategyInfo.unsortedFindInt("type_id", typeId);
				strValue = infoRow > 0 ? tblStrategyInfo.getString("value2", infoRow).trim() : "";

				GenData.setField(gendataTable, output_field_name, strValue);
			}
			
			// Account, From, Preferred UOM Qty
			else if (internal_field_name.equals("olfMtlTf"+"ToAcct"+"PreferredUOMQty"))
			{
				int findRow = tblStrategyInfo.unsortedFindString("type_name", "Unit", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strategyInfoUnit = tblStrategyInfo.getString("value2", findRow);
				int strategyInfoUnitValue = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strategyInfoUnit);
				
				findRow = tblStrategyInfo.unsortedFindString("type_name", "Qty", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strategyInfoQty = tblStrategyInfo.getString("value2", findRow);
				
				findRow = tblAcctInfoTo.unsortedFindString("type_name", "Reporting Unit", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String acctInfoReportingUnit = tblAcctInfoTo.getString("value", findRow);
				int acctInfoReportingUnitValue = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, acctInfoReportingUnit);

				String strValue = strategyInfoQty; 

				if(strategyInfoUnitValue!= acctInfoReportingUnitValue){
					String sql = "\nSELECT src_unit_id, dest_unit_id, factor"
							   + "\nFROM unit_conversion"
							   + "\nWHERE src_unit_id = " + strategyInfoUnitValue + " AND dest_unit_id = " + acctInfoReportingUnitValue;
							   ;
					Table convTable = Table.tableNew("SQL result with unit_conversion");
					int ret = DBaseTable.execISql(convTable, sql);
					if (ret != OLF_RETURN_SUCCEED) {
						String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
						throw new OException(errorMessage);
					}
					double factor = convTable.getDouble("factor", 1);
					double retRlt = Double.parseDouble(strategyInfoQty) * factor; 
					strValue = retRlt + "";
					
					convTable.destroy();
				} 
				
				
				
				GenData.setField(gendataTable, output_field_name, strValue);
			}			

			// Party Address, From BUnit
			else if (internal_field_name.startsWith("olfMtlTfFromBU"))
			{
				String strValue = retrievePartyAddressItem(internal_field_name.substring(14), intFromBUnit, tblAddressTypes, tblAddressItems, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// Party Address, To BUnit
			else if (internal_field_name.startsWith("olfMtlTfToBU"))
			{
				String strValue = retrievePartyAddressItem(internal_field_name.substring(12), intToBUnit, tblAddressTypes, tblAddressItems, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// VAT Details, 
			else if (internal_field_name.equals("olfMtlTf"+"VatDet"+"Amount"))
			{
				String strValue = tran == null ? "" : tran.getField(TRANF_FIELD.TRANF_POSITION.toInt(), 0, null, 0, 0);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// VAT Details, 
			else if (internal_field_name.equals("olfMtlTf"+"VatDet"+"ConvAmount"))
			{
				String strValue = tran == null ? "" : tran.getField(TRANF_FIELD.TRANF_CONV_AMOUNT.toInt(), 0, null, 0, 0);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// VAT Details, 
			else if (internal_field_name.equals("olfMtlTf"+"VatDet"+"Ccy"))
			{
				String strValue = tran == null ? "" : tran.getField(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0, null, 0, 0);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// VAT Details, 
			else if (internal_field_name.equals("olfMtlTf"+"VatDet"+"ConvCcy"))
			{
				String strValue = tran == null ? "" : tran.getField(TRANF_FIELD.TRANF_CONV_CCY.toInt(), 0, null, 0, 0);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// VAT Details, 
			else if (internal_field_name.equals("olfMtlTf"+"VatDet"+"FXRate"))
			{
				String strValue = tran == null ? "" : tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Ccy Conv FX Rate", 0, 0);
				GenData.setField(gendataTable, output_field_name, strValue);
			}

			// [n/a]
			else
				GenData.setField(gendataTable, output_field_name, "[n/a]");
		}	

		if (_viewTables)
		{
			container.setTable(1, container.addRow(), gendataTable.copyTable());
			container.viewTable();
		}

		// cleanup
		if (tran != null) tran.destroy();
		container.destroy(); container = null;
		addressFieldToColumn.clear(); addressFieldToColumn = null;
		contactFieldToColumn.clear(); contactFieldToColumn = null;
	}

	String retrievePartyAddressItem(String field_name, int intPartyId, Table tblAddressTypes, Table tblAddressItems, Table tblContactItems, HashMap<String, String> addressFieldToColumn, HashMap<String, String> contactFieldToColumn) throws OException
	{
		/* Structure of 'field_name':
		 * NOTE: this is derived from internal_field_name, being a substring
		 * 4 chars: short name of address type
		 * 0/4 chars: 'Cont' for Contact
		 * remaining chars: specific address field
		 * */

		String address_type = field_name.substring(0, 4);
		int address_row = tblAddressTypes.unsortedFindString("short", address_type, SEARCH_CASE_ENUM.CASE_SENSITIVE);

		if (address_row <= 0)
			return "[n/a]";

		int address_type_id = tblAddressTypes.getInt("id", address_row);

		int start_row = tblAddressItems.findInt("party_id", intPartyId, SEARCH_ENUM.FIRST_IN_GROUP),
			end_row   = tblAddressItems.findInt("party_id", intPartyId, SEARCH_ENUM.LAST_IN_GROUP);

		address_row = tblAddressItems.findIntRange("address_type", start_row, end_row, address_type_id, SEARCH_ENUM.FIRST_IN_GROUP);
		if (address_row <= 0)
			return "";

		String value = "";
		if (field_name.startsWith("Cont", 4))
		{
			int contact_id = tblAddressItems.getInt("contact_id", address_row), contact_row;
			if (contact_id == 0 || (contact_row = tblContactItems.findInt("id_number", contact_id, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
				return "";

			String item = field_name.substring(8);
			value = tblContactItems.getString(contactFieldToColumn.get(item), contact_row).trim();
		}
		else
		{
			String item = field_name.substring(4);
			value = tblAddressItems.getString(addressFieldToColumn.get(item), address_row).trim();
		}

		return value;
	}

	/**
	 * Returns VAT Number(s) if available
	 * @param bunit A business unit
	 * @return vat number or empty string
	 */
	private String retrieveTaxId(int bunit) throws OException
	{
		String sql
			= "select ti.tax_id vat_number"
		//	+ ",pr.business_unit_id business_unit"
		//	+ ",pr.legal_entity_id legal_entity"
		//	+ ",ti.jd_party_id,jd.country,le.country"
			+ " from tax_id ti"
			+ " join party_relationship pr on pr.legal_entity_id=ti.le_party_id"
			+ " join party_function pf on ti.jd_party_id=pf.party_id and pf.function_type=13"
		//	+ " join legal_entity le on le.party_id=ti.le_party_id"
		//	+ " join business_unit jd on jd.party_id=ti.jd_party_id and jd.country=le.country"
			+ " where pr.business_unit_id="+bunit
		;

		Table tbl = Table.tableNew();
		try
		{
			if (OLF_RETURN_SUCCEED == DBaseTable.execISql(tbl, sql))
			{
				tbl.makeTableUnique();
				String str = "";
				for (int r = 0, R = tbl.getNumRows(); ++r <= R; )
					str += ", "+ tbl.getString(1, r);
				if (str.length() > 2)
					str = str.substring(2);
				return str;
			}
		}
		finally { tbl.destroy(); }

		return "";
	}

	private HashMap<String, String> getAddressFieldToColumnMap()
	{
		HashMap<String,String> map = new HashMap<String, String>();
		map.put("DefFlag",  "default_flag");
		map.put("Addr1",    "addr1");
		map.put("Addr2",    "addr2");
		map.put("City",     "city");
		map.put("State",    "state_id");
		map.put("Country",  "country");
		map.put("MailCode", "mail_code");
		map.put("Phone",    "phone");
		map.put("Fax",      "fax");
		map.put("Desc",     "description");
		map.put("RefName",  "addr_reference_name");
		map.put("County",   "county_id");
		map.put("IRSTN",    "irs_terminal_num");
		map.put("GeogZone", "geographic_zone");
		return map;
	}

	private HashMap<String, String> getContactFieldToColumnMap()
	{
		HashMap<String,String> map = new HashMap<String, String>();
		map.put("FirstName", "first_name");
		map.put("LastName",  "last_name");
		map.put("Name",      "name");
		map.put("EmpId",     "employee_id");
		map.put("Title",     "title");
		map.put("Addr1",     "addr1");
		map.put("Addr2",     "addr2");
		map.put("City",      "city");
		map.put("State",     "state_id");
		map.put("Country",   "country");
		map.put("MailCode",  "mail_code");
		map.put("Phone",     "phone");
		map.put("Fax",       "fax");
		map.put("Email",     "email");
		map.put("GeogZone",  "geographic_zone");
		return map;
	}
/*
	private Table getContactItemsTable_bak(String tableName, Table tblAddressItems) throws OException
	{
		int queryContactId = Query.tableQueryInsert(tblAddressItems, "contact_id");
		String sql  = "select p.*, c.geographic_zone"
					+ " from personnel p"
					+ " join query_result q on p.id_number = q.query_result and q.unique_id = " + queryContactId
					+ " left join country c on p.country = c.id_number";

		Table tbl = Table.tableNew(tableName);
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);
		Query.clear(queryContactId);

		tbl.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
		tbl.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
		tbl.setColFormatAsRef("geographic_zone", SHM_USR_TABLES_ENUM.GEOGRAPHIC_ZONE_TABLE);

		for (int row = tbl.getNumRows(), state_id = tbl.getColNum("state_id"); row > 0; --row)
			if (tbl.getInt(state_id, row) <= 0)
				tbl.setInt(state_id, row, -1);

		tbl.group("id_number");

		tbl.convertColToString(tbl.getColNum("state_id"));
		tbl.convertColToString(tbl.getColNum("country"));
		tbl.convertColToString(tbl.getColNum("geographic_zone"));

		return tbl;
	}
*/
	private Table getContactItemsTable(String tableName, Table tblAddressItems) throws OException
	{
		Table tbl = Table.tableNew();

		Table tblIDs = Table.tableNew();
		try
		{
			// prepare identifiers for retrieval from database
			tblIDs.addCols("I(id_number)"); // personnel.id_number
			tblAddressItems.copyColDistinct("contact_id", tblIDs, "id_number");

			// getting all columns
			tbl.setTableName("personnel");
			DBUserTable.structure(tbl);
			tbl.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
			tbl.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
			DBaseTable.loadFromDb(tbl, "personnel", tblIDs);
			tblIDs.clearDataRows();

			// clean unspecified 'state_id'
			for (int row = tbl.getNumRows(), state_id = tbl.getColNum("state_id"); row > 0; --row)
				if (tbl.getInt(state_id, row) <= 0)
					tbl.setInt(state_id, row, -1);

			// append 'geographic_zone'
			tbl.addCols("I(geographic_zone)");
			tbl.setColFormatAsRef("geographic_zone", SHM_USR_TABLES_ENUM.GEOGRAPHIC_ZONE_TABLE);
			// prepare identifiers for retrieval from database
			tbl.copyColDistinct("country", tblIDs, "id_number"); // country.id_number

			// retrieve 'geographic_zone'
			Table tblGZ = Table.tableNew();
			try
			{
				tblGZ.addCols("I(id_number)I(geographic_zone)");
				DBaseTable.loadFromDb(tblGZ, "country", tblIDs);
				if (tblGZ.getNumRows() > 0)
					// apply retrieved values
					tbl.select(tblGZ, "geographic_zone", "id_number EQ $country");
			}
			finally { tblGZ.destroy(); }
		}
		finally { tblIDs.destroy(); }

		// convert IDs to Names/Strings
		tbl.convertColToString(tbl.getColNum("state_id"));
		tbl.convertColToString(tbl.getColNum("country"));
		tbl.convertColToString(tbl.getColNum("geographic_zone"));

		// name & sort
		tbl.setTableName(tableName);
		tbl.group("id_number");
		return tbl;
	}

	private Table getPartyNamesTable(String tableName, int fromBU, int toBU) throws OException
	{
		String sql = "select party_id, short_name, long_name from party where party_id in ("+fromBU+","+toBU+")";
		Table tbl = Table.tableNew(tableName);
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);
		return tbl;
	}

	private Table getAddressItemsTable(String tableName, int fromBU, int toBU, int today) throws OException
	{
		String sql  = "select a.*, c.geographic_zone from party_address a"
					+ " join (select party_id, address_type, MAX(effective_date) effective_date_max from party_address"
					+ "  where party_id in (" + fromBU + "," + toBU + ")"
					+ "  and effective_date <= '" + OCalendar.formatJdForDbAccess(today) + "'"
					+ "  group by party_id, address_type) h"
					+ "   on a.party_id = h.party_id and a.address_type = h.address_type and a.effective_date = h.effective_date_max"
					+ " left join country c on a.country = c.id_number";

		Table tbl = Table.tableNew(tableName);
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);

		tbl.setColFormatAsRef("default_flag", SHM_USR_TABLES_ENUM.NO_YES_TABLE);
		tbl.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
		tbl.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
		tbl.setColFormatAsRef("county_id", SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE);
		tbl.setColFormatAsRef("geographic_zone", SHM_USR_TABLES_ENUM.GEOGRAPHIC_ZONE_TABLE);

		for (int row = tbl.getNumRows(), state_id = tbl.getColNum("state_id"); row > 0; --row)
			if (tbl.getInt(state_id, row) <= 0)
				tbl.setInt(state_id, row, -1);

		tbl.group("party_id, address_type, default_flag");

		tbl.convertColToString(tbl.getColNum("default_flag"));
		tbl.convertColToString(tbl.getColNum("state_id"));
		tbl.convertColToString(tbl.getColNum("country"));
		tbl.convertColToString(tbl.getColNum("county_id"));
		tbl.convertColToString(tbl.getColNum("geographic_zone"));

		return tbl;
	}

	Table getAddressTypesTable(String tableName) throws OException
	{
		String sql = "select address_type_name name, '' short, address_type_id id, 1 counter from party_address_type order by address_type_name, address_type_id desc";

		Table tbl = Table.tableNew(tableName);
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);
		int row = tbl.getNumRows();
		String str = null;
		int counter = 0;
		while (row > 0)
		{
			str = tbl.getString(1, row);
			if (str.length() > 4)
				str = str.substring(0, 4).trim();
			str = str.replace(' ', '_');
			while (str.length() < 4)
				str += "_";

			++counter;
			if (tbl.unsortedFindString(2, str, SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
				tbl.setInt(4, row, counter);
			else
				counter = 1;
			tbl.setString(2, row, str);

			--row;
		}

		/*
		// next step: number ambiguous short names
		tbl.addCol("row_num", COL_TYPE_ENUM.COL_INT);
		tbl.setColIncrementInt("row_num", 1, 1);
		// do something
		tbl.delCol("row_num");
		*/

		return tbl;
	}
}
