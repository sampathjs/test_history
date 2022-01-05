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
import com.openlink.util.constrepository.ConstRepository;

/*
 * History:
 *              V1.1                           - Initial Version
 * 2021-05-31   V1.2    Prashanth   EPI-1810   - Alert 'Invalid transfer strategy found missing' enhanced to capture 
 *                                               deals booked directly in validated status
 */
 
public class TransfersValidationSql {

	protected static final ConstRepository _constRepo = null;

	public static String checkForTaxDeals(int queryId, String bufferTime)
			throws OException {
		String checkForTaxDeals;
		checkForTaxDeals = "SELECT  usd.deal_num,usd.tran_num,usd.tran_status,usd.status,usd.last_updated,usd.version_number,usd.retry_count,usd.actual_cash_deal_count ,usd.expected_cash_deal_count,'Expected and Actual cash deals count is not matching' AS Description \n"
							+ "FROM  user_strategy_deals usd \n"
							+ "WHERE usd.actual_cash_deal_count <> usd.expected_cash_deal_count \n"
							+ "AND usd.status like ('Succeeded') \n"
							+ "AND usd.last_updated < DATEADD(minute,-"+bufferTime+", Current_TimeStamp) \n"
							+ "AND usd.process_Type NOT LIKE 'Exclude'"
							+ "AND usd.tran_status in ("+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+ ","+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+ " )\n";
							//+ ""\n"; 
		return checkForTaxDeals;
	}

	public static String strategyForValidation(String symtLimitDate) throws OException {
		String limitDate;
		int currentDate = OCalendar.getCurrentSessionDate();
		int jdConvertDate = OCalendar.parseStringWithHolId(symtLimitDate, 0, currentDate);
		limitDate = OCalendar.formatJd(jdConvertDate);
		StringBuilder strategyDeals = new StringBuilder();
		strategyDeals.append("SELECT ab.deal_tracking_num as strategyDealNum FROM ab_tran ab ");
		strategyDeals.append("\nLEFT JOIN user_strategy_deals us ON ab.deal_tracking_num  = us.deal_num ");
		strategyDeals.append("\n          AND us.status IN ('Succeeded', 'Pending')");
		strategyDeals.append("\nWHERE ab.tran_status in ( ").append(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt())
				.append(", ").append(TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()).append(")");
		strategyDeals.append("\n  AND ab.toolset = ").append(TOOLSET_ENUM.COMPOSER_TOOLSET.toInt());
		strategyDeals.append("\n  AND ab.ins_type = ").append(INS_TYPE_ENUM.strategy.toInt());
		strategyDeals.append("\n  AND ab.trade_date >='").append(limitDate).append("'");
		strategyDeals.append("\n AND ab.deal_tracking_num NOT IN (SELECT deal_number FROM USER_jm_strategy_exclusion)");

		return strategyDeals.toString();

	}

	public static String fetchDataForAllStrategies(int qid, String retry_limit) throws OException {
		StringBuilder strategyDeals = new StringBuilder();
		strategyDeals.append("SELECT ab.deal_tracking_num as StrategyDealNum, ab.tran_status as StrategyTranStatus");
		strategyDeals.append("\n , COUNT(ab1.deal_tracking_num) as CountOfCashDeal, ab1.tran_status as CashTranStatus");
		strategyDeals.append("\n , usd.deal_num, usd.tran_num, usd.tran_status, usd.status, usd.last_updated");
		strategyDeals.append("\n , usd.version_number, usd.retry_count");
		strategyDeals.append("\n FROM ab_tran ab");
		strategyDeals.append("\n JOIN query_result qr ON qr.query_result = ab.deal_tracking_num ");
		strategyDeals.append("\n             AND qr.unique_id = ").append(qid);
		strategyDeals.append("\n LEFT JOIN ab_tran_info ai ON ai.value = ab.deal_tracking_num");
		strategyDeals.append("\n             AND ai.type_id = ").append(Constants.TranIdStrategyNum);
		strategyDeals.append("\n LEFT JOIN ab_tran ab1 ON ai.tran_num = ab1.tran_num");
		strategyDeals.append("\n JOIN USER_strategy_deals usd ON ab.deal_tracking_num = usd.deal_num");
		strategyDeals.append("\nWHERE ab.toolset = ").append(TOOLSET_ENUM.COMPOSER_TOOLSET.toInt());
		strategyDeals.append("\n  AND ab.ins_type = ").append(INS_TYPE_ENUM.strategy.toInt());
		strategyDeals.append("\n  AND ab.current_flag = 1");
		strategyDeals.append("\n  AND ab1.toolset = ").append(TOOLSET_ENUM.CASH_TOOLSET.toInt());
		strategyDeals.append("\n  AND ab1.ins_sub_type = ").append(INS_SUB_TYPE.cash_transfer.toInt());
		strategyDeals.append("\n  AND ab1.current_flag = 1");
		strategyDeals.append("\n  AND ab.deal_tracking_num = 0");
		strategyDeals.append("\nGROUP BY ab.deal_tracking_num, ab.tran_status, ab1.tran_status, usd.deal_num");
		strategyDeals.append("\n   , usd.tran_num, usd.tran_status, usd.status, usd.last_updated, usd.version_number");
		strategyDeals.append("\n   , usd.retry_count");

		// Changes to fetch strategies directly booked in validated status
		// In the above query if we use LEFT JOIN on user_strategy_deals, the query will take 20+mins
		// To increase the performance below UNION is used
		
		strategyDeals.append("\n\n UNION ALL \n");
		strategyDeals.append("\nSELECT ab.deal_tracking_num as StrategyDealNum, ab.tran_status as StrategyTranStatus");
		strategyDeals.append("\n , 0 as CountOfCashDeal, 0 as CashTranStatus, ab.deal_tracking_num deal_num");
		strategyDeals.append("\n , ab.tran_num tran_num, ab.tran_status tran_status, usd.status, usd.last_updated");
		strategyDeals.append("\n , usd.version_number, ").append(retry_limit).append(" retry_count");
		strategyDeals.append("\n FROM ab_tran ab");
		strategyDeals.append("\n JOIN query_result qr ON qr.query_result = ab.deal_tracking_num ");
		strategyDeals.append("\n             AND qr.unique_id = ").append(qid);
		strategyDeals.append("\n LEFT JOIN USER_strategy_deals usd ON ab.deal_tracking_num = usd.deal_num");
		strategyDeals.append("\nWHERE NOT EXISTS ( SELECT DISTINCT CAST(ai.value AS INT) value, ai.tran_num");
		strategyDeals.append("\n                     FROM ab_tran_info ai");
		strategyDeals.append("\n                    WHERE ai.type_id = ").append(Constants.TranIdStrategyNum);
		strategyDeals.append("\n                      AND ai.value = ab.deal_tracking_num)");
		
		return strategyDeals.toString();

	}

}
