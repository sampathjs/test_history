package com.openlink.sc.bo.docproc;
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
 * 0.6  - external Mail Groups may regard to internal parties
 * 0.7  - remove retrieval of personnel from LE's 
 * 
 * 2017-05-22	V0.8	jwaechter	- Added retrieval of JM Group BU
 *                                  - Added retrieval of Business Unit Owner Party
 * 2017-07-05	V1      sma			- CR46 added getConfirmCopyFunctionalGroup() 
 * 									using constants repository variable "Confirm Copy Functional Group" 
 * 									for olfMailGroupExt_JMGroupConfirmCopy
 * 2020-08-17   V1.1    kumarh02    - Changes for P2907                    		       
 */		 

import java.util.HashMap;

import standard.back_office_module.include.JVS_INC_STD_DocMsg;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.olf.jm.logging.Logging;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_MODULE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_MOD_PartyData extends OLI_MOD_ModuleBase implements IScript {
	
	private static final String CONST_REPO_VAR_CFLOW = "fx_cflow_type";
	protected ConstRepository _constRepo;
	protected static boolean _viewTables = false;

	public void execute(IContainerContext context) throws OException {
		_constRepo = new ConstRepository("BackOffice", "OLI-PartyData");
		initLogging ();

		try {
			Table argt = context.getArgumentsTable();
			if (argt.getInt("GetItemList", 1) == 1) 			{
				// if mode 1
				//Generates user selectable item list
				Logging.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			} else 		{
				//if mode 2
				//Gets generation data
				Logging.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}
		} catch (Exception e) {
			Logging.error("Exception: " + e.getMessage());
		}finally{
			Logging.close();
		}

		
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
		} catch (Exception e)	{
			// do something
		}

		try {
			_viewTables = _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		} catch (Exception e) {
			// do something
		}
	}

	/*
	 * Add items to selection list
	 */
	private void createItemsForSelection(Table itemListTable) throws OException {
		String groupName = null;
		groupName = "Party Addresses";
		createAddressesItems(itemListTable, groupName);

		groupName = "Mail Groups";
		createMailGroupItems(itemListTable, groupName);

		groupName = "JM Group BU";
		createBusinessUnitOwnerItems (itemListTable, groupName);

		if (_viewTables){
			itemListTable.viewTable();
		}
	}

	private void createBusinessUnitOwnerItems(Table itemListTable, String groupName) throws OException {
		
		ItemList.add(itemListTable, groupName , "Long Name",  "olfExtJMConfirmCopyBULongName", 1);
		ItemList.add(itemListTable, groupName , "Short Name",  "olfExtJMConfirmCopyBUShortName", 1);
	}

	private void createAddressesItems(Table itemListTable, String groupName) throws OException {
		Table tblAddressTypes = getAddressTypesTable("AddressTypes");
		try {
			createAddressItemGroup(itemListTable, groupName + ", External Business Unit", "olfExtBU", tblAddressTypes);
			createAddressItemGroup(itemListTable, groupName + ", External Legal Entity",  "olfExtLE", tblAddressTypes);
			createAddressItemGroup(itemListTable, groupName + ", Internal Business Unit", "olfIntBU", tblAddressTypes);
			createAddressItemGroup(itemListTable, groupName + ", Internal Legal Entity",  "olfIntLE", tblAddressTypes);
			createAddressItemGroup(itemListTable, groupName + ", Business Unit Owner Party", "olfExtJMConfirmCopyParty", tblAddressTypes);
		} finally {
			if (Table.isTableValid(tblAddressTypes) == 1) {
				tblAddressTypes.destroy();	
			}
		}
	}

	private void createAddressItemGroup(Table itemListTable, String groupName, String fieldPrefix, Table addressTypes) throws OException {
		String groupNameFull = null, fieldPrefixFull = null;

		for (int row=addressTypes.getNumRows(); row > 0; --row) {
			groupNameFull = groupName + ", " + addressTypes.getString("name", row);
			fieldPrefixFull = fieldPrefix + addressTypes.getString("short", row);
			
			ItemList.add(itemListTable, groupNameFull, "Default",          fieldPrefixFull + "DefFlag", 1);
			ItemList.add(itemListTable, groupNameFull, "Address 1",        fieldPrefixFull + "Addr1", 1);
			ItemList.add(itemListTable, groupNameFull, "Address 2",        fieldPrefixFull + "Addr2", 1);
			ItemList.add(itemListTable, groupNameFull, "City",             fieldPrefixFull + "City", 1);
			ItemList.add(itemListTable, groupNameFull, "State",            fieldPrefixFull + "State", 1);
			ItemList.add(itemListTable, groupNameFull, "Country",          fieldPrefixFull + "Country", 1);
			ItemList.add(itemListTable, groupNameFull, "Mail Code",        fieldPrefixFull + "MailCode", 1);
			ItemList.add(itemListTable, groupNameFull, "Phone",            fieldPrefixFull + "Phone", 1);
			ItemList.add(itemListTable, groupNameFull, "Fax",              fieldPrefixFull + "Fax", 1);
			ItemList.add(itemListTable, groupNameFull, "Description",      fieldPrefixFull + "Desc", 1);
			createAddressItemContact(itemListTable, groupNameFull + ", Contact", fieldPrefixFull + "Cont");
			ItemList.add(itemListTable, groupNameFull, "Reference Name",   fieldPrefixFull + "RefName", 1);
			ItemList.add(itemListTable, groupNameFull, "County",           fieldPrefixFull + "County", 1);
			ItemList.add(itemListTable, groupNameFull, "IRS Terminal Num", fieldPrefixFull + "IRSTN", 1);
			ItemList.add(itemListTable, groupNameFull, "Geog Zone",        fieldPrefixFull + "GeogZone", 1);
		}
	}

	private void createAddressItemContact(Table itemListTable, String groupName, String fieldPrefix) throws OException {
		ItemList.add(itemListTable, groupName, "First Name",  fieldPrefix + "FirstName", 1);
		ItemList.add(itemListTable, groupName, "Last Name",   fieldPrefix + "LastName", 1);
		ItemList.add(itemListTable, groupName, "Name",        fieldPrefix + "Name", 1);
		ItemList.add(itemListTable, groupName, "Employee Id", fieldPrefix + "EmpId", 1);
		ItemList.add(itemListTable, groupName, "Title",       fieldPrefix + "Title", 1);
		ItemList.add(itemListTable, groupName, "Address 1",   fieldPrefix + "Addr1", 1);
		ItemList.add(itemListTable, groupName, "Address 2",   fieldPrefix + "Addr2", 1);
		ItemList.add(itemListTable, groupName, "City",        fieldPrefix + "City", 1);
		ItemList.add(itemListTable, groupName, "State",       fieldPrefix + "State", 1);
		ItemList.add(itemListTable, groupName, "Country",     fieldPrefix + "Country", 1);
		ItemList.add(itemListTable, groupName, "Geog Zone",   fieldPrefix + "GeogZone", 1);
		ItemList.add(itemListTable, groupName, "Mail Code",   fieldPrefix + "MailCode", 1);
		ItemList.add(itemListTable, groupName, "Phone",       fieldPrefix + "Phone", 1);
		ItemList.add(itemListTable, groupName, "Fax",         fieldPrefix + "Fax", 1);
		ItemList.add(itemListTable, groupName, "Email",       fieldPrefix + "Email", 1);
		//	ItemList.add(itemListTable, groupName, "Zip",         fieldPrefix + "Zip", 1);
	}

	private void createMailGroupItems(Table tblItemList, String groupName) throws OException {
		final String TABLE_FUNCTIONAL_GROUP = "functional_group";
		Table tbl = Table.tableNew(TABLE_FUNCTIONAL_GROUP);
		try {
			tbl.addCols("S(name)");
			DBaseTable.loadFromDb(tbl, TABLE_FUNCTIONAL_GROUP);
			// TODO check uniqueness of Functional Group names
			String sOriginalName, sFeasibleName;
			for (int row = tbl.getNumRows(), colName = tbl.getColNum("name"); row > 0; --row) {
				sFeasibleName = getFeasibleFuncGroupName(sOriginalName = tbl.getString(colName, row));
				ItemList.add(tblItemList, groupName+",for Internal Parties", sOriginalName, "olfMailGroupInt_"+sFeasibleName, 1);
				ItemList.add(tblItemList, groupName+",for External Parties", sOriginalName, "olfMailGroupExt_"+sFeasibleName, 1);
			}
			
			ItemList.add(tblItemList, groupName+",for External Parties", "JM Group Confirm Copy", "olfMailGroupExt_JMGroupConfirmCopy", 1);
		} finally {
			if (Table.isTableValid(tbl) == 1) {
				tbl.destroy(); 
			}
		}
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException {
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
		itemlistTable.group("output_field_name");

		if (gendataTable.getNumRows() == 0) {
			gendataTable.addRow();
		}

		int intExtLEntity = eventTable.getInt("external_lentity", 1);
		int intIntLEntity = eventTable.getInt("internal_lentity", 1);
		int intExtBUnit   = eventTable.getInt("external_bunit", 1);
		int intIntBUnit   = eventTable.getInt("internal_bunit", 1);
		int tranNum       = eventTable.getInt("tran_num", 1); 
		
		int olfSetExtMetaAcct = 0;
		String olfSetExtMetaAcctStr = JVS_INC_STD_DocMsg.GenData.getField("olfSetExtMetaAcct");
		if (olfSetExtMetaAcctStr != null && olfSetExtMetaAcctStr.trim().length() > 0) {
			olfSetExtMetaAcct = Integer.parseInt(olfSetExtMetaAcctStr);			
		}
		int businessOwner = getBusinessOwnerForAccount (olfSetExtMetaAcct);

		Table container = Table.tableNew(getClass().getSimpleName());
		container.addCol("tables", COL_TYPE_ENUM.COL_TABLE);
		if (_viewTables){
			container.setTable(1, container.addRow(), itemlistTable.copyTable());
		}
		Table tblAddressTypes = getAddressTypesTable("AddressTypes");
		container.setTable(1, container.addRow(), tblAddressTypes);
		Table tblAddressItems = getAddressItemsTable("AddressItems", intExtLEntity, intIntLEntity, intExtBUnit, intIntBUnit, OCalendar.today());
		Table tblAddressItemsJMBu = getAddressItemsTableJMBu("AddressItems", businessOwner, OCalendar.today());
		container.setTable(1, container.addRow(), tblAddressItems);
		Table tblContactItems = getContactItemsTable("ContactItems", tblAddressItems);
		container.setTable(1, container.addRow(), tblContactItems);

		Table tblMailGroupItems = getMailGroupsItemTable("MailGroupItems", intExtLEntity, intIntLEntity, intExtBUnit, intIntBUnit, businessOwner, tranNum);
		//		Table tblMailGroupItems = getMailGroupsItemTable("MailGroupItems", intExtBUnit, intIntBUnit);
		container.setTable(1, container.addRow(), tblMailGroupItems);

		HashMap<String,String> addressFieldToColumn = getAddressFieldToColumnMap();
		HashMap<String,String> contactFieldToColumn = getContactFieldToColumnMap();

		HashMap<String,String> funcMailGroupsInt = new HashMap<String, String>(itemlistTable.getNumRows());
		HashMap<String,String> funcMailGroupsExt = new HashMap<String, String>(itemlistTable.getNumRows());

		//Add the required fields to the GenData table
		//Only fields that are checked in the item list will be added
		itemlistTable.group("output_field_name");

		String internal_field_name = null;
		String output_field_name   = null;
		int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
		int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

		Logging.debug("Prepared data");
		for (int row = 0, numRows = itemlistTable.getNumRows(); ++row <= numRows; ) {
			internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
			output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

			if (internal_field_name == null || internal_field_name.trim().length() == 0){
				continue;
			} else if (internal_field_name.startsWith("olfExtJMConfirmCopyParty")) {
				// Party Address, Business Unit Owner Party
				String strValue = retrievePartyAddressItem(internal_field_name.substring(24), businessOwner, tblAddressTypes, tblAddressItemsJMBu, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			} else if (internal_field_name.startsWith("olfExtBU")) {			
				// Party Address, External BUnit
				String strValue = retrievePartyAddressItem(internal_field_name.substring(8), intExtBUnit, tblAddressTypes, tblAddressItems, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			} else if (internal_field_name.startsWith("olfExtLE"))			{
				// Party Address, External LEntity
				String strValue = retrievePartyAddressItem(internal_field_name.substring(8), intExtLEntity, tblAddressTypes, tblAddressItems, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			} else if (internal_field_name.startsWith("olfIntBU"))			{
				// Party Address, Internal BUnit
				String strValue = retrievePartyAddressItem(internal_field_name.substring(8), intIntBUnit, tblAddressTypes, tblAddressItems, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			} 			else if (internal_field_name.startsWith("olfIntLE"))			{
				// Party Address, Internal LEntity
				String strValue = retrievePartyAddressItem(internal_field_name.substring(8), intIntLEntity, tblAddressTypes, tblAddressItems, tblContactItems, addressFieldToColumn, contactFieldToColumn);
				GenData.setField(gendataTable, output_field_name, strValue);
			} else if (internal_field_name.startsWith("olfMailGroupInt_"))			{
				//Functional/Mail Group, for Internal Parties
				funcMailGroupsInt.put(internal_field_name.substring(16), output_field_name);
			} 		else if (internal_field_name.startsWith("olfMailGroupExt_"))			{
				//Functional/Mail Group, for External Parties
				funcMailGroupsExt.put(internal_field_name.substring(16), output_field_name);
			} else if (   internal_field_name.equals("olfExtJMConfirmCopyBULongName") || internal_field_name.equals("olfExtJMConfirmCopyBUShortName")) {
				
				String strValue = null;
				if (internal_field_name.equals("olfExtJMConfirmCopyBULongName")) {
					strValue = Ref.getPartyLongName(businessOwner);
				} else {
					strValue = Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, businessOwner);
				}
				GenData.setField(gendataTable, output_field_name, strValue);
			}  else{
				// [n/a]
				GenData.setField(gendataTable, output_field_name, "[n/a]");
			}
		}

		if (funcMailGroupsInt.size() > 0) {
			Table tbl = tblMailGroupItems.copyTable();
			try {
				tbl.deleteWhereValue("int_ext", 1);// internal remain
				setMailGroupFields(gendataTable,tbl,funcMailGroupsInt);
			} finally {
				if (Table.isTableValid(tbl) == 1) {
					tbl.destroy();	
				}
			}
		}
		
		if (funcMailGroupsExt.size() > 0) {
			Table tbl = tblMailGroupItems.copyTable();
			try {
				tbl.deleteWhereValue("int_ext", 0);// external remain
				setMailGroupFields(gendataTable,tbl,funcMailGroupsExt);
			} finally {
				if (Table.isTableValid(tbl) == 1) {
					tbl.destroy();	
				}
			}
		}

		if (_viewTables) {
			container.setTable(1, container.addRow(), gendataTable.copyTable());
			container.viewTable();
		}

		// cleanup
		if (Table.isTableValid(container) == 1) {
			container.destroy();
			container = null;
		}
		
		addressFieldToColumn.clear(); addressFieldToColumn = null;
		contactFieldToColumn.clear(); contactFieldToColumn = null;
	}

	private Table getAddressItemsTableJMBu(String string, int businessOwner, int today) throws OException {
		String sql  = "select a.*, c.geographic_zone from party_address a"
				+ " join (select party_id, address_type, MAX(effective_date) effective_date_max from party_address"
				+ "  where party_id in (" + businessOwner + ")"
				+ "  and effective_date <= '" + OCalendar.formatJdForDbAccess(today) + "'"
				+ "  group by party_id, address_type) h"
				+ "   on a.party_id = h.party_id and a.address_type = h.address_type and a.effective_date = h.effective_date_max"
				+ " left join country c on a.country = c.id_number";

		Table tbl = Table.tableNew("JM Business Owner Table");
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);

		tbl.setColFormatAsRef("default_flag", SHM_USR_TABLES_ENUM.NO_YES_TABLE);
		tbl.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
		tbl.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
		tbl.setColFormatAsRef("county_id", SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE);
		tbl.setColFormatAsRef("geographic_zone", SHM_USR_TABLES_ENUM.GEOGRAPHIC_ZONE_TABLE);

		for (int row = tbl.getNumRows(), state_id = tbl.getColNum("state_id"); row > 0; --row){
			if (tbl.getInt(state_id, row) <= 0){
				tbl.setInt(state_id, row, -1);
			}
		}

		tbl.group("party_id, address_type, default_flag");

		tbl.convertColToString(tbl.getColNum("default_flag"));
		tbl.convertColToString(tbl.getColNum("state_id"));
		tbl.convertColToString(tbl.getColNum("country"));
		tbl.convertColToString(tbl.getColNum("county_id"));
		tbl.convertColToString(tbl.getColNum("geographic_zone"));

		return tbl;	
	}

	private int getBusinessOwnerForAccount(int olfSetExtMetaAcct) throws OException {
		String sql = "\nSELECT business_unit_owner FROM account WHERE account_id = " +  olfSetExtMetaAcct ;
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String error = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
				Logging.error(error);
				throw new OException (error);
			}
			if (sqlResult.getNumRows() == 0) {
				return 0;
			} else {
				return sqlResult.getInt("business_unit_owner", 1);
			}
		} finally {
			if (Table.isTableValid(sqlResult) == 1) {
				sqlResult.destroy();
				sqlResult = null;
			}
		}		
	}

	String retrievePartyAddressItem(String field_name, int intPartyId, Table tblAddressTypes, Table tblAddressItems, Table tblContactItems, HashMap<String, String> addressFieldToColumn, HashMap<String, String> contactFieldToColumn) throws OException	{
		
		/* Structure of 'field_name':
		 * NOTE: this is derived from internal_field_name, being a substring
		 * 4 chars: short name of address type
		 * 0/4 chars: 'Cont' for Contact
		 * remaining chars: specific address field
		 * */

		String address_type = field_name.substring(0, 4);
		int address_row = tblAddressTypes.unsortedFindString("short", address_type, SEARCH_CASE_ENUM.CASE_SENSITIVE);

		if (address_row <= 0) {
			return "[n/a]";
		}

		int address_type_id = tblAddressTypes.getInt("id", address_row);
		int start_row = tblAddressItems.findInt("party_id", intPartyId, SEARCH_ENUM.FIRST_IN_GROUP),
				end_row   = tblAddressItems.findInt("party_id", intPartyId, SEARCH_ENUM.LAST_IN_GROUP);

		address_row = tblAddressItems.findIntRange("address_type", start_row, end_row, address_type_id, SEARCH_ENUM.FIRST_IN_GROUP);
		if (address_row <= 0) {
			return "";
		}

		String value = "";
		if (field_name.startsWith("Cont", 4)) {
			int contact_id = tblAddressItems.getInt("contact_id", address_row), contact_row;
			if (contact_id == 0 || (contact_row = tblContactItems.findInt("id_number", contact_id, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0){
				return "";
			}
			String item = field_name.substring(8);
			value = tblContactItems.getString(contactFieldToColumn.get(item), contact_row).trim();
		} else {
			String item = field_name.substring(4);
			value = tblAddressItems.getString(addressFieldToColumn.get(item), address_row).trim();
		}
		return value;
	}

	private HashMap<String, String> getAddressFieldToColumnMap() {
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

	private HashMap<String, String> getContactFieldToColumnMap() {
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

	private Table getContactItemsTable(String tableName, Table tblAddressItems) throws OException {
		int queryContactId = -1;
		try {
			queryContactId = Query.tableQueryInsert(tblAddressItems, "contact_id");
			String sql  = "SELECT p.*, c.geographic_zone"
					+ " FROM personnel p"
					+ " JOIN query_result q ON p.id_number = q.query_result AND q.unique_id = " + queryContactId
					+ " LEFT JOIN country c ON p.country = c.id_number";

			Table tbl = Table.tableNew(tableName);
			@SuppressWarnings("unused")
			int ret = DBaseTable.execISql(tbl, sql);

			tbl.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
			tbl.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
			tbl.setColFormatAsRef("geographic_zone", SHM_USR_TABLES_ENUM.GEOGRAPHIC_ZONE_TABLE);

			for (int row = tbl.getNumRows(), state_id = tbl.getColNum("state_id"); row > 0; --row){
				if (tbl.getInt(state_id, row) <= 0){
					tbl.setInt(state_id, row, -1);
				}
			}
			
			tbl.group("id_number");
			tbl.convertColToString(tbl.getColNum("state_id"));
			tbl.convertColToString(tbl.getColNum("country"));
			tbl.convertColToString(tbl.getColNum("geographic_zone"));
			return tbl;
			
		} finally {
			if (queryContactId > -1) {
				Query.clear(queryContactId);
			}
		}
	}

	private Table getAddressItemsTable(String tableName, int extLE, int intLE, int extBU, int intBU, int today) throws OException {
		String sql  = "SELECT a.*, c.geographic_zone FROM party_address a"
				+ " JOIN (SELECT party_id, address_type, MAX(effective_date) effective_date_max FROM party_address"
				+ "  WHERE party_id in (" + extLE + "," + intLE + "," + extBU + "," + intBU + ")"
				+ "  AND effective_date <= '" + OCalendar.formatJdForDbAccess(today) + "'"
				+ "  GROUP BY party_id, address_type) h"
				+ "   ON a.party_id = h.party_id AND a.address_type = h.address_type AND a.effective_date = h.effective_date_max"
				+ " LEFT JOIN country c on a.country = c.id_number";

		Table tbl = Table.tableNew(tableName);
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);

		tbl.setColFormatAsRef("default_flag", SHM_USR_TABLES_ENUM.NO_YES_TABLE);
		tbl.setColFormatAsRef("state_id", SHM_USR_TABLES_ENUM.STATES_TABLE);
		tbl.setColFormatAsRef("country", SHM_USR_TABLES_ENUM.COUNTRY_TABLE);
		tbl.setColFormatAsRef("county_id", SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE);
		tbl.setColFormatAsRef("geographic_zone", SHM_USR_TABLES_ENUM.GEOGRAPHIC_ZONE_TABLE);

		for (int row = tbl.getNumRows(), state_id = tbl.getColNum("state_id"); row > 0; --row){
			if (tbl.getInt(state_id, row) <= 0){
				tbl.setInt(state_id, row, -1);
			}
		}

		tbl.group("party_id, address_type, default_flag");
		tbl.convertColToString(tbl.getColNum("default_flag"));
		tbl.convertColToString(tbl.getColNum("state_id"));
		tbl.convertColToString(tbl.getColNum("country"));
		tbl.convertColToString(tbl.getColNum("county_id"));
		tbl.convertColToString(tbl.getColNum("geographic_zone"));

		return tbl;
	}

	Table getAddressTypesTable(String tableName) throws OException {
		String sql = "SELECT address_type_name name, '' short, address_type_id id, 1 counter FROM party_address_type order by address_type_name, address_type_id desc";
		Table tbl = Table.tableNew(tableName);
		@SuppressWarnings("unused")
		int ret = DBaseTable.execISql(tbl, sql);
		int row = tbl.getNumRows();
		String str = null;
		int counter = 0;
		while (row > 0) {
			
			str = tbl.getString(1, row);
			if (str.length() > 4){
				str = str.substring(0, 4).trim();
			}
			str = str.replace(' ', '_');
			while (str.length() < 4){
				str += "_";
			}

			++counter;
			if (tbl.unsortedFindString(2, str, SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0){
				tbl.setInt(4, row, counter);
			} else{
				counter = 1;
			}
			tbl.setString(2, row, str);

			--row;
		}
		return tbl;
	}

	private Table getMailGroupsItemTable(String tableName, int extLE, int intLE, int extBU, int intBU, int businessOwner, int tranNum) throws OException {
		
		// Mind: counter party may be an internal party as well
		String sql
		// retrieve data for 'internal' parties, i.e. our parties
		= " (select distinct fg.name, '' field,0 int_ext,p.email/*,p.name personnel_name,pa.party_id*/"
				+ " from functional_group fg"
				+ " join personnel_functional_group pfg on fg.id_number=pfg.func_group_id"
				+ " join personnel p on pfg.personnel_id=p.id_number and p.status=1"
				+ " join party_personnel pp on p.id_number=pp.personnel_id"
				+ " join party pa on pp.party_id=pa.party_id and pa.party_status=1"
				+ " and pa.party_id in (" + intBU + "))"
				+ " union"
				// retrieve data for 'external' parties, i.e. counter parties
				+ " (select distinct fg.name, '' field,1 int_ext,p.email/*,p.name personnel_name,pa.party_id*/"
				+ " from functional_group fg"
				+ " join personnel_functional_group pfg on fg.id_number=pfg.func_group_id"
				+ " join personnel p on pfg.personnel_id=p.id_number and p.status=1"
				+ " join party_personnel pp on p.id_number=pp.personnel_id"
				+ " join party pa on pp.party_id=pa.party_id and pa.party_status=1"
				+ " and pa.party_id in (" + extBU + "))"
				+ " UNION ("
				+ " SELECT DISTINCT 'JMGroupConfirmCopy', '' field,1 int_ext,p.email/*,p.name personnel_name,pa.party_id*/"
				+ " FROM functional_group fg"
				+ " JOIN personnel_functional_group pfg ON fg.id_number=pfg.func_group_id"
				+ " JOIN personnel p ON pfg.personnel_id=p.id_number and p.status=1"
				+ " JOIN party_personnel pp ON p.id_number=pp.personnel_id"
				+ " JOIN party pa ON pp.party_id=pa.party_id AND pa.party_status=1"
				+ " AND pa.party_id in (" + businessOwner + ")"
				+ " JOIN ab_tran ab ON ab.tran_num = " + tranNum
				+ "   AND ((ab.ins_type = 26001 AND ab.cflow_type IN (" + getFXCflowType() +")) " // 26001 = FX
				+ "        OR ab.ins_type = 30201)" //, 30201 = METAL-SWAP
				//+ " WHERE fg.name = 'Trade Confirmations UK'"
				+ " WHERE fg.name in ('" + getConfirmCopyFunctionalGroup() + "')"				
				+ "   AND " + businessOwner + " != " + extBU
				+ " ) ORDER BY 1,4"
				;

		Table tbl = Table.tableNew(tableName);
		DBaseTable.execISql(tbl, sql);

		Logging.debug("EXEC "+sql);
		
		
		return tbl;
	}

	private void setMailGroupFields(Table gendataTable, Table tblQueried, HashMap<String, String> funcMailGroups) throws OException {
		String sFeasibleName, sEmail;
		// remove rows with no email & set feasible name for later search
		for (int row = tblQueried.getNumRows(); row > 0; --row) {
			
			sEmail = tblQueried.getString(4, row);
			if (sEmail == null || sEmail.trim().length() == 0){
				tblQueried.delRow(row);
			} else {
				sFeasibleName = getFeasibleFuncGroupName(tblQueried.getString(1, row));
				if (funcMailGroups.containsKey(sFeasibleName)){
					tblQueried.setString(2, row, sFeasibleName);
				} else{
					tblQueried.delRow(row);
				}
			}
		}

		if (tblQueried.getNumRows() > 0) {
			String strValue;
			int row, row2;
			for (String s : funcMailGroups.keySet()) {
				row = tblQueried.findString(2, s, SEARCH_ENUM.FIRST_IN_GROUP);
				if (row <= 0){
					strValue = "";
				} else if (row == (row2 = tblQueried.findString(2, s, SEARCH_ENUM.LAST_IN_GROUP))){
					strValue = tblQueried.getString(4, row);
				} else {
					strValue = "";
					do
						strValue += ","+tblQueried.getString(4, row);
					while (++row <= row2);
					strValue = strValue.substring(1);
				}
				GenData.setField(gendataTable, funcMailGroups.get(s), strValue);
			}
		}  else{
			for (String s : funcMailGroups.keySet()){
				GenData.setField(gendataTable, funcMailGroups.get(s), "");
			}
		}
	}

	private String getFeasibleFuncGroupName(String originalName) throws OException {
		if (originalName == null || (originalName=originalName.trim()).length() == 0){
			throw new OException("Provided value is null or empty");
		}
		return originalName.replaceAll("\\s+", "").replaceAll("_", "").replaceAll("-", "");
	}
	
	private String getConfirmCopyFunctionalGroup() throws ConstantTypeException, ConstantNameException, OException {
		String functionGroups = "Trade Confirmations UK";
		functionGroups = _constRepo.getStringValue("Confirm Copy Functional Group", functionGroups);
		
		Logging.debug("Confirm Copy Functional Group - " + functionGroups);
		String confirmCopyFunctionGroups[] = functionGroups.split(",");
		if (confirmCopyFunctionGroups.length == 0) {
			throw new RuntimeException("Confirm Copy Functional Group in constants repository defined.");
		}
		
		String output=confirmCopyFunctionGroups[0].trim();
		for (int i = 1; i<confirmCopyFunctionGroups.length; i++) {
			output =output+"', '" +confirmCopyFunctionGroups[i].trim();
		}
		return output;
	}
	
	/*
	 * P2907 changes
	 * To fetch cflow_type from user_const_repository for FX used in JM-Confirm copy
	  */
	
	private String getFXCflowType() throws OException {

		String fxValidCflow = "";
		Table fxCflowTbl = Util.NULL_TABLE;		
		Table defaultFXCflowTbl = Table.tableNew();
		defaultFXCflowTbl.addCol("cflow_name", COL_TYPE_ENUM.COL_STRING);
		defaultFXCflowTbl.addRowsWithValues("(Forward),(Swap),(Spot),(Location Swap),(Quality Swap),(FX Funding Swap),(FX FwdFwd),(FX Drawdown)");	 
		

		try {

			fxCflowTbl = _constRepo.getMultiStringValue(CONST_REPO_VAR_CFLOW, defaultFXCflowTbl);

			int rowCount = fxCflowTbl.getNumRows();
			StringBuilder cFlowStr = new StringBuilder("");
			String cflow;
			for (int row = 1; row <= rowCount; row++) {
				cflow = fxCflowTbl.getString(1, row).trim();
				if (!cflow.isEmpty()) {
					cFlowStr.append(Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, cflow)).append(",");
				}
			}
			
			if (!cFlowStr.toString().trim().isEmpty()) {
				cFlowStr.setLength(cFlowStr.length() - 1);				
				fxValidCflow = cFlowStr.toString();
			} else {
				String msg = "Cons repository is not configured correctly";
				Logging.error(msg);
				throw new OException(msg);
			}

		}

		catch (Exception exp) {
			Logging.error("An exception has occured. " + exp.getMessage());
			throw new RuntimeException(exp);
		}

		finally {
			if (Table.isValidTable(fxCflowTbl))
				fxCflowTbl.destroy();

			if (Table.isValidTable(defaultFXCflowTbl))
				defaultFXCflowTbl.destroy();
		}
		return fxValidCflow;

	}
}
