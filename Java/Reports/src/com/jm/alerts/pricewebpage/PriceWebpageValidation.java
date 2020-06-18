package com.jm.alerts.pricewebpage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PriceWebpageValidation implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
		
		try {
			
			setupLog();
			
			PluginLog.info("BEGIN PriceWebpageValidation");
			
			ConstRepository constRepo = new ConstRepository("Alerts", "PriceWebpageValidation");
			
			Table tblAllDiffs = Table.tableNew();
			tblAllDiffs.addCol("ref_src", COL_TYPE_ENUM.COL_STRING);
			tblAllDiffs.addCol("metal", COL_TYPE_ENUM.COL_STRING);
			tblAllDiffs.addCol("actual_avg", COL_TYPE_ENUM.COL_STRING);
			tblAllDiffs.addCol("expected_avg", COL_TYPE_ENUM.COL_STRING);
			
			tblAllDiffs.setColTitle(1, "Ref Src");
			tblAllDiffs.setColTitle(2, "Metal");
			tblAllDiffs.setColTitle(3, "Actual Avg");
			tblAllDiffs.setColTitle(4, "Expected Avg");

			String strURL = constRepo.getStringValue("jm_prices_url");
			
			Document doc;

			//doc = Jsoup.connect("http://www.platinum.matthey.com/prices/price-tables").get();
			doc = Jsoup.connect(strURL).get();
			
	        Elements tables = doc.select("table");
	        for (Element table : tables) {
	        	
	        	String strMetal = table.attr("data-metal") ;
	        	
	        	Table tblMetalPrices = Table.tableNew(strMetal);
	        	tblMetalPrices.setTableName(strMetal);
	        	
	        	tblMetalPrices.addCol("weekday", COL_TYPE_ENUM.COL_STRING);
	        	tblMetalPrices.addCol("day", COL_TYPE_ENUM.COL_STRING);
	        	tblMetalPrices.addCol("HK_Opening", COL_TYPE_ENUM.COL_DOUBLE);
	        	tblMetalPrices.addCol("HK_Closing", COL_TYPE_ENUM.COL_DOUBLE);
	        	tblMetalPrices.addCol("LO_Opening", COL_TYPE_ENUM.COL_DOUBLE);
	        	tblMetalPrices.addCol("NY_Opening", COL_TYPE_ENUM.COL_DOUBLE);
	        	
	            Elements rows = table.getElementsByTag("tr");
	            
	            int intRowNum;
	            for(Element row : rows){

	            	String strValues = row.text();
	            	if(!strValues.contains("Month Average for all time zones") 
	            		&& !strValues.contains("Hong Kong London New York")
	            		&& !strValues.contains("08:30 14:00 09:00 09:30")){
	            		
	            		String [] strValuesArray = strValues.split(" ");
	            		intRowNum = tblMetalPrices.addRow();	            		

	            		for(int i=0;i<strValuesArray.length;i++){

	            			if(i==0 || i==1){
	            				tblMetalPrices.setString(i+1,intRowNum,strValuesArray[i]);	
	            			}
	            			else{
	            				
	            				String strValue = strValuesArray[i];
	            				double dblValue = Str.strToDouble(strValue,1);
	            				
	            				tblMetalPrices.setDouble(i+1, intRowNum, dblValue);
	            			}
	            		}
	            	}
	            }
	            
	            // Get Average value from webpage value
	            Table tblAverage = Table.tableNew(strMetal);
	            tblAverage.setTableName(strMetal);
	            
	            String strAverage = "Average";
	            tblAverage.select(tblMetalPrices, "*", "day EQ " + strAverage);

	            
	            // Get Calculated Average
	            intRowNum = tblMetalPrices.unsortedFindString("day", "Average", SEARCH_CASE_ENUM.CASE_INSENSITIVE); 
	            
	            tblMetalPrices.delRow(intRowNum);
	            
	            tblMetalPrices.setColValString("day", "Average");
	            
	            Table tblCalculatedAverage = tblAverage.copyTable();
	            tblCalculatedAverage.setTableName(strMetal + "_CALCULATED");
	            
	            tblCalculatedAverage.setColValDouble("HK_Opening", 0);
	            tblCalculatedAverage.setColValDouble("HK_Closing", 0);
	            tblCalculatedAverage.setColValDouble("LO_Opening", 0);
	            tblCalculatedAverage.setColValDouble("NY_Opening", 0);
	            
	            tblCalculatedAverage.select(tblMetalPrices, "SUM, HK_Opening,HK_Closing,LO_Opening,NY_Opening" , "day EQ $day");
	            
	            for(int i = 1;i<=tblMetalPrices.getNumCols();i++){
	            
	            	int intNumRows=0;
	            	String strColName = tblMetalPrices.getColName(i);
	            	if(!strColName.equals("day") && !strColName.equals("weekday")){
	            		
		            	for(int j=1;j<=tblMetalPrices.getNumRows() ;j++){
		            		if(tblMetalPrices.getDouble(i,j) != 0.0)
		            		{
		            			intNumRows++;
		            		}
		            	}
		            	if(intNumRows > 0){

			            	double dblValue = tblCalculatedAverage.getDouble(strColName,1);
			            	dblValue = dblValue/intNumRows;
 
	        				BigDecimal bd = BigDecimal.valueOf(dblValue);
	        				bd = bd.setScale(2, RoundingMode.HALF_UP);
	        				dblValue =  bd.doubleValue();
	        				tblCalculatedAverage.setDouble(strColName,1,dblValue-100);
		            	}
	            	}
	            }
	            
	            // Get Differences
	            Table tblDiff = Table.tableNew();
	            diffTables(strMetal, tblAverage, tblCalculatedAverage, tblDiff, tblAllDiffs);
	            
	            tblAverage.destroy();
	            tblMetalPrices.destroy();
	            tblCalculatedAverage.destroy();
	            tblDiff.destroy();

	        }
	        
	        if(tblAllDiffs.getNumRows() > 0){

	        	StringBuilder reportOutputString = new StringBuilder();
	        	
	        	reportOutputString.append("<BR> ENDUR SUPPORT TEAM ALERT FOR " + strURL + "<BR>");
	        	reportOutputString.append("<BR> The following metal prices have a average total that does not match the average of the individual days.<BR>");
	        	reportOutputString.append("<BR> Please verify that all daily upload files to the FTP site have the correct time label and ask the user to re-save the price if needed.");

	        	String htmlBody = com.matthey.utilities.Utils.convertTabletoHTMLString(tblAllDiffs,true,"Price Webpage Validation");
	        	
	        	StringBuilder mailSignature = com.matthey.utilities.Utils.standardSignature();
	        	
	        	reportOutputString.append(htmlBody);
	        	reportOutputString.append(mailSignature);
	        	
	        	String listOfUsers = constRepo.getStringValue("email_recipients");
	        	String toList = com.matthey.utilities.Utils.convertUserNamesToEmailList(listOfUsers);

				String emailSubject="Price Webpage Validation - average price mismatch";
				String mailServiceName="mail";
				
				PluginLog.info("Sending out email to : " + toList);
				com.matthey.utilities.Utils.sendEmail(toList, emailSubject, reportOutputString.toString(),"",mailServiceName); 

	        }else{
	        	
	        	PluginLog.info("No differences found between average and calculated average.");
	        }
	        
		} catch (IOException e) {
			e.printStackTrace();
		}

		PluginLog.info("END PriceWebpageValidation");
    }
    
    
    private void diffTables(String strMetal, Table tblMetalPrices, Table tblCalculatedAverage, Table tblDiff, Table tblAllDiffs) throws OException{
    	
    	tblMetalPrices.diffTablesDetail(tblCalculatedAverage,tblDiff);
    	
    	if(tblDiff.getNumRows() > 0){
    		
    		tblDiff.delCol("col");
    		tblDiff.delCol("row");

    		tblDiff.setColName("val1", "actual_avg");
    		tblDiff.setColName("val2", "expected_avg");

    		// format doubles to 2 dp
    		for(int i =1;i<tblDiff.getNumRows();i++){
    			
    			String strDbl = tblDiff.getString("actual_avg",i);
				double dblValue = Str.strToDouble(strDbl,1);

				DecimalFormat df = new DecimalFormat("0.00");
				df.setRoundingMode(RoundingMode.UP);
				
				tblDiff.setString("actual_avg",i,df.format(dblValue));
				
    			strDbl = tblDiff.getString("expected_avg",i);
				dblValue = Str.strToDouble(strDbl,1);

				tblDiff.setString("expected_avg",i,df.format(dblValue));
    		}
    		
    		tblDiff.setColName("col_name", "ref_src");
    		
    		tblDiff.addCol("metal", COL_TYPE_ENUM.COL_STRING);
    		tblDiff.setColValString("metal", strMetal);
    		
    		Table tblTmp = Table.tableNew();
    		
    		tblTmp.select(tblDiff, "ref_src,metal,actual_avg,expected_avg", "metal NE ''");
    		
    		tblTmp.copyRowAddAll(tblAllDiffs);
    		
    		tblTmp.destroy();
    	}
    }
    
	private void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("Alerts", "PriceWebpageValidation");
		String logLevel = constRepo.getStringValue("logLevel");

		try
		{
			if (logLevel == null || logLevel.isEmpty())
			{
				logLevel = "DEBUG";
			}
			String logFile = "PriceWebpageValidation.log";
			PluginLog.init(logLevel, logDir, logFile);

		}
		catch (Exception e)
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
    
}
