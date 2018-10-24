package com.olf.recon.rb.datasource.endurmetalledgerextract;

import java.math.BigDecimal;

import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.recon.rb.datasource.EndurExtract;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.Util;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class EndurMetalLedgerExtract extends EndurExtract {

	@Override
	protected void setOutputFormat(Table output) throws OException {
		output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("ins_type", COL_TYPE_ENUM.COL_INT);
		output.addCol("tran_status", COL_TYPE_ENUM.COL_INT);
		output.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		output.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT);
		output.addCol("internal_lentity_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("counterparty", COL_TYPE_ENUM.COL_INT);
		output.addCol("counterparty_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("trade_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("trade_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("value_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("value_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("metal", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("position_metal_unit", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("reconciliation_note", COL_TYPE_ENUM.COL_STRING);
		output.addCol("location", COL_TYPE_ENUM.COL_STRING);
	}

	@Override
	protected Table generateOutput(Table output) throws OException {
		Table tblCommodity = com.olf.openjvs.Util.NULL_TABLE;
		Table tblCash = com.olf.openjvs.Util.NULL_TABLE;
		Table tblLoanDep = com.olf.openjvs.Util.NULL_TABLE;

		try {
			Commodity commodityDeals = new Commodity(windowStartDate, windowEndDate);
			Cash cashDeals = new Cash(windowStartDate, windowEndDate);
			LoanDep loanDepDeals = new LoanDep(windowStartDate, windowEndDate);

			tblCommodity = commodityDeals.getData();
			tblCash = cashDeals.getData();
			tblLoanDep = loanDepDeals.getData();

			tblCommodity.copyRowAddAllByColName(output);
			tblCash.copyRowAddAllByColName(output);
			tblLoanDep.copyRowAddAllByColName(output);
			
			
			PluginLog.info("Enriching Supplementary Ref Data (ins type, party details)");
			enrichSupplementaryData(output);

			/* Calculate position */
			calculatePosition(output);
			

			/* Filter out rows which are not needed as per reference data param config in Report Builder */
			filterCounterparties(output, excludedCounterparties);
			filterIncludedLentites(output, includedLentites);

			/* Remove same day cancellations from output as these are not sent to JDE */
			removeSameDayCancellations(output);

			/* Remove rows that don't have a specific external bunit party info populated depending on the region etc */
			removeNonRegionalRecords(output);

			/* Add reconciliation notes */
			addReconciliationNotes(output, Constants.USER_JM_METAL_LEDGER_REC_NOTES, "deal_num", "deal_num");

			/* Apply rounding */
			round(output);
			
			return output;
			
		}
		finally
		{
			if (Table.isTableValid(tblCommodity)!=0) {
				tblCommodity.destroy();
			}
			if (Table.isTableValid(tblCash)!=0) {
				tblCash.destroy();
			}
			if (Table.isTableValid(tblLoanDep)!=0) {
				tblCash.destroy();
			}
		}

	}

	@Override
	protected void registerConversions(Table output) throws OException {
		regRefConversion(output, "ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		regRefConversion(output, "tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		regRefConversion(output, "buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
	}

	@Override
	protected void formatOutputData(Table output) throws OException {
		output.copyColFormatDate("trade_date", "trade_date_str", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		output.copyColFormatDate("value_date", "value_date_str", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		output.copyColFromRef("metal", "metal_str", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		/* Use Ref.getShortName as Ref.GetName only returns the first 30 chars - known OLF issue */ 
		int numRows = output.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int externalBunitId = output.getInt("counterparty", row);
			int internalLentityId = output.getInt("internal_lentity", row);

			output.setString("counterparty_str", row, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, externalBunitId));
			output.setString("internal_lentity_str", row, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, internalLentityId));
		}
	}

	@Override
	protected void groupOutputData(Table output) throws OException {
		output.group("deal_num");
		output.groupBy();
	}

	/**
	 * Adjust position based on buy sell flag and transaction status
	 * 
	 * @param output
	 * @throws OException
	 */
	private void calculatePosition(Table output) throws OException 
	{
		int numRows = output.getNumRows();

		for (int row = 1; row <= numRows; row++)
		{
			int buySell = output.getInt("buy_sell", row);
			int tranStatus = output.getInt("tran_status", row);
			double positionMetalUnit = Math.abs(output.getDouble("position_metal_unit", row));
			double positionToz = Math.abs(output.getDouble("position_toz", row));

			if (buySell == BUY_SELL_ENUM.BUY.toInt())
			{
				/* 
				 * If buy, metal position is positive, cash is negative
				 * If sell, metal position is negative, cash is positive
				 */
				positionMetalUnit *= -1;
				positionToz *= -1;
			}


			output.setDouble("position_metal_unit", row, positionMetalUnit);
			output.setDouble("position_toz", row, positionToz);


			/* Set all monetary fields to zero for Cancelled trades */
			if (tranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt())
			{
				output.setDouble("position_metal_unit", row, 0.0);
				output.setDouble("position_toz", row, 0.0);
			}
		}
	}

	/**
	 * Round positions as specified
	 * 
	 * @param output
	 * @throws OException
	 */
	private void round(Table output) throws OException
	{
		int numRows = output.getNumRows();

		for (int row = 1; row <= numRows; row++)
		{
			double positionToz = output.getDouble("position_toz", row);
			BigDecimal positionTozBD = Util.roundPosition(positionToz);   
			double roundedPositionToz = positionTozBD.doubleValue();
			output.setDouble("position_toz", row, roundedPositionToz);
		}
	}
}
