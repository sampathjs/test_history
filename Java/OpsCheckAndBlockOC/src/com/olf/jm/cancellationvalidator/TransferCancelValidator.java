package com.olf.jm.cancellationvalidator;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

/**
 * Concrete class specific to Composer Toolset
 * 
 * @author
 * 
 */
public class TransferCancelValidator extends AbstractValidator {

	private String strategyStatusToSkip = CancellationListener.strategyStatusToSkip;
	public static final String STRATEGY_NUM = "Strategy Num";

	public TransferCancelValidator(Transaction tran, Context context) {
		super(tran, "Cancellation is blocked for this deal due to business rules,\n"
				+ "cancellation needs to be approved by Finance,\nplease contact Support team for help", context);
	}

	@Override
	public boolean isCancellationAllowed() throws OException {

		boolean cancellationAllowed = false;
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		
		Table cashDeal = null;
		try {

			if (EnumToolset.Composer.compareTo(tran.getToolset()) == 0) {
				Logging.info("Checking Cancellation criteria for startegy ");
				cashDeal = getLinkedCashDeals();
				if(cashDeal!= null){
					int numOfCashDeals = cashDeal.getRowCount();
					Logging.info(String.format("Number of rows returned while retrieving latest cash deals for strategy# %s is %s", tran.getDealTrackingId(), numOfCashDeals));
					if(numOfCashDeals <=0){
						Logging.info("There was no linked cash deal to the strategy. It will be cancelled without any further checks");
						return cancellationAllowed = true;	
					}
					for(int i = 0; i < numOfCashDeals; i++){
						int businessUnit = cashDeal.getInt("business_unit", i);
						Date settleDate = cashDeal.getDate("settle_date", i);
						String settleDateString = sdf.format(settleDate);
						Logging.info("Settle Date as formatted String " + settleDateString);
						int settleDatejd = OCalendar.parseString(settleDateString);
						int tranNum = cashDeal.getInt("tran_num", i);
						Logging.info(String.format("Linked cash tran# %s Settle Date %s Business Unit %s", tranNum, settleDate, businessUnit));
						cancellationAllowed = hasMetalStatementRun(businessUnit, settleDatejd);
						if(!cancellationAllowed){
							break;
						}
					}
				}else{
					Logging.error("There was an error while retrieving linked cash deals for strategy " + tran.getDealTrackingId());
				}

			} else {
				Logging.info("Checking Cancellation criteria for Cash Deals");
				cancellationAllowed = isStrategyCancelled();

			}

			if (!cancellationAllowed) {
				Logging.info(" This deal doesn't meet the cancellation criteria for Cash/Strategies. It can't be cancelled");
			}
		} catch (OException exp) {
			Logging.error("There was an error checking cancellation criteria for Cash/Strategies  " + exp.getMessage());
			throw new OException(exp.getMessage());
		}finally{
			if(cashDeal!= null){
				cashDeal.dispose();
			} 
		}

		return cancellationAllowed;

	}

	private boolean hasMetalStatementRun(int businessUnit, int settleDate) throws OException {
		Table monthlyStmtRun = null;
		boolean flag = false;
		try {

			String query = " SELECT TOP 1 statement_period" + "  FROM USER_jm_monthly_metal_statement" + " WHERE internal_bunit = " + businessUnit
					+ " ORDER BY metal_statement_production_date  DESC";

			monthlyStmtRun = runSql(query);
			int resultRows = monthlyStmtRun.getRowCount();
			Logging.info("Number of rows returned from the user_jm_monnthly_metal_statement Table is " + resultRows);
			if (resultRows > 0) {
				String StmtRunDate = monthlyStmtRun.getString(0, 0);
				Logging.info("\n Latest Metal Statement Run date " + StmtRunDate);
				int jdStmtRunDate = OCalendar.parseString(StmtRunDate);
				if (!isSameMonth(settleDate, jdStmtRunDate) && !isFutureMonth(jdStmtRunDate, settleDate)) {
					flag = true;
					Logging.info("Metal statement has not been run for this deal trade Month/ BU combination. This can be cancelled");
				}
			}
		} finally {
			if (monthlyStmtRun != null) {
				monthlyStmtRun.dispose();
			}
		}
		return flag;
	}

	private Table getLinkedCashDeals() throws OException {

		Table cashDeal = null;
		//int numOfCashDeal = 0;
		//int cashDealSettleDate = 0;
		try {
			int dealNumber = tran.getDealTrackingId();
			Logging.info("Getting linked cash transfer deals for stratgegy " + dealNumber);
			String sql = "SELECT at.settle_date AS settle_date, at.internal_bunit as business_unit, at.tran_num as tran_num FROM ab_tran_info ati \n " + "JOIN ab_tran at ON ati.tran_num = at.tran_num \n"
					+ "JOIN tran_info_types ti ON ati.type_id = ti.type_id  \n" + "WHERE ti.type_name = 'Strategy Num' AND ati.value = " + dealNumber + "\n"
					+ " AND at.tran_status IN ( " + EnumTranStatus.Validated.getValue() + "," + EnumTranStatus.Matured.getValue()
					+ "," + EnumTranStatus.CancelledNew.getValue() + ") AND at.current_flag = 1";

			cashDeal = runSql(sql);
		/*	if(cashDeal != null){
				numOfCashDeal = cashDeal.getRowCount();	
			}
			

			
			if (numOfCashDeal <= 0) {

				Logging.info("No Cash deals found for this strategy it can be cancelled without checking any further criteria");
				//cashDealSettleDate = -1;
				//return cashDealSettleDate;
				throw new OException ("No Cash deal linked to this strategy. It can be cancelled without any further checks");

			}*/
			

			
			//Date settleDate = cashDeal.getDate("settle_date", 0);
			/*Logging.info("Minimum Settle date on the cash deals linked to this strategy " + settleDate);
			if(settleDate == null){
				Logging.info("No Cash deals found for this strategy it will be cancelled without checking any further criteria");
				cashDealSettleDate = -1;
				return cashDealSettleDate;
			}
			cashDealSettleDate = OCalendar.parseString(settleDate.toString());*/
			

		} catch (Exception exp) {
			Logging.error(exp.getMessage());
			throw new OException(exp.getMessage());
		}
		return cashDeal;
	}

	private boolean isStrategyCancelled() throws OException {
		boolean skipChecks = false;

		Transaction strategyTran = null;
		try {
			Logging.info("Cancellation trigerred for Cash Deal, Checking the status of associated strategy");
			int linkedStrategyNum = tran.getField(STRATEGY_NUM).getValueAsInt();
			Logging.info("Associated Strategy Num# " + linkedStrategyNum);
			if (linkedStrategyNum <= 0) {
				Logging.info("No Strategy Num found on this cash deal it can be cancelled if Metal statement has not been run");
				int settleDate = tran.getField(EnumTransactionFieldId.SettleDate).getValueAsInt();
				int businessUnit = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
				String businesUnitName = context.getStaticDataFactory().getReferenceObject(BusinessUnit.class, businessUnit).getName();
				Logging.info("Business Unit on the cash deal is " + businesUnitName);
				return skipChecks = hasMetalStatementRun(businessUnit, settleDate);
			}
			strategyTran = context.getTradingFactory().retrieveTransactionByDeal(linkedStrategyNum);
			String strategyStatus = strategyTran.getTransactionStatus().getName();
			Logging.info(String.format("Status of Strategy %s is %s  ", linkedStrategyNum, strategyStatus));
			if (strategyStatusToSkip.contains(strategyTran.getTransactionStatus().getName())) {
				Logging.info("Linked strategy is in one of the following status " + strategyStatusToSkip + " Cash deal can be cancelled");
				skipChecks = true;
			}

		} finally {
			if (strategyTran != null) {
				strategyTran.dispose();
			}
		}

		return skipChecks;
	}

}
