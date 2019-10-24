package com.olf.jm.metalstransfer.utils;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openrisk.trading.EnumBuySell;
import com.openlink.util.constrepository.ConstRepository;

public class TransfersValidationSql {

    protected static final ConstRepository _constRepo = null;

	public static String checkForTaxDeals(int queryId,int retry_limit) throws OException{
	String checkForTaxDeals;
	checkForTaxDeals = 	"SELECT  usd.deal_num,usd.tran_num,usd.tran_status,usd.status,usd.last_updated,usd.version_number,usd.retry_count,C.reason,C.expected_cash_deal_count, C.actual_cash_deal_count \n"
						+ "FROM (\n";
	checkForTaxDeals+= 	"SELECT coalesce(A.value,B.value) as strategy_deal,ISNULL(A.expected_cash_deal_count,2) expected_Cash_deal_count ,B.actual_cash_deal_count,IIF(B.actual_cash_deal_count-A.expected_Cash_deal_count!=0,'Expected tax Deals are missing','Matched') as reason \n"
					   	+ "FROM (\n";
	checkForTaxDeals+= 	"SELECT distinct(ai.value), count(*)+2 as expected_cash_deal_count \n"
					  	+ "FROM  ab_tran ab\n"
					  	+ "LEFT JOIN ab_tran_info ai \n"
			          		+ "ON ab.tran_num = ai.tran_num \n"
			          		+ "AND  ab.buy_sell = " +EnumBuySell.Sell.getValue() + "\n"
			          	+ "LEFT JOIN query_result  qr \n"
			          		+ "ON qr.query_result = value AND qr.unique_id ="+queryId+" \n"
						+ "LEFT JOIN ab_tran_tax abt \n"
							+ "ON abt.tran_num = ab.tran_num \n" 
						+ "JOIN ab_tran_tax abt2 \n"
							+ "ON abt2.tran_num = ab.tran_num \n"
							+ "AND abt2.tax_tran_type = -1 \n"
						+ "LEFT  JOIN tax_tran_subtype_restrict tst \n"
							+ "ON (tst.tax_tran_subtype_id = abt.tax_tran_subtype)\n"
						+ "LEFT JOIN tax_rate tr2 \n"
							+ "ON tr2.tax_rate_id = tst.tax_rate_id \n" 
							+ "WHERE  ai.type_id = "+Constants.TranIdStrategyNum+ "\n"
								+ "AND ab.toolset ="+TOOLSET_ENUM.CASH_TOOLSET.toInt() +"\n" 
								+ "AND ab.ins_sub_type = "+INS_SUB_TYPE.cash_transfer.toInt()+"\n"
								+ "AND ab.tran_status ="+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+"\n"
								+ "AND ab.current_flag = 1 \n"
								//+ "AND ai.value in ("+queryId+")\n" 
								+ "AND tr2.charge_rate >0\n"
								+ "Group by ai.value";
	checkForTaxDeals+=	")A\n";
	checkForTaxDeals+=  "FULL JOIN (\n";
	checkForTaxDeals+=	"SELECT ati.value,count(*) as actual_cash_deal_count \n"
						+ " FROM \n"
							+ "ab_tran_info_view ati \n" 
							+ "inner join ab_tran ab on ati.tran_num = ab.tran_num \n"
								+ "AND type_id = "+Constants.TranIdStrategyNum+ "\n"
								+ "LEFT JOIN query_result  qr \n"
				          		+ "ON qr.query_result = value AND qr.unique_id ="+queryId+" \n" 
								+ "WHERE \n"
								+ "ab.current_flag = 1 \n" 
								+ "AND tran_status = "+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+"\n"
								+ "AND ab.cflow_type in ("+CFLOW_TYPE.UPFRONT_CFLOW.toInt()+","+ Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE,"VAT") +")\n"
								+ "Group by ati.value \n";
	checkForTaxDeals+=	 ")B\n"
						+ "ON A.value = B.value)C\n";
	checkForTaxDeals+=	"JOIN USER_strategy_deals usd \n"
						 +"ON C.strategy_deal = usd.deal_num \n"
						 	+"-- AND usd.retry_count < "+retry_limit+" \n"
						 	+"AND usd.status = 'Succeeded'\n"
							+ "WHERE reason not Like '%Matched%'\n";
	return checkForTaxDeals;
    }
					
    public static String validateCashTransfer(int queryId, String strExcludedTrans, int iReportingStartDate, String timeWindow) throws OException{	  
    	
	String validateCashTransfer;
			
			//Strategy is New, Cash is Validated
	validateCashTransfer = "SELECT usd.deal_num,usd.tran_num,usd.tran_status,usd.status,usd.last_updated,usd.version_number,usd.retry_count,A.reason,expected_Cash_deal_count = 0,actual_cash_deal_count=0 \n"+
					"FROM (SELECT 'Strategy is New, Cash is Validated' as reason, ab_strategy.deal_tracking_num as strategy_deal_num,\n" +
                    "  ab_strategy.tran_status ,ab_strategy.internal_bunit, ab_strategy.external_bunit, ab_strategy.reference, ab_strategy.trade_date \n" +
                    " FROM  ab_tran ab_strategy \n" +
                    "  INNER JOIN ab_tran ab_cash ON(ab_strategy.reference = ab_cash.reference AND ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num)\n" + 
                    "  INNER JOIN  query_result qr \n"+
                    "  ON ab_strategy.deal_tracking_num = qr.query_result  \n"+
                    "  AND qr.unique_id in ( "+queryId+" )\n"+
                    " WHERE ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "New")+ " \n" + 
                    "  AND ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n" + 
                    "  AND ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n" +
                    "  AND ab_strategy.trade_time <= DATEADD(mi,"+timeWindow+",getdate()) \n" +
                    "  AND ab_strategy.trade_time > "+ iReportingStartDate  + " \n" +
                 
                   ((!strExcludedTrans.isEmpty() && !strExcludedTrans.equals("") && !strExcludedTrans.equals(" ")) ?
                           "  AND ab_strategy.tran_num NOT IN (" + strExcludedTrans + " ) \n" : "") ;

           // Strategy is New, Cash deal does not exist
	validateCashTransfer += " UNION ALL SELECT 'Strategy is New, Cash deal does not exist' as reason, ab_strategy.deal_tracking_num as strategy_deal_num,\n" +
                     "  ab_strategy.tran_status, ab_strategy.internal_bunit, ab_strategy.external_bunit, ab_strategy.reference, ab_strategy.trade_date \n" +
                     " FROM ab_tran ab_strategy \n" + 
                     "  LEFT OUTER JOIN ab_tran ab_cash ON(ab_strategy.reference = ab_cash.reference AND ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num AND ab_cash.tran_status in (" +  Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated") + "," + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Matured") + "))\n" +
                     "  INNER JOIN  query_result qr \n"+
                     " ON ab_strategy.deal_tracking_num = qr.query_result\n"+  
                     " AND qr.unique_id in ( "+queryId+" ) \n"+
                     " WHERE ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "New")+ " \n" +
                     "  AND ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n" +
                     "  AND ab_cash.tran_status is null \n" +
                     "  AND ab_strategy.trade_time <= DATEADD(mi,"+timeWindow+",getdate()) \n" +
                     "  AND ab_strategy.trade_time > "+ iReportingStartDate  + " \n" +
                     
                     ( (!strExcludedTrans.isEmpty() && !strExcludedTrans.equals("") && !strExcludedTrans.equals(" ")) ? 
                             "  AND ab_strategy.tran_num NOT IN (" + strExcludedTrans + " ) \n" : "" ); 

           // Strategy is Deleted, Cash is Validated
	validateCashTransfer += " UNION ALL SELECT 'Strategy is Deleted, Cash is Validated' as reason, ab_strategy.deal_tracking_num as strategy_deal_num,\n" +
                     "  ab_strategy.tran_status, ab_strategy.internal_bunit, ab_strategy.external_bunit, ab_strategy.reference, ab_strategy.trade_date \n"+
                     " FROM ab_tran ab_strategy\n" + 
                     "  LEFT OUTER JOIN ab_tran ab_cash ON(ab_strategy.reference = ab_cash.reference AND ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num AND ab_cash.tran_status in  (" +  Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated") + "))\n" +
                     "	INNER JOIN  query_result qr \n"+
                     "	ON ab_strategy.deal_tracking_num = qr.query_result  \n"+
                     "	AND qr.unique_id in ( "+queryId+" ) \n"+
                     " WHERE ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Deleted") + " \n" +
                     "  AND ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n" +
                     "  AND ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n" +
                     "  AND ab_strategy.trade_time <= DATEADD(mi,"+timeWindow+",getdate()) \n" +
                     "  AND ab_strategy.trade_time > "+ iReportingStartDate  + " \n" +
                    
                     ((!strExcludedTrans.isEmpty() && !strExcludedTrans.equals("") && !strExcludedTrans.equals(" ")) ? 
                             "  AND ab_strategy.tran_num NOT IN (" + strExcludedTrans + " ) \n" : "");

           // Strategy is Validated, Cash is Cancelled
	validateCashTransfer += " UNION ALL ( ";
	validateCashTransfer += " SELECT 'Strategy is Validated, Cash is Cancelled' as reason, ab_strategy.deal_tracking_num as strategy_deal_num,\n" +
                     "  ab_strategy.tran_status, ab_strategy.internal_bunit, ab_strategy.external_bunit, ab_strategy.reference, ab_strategy.trade_date \n" +
                     " FROM ab_tran ab_strategy \n" + 
                     "  INNER JOIN ab_tran ab_cash ON (ab_strategy.reference = ab_cash.reference AND ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num AND ab_cash.tran_status in (" + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Cancelled") + "))\n" +
                     " 	INNER JOIN  query_result qr \n"+
                     " 	ON ab_strategy.deal_tracking_num = qr.query_result  \n"+
                     "	AND qr.unique_id in ( "+queryId+" ) \n"+
                     " WHERE ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n" +
                     "  AND ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n" +
                     "  AND ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Cancelled")+ " \n" +
                     "  AND ab_strategy.trade_time > "+ iReportingStartDate + " \n" +
                    
                     "  AND ab_strategy.trade_time <= DATEADD(mi,"+timeWindow+",getdate()) \n" ;
	validateCashTransfer += " EXCEPT \n";
	validateCashTransfer += " SELECT 'Strategy is Validated, Cash is Cancelled' as reason, ab_strategy.deal_tracking_num as strategy_deal_num,\n" +
                     "  ab_strategy.tran_status, ab_strategy.internal_bunit, ab_strategy.external_bunit, ab_strategy.reference, ab_strategy.trade_date \n" +
                     " FROM ab_tran ab_strategy \n" + 
                     "  INNER JOIN ab_tran ab_cash ON(ab_strategy.reference = ab_cash.reference AND ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num AND ab_cash.tran_status in (3,4))\n" +
                     "	INNER JOIN  query_result qr \n"+
                     "	ON ab_strategy.deal_tracking_num = qr.query_result  \n"+
                     "	AND qr.unique_id in ( "+queryId+" ) \n"+
                     " WHERE ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n" +
                     "  AND ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n" +
                     "  AND ab_cash.tran_status IN (" + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated") + "," + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Matured") + ") \n" +
                     "  AND ab_strategy.trade_time <= DATEADD(mi,"+timeWindow+",getdate()) \n" +
                    
                     "  AND ab_strategy.trade_time > "+ iReportingStartDate  + " \n" ;
	 validateCashTransfer += "))A \n";
	 validateCashTransfer += "LEFT JOIN User_strategy_deals usd \n"+
			 				 "ON usd.deal_num = A.strategy_deal_num \n"+
			 				 	"WHERE  usd.status = 'Succeeded'\n";
	 return validateCashTransfer;
	 
}
    
    
    public static String strategyForValidation(String symtLimitDate) throws OException{
    	String limitDate;
		int currentDate = OCalendar.getServerDate();
		int jdConvertDate = OCalendar.parseStringWithHolId(symtLimitDate,0,currentDate);
		limitDate = OCalendar.formatJd(jdConvertDate);
    	String strategyDeals = "SELECT ab.deal_tracking_num as strategyDealNum \n"
    							+"FROM ab_tran ab \n"
    							+"JOIN user_strategy_deals us "
    							  + "ON ab.deal_tracking_num  = us.deal_num \n"
    							  + "WHERE us.status = 'Succeeded' \n"
    							  + "AND ab.toolset ="+TOOLSET_ENUM.COMPOSER_TOOLSET.toInt()+"\n"
    							  +	"AND ins_type = "+INS_TYPE_ENUM.strategy.toInt()+"\n"
    							  + "AND trade_date >='"+limitDate+"'\n";
    							
		return strategyDeals;
    	
    }

	
}

