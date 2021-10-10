package com.olf.jm.util;

import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transactions;

public class SimUtil {

	public SimUtil() {
	}

	public ConstTable getTranResults(RevalResults revalResults, ResultType resultType) throws OpenRiskException {
		ConstTable tranResult = revalResults.getTransactionResult(resultType).getConstTable();
		validateResult(resultType, tranResult);
		return tranResult;
	}

	public ConstTable getGenResults(RevalResults revalResults, ResultType resultType) throws OpenRiskException {
		ConstTable genResult = revalResults.getGeneralResult(resultType).getConstTable();
		validateResult(resultType, genResult);
		return genResult;
	}

	public ConstTable getGenResults(RevalResults revalResults, ResultType resultType, int instrumentType, int discountIndex,
			int projectionIndex) throws OpenRiskException {
		ConstTable genResult = revalResults.getGeneralResultTable(resultType, instrumentType, discountIndex, projectionIndex);
		validateResult(resultType, genResult);
		return genResult;
	}
	
	private void validateResult(ResultType resultType, ConstTable result) throws OpenRiskException {
		if (result == null || result.getRowCount() == 0) {
			throw new OpenRiskException("Failed to get sim results for " + resultType.getName());
		}
	}
	
	public Table getTranList(Transactions transactions, EnumTransactionFieldId[] fields) {

		Table tranList = transactions.asTable(fields);
		renameColumns(tranList);
		return tranList;
	}

	private void renameColumns(Table table) {

		renameColumn(table, EnumTransactionFieldId.DealTrackingId.getName(), "deal_num");
		renameColumn(table, EnumTransactionFieldId.TransactionId.getName(), "tran_num");
		renameColumn(table, EnumTransactionFieldId.InstrumentId.getName(), "ins_num");
		renameColumn(table, EnumTransactionFieldId.TransactionGroup.getName(), "tran_group");
		renameColumn(table, EnumTransactionFieldId.BuySell.getName(), "buy_sell");
		renameColumn(table, EnumTransactionFieldId.TransactionType.getName(), "tran_type");
		renameColumn(table, EnumTransactionFieldId.Toolset.getName(), "toolset");
		renameColumn(table, EnumTransactionFieldId.InstrumentType.getName(), "ins_type");
		renameColumn(table, EnumTransactionFieldId.InstrumentSubType.getName(), "ins_sub_type");
		renameColumn(table, EnumTransactionFieldId.InternalBusinessUnit.getName(), "internal_bunit");
		renameColumn(table, EnumTransactionFieldId.InternalLegalEntity.getName(), "internal_lentity");
		renameColumn(table, EnumTransactionFieldId.InternalPortfolio.getName(), "portfolio_id");
		renameColumn(table, EnumTransactionFieldId.ExternalBusinessUnit.getName(), "external_bunit");
		renameColumn(table, EnumTransactionFieldId.ExternalLegalEntity.getName(), "external_lentity");
		renameColumn(table, EnumTransactionFieldId.DealIndexGroup.getName(), "idx_group");
		renameColumn(table, EnumTransactionFieldId.DealIndexSubGroup.getName(), "idx_subgroup");
		renameColumn(table, EnumTransactionFieldId.PartyAgreement.getName(), "party_agreement_id");
		renameColumn(table, EnumTransactionFieldId.MasterNettingAgreement.getName(), "master_netting_agreement");
		renameColumn(table, EnumTransactionFieldId.SettleDate.getName(), "settle_date");
		renameColumn(table, EnumTransactionFieldId.SettleCurrency.getName(), "settle_ccy");
		renameColumn(table, EnumTransactionFieldId.Ticker.getName(), "ticker");
	}

	private void renameColumn(Table table, String fromName, String toNmae) {

		if (table.isValidColumn(fromName)) {
			table.setColumnName(table.getColumnId(fromName), toNmae);
		}
	}
}