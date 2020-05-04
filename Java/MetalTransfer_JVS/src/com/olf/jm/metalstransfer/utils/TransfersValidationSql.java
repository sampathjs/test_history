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

public class TransfersValidationSql {

	protected static final ConstRepository _constRepo = null;

	public static String checkForTaxDeals(int queryId, String bufferTime)
			throws OException {
		String checkForTaxDeals;
		checkForTaxDeals = "SELECT  usd.deal_num,usd.tran_num,usd.tran_status,usd.status,usd.last_updated,usd.version_number,usd.retry_count,usd.actual_cash_deal_count ,usd.expected_cash_deal_count,'Expected and Actual cash deals count is not matching' AS description \n"
							+ "FROM  user_strategy_deals usd \n"
							+ "WHERE usd.actual_cash_deal_count <> usd.expected_cash_deal_count \n"
							+ "AND usd.status like ('Succeeded')OR usd.status like ('Pending') \n"
							+ "AND usd.last_updated < DATEADD(minute,-"+bufferTime+", Current_TimeStamp) \n"
							+ "AND usd.process_Type NOT LIKE 'Exclude'"
							+ "AND usd.tran_status in ("+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+ ","+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+ " )\n";
							//+ ""\n"; 
		return checkForTaxDeals;
	}

	public static String strategyForValidation(String symtLimitDate) throws OException {
		String limitDate;
		int currentDate = OCalendar.getServerDate();
		int jdConvertDate = OCalendar.parseStringWithHolId(symtLimitDate, 0,currentDate);
		limitDate = OCalendar.formatJd(jdConvertDate);
		String strategyDeals = "SELECT ab.deal_tracking_num as strategyDealNum \n"
								+ "FROM ab_tran ab \n"
									+ "JOIN user_strategy_deals us "
										+ "ON ab.deal_tracking_num  = us.deal_num \n"
									+ "WHERE us.status = 'Succeeded' \n"
										+ "OR us.status = 'Pending' \n"
										+ "AND us.tran_status in ("+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+") \n"
										+ "AND ab.toolset ="+ TOOLSET_ENUM.COMPOSER_TOOLSET.toInt()	+ "\n"
										+ "AND ins_type = "+ INS_TYPE_ENUM.strategy.toInt()+ "\n"
										+ "AND trade_date >='"+ limitDate+ "'\n"
										+ "AND us.deal_num NOT IN (SELECT deal_number FROM USER_jm_strategy_exclusion) ";

		return strategyDeals;

	}

	public static String fetchDataForAllStrategies(int qid) throws OException {
		String strategyDeals = "SELECT ab.deal_tracking_num as StrategyDealNum,ab.tran_status as StrategyTranStatus ,COUNT(ab1.deal_tracking_num) as CountOfCashDeal ,ab1.tran_status as CashTranStatus,usd.deal_num,usd.tran_num,usd.tran_status,usd.status,usd.last_updated,usd.version_number,usd.retry_count \n"
									+ "FROM ab_tran ab \n"
										+ "INNER JOIN query_result qr\n"
											+ "ON qr.query_result = ab.deal_tracking_num\n"
												+ "AND qr.unique_id = "	+ qid+ "\n"
										+ "LEFT  JOIN ab_tran_info ai \n"
											+ "ON ai.value = ab.deal_tracking_num \n"
												+ "AND ai.type_id = "+ Constants.TranIdStrategyNum+ "\n"
										+ "LEFT JOIN ab_tran ab1 \n"
											+ "ON ai.tran_num = ab1.tran_num \n"
										+ "INNER JOIN USER_strategy_deals usd \n"
											+ "ON ab.deal_tracking_num = usd.deal_num\n"
												+ "AND ab.toolset ="+ TOOLSET_ENUM.COMPOSER_TOOLSET.toInt()+ "\n"
												+ "AND ab.ins_type = "+ INS_TYPE_ENUM.strategy.toInt()+ "\n"
												+ "AND ab.current_flag = 1\n"
												+ "AND ab1.toolset ="+ TOOLSET_ENUM.CASH_TOOLSET.toInt()+ "\n"
												+ "AND ab1.ins_sub_type = "	+ INS_SUB_TYPE.cash_transfer.toInt()+ "\n"
												+ "AND ab1.current_flag = 1\n"
												+ "GROUP BY ab.deal_tracking_num,ab.tran_status,ab1.tran_status,usd.deal_num,usd.tran_num,usd.tran_status,usd.status,usd.last_updated,usd.version_number,usd.retry_count";

		return strategyDeals;

	}

}
