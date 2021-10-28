package com.olf.jm.monitorrollingtrades.ops;

import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.tracegroup.transformer.tbeans.excel.ExcelComponentParseException;

public class RollingTrade {

	private boolean isRollingTrade;
	
	/** The tran info for rolling reasons */
	public static final String ROLLING_REASON = "Rolling Reason";
	
	/** The tran info for rolling reasons */
	public static final String ROLLED_FROM = "Rolled From";

	public boolean isRollingTrade() {
		return isRollingTrade;
	}

	public void setRollingTrade(boolean isRollingTrade) {
		this.isRollingTrade = isRollingTrade;
	}

	public boolean isRolledMultipleTimes() {
		return rolledMultipleTimes;
	}

	public void setRolledMultipleTimes(boolean rolledMultipleTimes) {
		this.rolledMultipleTimes = rolledMultipleTimes;
	}

	public String getRollingReason() {
		return rollingReason;
	}

	public void setRollingReason(String rollingReason) {
		this.rollingReason = rollingReason;
	}

	public String getRolledFrom() {
		return rolledFrom;
	}

	public void setRolledFrom(String rolledFrom) {
		this.rolledFrom = rolledFrom;
	}

	public int getExtLe() {
		return extLe;
	}

	public void setExtLe(int extLe) {
		this.extLe = extLe;
	}

	public String getSettleDate() {
		return settleDate;
	}

	public void setSettleDate(String settleDate) {
		this.settleDate = settleDate;
	}

	public double getPosition() {
		return position;
	}

	public void setPosition(double position) {
		this.position = position;
	}

	public int getTranNum() {
		return tranNum;
	}

	public void setTranNum(int tranNum) {
		this.tranNum = tranNum;
	}

	private boolean rolledMultipleTimes;
	private String rollingReason;
	private String rolledFrom;
	private int extLe;
	private String settleDate;
	private double position;
	private int tranNum;
	private int cflowType;

	private static IOFactory iof;
	private static TableFactory tf;

	public RollingTrade(Session session, Transaction tran) {
		this.extLe = tran.getField(EnumTransactionFieldId.ExternalLegalEntity).getValueAsInt();
		this.settleDate = tran.getField(EnumTransactionFieldId.SettleDate).getValueAsString();
		this.position = tran.getField(EnumTransactionFieldId.Position).getValueAsDouble();
		tranNum = tran.getTransactionId();
		cflowType = tran.getField(EnumTransactionFieldId.CashflowType).getValueAsInt();
		rolledFrom = tran.getField(ROLLED_FROM).getValueAsString();
		rollingReason =  tran.getField(ROLLING_REASON).getValueAsString();
				
		iof = session.getIOFactory();
		tf = session.getTableFactory();
		getRolledTradeDetails();
	}

	private void getRolledTradeDetails() {

		Table rolledTrade = tf.createTable();
		try {

			if (cflowType == EnumCashflowType.Fx.getValue() || cflowType == EnumCashflowType.FxSpot.getValue()) {
				setRollingTrade(false);
				setRolledMultipleTimes(false);
				setRolledFrom("");
			} else {
				StringBuilder sql = new StringBuilder();
				sql.append("WITH trades AS (");
				sql.append("\nSELECT p.deal_tracking_num, p.position, p.settle_date, p.price");
				sql.append("\n 	, rt1.deal_tracking_num rt1_deal_num, rt1.tran_num rt1_tran_num, rt1.trade_date rt1_trade_date");
				sql.append("\n  , rt1.settle_date rt1_value_date");
				sql.append("\n 	, rt2.deal_tracking_num rt2_deal_num, rt2.tran_num rt2_tran_num, rt2.trade_date rt2_trade_date");
				sql.append("\n  , rt2.settle_date rt2_value_date");
				sql.append("\n 	, 1 number_of_times_rolled");
				sql.append("\n  FROM ab_tran p");
				sql.append("\n  JOIN ab_tran rt1 ON p.external_bunit = rt1.external_bunit AND p.position = rt1.position*-1");
				sql.append("\n       AND p.currency = rt1.currency AND p.settle_date = rt1.settle_date ");
				sql.append("\n 		 AND rt1.cflow_type IN (").append(EnumCashflowType.FxSwap.getValue()).append(", ");
				sql.append(EnumCashflowType.FxLocationSwap.getValue()).append(", ").append(EnumCashflowType.FxQualitySwap.getValue());
				sql.append(")\n      AND rt1.ins_sub_type = ").append(EnumInsSub.FxNearLeg.getValue());
				sql.append("\n  LEFT JOIN ab_tran rt2 ON rt1.tran_group = rt2.tran_group ");
				sql.append("\n       AND rt2.ins_sub_type = ").append(EnumInsSub.FxFarLeg.getValue());
				sql.append("\n       AND rt2.settle_date > p.settle_date");
				sql.append("\n WHERE p.tran_status IN (").append(EnumTranStatus.New.getValue()).append(", ");
				sql.append(EnumTranStatus.Validated.getValue()).append(", ").append(EnumTranStatus.Matured.getValue()).append(") ");
				sql.append("AND p.current_flag = 1 AND p.tran_type = 0");
				sql.append("\n   AND rt1.tran_status IN (").append(EnumTranStatus.New.getValue()).append(", ");
				sql.append(EnumTranStatus.Validated.getValue()).append(", ").append(EnumTranStatus.Matured.getValue()).append(") ");
				sql.append("AND rt1.current_flag = 1 AND rt1.tran_type = 0");
				sql.append("\n   AND p.ins_sub_type =( CASE WHEN p.cflow_type IN (").append(EnumCashflowType.Fx.getValue()).append(",");
				sql.append(EnumCashflowType.FxSpot.getValue()).append(")");
				sql.append(" THEN ").append(EnumInsSub.FxNearLeg.getValue()).append(" ELSE ").append(EnumInsSub.FxFarLeg.getValue());
				sql.append(" END)");
				sql.append("\n   AND rt2.deal_tracking_num IS NOT NULL");
				sql.append("\n   AND rt1.external_lentity = ").append(extLe).append("\n ),");
				sql.append("\n roll(deal_tracking_num, position, settle_date, price, rt1_deal_num, rt1_tran_num, rt1_trade_date");
				sql.append("\n   , rt1_value_date, rt2_deal_num, rt2_tran_num, rt2_trade_date, rt2_value_date, number_of_times_rolled)");
				sql.append("\n AS ( SELECT deal_tracking_num, position, settle_date, price, rt1_deal_num, rt1_tran_num, rt1_trade_date");
				sql.append("\n   , rt1_value_date, rt2_deal_num, rt2_tran_num, rt2_trade_date, rt2_value_date, number_of_times_rolled ");
				sql.append("\n        FROM trades AS firstRoll WHERE rt1_deal_num IS NOT NULL");
				sql.append("\n      UNION ALL");
				sql.append("\n      SELECT nextRoll.deal_tracking_num, nextRoll.position, nextRoll.settle_date, nextRoll.price");
				sql.append("\n         , nextRoll.rt1_deal_num, nextRoll.rt1_tran_num, nextRoll.rt1_trade_date, nextRoll.rt1_value_date");
				sql.append("\n         , nextRoll.rt2_deal_num, nextRoll.rt2_tran_num, nextRoll.rt2_trade_date, nextRoll.rt2_value_date");
				sql.append("\n         , roll.number_of_times_rolled +1");
				sql.append("\n        FROM trades AS nextRoll");
				sql.append("\n        JOIN roll ON roll.rt2_deal_num = nextRoll.deal_tracking_num");
				sql.append("\n    )");
				sql.append("\n SELECT rt1_deal_num deal_tracking_num, max(number_of_times_rolled) number_of_times_rolled");
				sql.append("\n   FROM roll");
				sql.append("\n  WHERE rt1_tran_num = ").append(this.tranNum);
				sql.append("\n  GROUP BY rt1_deal_num");
				sql.append("\n OPTION(MAXRECURSION 100)");
				
				rolledTrade = iof.runSQL(sql.toString());
				int numOfRolledTrades = rolledTrade.getRowCount();
				setRollingTrade(numOfRolledTrades <= 0 ? false : true);
				setRolledMultipleTimes((numOfRolledTrades <= 0 || rolledTrade.getInt("number_of_times_rolled", 0) <= 1) ? false : true);
			}
		} catch (Exception e) {
			Logging.error("Query Failed to get rolled transaction:" + e.getMessage());
			throw new ExcelComponentParseException("Query Failed to get rolled transaction:" + e.getMessage());
		} finally {
			if (rolledTrade != null) {
				rolledTrade.dispose();
			}
		}
	}

}