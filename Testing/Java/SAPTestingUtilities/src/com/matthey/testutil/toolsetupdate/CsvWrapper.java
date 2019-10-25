package com.matthey.testutil.toolsetupdate;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public class CsvWrapper 
{
	public static final String OUR_UNIT_COLUMN = "our_unit";
	public static final String OUR_PFOLIO_COLUMN = "our_pfolio";
	public static final String C_E_UNIT_COLUMN = "c/e_unit";
	public static final String TRADER_COLUMN = "trader";
	public static final String BUY_SELL_COLUMN = "buy_sell";
	public static final String TRADE_DATE_COLUMN = "trade_date";
	public static final String FX_DATE_COLUMN = "fx_date";
	public static final String CFLOW_TYPE_COLUMN = "cflow_type";
	public static final String METAL_WEIGHT_COLUMN = "metal_weight";
	public static final String METAL_UOM_COLUMN = "metal_uom";
	public static final String TRADE_PRICE_COLUMN = "trade_price";
	public static final String TICKER_COLUMN = "ticker";
	public static final String DEAL_COMMENT_COLUMN = "deal_comment";
	public static final String STATUS_IN_ENDUR_COLUMN = "status_in_endur";
	public static final String MTR_NUM = "metal_transfer_request_number";
	public static final String TO_ACC_LOCO = "to_acc_loco";
	public static final String TO_ACC_FORM = "to_acc_form";
	public static final String TO_ACC = "to_acc";
	public static final String TO_ACC_BU = "to_acc_bu";
	public static final String SETTLE_DATE = "settle_date";
	public static final String DEAL_NUM_COLUMN = "deal_num";
	public static final String TEST_REFERENCE_COLUMN = "test_reference";

	private Table tblCsvData;
	
	public CsvWrapper(Table tblCsvData)
	{
		this.tblCsvData = tblCsvData;
	}
	
	public DealDelta getDealDelta(int csvRow) throws OException
	{
		DealDelta dealDelta = new DealDelta();

		String ourUnit = tblCsvData.getString(OUR_UNIT_COLUMN, csvRow);
		dealDelta.setOurUnit(ourUnit);

		String ourPfolio = tblCsvData.getString(OUR_PFOLIO_COLUMN, csvRow);
		dealDelta.setOurPfolio(ourPfolio);

		String ceUnit = tblCsvData.getString(C_E_UNIT_COLUMN, csvRow);
		dealDelta.setCeUnit(ceUnit);

		String trader = tblCsvData.getString(TRADER_COLUMN, csvRow);
		dealDelta.setTrader(trader);

		String buySell = tblCsvData.getString(BUY_SELL_COLUMN, csvRow);
		dealDelta.setBuySell(buySell);

		String tradeDate = tblCsvData.getString(TRADE_DATE_COLUMN, csvRow);
		dealDelta.setTradeDate(tradeDate);

		String fxDate = tblCsvData.getString(FX_DATE_COLUMN, csvRow);
		dealDelta.setFxDate(fxDate);

		String cflowType = tblCsvData.getString(CFLOW_TYPE_COLUMN, csvRow);
		dealDelta.setCflowType(cflowType);

		String metalWeight = tblCsvData.getString(METAL_WEIGHT_COLUMN, csvRow);
		dealDelta.setMetalWeight(metalWeight);

		String metalUom = tblCsvData.getString(METAL_UOM_COLUMN, csvRow);
		dealDelta.setMetalUom(metalUom);

		String tradePrice = tblCsvData.getString(TRADE_PRICE_COLUMN, csvRow);
		dealDelta.setTradePrice(tradePrice);

		String ticker = tblCsvData.getString(TICKER_COLUMN, csvRow);
		dealDelta.setTicker(ticker);

		String dealComment = tblCsvData.getString(DEAL_COMMENT_COLUMN, csvRow);
		dealDelta.setDealComment(dealComment);
		
		String statusInEndur = tblCsvData.getString(STATUS_IN_ENDUR_COLUMN, csvRow);
		dealDelta.setStatusInEndur(statusInEndur);

		String testReference = tblCsvData.getString(TEST_REFERENCE_COLUMN, csvRow);
		dealDelta.setTestReference(testReference);
		
		String mtrNum = tblCsvData.getString(MTR_NUM, csvRow);
		dealDelta.setMtrNum(mtrNum);
		
		String toAccLoco = tblCsvData.getString(TO_ACC_LOCO, csvRow);
		dealDelta.setToAccLoco(toAccLoco);
		
		String toAccForm = tblCsvData.getString(TO_ACC_FORM, csvRow);
		dealDelta.setToAccForm(toAccForm);
		
		String toAcc = tblCsvData.getString(TO_ACC, csvRow);
		dealDelta.setToAcc(toAcc);
		
		String toAccBu = tblCsvData.getString(TO_ACC_BU, csvRow);
		dealDelta.setToAccBu(toAccBu);
		
		String settleDate = tblCsvData.getString(SETTLE_DATE, csvRow);
		dealDelta.setSettleDate(settleDate);
		
		return dealDelta;
	}
}
