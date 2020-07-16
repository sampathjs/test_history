package com.olf.jm.pricewebservice.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;
/*
 * History:
 * 2015-09-27	V1.0	jwaechter	- initial version
 * 2016-11-22	V1.1	jwaechter	- Changed logic for calculation
 * 									  of reset and RFIS date.
 * 
 */

/**
 * This class saves historical prices in case an index is saved for certain
 * Closing Dataset Types ( {@value #RELEVANT_MARKET_DATA_SETS_ARRAY} ).
 * 
 * @author jwaechter
 * @version 1.1
 */
public class SaveHistoricalPrices implements IScript {
	private static final String[] RELEVANT_INDEXES_ARRAY = { "JM_Base_Price", "NON_JM_EUR_Price", "NON_JM_GBP_Price", "NON_JM_USD_Price"};
	
	private static final Set<String> RELEVANT_INDEXES_SETS = new HashSet<>(Arrays.asList(RELEVANT_INDEXES_ARRAY));
	
	private static final String DEFAULT_HOL_SCHEDULE = "USD";

	private String defaultHolName=DEFAULT_HOL_SCHEDULE;

	private int defaultHolId=0;

	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init (context);
			process(context);
		} catch (Exception ex) {
			Logging.error(this.getClass().getName() + " terminated abnormaly: \n" + ex.toString());
			throw ex;
		}finally{
			Logging.info(this.getClass().getName() + " ended successfully");
			Logging.close();
		}
		
	}
	
	private void process(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		int universal = argt.getInt ("universal", 1);
		int idxMarketDataTypeId = argt.getInt("close", 1);		
		if (universal != 0) {
			return;
		}		
		String idxMarketDataType = Ref.getName (SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE, idxMarketDataTypeId);
		int refSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, idxMarketDataType);
		Table indexList = argt.getTable("index_list", 1);
		int indexId = indexList.getInt("index_id", 1);
		String indexName = Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, indexId);
		if (!RELEVANT_INDEXES_SETS.contains(indexName)) {
			return;
		}

		Table mapping = DBHelper.retrieveBasePriceFixings(indexId);
		Table marketData = indexList.getTable("market_data", 1);
		Table gptData = marketData.getTable("gptdata", 1);		
		ODateTime od = argt.getDateTime("date", 1);
		int today = od.getDate();

		importHistoricalPrices(refSourceId, indexId, indexName, mapping, gptData, today);       
		
		importMissingSWCPrices(today,mapping);
	}

	
	
	private void importMissingSWCPrices(int intToday, Table mapping) { //throws OException {
		
		// SWC deals have been modelled where the reset schedule has both LME_PM and JM NY Opening calendars.
		// The reference source per leg on these deals can only be either LME PM or JM NY Opening - this means the saved prices is only ever against one holiday schedule 
		// The reset schedule (single holiday schedule ) sometime does not not match the saved price schedule (multiple holiday schedule)
		
		// This function will identify days where for a given reset date the NGBD for the NY_Opening < LME_PM
		// On such days it will create a duplicate row in the historical table for NY Opening with the same price but the start date for the LME_PM row 
		
		try
		{
			
			int intNGBD_NYO = OCalendar.parseStringWithHolId("2d", Ref.getValue(SHM_USR_TABLES_ENUM.HOL_TABLE, "JM NY 0930"), intToday);
			int intNGBD_LMEPM = OCalendar.parseStringWithHolId("2d", Ref.getValue(SHM_USR_TABLES_ENUM.HOL_TABLE, "LME-XPTXPD-PM"), intToday);
			
			if(intNGBD_NYO<intNGBD_LMEPM){
				
				Logging.info("SWC deal - extra price added as NGBD LME_PM is ahead of NGBD JM NYO");
				
				Logging.info("For reset date " + OCalendar.formatDateInt(intToday) +  " LME PM start date is " + OCalendar.formatDateInt(intNGBD_LMEPM) + " and NY Opening start date is " + OCalendar.formatDateInt(intNGBD_NYO));
				
				Logging.info("A row will be inserted for NY Opening start date " + OCalendar.formatDateInt(intNGBD_LMEPM) + " with the price of " + OCalendar.formatDateInt(intNGBD_NYO));

				String strSQL;
				
				Table tblExistingRow = Table.tableNew();
				
				strSQL = "SELECT \n";
				strSQL += "* \n";
				strSQL += "FROM \n";
				strSQL += "idx_historical_prices idxp \n";
				strSQL += "WHERE \n";
				strSQL += "1=1\n";
				strSQL += "AND idxp.reset_date = " + intToday + " \n";
				strSQL += "AND idxp.ref_source = " + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, "JM NY Opening") + " \n";
				
				DBaseTable.execISql(tblExistingRow, strSQL);

				Table importTable = Table.tableNew("New Historical Prices ");
				Table errorLog = Table.tableNew();
				
				importTable.addCol( "index_id", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "date", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "end_date", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "yield_basis", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "ref_source", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "index_location", COL_TYPE_ENUM.COL_INT);
				importTable.addCol( "price", COL_TYPE_ENUM.COL_DOUBLE);			
				
				int intTargetIndexId=0;
				int intRefSourceId=0;
				double dblPrice=0.0;
				
				for(int i = 1;i<=tblExistingRow.getNumRows();i++){
					
					Logging.info("Populating row " + i + " of " + tblExistingRow.getNumRows());
					
					int importRow = importTable.addRow();
					
					intTargetIndexId = tblExistingRow.getInt("index_id",i);
					importTable.setInt("index_id", importRow, intTargetIndexId);
					importTable.setInt("date", importRow, intToday);
					importTable.setInt("start_date", importRow, intNGBD_LMEPM);
					importTable.setInt("end_date", importRow, intNGBD_LMEPM);
					importTable.setInt("ref_source", importRow, intRefSourceId);
					importTable.setInt("yield_basis", importRow, 0);
					importTable.setInt("index_location", importRow, 0);
					
					intRefSourceId = tblExistingRow.getInt("ref_source",i);
					importTable.setInt("ref_source", importRow, intRefSourceId);
					
					dblPrice = tblExistingRow.getDouble("price",i);
					importTable.setDouble("price", importRow, dblPrice);
					
				}

				if(importTable.getNumRows() > 0){

					int ret = Index.tableImportHistoricalPrices(importTable, errorLog);
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
						throw new OException ("Error importing historical prices");
					}
					else{
						Logging.info("Saved missing SWC NYO prices");
					}
				}
				
				tblExistingRow.destroy();
				importTable.destroy();
				errorLog.destroy();
			}
			
		}catch(Exception e){
			
			Logging.info("Caught exception " + e.toString());
		}
		
		
	}
	
	private void importHistoricalPrices(int refSourceId, int indexId, String indexName, Table mapping, Table gptData, int today) throws OException {
		Table importTable = null;
		Map<Integer, Integer> refSource2Holiday= DBHelper.retrieveRefSourceToHolidayMapping();
		int holId;
		if (!refSource2Holiday.containsKey(refSourceId)) {
			holId = defaultHolId;			
			Logging.warn ("There is no mapping for ref source id #" + refSourceId +	" to a holiday id defined in table " + DBHelper.USER_JM_PRICE_WEB_REF_SOURCE_HOL );
		} else {
			holId = refSource2Holiday.get(refSourceId);
		}
		
		try {
			importTable = Table.tableNew("New Historical Prices for " + indexName);
			Table errorLog = Table.tableNew();
			
			importTable.addCol( "index_id", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "end_date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "yield_basis", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "ref_source", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "index_location", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "price", COL_TYPE_ENUM.COL_DOUBLE);			
			
			for (int row=mapping.getNumRows(); row >= 1; row--) {
				int gptMappingId = mapping.getInt("src_gpt_id", row);
				int targetIndexId = mapping.getInt("target_index_id", row);
				int spotDay = OCalendar.parseStringWithHolId("2d", holId, today);					
				
				for (int gptRow = gptData.getNumRows(); gptRow >= 1; gptRow--) {
					int gptDataId = gptData.getInt("id", gptRow);
					double price = gptData.getDouble("input", gptRow);
					if (gptDataId == gptMappingId && price > 0.0) {
						int importRow = importTable.addRow();
						importTable.setInt("index_id", importRow, targetIndexId);
						importTable.setInt("date", importRow, today);
						importTable.setInt("start_date", importRow, spotDay);
						importTable.setInt("end_date", importRow, spotDay);
						importTable.setInt("ref_source", importRow, refSourceId);
						importTable.setInt("yield_basis", importRow, 0);
						importTable.setInt("index_location", importRow, 0);
						importTable.setInt("ref_source", importRow, refSourceId);
						importTable.setDouble("price", importRow, price);
					}
				}
			}
			int ret = Index.tableImportHistoricalPrices(importTable, errorLog);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error importing historical prices for " + indexName);
			}
		} finally {
			importTable = TableUtilities.destroy(importTable);
		}
	}

	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		defaultHolName = constRepo.getStringValue("defaultHolName", DEFAULT_HOL_SCHEDULE);
		defaultHolId = Ref.getValue (SHM_USR_TABLES_ENUM.HOL_ID_TABLE, defaultHolName);
		if (defaultHolId < 0) {
			defaultHolId = Ref.getValue (SHM_USR_TABLES_ENUM.HOL_ID_TABLE, DEFAULT_HOL_SCHEDULE);
			Logging.warn("The provided holiday name from Const Repository entry " + DBHelper.CONST_REPOSITORY_CONTEXT + "\\" + DBHelper.CONST_REPOSITORY_SUBCONTEXT + "\\defaultHolName=" +  defaultHolName + " is not valid");
		}
		try {
			Logging.init(this.getClass(), DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(this.getClass().getName() + " started");
	}	
}
