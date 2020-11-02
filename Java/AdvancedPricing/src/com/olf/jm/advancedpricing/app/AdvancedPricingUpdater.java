package com.olf.jm.advancedpricing.app;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.model.TranInfoField;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-12	V1.0	lma 	- initial version
 * 2020-05-28   V1.1	jwa		- No longer match deals of mixed pricing type
 *                              - removed unused code
 */
/**
 * Class performs the matching logic:
 * For same customer Id and same metal type, loop through buy validated deals oldest first,
 * matching them to sell deals where sell deals are validated and Match status N/P, oldest first.
 * Update the user-tables for buy deals, sell deals and their relationship.
 *
 * @author sma
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AdvancedPricingUpdater extends AbstractGenericScript {

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";

	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";

	private final Map<Integer, String> dealToPricingType = new HashMap<>();

	/**
	 * For same customer Id and same metal type, loop through buy validated deals oldest first,
	 * matching them to sell deals where sell deals are validated and Match status N/P, oldest first.
	 * Update the user-tables for buy deals, sell deals and their relationship.
	 * {@inheritDoc}
	 */
	public Table execute(final Context context, final EnumScriptCategory category, final ConstTable table) {
		try {
			init (context, this.getClass().getSimpleName());


			Table oldTblBuyDeals  = context.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName());
			Table oldTblSellDeals  = context.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName());
			getPriceTypeForDeals(context, oldTblBuyDeals, oldTblSellDeals);

			if(oldTblBuyDeals.getRowCount() <= 0) {
				//no matching, no need to update user-tables
				Logging.info(this.getClass().getName() + " no new buy deals to match.");
			} else {
				TableFactory tf = context.getTableFactory();
				Table tblMetalTypes = tf.createTable();
				tblMetalTypes.selectDistinct(oldTblBuyDeals, "metal_type", "[In.deal_num] > 0");
				tblMetalTypes.sortStable("metal_type", true);
				for(int metalTypeRow = 0; metalTypeRow <= tblMetalTypes.getRowCount()-1; metalTypeRow++) {
					int metalType = tblMetalTypes.getInt("metal_type", metalTypeRow);
					Table tblCustomIds = tf.createTable();
					tblCustomIds.selectDistinct(oldTblBuyDeals, "customer_id", "[In.metal_type] == " + metalType);
					tblCustomIds.sortStable("customer_id", true);

					Table tblBuyDeals = tf.createTable();
					Table tblSellDeals = tf.createTable();
					Table tblLink = tf.createTable();

					for(int customRow = 0; customRow <= tblCustomIds.getRowCount()-1; customRow++) {
						int customId = tblCustomIds.getInt("customer_id", customRow);
						Table customDispatchDealData = tf.createTable();
						Table customFxSellDealData = tf.createTable();
						customDispatchDealData.select(oldTblBuyDeals, "*",
													  "[In.customer_id] == " + customId + " AND [In.metal_type] == " + metalType);
						customFxSellDealData.select(oldTblSellDeals, "*",
													"[In.customer_id] == " + customId  + " AND [In.metal_type] == " + metalType);

						Table customLink = tf.createTable();
						linkFXSellAndDispatch(context, customDispatchDealData, customFxSellDealData, customLink);

						if(customLink.getRowCount() > 0) {
							tblBuyDeals.selectDistinct(customLink, "buy_deal_num->deal_num", "[In.buy_deal_num] > 0");
							tblBuyDeals.select(customDispatchDealData, "*", "[In.deal_num] == [Out.deal_num]");

							tblSellDeals.selectDistinct(customLink, "sell_deal_num->deal_num", "[In.sell_deal_num] > 0");
							tblSellDeals.select(customFxSellDealData, "*", "[In.deal_num] == [Out.deal_num]");

							tblLink.select(customLink, "*", "[In.buy_deal_num] > 0");
						}

						customDispatchDealData.dispose();
						customFxSellDealData.dispose();
						customLink.dispose();
					}

					if(tblBuyDeals.getRowCount() > 0){
						UserTable userTableBuy = context.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName());
						userTableBuy.updateRows(tblBuyDeals, "deal_num, metal_type");
						userTableBuy.dispose();
					}

					if(tblSellDeals.getRowCount() > 0){
						UserTable userTableSell = context.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName());
						userTableSell.updateRows(tblSellDeals, "deal_num, metal_type");
						userTableSell.dispose();
					}

					if(tblLink.getRowCount() > 0){
						UserTable userTableLink = context.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName());
						userTableLink.insertRows(tblLink);
						userTableLink.dispose();
					}

					tblBuyDeals.dispose();
					tblSellDeals.dispose();
					tblLink.dispose();

					tblCustomIds.dispose();
				}
				tblMetalTypes.dispose();
				oldTblBuyDeals.dispose();
				oldTblSellDeals.dispose();
			}
			Logging.info(this.getClass().getName() + " ended\n");

			return null;

		} catch (Throwable t)  {
			Logging.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		}finally{
			Logging.close();
		}
	}

	private void getPriceTypeForDeals(Session session, Table oldTblBuyDeals, Table oldTblSellDeals) {
		dealToPricingType.clear();
		Set<Integer> distinctDeals = new HashSet<>();
		for (int row=oldTblBuyDeals.getRowCount()-1; row >= 0; row--) {
			int dealNum = oldTblBuyDeals.getInt("deal_num", row);
			distinctDeals.add(dealNum);
		}
		for (int row=oldTblSellDeals.getRowCount()-1; row >= 0; row--) {
			int dealNum = oldTblSellDeals.getInt("deal_num", row);
			distinctDeals.add(dealNum);
		}

		try (QueryResult qr = session.getIOFactory().createQueryResult()) {
			for (Integer dealNum : distinctDeals) {
				qr.add(dealNum);
			}
			String sql =
					"\nSELECT ab.deal_tracking_num, ati.value "
					+  "\nFROM " + qr.getDatabaseTableName() + " qr"
					+  "\n  INNER JOIN ab_tran ab"
					+  "\n    ON ab.deal_tracking_num = qr.query_result"
					+  "\n  INNER JOIN tran_info_types tit"
					+  "\n    ON tit.type_name = '" + TranInfoField.PRICING_TYPE.getName() + "'"
					+  "\n  INNER JOIN ab_tran_info ati "
					+  "\n    ON ati.tran_num = ab.tran_num"
					+  "\n      AND ati.type_id = tit.type_id"
					+  "\nWHERE qr.unique_id = " + qr.getId()
					+  "\n  AND ab.current_flag = 1"
					;
			try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
				if (sqlResult.getRowCount() == 0) {
					Logging.warn("No results for SQL " + sql);
				}
				for (int row=sqlResult.getRowCount()-1; row >= 0; row--) {
					int dealtrackingNum = sqlResult.getInt("deal_tracking_num", row);
					String pricingType = sqlResult.getString("value", row);
					dealToPricingType.put(dealtrackingNum, pricingType);
				}
			} catch (Exception ex) {
				throw new RuntimeException ("Error executing SQL " + sql);
			}
		}
	}

	/**
	 * Matching criteria buy to sell
	 */
	private void linkFXSellAndDispatch(Session session, Table dispatchData, Table sellFxDealData, Table tblLink) {
		tblLink.addColumn("buy_deal_num", EnumColType.Int);
		tblLink.addColumn("sell_deal_num", EnumColType.Int);
		tblLink.addColumn("match_volume", EnumColType.Double);
		tblLink.addColumn("match_date", EnumColType.DateTime);
		tblLink.addColumn("metal_type", EnumColType.Int);
		tblLink.addColumn("buy_ins_type", EnumColType.Int);
		tblLink.addColumn("sell_price", EnumColType.Double);
		tblLink.addColumn("settle_amount", EnumColType.Double);
		tblLink.addColumn("sell_event_num", EnumColType.Int);
		tblLink.addColumn("invoice_doc_num", EnumColType.Int);
		tblLink.addColumn("invoice_status", EnumColType.String);
		tblLink.addColumn("last_update", EnumColType.DateTime);

		Date businessDate = session.getBusinessDate();

		dispatchData.sortStable("deal_num", true);
		for(int d = 0; d <=  dispatchData.getRowCount()-1; d++) {
			if("M".equalsIgnoreCase(dispatchData.getString("match_status", d))){
				continue;
			}
			if("E".equalsIgnoreCase(dispatchData.getString("match_status", d))){
				continue;
			}

			int dispatchDealNum = dispatchData.getInt("deal_num", d);
			int metalType = dispatchData.getInt("metal_type", d);
			String dispatchPricingType = dealToPricingType.get(dispatchDealNum);

			boolean dispatchMatched = false;

			sellFxDealData.sortStable("deal_num", true);

			for(int f = 0; f <=  sellFxDealData.getRowCount()-1; f++) {
				if("M".equalsIgnoreCase(sellFxDealData.getString("match_status", f))){
					continue;
				}
				if("E".equalsIgnoreCase(sellFxDealData.getString("match_status", f))){
					continue;
				}

				double dispatchLeftVolToz = dispatchData.getDouble("volume_left_in_toz", d);

				int sellFXDealNum = sellFxDealData.getInt("deal_num", f);
				String sellFXPricingType = dealToPricingType.get(sellFXDealNum);

				if (!sellFXPricingType.equalsIgnoreCase(dispatchPricingType)) {
					continue;
				}

				double sellFXLeftVolToz = sellFxDealData.getDouble("volume_left_in_toz", f);
				double matchLeftToz = Math.abs(dispatchLeftVolToz)-Math.abs(sellFXLeftVolToz);

				if (matchLeftToz >-0.000001 && matchLeftToz < 0.000001) {
					sellFxDealData.setDouble("volume_left_in_toz", f, 0);
					sellFxDealData.setString("match_status", f, "M");
					sellFxDealData.setDate("match_date", f, businessDate);

					dispatchData.setDouble("volume_left_in_toz", d, 0);
					dispatchData.setString("match_status", d, "M");
					dispatchData.setDate("match_date", d, businessDate);

					tblLink.addRow();
					tblLink.setInt("buy_deal_num", tblLink.getRowCount()-1, dispatchDealNum);
					tblLink.setInt("sell_deal_num", tblLink.getRowCount()-1, sellFXDealNum);
					tblLink.setDouble("match_volume", tblLink.getRowCount()-1, Math.abs(sellFXLeftVolToz));
					tblLink.setDate("match_date", tblLink.getRowCount()-1, businessDate);
					tblLink.setInt("metal_type", tblLink.getRowCount()-1, metalType);

					dispatchMatched=true;
				}
				else if (matchLeftToz <= -0.000001) {
					sellFxDealData.setDouble("volume_left_in_toz", f,  Math.abs(matchLeftToz));
					sellFxDealData.setString("match_status", f, "P");
					//	sellFxDealData.setString("match_date", f, currentDate);

					dispatchData.setDouble("volume_left_in_toz", d, 0);
					dispatchData.setString("match_status", d, "M");
					dispatchData.setDate("match_date", d, businessDate);

					tblLink.addRow();
					tblLink.setInt("buy_deal_num", tblLink.getRowCount()-1, dispatchDealNum);
					tblLink.setInt("sell_deal_num", tblLink.getRowCount()-1, sellFXDealNum);
					tblLink.setDouble("match_volume", tblLink.getRowCount()-1, Math.abs(dispatchLeftVolToz));
					tblLink.setDate("match_date", tblLink.getRowCount()-1, businessDate);
					tblLink.setInt("metal_type", tblLink.getRowCount()-1, metalType);

					dispatchMatched=true;
				}
				else if (matchLeftToz >= 0.000001) {
					sellFxDealData.setDouble("volume_left_in_toz", f, 0);
					sellFxDealData.setString("match_status", f, "M");
					sellFxDealData.setDate("match_date", f, businessDate);

					dispatchData.setDouble("volume_left_in_toz", d,  Math.abs(matchLeftToz));
					dispatchData.setString("match_status", d, "P");
					//dispatchData.setDate("match_date", d, businessDate);

					tblLink.addRow();
					tblLink.setInt("buy_deal_num", tblLink.getRowCount()-1, dispatchDealNum);
					tblLink.setInt("sell_deal_num", tblLink.getRowCount()-1, sellFXDealNum);
					tblLink.setDouble("match_volume", tblLink.getRowCount()-1, Math.abs(sellFXLeftVolToz));
					tblLink.setDate("match_date", tblLink.getRowCount()-1, businessDate);
					tblLink.setInt("metal_type", tblLink.getRowCount()-1, metalType);
				}

				if(tblLink.getRowCount() > 0) {
					int buyDealInsType = getBuyDealInsType(session, dispatchDealNum);
					tblLink.setInt("buy_ins_type", tblLink.getRowCount()-1, buyDealInsType);
					double sellPrice = getSellDealPrice(session, sellFXDealNum);
					tblLink.setDouble("sell_price", tblLink.getRowCount()-1, sellPrice);
					double matchedVolume = tblLink.getDouble("match_volume", tblLink.getRowCount()-1);
					double settleAmount = sellPrice * matchedVolume;
					DecimalFormat df2 = new DecimalFormat("###.##");
					double roundedSettleAmount = Double.parseDouble(df2.format(settleAmount));
					tblLink.setDouble("settle_amount", tblLink.getRowCount()-1, roundedSettleAmount);
					tblLink.setDate("last_update", tblLink.getRowCount()-1, session.getServerTime());
				}

				if(dispatchMatched){
					break; //The dispatch deal has been matched, go to next dispatch deal.
				}

			}
		}

	}

	/**
	 * Gets buy_ins_type
	 */
	private int getBuyDealInsType(Session session, int dealNum) {
		String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.buy_sell, ab.ins_type, ab.tran_status"
					 + "\nFROM ab_tran ab"
					 + "\nWHERE "
					 + "\nab.deal_tracking_num =" + dealNum
					 + "\n AND ab.current_flag = 1";
		Table dealData = session.getIOFactory().runSQL(sql);
		return dealData.getInt("ins_type", 0);
	}

	/**
	 * Gets sell_price from Tran Info Pricing Type
	 */
	private double getSellDealPrice(Session session, int dealNum) {

		//-------------------Gets sell_price from Tran Info Pricing Typ-------------------//
		//		double tradePrice = 0;
		//		String fieldName =  TranInfoField.TRADE_PRICE.getName();
		//		Transaction tran = session.getTradingFactory().retrieveTransactionByDeal(dealNum);
		//
		//		try(Field field = tran.getField(fieldName)) {
		//			if(field != null) {
		//				if(field.isApplicable()) {
		//					tradePrice = field.getValueAsDouble();
		//				} else {
		//					Logging.info("Field " + fieldName + " is not applicable.");
		//				}
		//			} else {
		//				Logging.info("Field " + fieldName + " is null");
		//			}
		//		}

		//TODO Unit is not applicable
		//		String unit = "";
		//		try(Field field = tran.getField(EnumTranfField.Unit.getName())) {
		//			if(field != null) {
		//				if(field.isApplicable()) {
		//					unit = field.getValueAsString();
		//				} else {
		//					Logging.info("Field " + EnumTranfField.Unit.getName() + " is not applicable.");
		//				}
		//			} else {
		//				Logging.info("Field " + fieldName + " is null");
		//			}
		//		}
		//
		//		double convFactor = getConversionFactor(session, "TOz", unit);

		//TODO to be tested
		//		double tradePriceTOz = tradePrice/convFactor;
		//-------------------Gets sell_price from Tran Info Trade Price and unit conversion -------------------//


		String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.buy_sell, ab.ins_type, ab.tran_status, ab.price"
					 + "\nFROM ab_tran ab"
					 + "\nWHERE "
					 + "\nab.deal_tracking_num =" + dealNum
					 + "\n AND ab.current_flag = 1 AND ab.tran_status = " + EnumTranStatus.Validated.getValue();
		Table dealData = session.getIOFactory().runSQL(sql);

		return dealData.getDouble("price", 0);

	}

	private void init(Session session, String pluginName)  {
		try {
			Logging.init(session, this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(pluginName + " started.");
	}
}
