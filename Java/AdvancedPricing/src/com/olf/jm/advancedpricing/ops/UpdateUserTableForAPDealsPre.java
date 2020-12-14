package com.olf.jm.advancedpricing.ops;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.model.TranInfoField;
import com.olf.jm.advancedpricing.persistence.HelpUtil;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * This Trading Pre Processing OpService checks if user amends the buy/sell
 * status on the existed FX deals and then process the deal to validated. If the
 * FX deal changed from buy to sell, set its match-status to 'E' in the
 * user-table USER_jm_ap_buy_dispatch_deals If the FX deal changed from sell to
 * buy, set its match-status to 'E' in the user-table USER_jm_ap_sell_deals No
 * need to check the buy/sell-amendment of the dispatch deals because the
 * buy/sell value cannot be changed on validated dispatched deals
 *
 * This Trading Pre Processing OpService also checks if user amends the Tran
 * Info 'Pricing Type' on the existed FX deals or dispatch deals If changed from
 * 'AP' to 'None' or 'DP', set its match-status to 'E' in the user-table
 * USER_jm_ap_buy_dispatch_deals or USER_jm_ap_sell_deals If changed to 'AP',
 * the deal will be considered in the post processing script
 * 'UpdateUserTableForAPDeals'.
 *
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class UpdateUserTableForAPDealsPre extends AbstractTradeProcessListener {

	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus,
									   PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		Transaction tran = null;
		try {
			init(this.getClass().getSimpleName());
			for (PreProcessingInfo<?> activeItem : infoArray) {
				if (activeItem.getInitialStatus() == EnumTranStatus.Validated) {
					tran = activeItem.getTransaction();
					int dealNum = tran.getDealTrackingId();

					// Get initial deal info from database
					Table dealTypeTbl = getDealType(context, dealNum);
					int initialBuySell = dealTypeTbl.getRowCount() > 0 ? dealTypeTbl.getInt("buy_sell", 0)
																	   : tran.getValueAsInt(EnumTransactionFieldId.BuySell);
					int insType = dealTypeTbl.getRowCount() > 0 ? dealTypeTbl.getInt("ins_type", 0)
																: tran.getInstrumentTypeObject().getId();
					Field targetPricingTypeField = tran.getField(TranInfoField.PRICING_TYPE.getName());
					String targetPricingType = targetPricingTypeField != null
											   ? targetPricingTypeField.getValueAsString() : "";
					String initialPricingType = dealTypeTbl.getRowCount() > 0 ? dealTypeTbl.getString("pricing_type", 0)
																			  : targetPricingType;

					String userTblToUpdate;
					if (insType == EnumInsType.CommPhysical.getValue()) {
						userTblToUpdate = ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName();
						// No need to check the buy/sell-amendment of the
						// dispatch deals because the buy/sell value cannot be
						// changed on validated dispatched deals
					} else if (insType == EnumInsType.FxInstrument.getValue()) {
						if (initialBuySell == EnumBuySell.Sell.getValue()) {
							userTblToUpdate = ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName();
						} else if (initialBuySell == EnumBuySell.Buy.getValue()) {
							userTblToUpdate = ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName();
						} else {
							throw new Exception("Please check the Buy/Sell Enum, a deal can only be buy or sell.");
						}
					} else {
						throw new Exception("The operation service is not implemented for the instrument.");
					}

					/*
					 * Once a deal has been matched it can't be modified! <br>
					 * This PreProcessing OpService will block modifications if
					 * its match-status is "M".
					 *
					 */
					String matchStatus = HelpUtil.retrieveMatchStatus(context, dealNum, userTblToUpdate);
					if (matchStatus.equalsIgnoreCase("M")) {
						return PreProcessResult.failed(
								"Amendments blocked as the deal has been matched! Please cancel the deal and rebook it.");
					}

					if (matchStatus.equalsIgnoreCase("P")) {
						return PreProcessResult.failed(
								"Amendments blocked as the deal has been partially matched! Please cancel the deal and rebook it.");
					}

					Table interchangeTable = HelpUtil.getInterchangeTable(context, clientData);

					boolean setMatchStatusToE = false;
					boolean changedPricingType = false;
					boolean changedBuySell = false;

					/*
					 *
					 * Checks if user amends the Tran Info 'Pricing Type' on the
					 * existing FX deals or dispatch deals If changed from 'AP'
					 * to 'None' or 'DP', set its match-status to 'E' in the
					 * user-table USER_jm_ap_buy_dispatch_deals or
					 * USER_jm_ap_sell_deals If changed to 'AP', the deal will
					 * be considered in the post processing script
					 * 'UpdateUserTableForAPDeals'.
					 *
					 */
					if (!targetPricingType.equalsIgnoreCase("AP")) {
						if ("AP".equalsIgnoreCase(initialPricingType)) {
							setMatchStatusToE = true;
							changedPricingType = true;
						}
					}

					/*
					 * Checks if user amends the buy/sell status on the existed
					 * FX deals and then process the deal to validated. If the
					 * FX deal changed from buy to sell, set its match-status to
					 * 'E' in the user-table USER_jm_ap_buy_dispatch_deals If
					 * the FX deal changed from sell to buy, set its
					 * match-status to 'E' in the user-table
					 * USER_jm_ap_sell_deals No need to check the
					 * buy/sell-amendment of the dispatch deals because the
					 * buy/sell value cannot be changed on validated dispatched
					 * deals
					 */
					if (insType == EnumInsType.CommPhysical.getValue()) {
						if (targetPricingType.equalsIgnoreCase("DP")) {
							setMatchStatusToE = true;
						}
					} else if (insType == EnumInsType.FxInstrument.getValue()) {
						int targetBuySell = tran.getField(EnumTransactionFieldId.BuySell.getValue()).getValueAsInt();
						if (targetBuySell != initialBuySell) {
							setMatchStatusToE = true;
							changedBuySell = true;
						}

					} else {
						throw new Exception("The operation service is not implemented for the instrument.");
					}

					if (setMatchStatusToE) {
						int row = interchangeTable.addRows(1);
						interchangeTable.setInt("deal_tracking_id", row, dealNum);
						interchangeTable.setString("ap_user_table_name", row, userTblToUpdate);

						String warnMsg = "";
						if (changedPricingType) {
							warnMsg = warnMsg + "The Pricing Type changed. ";
						}
						if (changedBuySell) {
							warnMsg = warnMsg + "The Buy/Sell Field changed. ";
						}
						Logging.warn(warnMsg + "The Advanced/Deferred Pricing deal " + dealNum
									 + " will be excluded in the user table " + userTblToUpdate);
					}
				}
			}
			Logging.info(this.getClass().getName() + " ended\n");

		} catch (Exception e) {
			String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
										  null != tran ? tran.getTransactionId() : -888, this.getClass().getSimpleName(),
										  e.getLocalizedMessage());
			Logging.error(reason);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			return PreProcessResult.failed(reason);
		} finally {
			Logging.close();
		}

		return PreProcessResult.succeeded();
	}

	private void init(String pluginName) {
		try {
			Logging.init(this.getClass(), "Util", "Advanced Pricing Updater");
			Logging.info(pluginName + " started");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Table getDealType(Session session, int dealNum) {
		String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.buy_sell, ab.ins_type, ab.tran_status, abtiv.value pricing_type "
					 + "\nFROM ab_tran ab" + "\n LEFT OUTER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num "
					 + "\n AND abtiv.type_name = '" + TranInfoField.PRICING_TYPE.getName() + "'" + "\nWHERE "
					 + "\nab.deal_tracking_num =" + dealNum + "\n AND ab.current_flag = 1";
		return session.getIOFactory().runSQL(sql);
	}
}
