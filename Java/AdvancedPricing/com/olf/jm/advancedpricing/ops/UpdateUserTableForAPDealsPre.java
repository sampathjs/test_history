package com.olf.jm.advancedpricing.ops;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.model.TranInfoField;
import com.olf.jm.advancedpricing.persistence.HelpUtil;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-07-12	V1.0	sma	- Initial Version
 */

/**
 * This Trading Pre Processing OpService checks if user amends the buy/sell status on the existed FX deals and then process the deal to validated.
 * If the FX deal changed from buy to sell, set its match-status to 'E' in the user-table USER_jm_ap_buy_dispatch_deals
 * If the FX deal changed from sell to buy, set its match-status to 'E' in the user-table USER_jm_ap_sell_deals
 * No need to check the buy/sell-amendment of the dispatch deals because the buy/sell value cannot be changed on validated dispatched deals
 * 
 * This Trading Pre Processing OpService also checks if user amends the Tran Info 'Pricing Type' on the existed FX deals or dispatch deals 
 * If changed from 'AP' to 'None' or 'DP', set its match-status to 'E' in the user-table USER_jm_ap_buy_dispatch_deals or USER_jm_ap_sell_deals
 * If changed to 'AP', the deal will be considered in the post processing script 'UpdateUserTableForAPDeals'.
 * 
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class UpdateUserTableForAPDealsPre extends AbstractTradeProcessListener {	

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
	
	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		Transaction tran = null;
		try {
			init (context, this.getClass().getSimpleName());
			if(targetStatus == EnumTranStatus.Validated) {
				
			
			for (PreProcessingInfo<?> activeItem : infoArray) {
				if (activeItem.getInitialStatus() == EnumTranStatus.Validated) {
				
					tran = activeItem.getTransaction();
					int dealNum = tran.getDealTrackingId();
				
				// Get initial deal info from database
				Table dealTypeTbl = getDealType(context, dealNum);
				int initialBuySell = dealTypeTbl.getInt("buy_sell", 0);
				int insType = dealTypeTbl.getInt("ins_type", 0);

				String userTblToUpdate = "";
				if(insType== EnumInsType.CommPhysical.getValue()){
					userTblToUpdate = ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName();
					//No need to check the buy/sell-amendment of the dispatch deals because the buy/sell value cannot be changed on validated dispatched deals	
				} else if (insType == EnumInsType.FxInstrument.getValue()){
					if(initialBuySell == EnumBuySell.Sell.getValue()) {
						userTblToUpdate = ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName();
					} else if(initialBuySell == EnumBuySell.Buy.getValue()) {
						userTblToUpdate = ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName();	
					} else {
						throw new Exception("Please check the Buy/Sell Enum, a deal can only be buy or sell.");
					}						
				} else {
					throw new Exception("The operation service is not implemented for the instrument.");
				}
				
				/**
				 * Once a deal has been matched it can't be modified! <br>
				 * This PreProcessing OpService will block modifications if its match-status is "M".
				 * 
				 */
				String matchStatus = HelpUtil.retrieveMatchStatus(context, dealNum, userTblToUpdate);
				if (matchStatus.equalsIgnoreCase("M")) {
					return PreProcessResult.failed("Amendments blocked as the deal has been matched! Please cancel the deal and rebook it.");
				}
				
				if (matchStatus.equalsIgnoreCase("P")) {
					return PreProcessResult.failed("Amendments blocked as the deal has been partially matched! Please cancel the deal and rebook it.");
				}				
			

				Table interchangeTable = HelpUtil.getInterchangeTable(context, clientData);

				//for (PreProcessingInfo<?> activeItem : infoArray) {
					//tran = activeItem.getTransaction();
					
						
						
						boolean setMatchStatusToE = false;
						boolean changedPricingType = false;
						boolean changedBuySell = false;
						
						 /**
						  * 
						  * Checks if user amends the Tran Info 'Pricing Type' on the existed FX deals or dispatch deals 
						  * If changed from 'AP' to 'None' or 'DP', set its match-status to 'E' in the user-table USER_jm_ap_buy_dispatch_deals or USER_jm_ap_sell_deals
						  * If changed to 'AP', the deal will be considered in the post processing script 'UpdateUserTableForAPDeals'.
						  * 
						  */
						String initialPricingType = dealTypeTbl.getString("pricing_type", 0);
						Field targetPricingType = tran.getField(TranInfoField.PRICING_TYPE.getName());
						if (targetPricingType != null && !targetPricingType.getValueAsString().equalsIgnoreCase("AP")) {
							if(initialPricingType!=null && "AP".equalsIgnoreCase(initialPricingType)){
								setMatchStatusToE = true;
								changedPricingType = true;
							}
						}
						
						/**
						 * Checks if user amends the buy/sell status on the existed FX deals and then process the deal to validated.
						 * If the FX deal changed from buy to sell, set its match-status to 'E' in the user-table USER_jm_ap_buy_dispatch_deals
						 * If the FX deal changed from sell to buy, set its match-status to 'E' in the user-table USER_jm_ap_sell_deals
						 * No need to check the buy/sell-amendment of the dispatch deals because the buy/sell value cannot be changed on validated dispatched deals
						 */
						
						if(insType== EnumInsType.CommPhysical.getValue()){
							//No need to check the buy/sell-amendment of the dispatch deals because the buy/sell value cannot be changed on validated dispatched deals	
						} else if (insType == EnumInsType.FxInstrument.getValue()){
							
							int targetBuySell = tran.getField(EnumTransactionFieldId.BuySell.getValue()).getValueAsInt();
							if(targetBuySell != initialBuySell){
								setMatchStatusToE = true;
								changedBuySell = true;
							}
								
						} else {
							throw new Exception("The operation service is not implemented for the instrument.");
						}
						
						if(setMatchStatusToE == true) {
							int row = interchangeTable.addRows(1);
							interchangeTable.setInt("deal_tracking_id", row, dealNum);
							interchangeTable.setString("ap_user_table_name", row, userTblToUpdate); 

							String warnMsg = "";
							if (changedPricingType)  {
								warnMsg = warnMsg + "The Pricing Type changed. "; 
							}
							if(changedBuySell) {
								warnMsg = warnMsg + "The Buy/Sell Field changed. ";
							}
							PluginLog.warn(warnMsg + "The Advanced Pricing deal " + dealNum + " will be exluded in the user table " + userTblToUpdate);
							
						}
						

					}
					

				}
			}
			PluginLog.info(this.getClass().getName() + " ended\n");

		} catch (Exception e) {
			String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s", null != tran ? tran.getTransactionId() : -888, this.getClass().getSimpleName(),
					e.getLocalizedMessage());
			PluginLog.error(reason);
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			return PreProcessResult.failed(reason);
		} 

		return PreProcessResult.succeeded();
	}

	/**
	 * Initial plug-in log by retrieving logging settings from constants repository.
	 * @param class1 
	 * @param context
	 */
	private void init(Session session, String pluginName)  {	
		try {
			String abOutdir = Util.getEnv("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPOSITORY_CONTEXT, 
					CONST_REPOSITORY_SUBCONTEXT);
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", pluginName + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			PluginLog.info(pluginName + " started");
		} catch (OException e) {
			PluginLog.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		}
	}
	
	private Table getDealType(Session session, int dealNum) {
	String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.buy_sell, ab.ins_type, ab.tran_status, abtiv.value pricing_type "
			+ "\nFROM ab_tran ab"
			+ "\n INNER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num "
			+ "\n AND abtiv.type_name = '"+ TranInfoField.PRICING_TYPE.getName()+"'"
			+ "\nWHERE "
			+ "\nab.deal_tracking_num =" + dealNum 
			+ "\n AND ab.current_flag = 1"; 
	Table dealData = session.getIOFactory().runSQL(sql);
	return dealData;
}
	
	

}


