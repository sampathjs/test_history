package com.openlink.jm.bo;

import java.math.BigDecimal;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.sc.bo.docproc.OLI_MOD_ModuleBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Created as copy of OLI_MOD_FXSwap. Contains fix to sign of pymt_amount
 * @author jwaechter
 * @version 1.0
 */
public class JM_MOD_FXSwap extends OLI_MOD_ModuleBase implements IScript {
	private final String TRAN_INFO_FORM = "Form";
	private final String TRAN_INFO_LOCO = "Loco";

	protected ConstRepository _constRepo;
	private int baseUnit;
	protected static boolean _viewTables;

	public void execute(IContainerContext context) throws OException {
		initPluginLog ();
		init();

		try {
			Table argt = context.getArgumentsTable();

			if (argt.getInt("GetItemList", 1) == 1)  {
				// if mode 1
				//Generates user selectable item list
				PluginLog.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			} else  {
				//if mode 2
				//Gets generation data
				PluginLog.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}
		} catch (Exception e) {
			PluginLog.error("Exception: " + e.getMessage());
		}

		PluginLog.exitWithStatus();
	}

	private void init() throws OException {
		_constRepo = new ConstRepository("BackOffice", "OLI-FXSwap");
		String baseUnitString = _constRepo.getStringValue("BaseUnit","TOz");
		if (baseUnitString == null || baseUnitString.isEmpty())
			throw new OException("Empty value in Const Repository for");
		baseUnit = com.olf.openjvs.Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, baseUnitString);
		
	}

	private void initPluginLog() {
		
		String logLevel = "Error", logFile  = getClass().getSimpleName() + ".log", logDir   = null;

		try {
			
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			if (logDir == null){
				PluginLog.init(logLevel);
			} else{ 
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			// do something
		}

		try {
			_viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		} catch (Exception e) {
			// do something
		}
	}

	/*
	 * Add items to selection list
	 */
	private void createItemsForSelection(Table itemListTable) throws OException {
		
		String groupName;

		/*		// copied from 
		groupName = "FX Trade Info";
		ItemList.add(itemListTable, groupName, "FX Date", "olfFxDate", 1);
		groupName = "FX Trade Info, Corporate Margin";
		ItemList.add(itemListTable, groupName, "Inter Or Corp", "olfFxInterOrCorp", 1);
		ItemList.add(itemListTable, groupName, "Corp Margin", "olfFxCorpMargin", 1);
		 */

		groupName = "FX-Swap Trade Info";
		ItemList.add(itemListTable, groupName, "FX-Swap Data", "Table_FXSwapData", 1);
		ItemList.add(itemListTable, groupName, "Total Amount", "olfFXSwapTotalAmount", 1);
		ItemList.add(itemListTable, groupName, "We - You", "olfFXSwapWeYou", 1);
		ItemList.add(itemListTable, groupName, "Num Value Dates", "olfNumValueDates", 1);

		if (_viewTables){
			itemListTable.viewTable();
		}
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException {
		
		int /*tranNum,*/ numRows, row;
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
//		Transaction tran;

		if (gendataTable.getNumRows() == 0){
			gendataTable.addRow();
		}

//		tranNum = eventTable.getInt("tran_num", 1);
//
//		tran = retrieveTransactionObjectFromArgt(tranNum);
//		if (Transaction.isNull(tran) == 1)
//		{
//			PluginLog.error ("Unable to retrieve transaction info due to invalid transaction object found. Tran#" + tranNum);
//		}
//		else
		{
			//Add the required fields to the GenData table
			//Only fields that are checked in the item list will be added
			numRows = itemlistTable.getNumRows();
			itemlistTable.group("output_field_name");

			//used as flags - not selected unless not 'null'
			String olfTblFxSwapData     = null
				,olfFXSwapTotalAmount = null
				,olfFXSwapWeYou       = null
				,olfNumValueDates     = null
				;

			String internal_field_name = null;
			String output_field_name   = null;
			int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
			int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

			for (row = 1; row <= numRows; row++) {
				internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
				output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

				if (internal_field_name == null || internal_field_name.trim().length() == 0){
					continue;
				}
				
				/*
				//FX Trade Info, FX Date
				else if (internal_field_name.equalsIgnoreCase("olfFxDate"))
				{
					String strValue = tran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt());
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//FX Trade Info, Corporate Margin, Inter Or Corp
				else if (internal_field_name.equalsIgnoreCase("olfFxInterOrCorp"))
				{
					String strValue = tran.getField(TRANF_FIELD.TRANF_FX_INTER_OR_CORP.toInt());
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//FX Trade Info, Corporate Margin, Corp Margin
				else if (internal_field_name.equalsIgnoreCase("olfFxCorpMargin"))
				{
					String strValue = tran.getField(TRANF_FIELD.TRANF_FX_CORP_MARGIN.toInt());
					GenData.setField(gendataTable, output_field_name, strValue);
				}
				 */

				//FX-Swap Trade Info, FX-Swap Data table
				else if (internal_field_name.equalsIgnoreCase("Table_FXSwapData")) {
					olfTblFxSwapData = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("olfFXSwapTotalAmount")) {
					//FX-Swap Trade Info, Total Amount
					olfFXSwapTotalAmount = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("olfFXSwapWeYou")) {
					//FX-Swap Trade Info, We - You
					olfFXSwapWeYou = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("olfNumValueDates")) {
					// FX-Swap Trade Info, Num Value Dates
					olfNumValueDates = output_field_name;
				} else{
					GenData.setField(gendataTable, output_field_name, "[n/a]");
				}

			}

			if (olfTblFxSwapData != null || olfFXSwapTotalAmount != null || olfFXSwapWeYou != null) {
				
				Table tbl = retrieveFxSwapData(eventTable);

				if (olfFXSwapTotalAmount != null || olfFXSwapWeYou != null) {
					double dTotal = 0F;

					Table tblSum = Table.tableNew();
					tblSum.addCols("F(pymt_amount)");
					tblSum.select(tbl, "SUM,pymt_amount", "tran_num GE 0");// any criteria
					if (tblSum.getNumRows() > 0) {
						if (olfFXSwapWeYou != null)
							dTotal = tblSum.getDouble(1, 1);
					} else{
						tblSum.addRow();
					}
					
					if (olfFXSwapWeYou != null) {
						String strValue = dTotal < 0F ? "We owe you" : "You owe us";
						GenData.setField(gendataTable, olfFXSwapWeYou, strValue);
					}
					
					if (olfFXSwapTotalAmount != null) {
						convertAllColsToString(tblSum);
						String strValue = tblSum.getString(1, 1);
						GenData.setField(gendataTable, olfFXSwapTotalAmount, strValue);
					}
					tblSum.destroy();
				}
				
				if(olfNumValueDates != null) {
					int date1 = tbl.getDate("settle_date", 1);
					int date2 = tbl.getDate("settle_date", 2);
					
					String strValue = date1 == date2 ? "1" : "2";
					GenData.setField(gendataTable, olfNumValueDates, strValue);
				}

				if (olfTblFxSwapData != null) {
					tbl.setTableName(olfTblFxSwapData);
					convertAllColsToString(tbl);
					GenData.setField(gendataTable, tbl);
				} else{ 
					tbl.destroy();
				}
			}
		}

		if (_viewTables){
			gendataTable.viewTable();
		}
	}

	// WIP !!
	private Table retrieveFxSwapData(Table tblEventData) throws OException {
		
		Table tblFxSwapData = Table.tableNew("result");
		tblFxSwapData.setTableTitle(tblFxSwapData.getTableName());
		try {
			tblFxSwapData.addCols(""
					// all columns come from 'ab_tran' - if not mentioned otherwise
					+ "I(tran_num)"
					+ "I(tran_group)"
					+ "I(ins_type)"
					+ "I(buy_sell)"
					+ "S(reference)"
					+ "I(internal_bunit)"
					+ "I(internal_lentity)"
					+ "I(external_bunit)"
					+ "I(external_lentity)"
					+ "I(internal_portfolio)"
					+ "I(external_portfolio)"
					+ "T(trade_date)"
					+ "T(settle_date)"
					+ "F(position)"
					+ "F(price)"
					+ "F(rate)"
					+ "F(pymt_amount)" // = price * position
					+ "I(currency)"
					+ "I(ccy1)" // from fx_tran_aux_data - to become 'fx_ccy1' ?
					+ "S(ccy1_desc)" // from currency
					+ "I(ccy2)" // from fx_tran_aux_data - to become 'fx_ccy2' ?
					+ "S(ccy2_desc)" // from currency
					+ "I(cflow_type)"
					+ "I(ins_sub_type)"
					+ "T(start_date)"
					+ "T(maturity_date)"
					+ "I(unit)"
					+ "I(template_tran_num)"
					+ "S(form)" // from ab_tran_info.value
					+ "S(loco)" // from ab_tran_info.value
					);

			tblFxSwapData.setColFormatAsRef("ins_type",           SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
			tblFxSwapData.setColFormatAsRef("buy_sell",           SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
			tblFxSwapData.setColFormatAsRef("internal_bunit",     SHM_USR_TABLES_ENUM.PARTY_TABLE);
			tblFxSwapData.setColFormatAsRef("internal_lentity",   SHM_USR_TABLES_ENUM.PARTY_TABLE);
			tblFxSwapData.setColFormatAsRef("external_bunit",     SHM_USR_TABLES_ENUM.PARTY_TABLE);
			tblFxSwapData.setColFormatAsRef("external_lentity",   SHM_USR_TABLES_ENUM.PARTY_TABLE);
			tblFxSwapData.setColFormatAsRef("internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			tblFxSwapData.setColFormatAsRef("external_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TYPE_TABLE);
			tblFxSwapData.setColFormatAsRef("currency",           SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblFxSwapData.setColFormatAsRef("ccy1",               SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblFxSwapData.setColFormatAsRef("ccy2",               SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblFxSwapData.setColFormatAsRef("cflow_type",         SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
			tblFxSwapData.setColFormatAsRef("ins_sub_type",       SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);
			tblFxSwapData.setColFormatAsRef("unit",               SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
			tblFxSwapData.setColFormatAsDate("trade_date",    DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblFxSwapData.setColFormatAsDate("settle_date",   DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblFxSwapData.setColFormatAsDate("start_date",    DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblFxSwapData.setColFormatAsDate("maturity_date", DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);

			Table tblTranGroup = getTranGroups(tblEventData);
			try {
				//	tblTranGroup.viewTable();

				long start = System.currentTimeMillis();
				loadFromDB(tblTranGroup, tblFxSwapData);
				PluginLog.debug("loadFromDB() took "+(System.currentTimeMillis()-start)+" ms");
				convertToDealUnit(tblFxSwapData);
			} catch (OException e) { 
				tblTranGroup.printTable(); throw e; 
			} finally { 
				tblTranGroup.destroy(); 
			}
		} catch (OException e) { 
			OConsole.print("\n"+e.toString()); 
		}

	//	tblFxSwapData.viewTable();
		return tblFxSwapData;
	}

	private void convertToDealUnit(Table tblFxSwapData) throws OException {
		try {
		// this function converts position(TOz) from ab_tran table to
		// unit(deal booked).This is happening for
		// only FX swaps and below function updates the position and price
		// per unit in memory table.
		int rowCount = tblFxSwapData.getNumRows();// loop for number of rows generated in table
		if (rowCount==0) {
			PluginLog.info("No Tran  to be processed in tblFxSwapData table");
		}//BaseUnit value is fetched from Const Repository
		for (int row = 1; row <= rowCount; row++) {

			int toUnit = tblFxSwapData.getInt("unit", row);
			if (baseUnit == toUnit) { // if conversion unit is same
				continue;
			} 
			Double position = tblFxSwapData.getDouble("position", row);
			Double price = tblFxSwapData.getDouble("price", row);
			Double convFactor = Transaction.getUnitConversionFactor(baseUnit, toUnit);
			if (Double.compare(convFactor,BigDecimal.ZERO.doubleValue())!=0){
			// if conversion factor is more than Zero
				Double newPosition = position * convFactor;
				Double newPrice = price / convFactor;
				tblFxSwapData.setDouble("position", row, newPosition);
				tblFxSwapData.setDouble("price",row,newPrice);
			}
		}
	} catch (OException e) {
		PluginLog.error("Error while updating the position and price after conversion.");
		throw e;
	}
		
	}

	private Table getTranGroups(Table tblEventData) throws OException 	{
		
		Table tbl = Table.tableNew("tran_group");
		tbl.setTableTitle(tbl.getTableName());
		try {
			tbl.addCols("I(tran_group)");
			tblEventData.copyColDistinct("tran_group", tbl, "tran_group");
			return tbl;
		} catch (OException e) {
			tbl.destroy();
			throw e;
		}
	}

	private void loadFromDB(Table tblTranGroup, Table tblResult) throws OException {
		
		int iTranInfoIdForm = -1, iTraninfoIdLoco = -1;
		Table tblTranInfoType = Table.tableNew("tran_info_types");
		tblTranInfoType.setTableTitle(tblTranInfoType.getTableName());
		DBaseTable.execISql(tblTranInfoType, "select type_id, type_name from tran_info_types where type_name in ('"+TRAN_INFO_FORM+"','"+TRAN_INFO_LOCO+"') order by type_name");
		if (tblTranInfoType.getNumRows() > 0) {
			
			int row = tblTranInfoType.findString(2, TRAN_INFO_FORM, SEARCH_ENUM.FIRST_IN_GROUP);
			if (row > 0) {
				iTranInfoIdForm = tblTranInfoType.getInt(1, row);
			}
			row = tblTranInfoType.findString(2, TRAN_INFO_LOCO, SEARCH_ENUM.FIRST_IN_GROUP);
			if (row > 0) {
				iTraninfoIdLoco = tblTranInfoType.getInt(1, row);
			}
		}


		Table tblAbTran = Table.tableNew("ab_tran");
		try {
			tblAbTran.setTableTitle(tblAbTran.getTableName());
			tblAbTran.addCols(""
					+ "I(tran_num)"
					+ "I(tran_group)"
					+ "I(ins_type)"
					+ "I(buy_sell)"
					+ "S(reference)"
					+ "I(internal_bunit)"
					+ "I(internal_lentity)"
					+ "I(external_bunit)"
					+ "I(external_lentity)"
					+ "I(internal_portfolio)"
					+ "I(external_portfolio)"
					+ "T(trade_date)"
					+ "T(settle_date)"
					+ "F(position)"
					+ "F(price)"
					+ "F(rate)"
//					+ "F(pymt_amount)" // = price * position
					+ "I(currency)"
//					+ "I(ccy1)" // from fx_tran_aux_data - to become 'fx_ccy1' ?
//					+ "S(ccy1_desc)" // from currency
//					+ "I(ccy2)" // from fx_tran_aux_data - to become 'fx_ccy2' ?
//					+ "S(ccy2_desc)" // from currency
					+ "I(cflow_type)"
					+ "I(ins_sub_type)"
					+ "T(start_date)"
					+ "T(maturity_date)"
					+ "I(unit)"
					+ "I(template_tran_num)"
//					+ "S(form)" // from ab_tran_info.value
//					+ "S(loco)" // from ab_tran_info.value
					);
			DBaseTable.loadFromDb(tblAbTran, tblAbTran.getTableName(), tblTranGroup);
		//	tblAbTran.viewTable();

			Table tblTranNum = Table.tableNew("tran_num");
			try {
				tblTranNum.setTableTitle(tblTranNum.getTableName());
				tblTranNum.addCols("I(tran_num)");
				tblAbTran.copyColDistinct("tran_num", tblTranNum, "tran_num");
			//	tblTranNum.viewTable();

				Table tblFxTranAuxData = Table.tableNew("fx_tran_aux_data");
				try {
					tblFxTranAuxData.setTableTitle(tblFxTranAuxData.getTableName());
					tblFxTranAuxData.addCols("I(tran_num)I(ccy1)I(ccy2)T(term_settle_date)T(far_base_settle_date)T(far_term_settle_date)");
					DBaseTable.loadFromDb(tblFxTranAuxData, tblFxTranAuxData.getTableName(), tblTranNum);
				//	tblFxTranAuxData.viewTable();
					tblFxTranAuxData.addCols("I(tran_group)");
					tblFxTranAuxData.select(tblAbTran, "tran_group", "tran_num EQ $tran_num");
				//	tblFxTranAuxData.viewTable();

					Table tblCcys = Table.tableNew("ccy_desc");
					try {
						tblCcys.addCols("I(id_number)");
						tblCcys.select(tblFxTranAuxData, "ccy1(id_number)","ccy1 GE 0");
						tblCcys.select(tblFxTranAuxData, "ccy2(id_number)","ccy2 GE 0");
						tblCcys.makeTableUnique();

						Table tblCurrency = Table.tableNew("currency");
						try {
							tblCurrency.setTableTitle(tblCurrency.getTableName());
							tblCurrency.addCols("I(id_number)S(description)");
							DBaseTable.loadFromDb(tblCurrency, tblCurrency.getTableName(), tblCcys);

							Table tblAbTranInfo = Table.tableNew("ab_tran_info");
							try {
								tblAbTranInfo.setTableTitle(tblAbTranInfo.getTableName());
								tblAbTranInfo.addCols("I(tran_num)I(type_id)S(value)");
								DBaseTable.loadFromDbWithWhere(tblAbTranInfo, tblAbTranInfo.getTableName(), tblTranNum, "type_id in ("+iTranInfoIdForm+","+iTraninfoIdLoco+")");
								//	tblAbTranInfo.viewTable();

								tblResult.select(tblAbTran, "*", "tran_group GT 0");
								tblResult.mathMultCol("price", "position", "pymt_amount");
								tblResult.mathMultColConst("pymt_amount", -1.0d, "pymt_amount");
								
								tblResult.setColValInt("ccy1", -1); tblResult.setColValInt("ccy2", -1);
								tblResult.select(tblFxTranAuxData, "ccy1,ccy2", "tran_group EQ $tran_group");
								
								tblResult.select(tblFxTranAuxData,"term_settle_date(settle_date)", "tran_num EQ $tran_num");
								tblResult.select(tblFxTranAuxData,"far_base_settle_date(maturity_date),far_term_settle_date(settle_date)", "tran_num NE $tran_num");
								
								
								tblResult.select(tblCurrency, "description(ccy1_desc)", "id_number EQ $ccy1");
								tblResult.select(tblCurrency, "description(ccy2_desc)", "id_number EQ $ccy2");
								tblResult.select(tblAbTranInfo, "value(form)", "tran_num EQ $tran_num and type_id EQ "+iTranInfoIdForm);
								tblResult.select(tblAbTranInfo, "value(loco)", "tran_num EQ $tran_num and type_id EQ "+iTraninfoIdLoco);
							} finally { 
								tblAbTranInfo.destroy(); 
							}
						} finally { 
							tblCurrency.destroy(); 
						}
					} finally { 
						tblCcys.destroy(); 
					}
				} finally { 
					tblFxTranAuxData.destroy(); 
				}
			} finally { 
				tblTranNum.destroy(); 
			}
		} finally { 
			tblAbTran.destroy(); 
		}
	}
}
