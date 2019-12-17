/********************************************************************************
 * Script Name: EOD_JM_ReRun_ResetFixings
 * Script Type: Main
 * 
 * Revision History:
 * Version Date       Author      Description
 ********************************************************************************/

package com.jm.eod.fixings;

import java.io.File;
import java.util.ArrayList;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.EndOfDay;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Report;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.ROW_POSITION_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_ReRun_ResetFixings implements IScript
{	

	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "ResetFixings";
	private static ConstRepository repository = null;
	
	public EOD_JM_ReRun_ResetFixings() {
		
	}
	
	
    public void execute(IContainerContext context) throws OException
    {       

        Table tblTrades = Table.tableNew();;
    	     	
		repository = new ConstRepository(CONTEXT, SUBCONTEXT);

        setUpLog(repository);
        
        int intTradingDate = Util.getTradingDate();
    	
        try{

        	int intNumDays = repository.getIntValue("NumDaysToCheck");
        	
        	int intCurrDate =  intTradingDate - intNumDays;
        	
        	ArrayList<String> strArrReports = new ArrayList<String>();
        	
        	for(int i=1;i<=intNumDays;i++){
        		
        		PluginLog.debug("Setting current date to " + OCalendar.formatDateInt(intCurrDate));
        		
        		Util.setCurrentDate(intCurrDate);
        		
        		PluginLog.debug("Running query EoD ReRun Fixings for current date  "  + OCalendar.formatDateInt(intCurrDate));
        		
            	int intRet =  Query.run("EoD Fixings ReRun");
                
            	String strSQL = "SELECT * from " + Query.getResultTableForId(intRet) + " query_result where unique_id = " + intRet;
            	
            	DBaseTable.execISql(tblTrades, strSQL);
            	
            	if(tblTrades.getNumRows() > 0){
            		
            		PluginLog.debug("Found unfixed " + tblTrades.getNumRows() + " trades for "  + OCalendar.formatDateInt(intCurrDate) );
            		
            		Table resetInfo = EndOfDay.resetDealsByTranList(tblTrades, intCurrDate);
            		
            		createReport( intTradingDate, resetInfo,strArrReports);
            		
            	}
            	else{
            		
            		PluginLog.debug("No unfixed trades found for "  + OCalendar.formatDateInt(intCurrDate) );
            	}
            	
            	intCurrDate = intCurrDate + 1;
            	Query.clear(intRet);
            	tblTrades.clearRows();
            	
        	}
        	
        	Util.setCurrentDate(intTradingDate); 
        	sendEmail(strArrReports);
        	
        }catch(Exception e){
        	
        	PluginLog.debug("Exception thrown  " + e.toString());
        	
        }finally{
        	
			int ret = Util.setCurrentDate(intTradingDate); 
			PluginLog.debug("Setting Current Date: "  + OCalendar.formatDateInt(intTradingDate) );
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = "Could not set currentDate back to " + OCalendar.formatJd(intTradingDate);
				throw new OException (errorMessage);
			}
			
			if(Table.isTableValid(tblTrades) == 1 ){tblTrades.destroy();}
			
        }

        PluginLog.debug("End of script");
		PluginLog.exitWithStatus();
    }
   

    private void sendEmail(ArrayList<String> strArrReports) {
    	
    	
    	try
    	{
    		
        	String strMessage = "";
        	
    		EmailMessage mymessage = EmailMessage.create();

			mymessage.addSubject("ReRun Prior Fixings complete. " + OCalendar.formatJd(OCalendar.today()));
			
			ConstRepository repository = new ConstRepository(CONTEXT, SUBCONTEXT);
			
			StringBuilder sb = new StringBuilder();
			
			String recipients1 = repository.getStringValue("email_recipients1");
			
			sb.append(recipients1);
			String recipients2 = repository.getStringValue("email_recipients2");
			
			if(!recipients2.isEmpty() & !recipients2.equals("")){
				
				sb.append(";");
				sb.append(recipients2);
			}
			

			mymessage.addRecipients(sb.toString());
			    		
        	if(strArrReports.size() > 0){
        		
            	// For each report generated attach to email and send to user
            	for(int i =0;i<strArrReports.size();i++){
            		
            		PluginLog.debug("strArrReports is " + strArrReports.get(i));
            		strMessage += "\nCreated report " + strArrReports.get(i);
            		
            		if(new File(strArrReports.get(i)).exists() ){
            			
            			mymessage.addAttachments(strArrReports.get(i), 0, null);
            		}
            	}

        		mymessage.addBodyText(strMessage, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

            	
        	}
        	else{
        		
        		strMessage += "No deals with missing resets found ";
            	
        		mymessage.addBodyText(strMessage, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
        	}

        	PluginLog.debug("Sending email");
			mymessage.send("Mail");
			mymessage.dispose();

    		
    	}
    	catch(OException e){
    		PluginLog.debug("Caught exception " + e.toString());
    	}

    	
    }
    
    
    /**
     * Prints status report
     * @param resetInfo table: reset info from fixing process
     * @param region code: regional identifier
     */
    private void createReport(int intTradingDate, Table resetInfo, ArrayList<String> strArrReports) throws OException
    {  
		Table errors = Table.tableNew();
		String cols = "tran_num, deal_num, param_seq_num, profile_seq_num, reset_seq_num, spot_value, value, message";
		errors.select(resetInfo, cols, "success EQ 0");

		formatOutput(errors);
		errors.group("tran_num, param_seq_num, profile_seq_num, reset_seq_num");

		errors.setTitleBreakPosition(ROW_POSITION_ENUM.ROW_BOTH);
		errors.setTitleAboveChar("=");
		errors.setTitleBelowChar("=");
		errors.showTitleBreaks();
		
        String filename = "ReRun_Reset_Fixings_" + OCalendar.formatDateInt(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH) + ".eod";
        
        String title = "Reset Fixings Report for " + OCalendar.formatJd(OCalendar.today(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
        
        if(errors.getNumRows() > 0)
        {
        	
            Report.reportStart(filename, title);
            errors.setTableTitle(title);

            Report.reportEnd();
            
            errors.printTableToFile(Util.reportGetDirForDate(intTradingDate) + "\\" + filename);
            
    		PluginLog.debug("Reset fixing errors found: "  + errors.getNumRows());
        }
        else
        {
        	
            errors.setTableTitle("No reset fixing errors.");

            errors.hideHeader();
            
        	errors.hideTitleBreaks();
        	errors.hideTitles();
        	errors.noFormatPrint();


        	errors.colAllHide();
            
            errors.printTableToFile(Util.reportGetDirForDate(intTradingDate) + "\\" + filename);
            
        }
		
        strArrReports.add(Util.reportGetDirForDate(intTradingDate) + "\\" + filename);
        

        errors.destroy();
    }
    
	private void formatOutput(Table report) throws OException
	{	
		report.setColTitle( "tran_num", "Tran\nNum");
		report.setColTitle( "deal_num", "Deal\nNum");
		report.setColTitle( "param_seq_num", "Param\nSeq Num");
		report.setColTitle( "profile_seq_num", "Profile\nSeq Num");
		report.setColTitle( "reset_seq_num", "Reset\nSeq Num");
		report.setColTitle( "spot_value", "Spot\nValue");
		report.setColTitle( "value", "\nValue");
		report.setColTitle( "message", "\nError Message");
		
		report.formatSetJustifyRight( "tran_num");
		report.formatSetJustifyRight( "deal_num");
		report.formatSetJustifyCenter( "param_seq_num");
		report.formatSetJustifyCenter( "profile_seq_num");
		report.formatSetJustifyCenter( "reset_seq_num");
		report.formatSetJustifyRight( "spot_value");
		report.formatSetJustifyRight( "value");
		report.formatSetJustifyLeft( "message");
		
		report.formatSetWidth( "tran_num", 9);
		report.formatSetWidth( "deal_num", 9);
		report.formatSetWidth( "param_seq_num", 9);
		report.formatSetWidth( "profile_seq_num", 9);
		report.formatSetWidth( "reset_seq_num", 9);
		report.formatSetWidth( "spot_value", 15); 
		report.formatSetWidth( "value", 15);
		report.formatSetWidth( "message", 80);
		
		report.setRowHeaderWidth(1);
	}
    

	private void setUpLog(ConstRepository repository) throws OException {
		try {
			String abOutdir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			 
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = repository.getStringValue("logLevel", "DEBUG"); 
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir = repository.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException("Error initializing PluginLog", e);
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error initializing the ConstRepo", ex);
		}
		
	}
}  