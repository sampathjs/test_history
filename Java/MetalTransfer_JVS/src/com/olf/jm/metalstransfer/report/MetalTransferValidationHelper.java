package com.olf.jm.metalstransfer.report;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author             | Description                                                                  |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 18-Aug-2021 |               | Rohit Tomar        | Initial version.                      									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

import com.matthey.utilities.Utils;
import com.matthey.utilities.enums.EndurTranInfoField;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.openlink.util.constrepository.ConstRepository;

/**
 * This is a helper class for the metal validation report.
 * This report can be triggered for any region ad-hoc by BO users.
 * This report contains the strategy deals along with its cash,VAT and transfer charges deal details.
 * It also contains a comment column which indicates if we have any issue with the corresponding strategy
 * 
 * 
 * @author TomarR01
 *
 */
public class MetalTransferValidationHelper {	
	
	 public static final String PARTY = "party";
	 public static final String START_DATE = "start_date";
	 public static final String END_DATE = "end_date";
	 public static final String STATUS = "status";
	 public static final String EMAIL_FLAG = "email_flag";
	 public static final String DEAL_COUNT = "deal_count";
	 public static final String TRANSFER_CHARGE = "Transfer Charge";
	 public static final String DEAL_COUNT_NUM = "deal_count_num";
	 public static final String AB_OUTDIR = "AB_OUTDIR";
	 public static final String COMMENTS = "comments";
	 public static final String STRATEGY_DEAL_NUM = "strategy_deal_num";
	 public static final String CASH_DEAL_NUM = "cash_deal_number";
	 public static final String CASH_DEAL_STATUS = "cash_deal_status";
	 public static final String EXPECTED_COUNT = "expected_cash_deal_count";
	 public static final String ACTUAL_COUNT = "actual_cash_deal_count"; 
	 public static final String VALIDATION_REPORT = "MetalTransferValidation_Report"; 
	 public static final String EXCEPTION_REPORT = "MetalTransferException_Report"; 
	 public static final String CONTEXT = "Reports";  
	 public static final String SUB_CONTEXT = "MetalTransferValidation";
	 public static final String EMAILSERVICE = "emailServiceName";
	 public static final String UKRECEIPIENT = "ukrecipients";
	 public static final String HKRECEIPIENT = "hkrecipients";
	 public static final String USRECEIPIENT = "usrecipients";
	 public static final String CNRECEIPIENT = "cnrecipients";
	 public static final String SUBJECT = "subject";
	 public static final String MAILBODY = "mailbody";
	 public static final String DATABASE = "database";
	 public static final String VALIDATED = "Validated";
	 public static final String NEW = "New";
	 public static final String DELETED = "Deleted";
	 public static final String CANCELLED = "Cancelled";
	 public static final String YES = "Yes";
	 public static final String NO = "No";	
	 public static final String FLAG = "flag";
	 public static final String DATE = "date";
	 public static final String SETTLEDATE = "Settle Date";
	 public static final String TRADEDATE = "Trade Date";	
	
	
	/**
	 * @param party
	 * @param start_date
	 * @param end_date
	 * @param status
	 * @return
	 * @throws OException
	 */
	public Table getTransferData(String party,String date_type,String start_date,String end_date, String status) throws OException{
		
		Table tblData = Table.tableNew();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT *,RANK() OVER(PARTITION BY strategy_deal_num ORDER BY cash_deal_number)-1 deal_count ")
		.append("\n FROM ( ")
		.append("\n SELECT * FROM ( ")
		.append("\n SELECT p.short_name AS party_name,usd.deal_num as strategy_deal_num,ts.name as Strategy_deal_status,metal.value AS metal,")
		.append("\n unit.value AS unit,FORMAT(st.trade_date,'dd-MMM-yyyy') AS trade_date,FORMAT(st.settle_date,'dd-MMM-yyyy') AS settle_date,")
		.append("\n usd.expected_cash_deal_count,usd.actual_cash_deal_count, ")
		.append("\n ab.deal_tracking_num AS cash_deal_number,cflow.name AS cflow_type,ts2.name AS cash_deal_status,")
		.append("\n CASE WHEN abe.unit != 55 AND cflow.name = 'Upfront'  THEN (abes.settle_amount/uc.factor) ELSE abes.settle_amount END AS ohd_settle_amount,")
		.append("\n iacc.account_name AS internal_account,eacc.account_name AS external_account,")
		.append("\n RANK() OVER(PARTITION BY usd.deal_num ORDER BY ab.deal_tracking_num desc)-1 deal_count_num")
		.append("\n FROM user_strategy_deals usd ")
		.append("\n JOIN ab_tran st ON st.deal_tracking_num = usd.deal_num AND st.tran_status = usd.tran_status AND st.current_flag = 1 ")
		.append("\n JOIN party p ON p.party_id = st.internal_bunit AND p.short_name IN ('").append(party.replace(" , ", "','")).append("')")
		.append("\n JOIN ab_tran_info strnum ON strnum.value = usd.deal_num AND strnum.type_id = ").append(EndurTranInfoField.STRATEGY_NUM.toInt())
		.append("\n JOIN ab_tran_info metal ON metal.tran_num = st.tran_num AND metal.type_id = ").append(EndurTranInfoField.METAL.toInt()) 
		.append("\n JOIN ab_tran_info unit ON unit.tran_num = st.tran_num AND unit.type_id = ").append(EndurTranInfoField.UNIT.toInt())
		.append("\n JOIN trans_status ts ON ts.trans_status_id = usd.tran_status ")
		.append("\n JOIN ab_tran ab ON ab.tran_num = strnum.tran_num AND ab.current_flag =1 ") 
		.append("\n JOIN trans_status ts2 ON ts2.trans_status_id = ab.tran_status ")
		.append("\n JOIN ab_tran_event abe ON abe.tran_num = ab.tran_num and abe.event_type = ").append(EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt())
		.append("\n JOIN ab_tran_event_settle abes ON abes.event_num = abe.event_num ")
		.append("\n LEFT JOIN account iacc ON iacc.account_id = abes.int_account_id ")
		.append("\n LEFT JOIN account eacc ON eacc.account_id = abes.ext_account_id ")		
		.append("\n JOIN cflow_type cflow ON cflow.id_number = abe.pymt_type AND abe.pymt_type != ").append(Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, TRANSFER_CHARGE))
		.append("\n LEFT JOIN unit_conversion uc ON uc.src_unit_id = abe.unit AND uc.dest_unit_id = 55 ");
		
		if(date_type.equalsIgnoreCase(MetalTransferValidationHelper.SETTLEDATE)){
			sql.append("\n WHERE st.settle_date >= '").append(start_date.substring(0, start_date.indexOf(" ")).trim()).append("'")
			.append("\n AND st.settle_date <= '").append(end_date.substring(0, end_date.indexOf(" ")).trim()).append("'");
		}
		else{
			sql.append("\n WHERE st.trade_date >= '").append(start_date.substring(0, start_date.indexOf(" ")).trim()).append("'")
			.append("\n AND st.trade_date <= '").append(end_date.substring(0, end_date.indexOf(" ")).trim()).append("'");
		}
		
		sql.append("\n AND ts.name IN ('").append(status.replace(" , ", "','")).append("')")
		.append("\n ) temp \n");
		
		if (status.equalsIgnoreCase(VALIDATED) || status.equalsIgnoreCase(NEW)) {
			sql.append("\n WHERE deal_count_num < expected_cash_deal_count \n");
		}	
		
		sql.append("\n UNION \n")
		.append("\n SELECT p.short_name AS party_name,usd.deal_num as strategy_deal_num,ts.name as Strategy_deal_status,metal.value AS metal,")
		.append("\n unit.value AS unit,FORMAT(st.trade_date,'dd-MMM-yyyy') AS trade_date,FORMAT(st.settle_date,'dd-MMM-yyyy') AS settle_date,")
		.append("\n usd.expected_cash_deal_count,usd.actual_cash_deal_count, ")
		.append("\n ab.deal_tracking_num AS cash_deal_number,cflow.name AS cflow_type,ts2.name AS cash_deal_status,")
		.append("\n CASE WHEN abe.unit != 55 AND cflow.name = 'Upfront'  THEN (abes.settle_amount/uc.factor) ELSE abes.settle_amount END AS ohd_settle_amount,")
		.append("\n iacc.account_name AS internal_account,eacc.account_name AS external_account,")
		.append("\n RANK() OVER(PARTITION BY usd.deal_num ORDER BY ab.deal_tracking_num desc)-1 deal_count_num")
		.append("\n FROM user_strategy_deals usd ")
		.append("\n JOIN ab_tran st ON st.deal_tracking_num = usd.deal_num AND st.tran_status = usd.tran_status AND st.current_flag = 1 ")
		.append("\n JOIN party p ON p.party_id = st.internal_bunit AND p.short_name IN ('").append(party.replace(" , ", "','")).append("')")
		.append("\n JOIN ab_tran_info strnum ON strnum.value = usd.deal_num AND strnum.type_id = ").append(EndurTranInfoField.STRATEGY_NUM.toInt())
		.append("\n JOIN ab_tran_info metal ON metal.tran_num = st.tran_num AND metal.type_id = ").append(EndurTranInfoField.METAL.toInt())
		.append("\n JOIN ab_tran_info unit ON unit.tran_num = st.tran_num AND unit.type_id = ").append(EndurTranInfoField.UNIT.toInt())
		.append("\n JOIN trans_status ts ON ts.trans_status_id = usd.tran_status ")
		.append("\n JOIN ab_tran ab ON ab.tran_num = strnum.tran_num AND ab.current_flag =1 ") 
		.append("\n JOIN trans_status ts2 ON ts2.trans_status_id = ab.tran_status ")
		.append("\n JOIN ab_tran_event abe ON abe.tran_num = ab.tran_num and abe.event_type = ").append(EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt())
		.append("\n JOIN ab_tran_event_settle abes ON abes.event_num = abe.event_num ")
		.append("\n LEFT JOIN account iacc ON iacc.account_id = abes.int_account_id ")
		.append("\n LEFT JOIN account eacc ON eacc.account_id = abes.ext_account_id ")		
		.append("\n JOIN cflow_type cflow ON cflow.id_number = abe.pymt_type AND abe.pymt_type = ").append(Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, TRANSFER_CHARGE))
		.append("\n LEFT JOIN unit_conversion uc ON uc.src_unit_id = abe.unit AND uc.dest_unit_id = 55 ");
		
		if(date_type.equalsIgnoreCase(MetalTransferValidationHelper.SETTLEDATE)){
			sql.append("\n WHERE st.settle_date >= '").append(start_date.substring(0, start_date.indexOf(" ")).trim()).append("'")
			.append("\n AND st.settle_date <= '").append(end_date.substring(0, end_date.indexOf(" ")).trim()).append("'");
		}
		else{
			sql.append("\n WHERE st.trade_date >= '").append(start_date.substring(0, start_date.indexOf(" ")).trim()).append("'")
			.append("\n AND st.trade_date <= '").append(end_date.substring(0, end_date.indexOf(" ")).trim()).append("'");
		}
		
		sql.append("\n AND ts.name IN ('").append(status.replace(" , ", "','")).append("')")
		.append("\n ) temp") ;
		
		Logging.info(" sql : "+sql.toString());

		DBaseTable.execISql(tblData, sql.toString());
		
		//clearing the StringBuilder
		sql.setLength(0);

		return tblData;

	}
	

	/**This method set the column label and save the report 
	 *
	 * @param tblData
	 * @param status
	 * @return
	 * @throws OException
	 */
	public void formatAndSaveReport(Table tblData,String status, String filePath) throws OException {

		try {

			for (int col = 1; col <= tblData.getNumCols(); col++) {
				tblData.setColTitle(col, tblData.getColName(col).replace("_", " ").toUpperCase());
			}
			
			tblData.excelSave(filePath,status, "A1", 0);
			Logging.info("file saved at : " + filePath);

		} catch (OException e) {
			Logging.error("Error while save the report. reason : " + e.getMessage());
			throw new OException("Error while save the report. reason : " + e.getMessage());
		}
		
	}
	
	/**
	 * @param reportName
	 * @return
	 * @throws OException
	 */
	public String getFilePath(String reportName) throws OException{

		String filePath = "";
		String user = "";
		try {

			user = Ref.getUserName();
			filePath = "";

			String reportDirToday = Util.reportGetDirForToday();

			filePath = reportDirToday + "/" + reportName + "_" + user + "_" + Util.timeGetServerTimeHMS().replace(":", "")+".xlsx";

			filePath = filePath.replace("/", "\\");
			
		} catch (OException e) {
			Logging.error("Error while getiing the file path. reason : " + e.getMessage());
			throw new OException("Error while getiing the file path. reason : " + e.getMessage());
		}
		return filePath;

	}
	
	
	/**This method create the table structure and add the required number of columns according to the max number 
	 * of cash deals for a strategy deal
	 * 
	 * @param tblData
	 * @return
	 * @throws OException
	 */
	public int formatTable(Table tblData) throws OException{
		
		int maxColumn = 0;
		
		try {
			tblData.sortCol(DEAL_COUNT, TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_DESCENDING);
			maxColumn = tblData.getInt(DEAL_COUNT, 1);
			
			for(int col = 1 ;col <= maxColumn; col++){
				tblData.addCol("cash_deal_number_"+col, COL_TYPE_ENUM.COL_INT);
				tblData.addCol("cflow_type_"+col, COL_TYPE_ENUM.COL_STRING);
				tblData.addCol("cash_deal_status_"+col, COL_TYPE_ENUM.COL_STRING);
				tblData.addCol("settle_amount_"+col, COL_TYPE_ENUM.COL_DOUBLE);
				tblData.addCol("internal_account_"+col, COL_TYPE_ENUM.COL_STRING);
				tblData.addCol("external_account_"+col, COL_TYPE_ENUM.COL_STRING);
			}
			
		} catch (OException e) {
			Logging.error("Error while adding columns to table. reason : "+e.getMessage());
			throw new OException("Error while adding columns to table. reason : "+e.getMessage());
		}
		return maxColumn;
		
	}
	
	/**This method transpose row data to the column for a strategy deal
	 * 
	 * 
	 * @param tblData
	 * @param maxCol
	 * @throws OException
	 */
	public void prepareData(Table tblData,int maxCol,String status) throws OException{
		
		Table origData = null;
		
		try {
			origData = tblData.copyTable();
			
			tblData.clearDataRows();
			
			tblData.select(origData, "*", DEAL_COUNT+" EQ 0");
			
			for(int loop = 1; loop <= maxCol;loop++){
				
			tblData.select(origData, "cash_deal_number(cash_deal_number_"+loop+"),"
								   + "cash_deal_status(cash_deal_status_"+loop+"),"
								   + "cflow_type(cflow_type_"+loop+"),"
								   + "settle_amount(settle_amount_"+loop+"),"
								   + "internal_account(internal_account_"+loop+"),"
								   + "external_account(external_account_"+loop+")",
								   STRATEGY_DEAL_NUM+" EQ $"+STRATEGY_DEAL_NUM
								   + " AND "+DEAL_COUNT+" EQ "+loop);
			}
			tblData.delCol(DEAL_COUNT);
			tblData.delCol(DEAL_COUNT_NUM);
			
			//perform validation checks on the data
			validationChecks(tblData,status);
					
		} catch (OException e) {
			Logging.error("Error while adding data to table. reason : "+e.getMessage());
			throw new OException("Error while adding data to table. reason : "+e.getMessage());
		}
		finally {
			if(Table.isValidTable(origData) && origData!= null ){
				origData.destroy();
			}
		}
		
	}
	
	/**This method is for cancelled and deleted strategy deal. This check whether all the associated cash/vat deal are cancelled or not 
	 * and update the comments column according to that
	 * 
	 * 
	 * @param tblData
	 * @return
	 * @throws OException
	 */
	public Table preparaCancelledData(Table tblData) throws OException {

		Table finalData = Table.tableNew();
		Table tblTemp = Table.tableNew();
		int strDealNum = 0;
		int cashDealNum = 0;
		int dealNum = 0;
		String cashDealStatus = "";
		String problemDeals = null;
		Boolean flag = true;

		try {

			finalData.select(tblData,"party_name,strategy_deal_num,Strategy_deal_status,metal,unit,trade_date,settle_date",
									  DEAL_COUNT+" EQ 0"); 

			finalData.addCol(COMMENTS, COL_TYPE_ENUM.COL_STRING);

			for (int loop = 1; loop <= finalData.getNumRows(); loop++) {

				tblTemp.clearDataRows();
				flag = true;
				problemDeals = null;
				
				strDealNum = finalData.getInt(STRATEGY_DEAL_NUM, loop);
				tblTemp.select(tblData, "*", STRATEGY_DEAL_NUM+" EQ "+strDealNum);				
				
				for (int j = 1; j <= tblTemp.getNumRows(); j++) {

					dealNum = tblTemp.getInt(STRATEGY_DEAL_NUM, j);
					cashDealStatus = tblTemp.getString(CASH_DEAL_STATUS, j);
					cashDealNum = tblTemp.getInt(CASH_DEAL_NUM, j);

					if (dealNum == strDealNum && !(cashDealStatus.equalsIgnoreCase(CANCELLED) || cashDealStatus.equalsIgnoreCase(DELETED))) {
						flag = false;
						if(problemDeals == null)
							problemDeals = String.valueOf(cashDealNum);
						else
							problemDeals = problemDeals+","+String.valueOf(cashDealNum);
					}

				}

				if (flag)
					finalData.setString(COMMENTS, loop,"All the associated cash/VAT deals are cancelled");
				else
					finalData.setString(COMMENTS, loop,"Some of the associated cash/VAT deals are not cancelled. Please verify ("+problemDeals+")");
			}

		} catch (OException e) {
			Logging.error("Error -" + e.getMessage());
			throw new OException("Error -" + e.getMessage());
		}
		finally {
			if(Table.isValidTable(tblTemp) && tblTemp!= null ){
				tblTemp.destroy();
			}
		}
		return finalData;

	}
	
	/**
	 * @param tblData
	 * @param status
	 */
	private void validationChecks(Table tblData,String status) {

		Boolean countFlag;
		Boolean statusFlag;
		String problemDeals = null;
		int cashDealNum = 0;

		try {
			tblData.addCol(COMMENTS, COL_TYPE_ENUM.COL_STRING);

			for (int row = 1; row <= tblData.getNumRows(); row++) {

				countFlag = true;
				statusFlag = true;
				problemDeals = null;

				if (tblData.getInt(EXPECTED_COUNT, row) != tblData.getInt(ACTUAL_COUNT, row)) {
					countFlag = false;
				}

				for (int col = 1; col <= tblData.getNumCols(); col++) {
					
					if (tblData.getColType(col) == COL_TYPE_ENUM.COL_STRING.toInt()
							&& tblData.getColName(col).contains(CASH_DEAL_STATUS)) {
						if (tblData.getString(col, row) == null) {
							continue;
						}
						if (!(tblData.getString(col, row).equalsIgnoreCase(status.trim())
								|| tblData.getString(col, row).isEmpty() || tblData.getString(col, row).equals(" "))) {
							statusFlag = false;
							
							cashDealNum = tblData.getInt(col-2, row);
							
							if(problemDeals == null)
								problemDeals = String.valueOf(cashDealNum);
							else
								problemDeals = problemDeals+","+String.valueOf(cashDealNum);
						}
					}
				}

				if (countFlag && statusFlag)
					tblData.setString(COMMENTS, row,"Expected and actual deal count is same and all the cash/VAT deal(s) are in correct status");
				else
					tblData.setString(COMMENTS, row,"Cash deal(s) has a problem. Please verify ("+problemDeals+")");

			}
		} catch (Exception e) {			
			Logging.error("Error in validations, Reason : "+e.getMessage());
		}

	}
	
	/**
	 * @param filePath
	 * @param party
	 */
	public void sendReportOnMail(String filePath,String party){		
		
		try {		
			ConstRepository constRepo =  new ConstRepository(CONTEXT, SUB_CONTEXT);		
			
			String recipients = "";
			String emailService = constRepo.getStringValue(EMAILSERVICE);
			String subject = constRepo.getStringValue(SUBJECT).replace("<env>", Ref.getInfo().getString(DATABASE, 1));
			String mailBody = constRepo.getStringValue(MAILBODY).replace("<path>", filePath);
			
			if (party.contains("CN"))
				recipients = constRepo.getStringValue(CNRECEIPIENT);
			else if(party.contains("HK"))
				recipients = constRepo.getStringValue(HKRECEIPIENT);
			else if(party.contains("US"))
				recipients = constRepo.getStringValue(USRECEIPIENT);
			else
				recipients = constRepo.getStringValue(UKRECEIPIENT);			
			
			Utils.sendEmail(recipients, subject, mailBody, filePath, emailService);
			
		} catch (Exception e) {
			Logging.error("Error : "+e.getMessage());
			throw new RuntimeException("Error in sending mail. Reason : "+e.getMessage());
		}
		
	}
	
	/**
	 * @param party
	 * @param start_date
	 * @param end_date
	 * @param status
	 * @return
	 * @throws OException
	 */
	public Table getExceptionData(String party,String date_type,String start_date,String end_date) throws OException{
		
		Table tblData = Table.tableNew();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append(" SELECT p.short_name AS party,usd.deal_num as strategy_deal_num,usd.retry_count,ts.name as status_in_user_table,tsdb.name as status_in_db,")
		.append("\n 'user table is not in sync with DB' AS comments ")
		.append("\n FROM user_strategy_deals usd ")
		.append("\n JOIN ab_tran st ON st.deal_tracking_num = usd.deal_num AND st.current_flag = 1  AND st.tran_status != usd.tran_status ")
		.append("\n JOIN trans_status ts ON ts.trans_status_id = usd.tran_status ")
		.append("\n JOIN trans_status tsdb ON tsdb.trans_status_id = st.tran_status ")
		.append("\n JOIN party p ON p.party_id = st.internal_bunit ")		
		.append("\n JOIN (SELECT usd.deal_num,max(usd.version_number) AS version_number from USER_strategy_deals usd ")
		.append("\n JOIN ab_tran st ON st.deal_tracking_num = usd.deal_num AND st.current_flag = 1 ") 
		.append("\n JOIN trans_status ts ON ts.trans_status_id = usd.tran_status ")
		.append("\n JOIN party p ON p.party_id = st.internal_bunit AND p.short_name IN ('").append(party.replace(" , ", "','")).append("')");
		
		if(date_type.equalsIgnoreCase(MetalTransferValidationHelper.SETTLEDATE)){
			sql.append("\n WHERE st.settle_date >= '").append(start_date.substring(0, start_date.indexOf(" ")).trim()).append("'")
			.append("\n AND st.settle_date <= '").append(end_date.substring(0, end_date.indexOf(" ")).trim()).append("'");
		}
		else{
			sql.append("\n WHERE st.trade_date >= '").append(start_date.substring(0, start_date.indexOf(" ")).trim()).append("'")
			.append("\n AND st.trade_date <= '").append(end_date.substring(0, end_date.indexOf(" ")).trim()).append("'");
		}
		
		sql.append("\n GROUP BY usd.deal_num) temp ON temp.deal_num = usd.deal_num AND temp.version_number = usd.version_number  ");
		
		Logging.info(" sql : "+sql.toString());

		DBaseTable.execISql(tblData, sql.toString());
		
		//clearing the StringBuilder
		sql.setLength(0);

		return tblData;
		
	}
	
}
