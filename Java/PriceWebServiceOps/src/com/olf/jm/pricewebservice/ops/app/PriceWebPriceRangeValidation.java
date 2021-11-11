package com.olf.jm.pricewebservice.ops.app;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.apm.impl.datasink.database.DbProviderOLDB4J;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.FunctionalGroup;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-22	V1.0	ifernandes	- Initial Version
 */

/**
 * Checks that the input value has a difference from the last value which is not bigger 
 * than a difference of 2.5% from the previous days value  
 * @author ifernandes
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcMarketIndex })
public class PriceWebPriceRangeValidation extends AbstractGenericOpsServiceListener {
	
	
	
	@Override
	public PreProcessResult preProcess(final Context context, final EnumOpsServiceType type,
			final ConstTable table, final Table clientData) {
		try {
			
			Logging.init(this.getClass(), DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);

			Logging.info("\n\n********************* Start PriceWebPriceRangeValidation ***************************");
			process (context, table);
			Logging.info("**************** Succeeded PriceWebPriceRangeValidation  ********************");
			return PreProcessResult.succeeded();			
		} catch (Throwable t) {
			String message = t.getMessage();
			Logging.error("*************** Failed because of following Exception " + message + " ******************");
			return PreProcessResult.failed(message);
		}finally{
			Logging.close();
		}
	}
	
	private Table getCurrentPrices(Session session, int intRefSrc) {
		
		String strSQL = "SELECT ";
		strSQL += "REPLACE( REPLACE( i.label , '.USD', '') ,'FX_','') as idx_label \n";
		strSQL += ",ihp.price as db_price\n";
		strSQL += "FROM \n";
		strSQL += "idx_historical_prices ihp \n";
		strSQL += "INNER JOIN idx_def i on ihp.index_id = i.index_id and i.db_status = 1 \n";
		strSQL += "WHERE \n";
		strSQL += "ihp.ref_source = "  + intRefSrc + "\n";
		strSQL += "and ihp.reset_date =  ( \n";
		strSQL += "							SELECT MAX(h2.reset_date)       \n";
		strSQL += "                         FROM         							\n";
		strSQL += "                         		idx_historical_prices h2         \n";
		strSQL += "                                 INNER JOIN idx_def i2 on h2.index_id = i2.index_id and i2.db_status = 1 \n";
		strSQL += "                         WHERE 							\n";
		strSQL += "                                 i2.index_name in ('XOS.USD','XPD.USD','XPT.USD','XIR.USD','XRH.USD','XRU.USD','FX_EUR.USD','FX_GBP.USD')\n";
		strSQL += "                                 and h2.ref_source = " +intRefSrc + " )  \n";
		strSQL += "and i.index_name in ('XOS.USD','XPD.USD','XPT.USD','XIR.USD','XRH.USD','XRU.USD','FX_EUR.USD','FX_GBP.USD') \n";
		
		Table tblResult = session.getIOFactory().runSQL(strSQL);;

		return tblResult;
		
	}

	private void process(Session session, ConstTable table) throws OException {
		
		Table tblIndexList = table.getTable("index_list", 0);
		
		int intIndexId = tblIndexList.getInt("index_id", 0);
		
		String strIndex = Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE,intIndexId);
		
		if(strIndex.equals("JM_Base_Price")){
			
			Table tblMarketData = tblIndexList.getTable("market_data", 0);
			Table tblIdxDefn = tblIndexList.getTable("index_defn", 0);
			Table tblIdxGptDef = tblIdxDefn.getTable("idx_gpt_def", 0);
			
			Table tblGptData = tblMarketData.getTable("gptdata", 0);
			
			tblGptData.select(tblIdxGptDef, "gpt_name", "[In.gpt_id] == [Out.id]");
			
			int intMtkDataType = table.getInt("close",0);
			
			String strMktDataType = Ref.getName(SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE, intMtkDataType);
			
			int intRefSrc = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, strMktDataType);
			
			//Get current input  price
			//Get saved historical price for the selected ref source
			//Calculate absolute difference percentage as diff
			//If diff > 2.5 then soft block

			Table tblResult = null;
			
			tblResult = getCurrentPrices(session, intRefSrc);
			tblResult.select(tblGptData,"input->price","[In.gpt_name] == [Out.idx_label]");

			tblResult.addColumn("diff_perc", EnumColType.Double);
			
			for(int i=0;i<tblResult.getRowCount();i++){
				
				double dblInputPrice = tblResult.getDouble("price",i);
				double dblDBPrice= tblResult.getDouble("db_price",i);;

				double dblDiffPerc = (Math.abs((dblInputPrice-dblDBPrice))/Math.abs(dblDBPrice))*100;
				
				tblResult.setDouble("diff_perc", i, dblDiffPerc);
				
			}
			
			String strErrMsg = "";
			boolean blnBreachFound = false;
			
			ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT,DBHelper.CONST_REPOSITORY_SUBCONTEXT);
			double dblPriceRangeLimit = constRepo.getDoubleValue("price_range_limit"); 

			for(int i=0;i<tblResult.getRowCount();i++){
				
				double dblDiff = tblResult.getDouble("diff_perc", i);
	 
				if(dblDiff > dblPriceRangeLimit){
					
					double dblInputPrice = tblResult.getDouble("price",i);
					double dblDBPrice= tblResult.getDouble("db_price",i);;
					
					Logging.info("New Price " + dblInputPrice + " Orig Price " + dblDBPrice + " perc diff " + dblDiff );
					strErrMsg += "The input " +tblResult.getString("idx_label", i) + " price " + dblInputPrice + " has a large difference from the previous saved price " + dblDBPrice + " , Continue? \n";
					Logging.info(strErrMsg);
					blnBreachFound =true;
				}
				
			}
			
			if(blnBreachFound == true){
			
				int ret = Ask.okCancel(strErrMsg);
				
				if (ret == 0) {
					throw new RuntimeException("Cancelled by user");
				}
			}
		}
	}
	
}
