package com.olf.jm.metalstransfer.report;

import java.io.File;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.openjvs.Transaction;


public class StrategyDealsIntradayReport  implements IScript {

	@Override
	public void execute(IContainerContext context)  throws OException {

		setupLog();
		
		String emailBodyMsg;
		try{			

			PluginLog.info("Report data generation started");
			
			// Report TPM failures
			
			Table tblTPMFailure = fetchTPMfailure();
			
			if (tblTPMFailure.getNumRows() > 0){
				emailBodyMsg = "<html> \n\r"+
						"<head><title> Failure of TPM process expected for "+ tblTPMFailure.getNumRows() +"  deals attached.</title></head> \n\r" +
						"<p> <font size=\"3\" color=\"blue\">Kindly Update status as 'Pending' for deal in user_strategy_deals </font></p></body> \n\r"+
						"<html> \n\r";
				String message = "StrategyTPM_Failure" ;
				String strFilename = getFileName(message);
				sendEmail(tblTPMFailure,message,strFilename,emailBodyMsg);
			} else{
				PluginLog.info("No TPM failure found "+ ODateTime.getServerCurrentDateTime().toString());
			}
			
			
			tblTPMFailure.destroy();
			
			// Report intra-day deal booking failures
			
			// Get validated strategies booked today
			String strSQL;
			
			strSQL = "SELECT \n";
			strSQL += "ab.input_date\n";
			strSQL += ",ab.deal_tracking_num as deal_num \n";
			strSQL += ",ab.tran_num as tran_num \n";
			strSQL += "FROM  \n";
			strSQL += "ab_tran ab \n";
			strSQL += "WHERE \n";
			strSQL += "ab.tran_type = 39 \n"; // Trading Strategy
			strSQL += "AND ab.input_date = " + OCalendar.today() + " \n";
			strSQL += "AND ab.tran_status = 3 \n"; // validated 
			Table tblStrategyDeals = Table.tableNew();
			DBaseTable.execISql(tblStrategyDeals, strSQL);
			
			Table tblReport = Table.tableNew();
			
			tblReport.addCol("input_date", COL_TYPE_ENUM.COL_DATE_TIME);

			tblReport.addCol("strategy_deal_num", COL_TYPE_ENUM.COL_INT);
			tblReport.addCol("expected_cash_deal_count", COL_TYPE_ENUM.COL_INT);
			tblReport.addCol("actual_cash_deal_count", COL_TYPE_ENUM.COL_INT);
			tblReport.addCol("expected_tax_deal_count", COL_TYPE_ENUM.COL_INT);
			tblReport.addCol("actual_tax_deal_count", COL_TYPE_ENUM.COL_INT);
			
			for(int i = 1;i<=tblStrategyDeals.getNumRows();i++){
				
				int intRowNum = tblReport.addRow();

				tblReport.setDateTime("input_date", intRowNum, tblStrategyDeals.getDateTime("input_date",i));

				int intStrategyDealNum = tblStrategyDeals.getInt("deal_num",i);
				
				PluginLog.info("Strategy " + intStrategyDealNum);
				
				tblReport.setInt("strategy_deal_num", intRowNum, intStrategyDealNum);
				
				tblReport.setInt("expected_cash_deal_count", intRowNum, 2);
				
				int intActualCashDealCount = getActualCashDealCount(intStrategyDealNum, 0);

				tblReport.setInt("actual_cash_deal_count", intRowNum, intActualCashDealCount);

				int intStrategyTranNum = tblStrategyDeals.getInt("tran_num",i);

				int intExpectedTaxDealCount = getExpectedTaxDealCount(intStrategyTranNum);

				tblReport.setInt("expected_tax_deal_count", intRowNum, intExpectedTaxDealCount);

				int intActualTaxDealCount = getActualCashDealCount(intStrategyDealNum, 2018);

				tblReport.setInt("actual_tax_deal_count", intRowNum, intActualTaxDealCount);

			}
			
			String strReportFilename = getFileName("StrategyDeals");

			File fileReport = new File(strReportFilename);
			
			if(fileReport.exists()){ 
				fileReport.delete();
			}
			 	
			tblReport.printTableDumpToFile(strReportFilename);

			Table tblReportMismatch = tblReport.cloneTable();
			
			for(int i=1;i<=tblReport.getNumRows();i++){
				
				if((tblReport.getInt("expected_cash_deal_count",i) - tblReport.getInt("actual_cash_deal_count",i) > 0) 
					||
					(tblReport.getInt("expected_tax_deal_count",i) - tblReport.getInt("actual_tax_deal_count",i) > 0)){

					tblReport.copyRowAdd(i, tblReportMismatch);
					
				}
			}
			
			if(tblReportMismatch.getNumRows() > 0 ){

				emailBodyMsg = "<html> \n\r"+
						"<head><title> Cash booking process failed for "+ tblReportMismatch.getNumRows() +" strategy deals.</title></head> \n\r" +
						"<p> <font size=\"3\" color=\"blue\">Please check the report attached </font></p></body> \n\r"+
						"<html> \n\r";
				String message = "StrategyBooking_Failure" ;
				String strErrFilename = getFileName(message);
				sendEmail(tblReportMismatch,message,strErrFilename,emailBodyMsg);
			} else{
				PluginLog.info("No Strategy/Cash failures found "+ ODateTime.getServerCurrentDateTime().toString());
			}
			
			tblReportMismatch.destroy();
			tblStrategyDeals.destroy();
			
			tblReport.destroy();
			
	
		}catch (Exception exp) {
			PluginLog.error("Error while generating report " + exp.getMessage());
			exp.printStackTrace();
			Util.exitFail();
		}

	}

	private int getExpectedTaxDealCount(int intStrategyTranNum ) throws OException {
		
		int intExpectedTaxDealCount = 0;
	
		Transaction tranPtrStrategy = Transaction.retrieve(intStrategyTranNum);
		
		Transaction tranPtrTaxDeal  = retrieveTaxableCashTransferDeal(tranPtrStrategy);
		
		if (tranPtrTaxDeal == null) {
			PluginLog.info( "No taxable deal found, no tax needs to be assigned");
		}
		else{
			
			Table taxRates = retrieveTaxRateDetails(tranPtrTaxDeal);

			for(int i=1;i<=taxRates.getNumRows();i++){
				
				if(taxRates.getDouble("charge_rate", i) > 0){
				
					intExpectedTaxDealCount++;
				}
			}
			
			
			if(Table.isTableValid(taxRates)==1){taxRates.destroy();}
			

		}
		
		if(Transaction.isNull(tranPtrStrategy) != 1){tranPtrStrategy.destroy();}
		
		if(Transaction.isNull(tranPtrTaxDeal) != 1){tranPtrTaxDeal.destroy();}
		
		
		return intExpectedTaxDealCount;
	}
	
	
	private Table retrieveTaxRateDetails(Transaction taxableDeal) throws OException {
		
		int taxTypeId = retrieveTaxTypeId ( taxableDeal);
		int taxSubTypeId = retrieveTaxSubTypeId ( taxableDeal);

		if (taxTypeId == -1 || taxSubTypeId == -1 ) {

			PluginLog.info("Could not find either Tax Type or Tax Subtype.");
			return Table.tableNew("Empty placeholder used in case of no tax type / sub type");
		
		}
		String taxType = Ref.getName(SHM_USR_TABLES_ENUM.TAX_TRAN_TYPE_TABLE, taxTypeId);
		
		String taxSubType = Ref.getName(SHM_USR_TABLES_ENUM.TAX_TRAN_SUBTYPE_TABLE, taxSubTypeId);
				
		Table tblRates = Table.tableNew();
		
		try{

			String strSQL;
			
			strSQL = 				"\n SELECT tax.party_id, tax.charge_rate, add_subtract_id" +
					"\n   FROM tax_rate tax" +
					"\n   JOIN tax_tran_type_restrict    ttt ON (ttt.tax_rate_id = tax.tax_rate_id)" +
					"\n   JOIN tax_tran_subtype_restrict tst ON (tst.tax_rate_id = tax.tax_rate_id)" +
					"\n  WHERE ttt.tax_tran_type_id = " + taxTypeId +
					"\n    AND tst.tax_tran_subtype_id = " + taxSubTypeId;
				DBaseTable.execISql(tblRates, strSQL);
				if (tblRates.getNumRows() == 0) {
					PluginLog.info("No tax rate found for tax type " + taxType + " and sub type " +taxSubType);
				}
				else{
					PluginLog.info("Charge rate = " + tblRates.getDouble("charge_rate",1));
				}

		}catch(Exception e){
		
			PluginLog.info(e.toString());
		}
		

		return tblRates.copyTable();

		
	}
	
	private int retrieveTaxTypeId(Transaction taxableDeal) throws OException {
		
		String strSQL;

		int intTaxTypeId = -1;
		
		Table tblTaxType = Table.tableNew();

		try{

			strSQL = "\n SELECT abt.tax_tran_type" 
					+  "\n FROM ab_tran ab" 
					+  "\n     INNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group AND ab2.current_flag = 1"
					+  "\n     INNER JOIN ab_tran_tax abt ON abt.tran_num = ab2.tran_num"
					+  "\n  WHERE ab.deal_tracking_num = " + taxableDeal.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()) 
					+  "\n    AND abt.tax_tran_subtype = -1"
					+  "\n    AND ab.current_flag = 1";

			DBaseTable.execISql(tblTaxType, strSQL);

			if (tblTaxType.getNumRows() > 0) {

				intTaxTypeId = tblTaxType.getInt("tax_tran_type",1);
			}
			
		}catch(Exception e){
			
			PluginLog.info(e.toString());
		}finally{
			
			tblTaxType.destroy();
		}
		
		return intTaxTypeId;
		
	}
	
	private int retrieveTaxSubTypeId(Transaction taxableDeal) throws OException {
		
		String strSQL;
		int intTaxSubTypeId = -1;
		
		Table tblSubTaxType = Table.tableNew();
		
		try{
	
			strSQL = "\n SELECT abt.tax_tran_subtype" 
					+  "\n FROM ab_tran ab" 
					+  "\n     INNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group AND ab2.current_flag = 1"
					+  "\n     INNER JOIN ab_tran_tax abt ON abt.tran_num = ab2.tran_num"
					+  "\n  WHERE ab.deal_tracking_num = " + taxableDeal.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()) 
					+  "\n    AND abt.tax_tran_type = -1"
					+  "\n    AND ab.current_flag = 1"; 

			DBaseTable.execISql(tblSubTaxType, strSQL);

			if (tblSubTaxType.getNumRows() > 0) {

				intTaxSubTypeId = tblSubTaxType.getInt("tax_tran_subtype",1);
			}
			
		}catch(Exception e){
			PluginLog.info(e.toString());
		}finally{
			
			tblSubTaxType.destroy();
		}
		
		return intTaxSubTypeId;
		
	}

	
	private Transaction retrieveTaxableCashTransferDeal(Transaction strategy) throws OException {

		Table tblResults = Table.tableNew();
		
		Transaction tranPtrTaxableCashDeal = null;
		
		try{
			
			String toBunit = strategy.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0,"To A/C BU");
			
			int strategyNum = strategy.getTranNum();
			
			String strSQL;
			
			strSQL = "SELECT ab.tran_num \n";
			strSQL += "FROM ab_tran ab \n";
			strSQL += "JOIN ab_tran_info_view ativ ON (ativ.tran_num = ab.tran_num) \n";
			strSQL += "JOIN party pa ON (pa.party_id = ab.external_bunit) \n";
			strSQL += " WHERE ativ.type_name = 'Strategy Num' \n";
			strSQL += " AND value = '" + strategyNum + "' \n";
			strSQL += "AND ab.ins_type =  27001 \n"; // Strategy 
			strSQL += "AND ab.buy_sell = 1\n";
			strSQL += "AND pa.short_name ="  + "'" +toBunit.replace("'", "''") + "'" + "\n";
			strSQL += "\n";

			DBaseTable.execISql(tblResults, strSQL);
			for(int i =1;i<=tblResults.getNumRows();i++){
				int tranNum = tblResults.getInt("tran_num",i);
				tranPtrTaxableCashDeal = Transaction.retrieve(tranNum);
				break;
			}
			
		}catch(Exception e){
			PluginLog.info(e.toString());
		}finally{
			
			tblResults.destroy();
		}
		
		return tranPtrTaxableCashDeal;
	}

		
	private int getActualCashDealCount(int intStrategyDealNum , int intCflowType) throws OException {
		
		String strSQL;
		
		int intActualCashDealCount = -1;
		
		strSQL = "SELECT count(*) as actual_cash_deal_count \n";
		strSQL += "FROM \n";
		strSQL += "ab_tran_info_view ati  \n";
		strSQL += "inner join ab_tran ab on ati.tran_num = ab.tran_num AND type_id = 20044 and value = " + intStrategyDealNum + "  \n";
		strSQL += "WHERE \n";
		strSQL += "ab.current_flag = 1  \n";
		strSQL += "and tran_status = 3 \n";
		strSQL += "and cflow_type = " + intCflowType + " \n";

		Table tblActualCashDealCount = Table.tableNew();
		
		DBaseTable.execISql(tblActualCashDealCount, strSQL);
		intActualCashDealCount = tblActualCashDealCount.getInt("actual_cash_deal_count", 1); 
		tblActualCashDealCount.destroy();
		
		return intActualCashDealCount;
	}
	
	
	private Table fetchTPMfailure() throws OException {
		Table failureData = Util.NULL_TABLE;
		try{
			failureData = Table.tableNew();
			String sql = "SELECT deal_num as strategydeal, status , last_updated  "
					+ "FROM user_strategy_deals where  status =  'Running'"
					+ " AND last_updated < DATEADD(minute, -30, Current_TimeStamp)";
			PluginLog.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(failureData, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchTPMfailure "));
			}
			
		} catch (OException exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return failureData;

	}
	private String getFileName(String strFileName) {

		String strFilename;
		Table envInfo = Util.NULL_TABLE;
		StringBuilder fileName = new StringBuilder();

		String[] serverDateTime;
		try {

			serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			//String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			envInfo = Ref.getInfo();
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append(strFileName);
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			//fileName.append("_");
			//fileName.append(currentTime);
			fileName.append(".csv");
		}catch (OException e) {
			e.printStackTrace();
		}
		strFilename = fileName.toString();

		return strFilename;
	}

	private void sendEmail(Table tblResults, String message, String strFilename, String emailBodyMsg)
			throws OException {
		PluginLog.info("Attempting to send email (using configured Mail Service)..");

		Table envInfo = Util.NULL_TABLE;
		EmailMessage mymessage = null;       

		try {
			mymessage = EmailMessage.create();  
			ConstRepository repository = new ConstRepository("Alerts", "TransferValidation");

			String recipients1 = repository.getStringValue("email_recipients1");
			String recipients2 = repository.getStringValue("email_recipients2");
			/* Add subject and recipients */
			mymessage.addSubject("WARNING || "+ message+" ||"+ OCalendar.formatDateInt(OCalendar.today()));							

			mymessage.addRecipients(recipients1);
			mymessage.addCC(recipients2);

			StringBuilder emailBody = new StringBuilder();

			/* Add environment details */
			envInfo = Ref.getInfo();

			if (Table.isTableValid(envInfo) != 1) {
				throw new OException("Error getting environment details");
			}
			String html = emailBodyMsg.toString();
			emailBody.append(html);
			emailBody.append("\n\r\n\r");
			emailBody.append("This information has been generated from database: " + envInfo.getString("database", 1));
			emailBody.append(", on server: " + envInfo.getString("server", 1));

			emailBody.append("\n\r\n\r");


			emailBody.append("Endur trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()));
			emailBody.append(",business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			emailBody.append("\n\r\n\r");

			mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);


			File fileReport = new File(strFilename);
			
			if(fileReport.exists()){ 
				fileReport.delete();
			}
			
			tblResults.printTableDumpToFile(strFilename);

			/* Add attachment */
			if (new File(strFilename).exists()) {
				PluginLog.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);
				mymessage.send("Mail");
				PluginLog.info("Email sent to: " + recipients1 + " "+ recipients2);
			} else {
				PluginLog.info("Unable to send the output email !!!");
				PluginLog.info("File attachmenent not found: " + strFilename);
			}
			
		} catch (OException e) {

			e.printStackTrace();
		} finally {

			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
			if(mymessage != null){
			mymessage.dispose();
			}
		}
	}

	protected void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("Reports", "");
		String logLevel = constRepo.getStringValue("logLevel");

		try
		{

			if (logLevel == null || logLevel.isEmpty())
			{
				logLevel = "DEBUG";
			}
			String logFile = "StrategyIntradayReport2.log";
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
										