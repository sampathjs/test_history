package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class PriceImportReporting implements IScript { 

	@Override
	public void execute(IContainerContext context) throws OException {

		setUpLog();
		
		PluginLog.info("START PriceImportReporting");
		
		Table tblArgt = context.getArgumentsTable();
		String strRefSrc = tblArgt.getString("RefSrc", 1);
		
		Table tblImportedPrices  = null;
		Table tblStalePrices = Table.tableNew();
		
		Table tblMissingPrices = Table.tableNew();
		Table tblMissingDerivedFXRates = Table.tableNew();
		
		String strWhat;
		String strWhere;
		
		int intImportDate = OCalendar.today();
		
		PluginLog.info("Checking price import for ref src " + strRefSrc + " on " + OCalendar.formatDateInt(intImportDate));
		
		if(Str.isNull(strRefSrc) == 0 &&  !strRefSrc.isEmpty()){
			
			tblImportedPrices = getImportedPrices (strRefSrc, "", intImportDate);
			
			
			strWhat = "*";
			strWhere = "price EQ 0.0";
			tblMissingPrices.select(tblImportedPrices,strWhat,strWhere);
			
			if(tblMissingPrices.getNumRows() > 0){
				PluginLog.info("Found " + tblMissingPrices.getNumRows() + " missing prices " );
			}else{
				PluginLog.info("No missing prices found" );
			}
			
			// Get stale prices
			
			tblStalePrices.addCol("ref_src", COL_TYPE_ENUM.COL_STRING);
			tblStalePrices.addCol("metal", COL_TYPE_ENUM.COL_STRING);
			tblStalePrices.addCol("ccy", COL_TYPE_ENUM.COL_STRING);
			tblStalePrices.addCol("src_idx", COL_TYPE_ENUM.COL_STRING);
			tblStalePrices.addCol("target_idx", COL_TYPE_ENUM.COL_STRING);
			tblStalePrices.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);

			getUnchangedPrices(strRefSrc, "", intImportDate,tblStalePrices);
			
			if(tblStalePrices.getNumRows() > 0){
				PluginLog.info("Found " + tblStalePrices.getNumRows() + " stale prices " );
			}else{
				PluginLog.info("No stale prices found" );
			}
				
			if(!strRefSrc.equals("BFIX 1400") && !strRefSrc.equals("BFIX 1500")){
			
				tblMissingDerivedFXRates= getMissingDerivedFXRates(strRefSrc, intImportDate, tblImportedPrices  );
			}
			
			if(tblMissingDerivedFXRates.getNumRows() > 0){
				PluginLog.info("Found " + tblMissingDerivedFXRates.getNumRows() + " missing derived fx rates " );
			}else{
				PluginLog.info("No missing fx derived rates found" );
			}
					
			sendEmailReport(strRefSrc, tblMissingPrices,  tblStalePrices, tblMissingDerivedFXRates);
		
		}
		else{
			
			PluginLog.info("Could not find Ref Src variable from TPM input");
		}
		
		if(Table.isTableValid(tblStalePrices)==1){tblStalePrices.destroy();}
		
		if(Table.isTableValid(tblImportedPrices)==1){tblImportedPrices.destroy();}
		
		if(Table.isTableValid(tblMissingPrices)==1){tblMissingPrices.destroy();}
		
		if(Table.isTableValid(tblMissingDerivedFXRates)==1){tblMissingDerivedFXRates.destroy();}
		
		PluginLog.info("END PriceImportReporting");
		
	}
	
	private Table getMissingDerivedFXRates(String strRefSrc, int intImportDate, Table tblImportedPrices  ) throws OException {
		
		Table tblMissingDerivedFXRates=Table.tableNew();
		
		try{
			
			String strSQL;
			
			strSQL = " SELECT\n"; 
			strSQL += "target_idx.index_name \n";
			strSQL += "FROM \n"; 
			strSQL += "(SELECT index_id, index_name FROM idx_def where index_name IN ('FX_GBP.USD', 'FX_EUR.USD') AND db_status = 1) target_idx 										\n";
			strSQL += "LEFT JOIN  (SELECT                																																\n";
			strSQL += "            *                                                                                                     												\n";
			strSQL += "            FROM                                                                                                   												\n";
			strSQL += "            idx_historical_prices                                                                                  												\n";
			strSQL += "            WHERE                                                         					                      												\n";
			strSQL += "            index_id in (" + Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "FX_GBP.USD")+ "," + Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "FX_EUR.USD") + ") \n";
			strSQL += "            and reset_date = " + OCalendar.today() + 														     	   					    					"\n";
			strSQL += "            AND  ref_source = " + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, strRefSrc) + ") hist_px       						     					\n";
			strSQL += "on target_idx.index_id = hist_px.index_id 																														\n";			
			strSQL += "LEFT JOIN user_jm_ref_src urs on urs.ref_src = target_idx.index_name \n";
			strSQL += "WHERE \n";
			strSQL += "price is null";
			
			
			DBaseTable.execISql(tblMissingDerivedFXRates, strSQL);
 
			
		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.toString());
		}
		
		
		
		return tblMissingDerivedFXRates; 
		
	}
	
	
	private void sendEmailReport(String strRefSrc, Table tblMissingPrices, Table tblStalePrices, Table tblMissingDerivedFXRates) 
	{
		/* Add environment details */
		Table tblInfo = null;
		
		try
		{
			
			ConstRepository constRep = new ConstRepository("Alerts", "AutoPriceFeed");
			
			StringBuilder sb = new StringBuilder();
			
			String recipients1 = constRep.getStringValue("email_recipients1");
			
			sb.append(recipients1);
			String recipients2 = constRep.getStringValue("email_recipients2");
			
			if(!recipients2.isEmpty() & !recipients2.equals("")){
				
				sb.append(";");
				sb.append(recipients2);
			}

			EmailMessage mymessage = EmailMessage.create();
			
			if(tblMissingPrices.getNumRows() > 0 || tblStalePrices.getNumRows() > 0 || tblMissingDerivedFXRates.getNumRows() > 0){
				
				/* Add subject and recipients */
				
				mymessage.addSubject("WARNING | Price Import " + strRefSrc + " failed.");
				mymessage.addRecipients(sb.toString());
				
				StringBuilder builder = new StringBuilder();
				tblInfo = com.olf.openjvs.Ref.getInfo();
				if (tblInfo != null)
				{
					builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
					builder.append(", on server: " + tblInfo.getString("server", 1));
					
					builder.append("<BR>");
				}
				
				builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
				builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
				
				if(tblMissingPrices.getNumRows() > 0){

					builder.append("<BR><BR>");
					builder.append("The following prices failed to import and have not been distributed.");
					builder.append("<BR><BR>");
					builder.append("\n\nRef Src \t\tMetal \t\tCcy\n\n");
					builder.append("<BR>");

					for(int i=1;i<=tblMissingPrices.getNumRows();i++){
						
						builder.append("<BR>");
						builder.append(tblMissingPrices.getString("ref_src",i).trim() 
									   + "\t\t" + tblMissingPrices.getString("metal",i).trim()
									   + "\t\t" + tblMissingPrices.getString("ccy",i).trim() + "\n");
					}
				}
				else{
					
					PluginLog.info("All prices present from import for " + strRefSrc + " on " + OCalendar.formatDateInt(Util.getBusinessDate()));
				}
				
				if(tblStalePrices.getNumRows() > 0){
					
					builder.append("\n\n");
					builder.append("<BR><BR>");
					builder.append("The following prices are the same as yesterday and have not been distributed.");
					builder.append("<BR>");
					builder.append("\n\nRef Src \t\tMetal \t\tCcy\n\n");

					for(int i=1;i<=tblStalePrices.getNumRows();i++){
						
						builder.append("<BR>");
						builder.append(tblStalePrices.getString("ref_src",i).trim()
									   + "\t\t" + tblStalePrices.getString("metal",i).trim()
									   + "\t\t" + tblStalePrices.getString("ccy",i).trim() + "\n");
					}
				}
				else{
					PluginLog.info("All prices are different from previous import for " + strRefSrc + " on " + OCalendar.formatDateInt(Util.getBusinessDate()));
				}
				
				
				if(tblMissingDerivedFXRates.getNumRows() > 0){
					
					builder.append("\n\n");
					builder.append("<BR><BR>");
					builder.append("The following indexes have not had an fx rate saved for the ref src " +strRefSrc + ".");
					builder.append("<BR>");

					for(int i=1;i<=tblMissingDerivedFXRates.getNumRows();i++){
						
						builder.append("<BR>");
						builder.append(tblMissingDerivedFXRates.getString("index_name",i).trim() + "\n");
					}
					
				}else{
					
					PluginLog.info("All derived fx rates are present for " + strRefSrc + " on " + OCalendar.formatDateInt(Util.getBusinessDate()));
				}
				
				
				mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);

				PluginLog.info("Attempting to send email (using configured Mail Service)..");
				mymessage.send("Mail");
				mymessage.dispose();
				
				PluginLog.info("Email sent to: " + sb.toString());
				
				if (tblInfo != null)
				{
					tblInfo.destroy();	
				}
			}
			

		}
		catch (Exception e)
		{

			PluginLog.info("Exception caught " + e.toString());
		}
	}	
	
	
	public static boolean allPricesImported (String strRefSrc, String strTargetIdx, int intImportDate) throws OException {
		
		boolean blnAllPricesImported = false;
		Table tblImportedPrices=null;
		
		try{

			int intExpectedNumPrices = -1;
			
			tblImportedPrices = getImportedPrices (strRefSrc, strTargetIdx, intImportDate);
			
			if(tblImportedPrices.getNumRows() > 0){
				
				intExpectedNumPrices = tblImportedPrices.getNumRows();
			}
			
			// Check all prices have been imported 
			int intNumSavedPrices=0;
			for(int j=tblImportedPrices.getNumRows();j>0;j--){
				if(tblImportedPrices.getDouble("price", j) != 0.0){
					intNumSavedPrices++;
				}
			}
			
			if(intExpectedNumPrices == intNumSavedPrices){
				blnAllPricesImported = true;

			}
			
		}catch(Exception e){
			
			throw new OException(e.toString());
		}finally{
			
			if(Table.isTableValid(tblImportedPrices) == 1 ){tblImportedPrices.destroy();}
		}
		
		return blnAllPricesImported;
	}

	
	public static void getUnchangedPrices(String strRefSrc, String strTargetIdx, int intImportDate, Table tblSamePrices) throws OException {
		
		Table tblTodayPrices=null;
		Table tblYestPrices=null;

		try{

			int intPrevImportDate = OCalendar.getLgbd(intImportDate);
			
			// Check for a difference to yesterdays prices if they exist -  if yesterday's prices don't exist then the default return is true 
			if(PriceImportReporting.allPricesImported(strRefSrc, strTargetIdx, OCalendar.today()) == true
			&& PriceImportReporting.allPricesImported(strRefSrc, strTargetIdx, intPrevImportDate) == true){

				tblTodayPrices = getImportedPrices(strRefSrc,strTargetIdx, intImportDate);
				
				tblYestPrices = getImportedPrices(strRefSrc,strTargetIdx, intPrevImportDate);
				
				String strWhat;
				String strWhere;
				
				strWhat = "price(yest_price)";
				strWhere = "src_idx EQ $src_idx";
				tblTodayPrices.select(tblYestPrices,strWhat, strWhere);
				
				tblTodayPrices.addCol("diff", COL_TYPE_ENUM.COL_DOUBLE);
				
				tblTodayPrices.mathSubCol("price","yest_price","diff");

				
				strWhat = "*";
				strWhere = "diff EQ 0.0";
				
				tblSamePrices.select(tblTodayPrices, strWhat, strWhere);
				
				
				tblSamePrices.delCol("yest_price");
				tblSamePrices.delCol("diff");
				
			}
			
		}catch(Exception e){
			
			throw new OException(e.toString());
		}finally{
			
			if(Table.isTableValid(tblYestPrices) == 1 ){tblYestPrices.destroy();}
			if(Table.isTableValid(tblTodayPrices) == 1 ){tblTodayPrices.destroy();}
		}
		
	}
	
	
	
	public static boolean areYestPricesDifferent (String strRefSrc, String strTargetIdx, int intImportDate) throws OException {		

		boolean blnYestPricesDifferent = true;

		Table tblUnchangedPrices = Table.tableNew(); 
				
		getUnchangedPrices(strRefSrc, strTargetIdx, intImportDate,tblUnchangedPrices);
		
		if(Table.isTableValid(tblUnchangedPrices) == 1 && tblUnchangedPrices.getNumRows() > 0){
			
			blnYestPricesDifferent = false;
		}
		
		if(Table.isTableValid(tblUnchangedPrices) == 1) { tblUnchangedPrices.destroy();}

		
		return blnYestPricesDifferent;
		
	}
	
	public static Table getImportedPrices (String strRefSrc, String strTargetIdx, int intImportDate) throws OException {
		
		Table tblImportedPrices = Table.tableNew();
		
		try{

			String strSQL;
			strSQL = "SELECT \n";
			strSQL += "urs.* \n";
			strSQL += ",ipx.price \n";
			strSQL += "FROM \n";
			strSQL += "USER_jm_ref_src urs \n";
			strSQL += "INNER JOIN idx_def s_idx on s_idx .index_name = urs.src_idx and s_idx.db_status = 1  \n";
			strSQL += "INNER JOIN idx_def t_idx on t_idx.index_name = urs.target_idx and t_idx.db_status = 1 \n";
			strSQL += "LEFT JOIN idx_historical_prices ipx on ipx.index_id = s_idx.index_id and ipx.reset_date = '" + OCalendar.formatDateInt(intImportDate) +"' \n";
			strSQL += "WHERE \n";
			strSQL += "urs.ref_src = '" + strRefSrc + "' \n";
			if(Str.isNull(strTargetIdx) == 0 && !strTargetIdx.equals("")){
				strSQL += "and urs.target_idx = '" +strTargetIdx  + "' \n ";	
			}
			strSQL += "order by target_idx \n";
			
			DBaseTable.execISql(tblImportedPrices, strSQL);

		}catch(Exception e){
			
			throw new OException(e.toString());
		}
		
		return tblImportedPrices;
		
	}

	private static void setUpLog() throws OException {
		
    	String logDir   = Util.reportGetDirForToday();
    	String logFile =  "PriceImportReporting.log";
    	
		try{
			PluginLog.init("DEBUG", logDir, logFile );	
		}
		catch(Exception e){
			
        	String msg = "Failed to initialise log file: " + Util.reportGetDirForToday() + "\\" + logFile;
        	throw new OException(msg);
		}
	}
	
}
