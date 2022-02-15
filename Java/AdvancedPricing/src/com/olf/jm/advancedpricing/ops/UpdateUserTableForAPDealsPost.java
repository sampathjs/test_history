package com.olf.jm.advancedpricing.ops;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.model.TranInfoField;
import com.olf.jm.advancedpricing.persistence.HelpUtil;
import com.olf.jm.advancedpricing.persistence.SettleSplitUtil;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.scheduling.EnumVolume;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumSettleType;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-12	V1.0	sma	- Initial Version
 * 2017-10-11   V1.1    sma - Each FX deal has only one metal type which can be updated in user table with its deal number
 * 2018-03-14   V1.2    smc - add support for dispatch deals with multiple legs
 */

/**
 * This Trading Post Processing OpService will be implemented for FX Sell deals with Tran Info Pricing Type = "AP"
 * On deal validation, update the user table USER_jm_ap_sell_deals on deal validation.
 * On deal cancellation, update the user tables USER_jm_ap_sell_deals, check its relationship and update the linked buy deal in USER_jm_ap_buy_dispatch_deals.
 * 
 * This Trading Post Processing OpService will be implemented for FX Buy deals and dispatch deals with Tran Info Pricing Type = "AP"
 * On deal validation, update the user table USER_jm_ap_buy_dispatch_deals on deal validation.
 * On deal cancellation, update the user tables USER_jm_ap_buy_dispatch_deals, check its relationship and update and the linked sell deal in USER_jm_ap_sell_deals.
 *
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class UpdateUserTableForAPDealsPost extends AbstractTradeProcessListener {	

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
	
    private Session currentSession;
	
	private SettleSplitUtil settleSplitUtil;
	
	@Override
	public void postProcess(final Session session, final DealInfo<EnumTranStatus> deals,
			final boolean succeeded, final Table clientData) {
		Transaction tran = null;
		try {
			init(this.getClass().getSimpleName());
			currentSession = session;
			//Excluded the identified deals from pre-processing script
			Table interchangeTable = HelpUtil.getInterchangeTable(currentSession, clientData);
			for (int row = interchangeTable.getRowCount()-1; row >= 0; row--) {
				int dealNum = interchangeTable.getInt("deal_tracking_id", row);
				String userTblToUpdate = interchangeTable.getString("ap_user_table_name", row);		
				updateMatchStatusToE(userTblToUpdate, dealNum);
			}
			
			
			for (int tranNum : deals.getTransactionIds()) {
				tran = currentSession.getTradingFactory().retrieveTransactionById(tranNum);
				int dealNum = tran.getDealTrackingId();
				String pricingTypeVal = null;
				try (Field pricingType = tran.getField(TranInfoField.PRICING_TYPE.getName())) {
					if(pricingType != null){
						pricingTypeVal = pricingType.getValueAsString();
					}
				}

				if (!pricingTypeVal.equalsIgnoreCase("AP")){
					continue;
				}
				
				Table dealDetail = null;
				Table dealTypeTbl = getDealType(tranNum);
				int insType = dealTypeTbl.getInt("ins_type", 0);
				UserTable userTableTriggeredDeal = null;
				Table oldDealTbl = null;
				int tranStatus = dealTypeTbl.getInt("tran_status", 0); 
				
				boolean isBuyDispatch = false;
				if(insType== EnumInsType.CommPhysical.getValue()){
					dealDetail = getDispatchDealDetail(dealNum);
					if(dealDetail.getRowCount()<=0 && tranStatus == EnumTranStatus.Validated.getValue()) { // nomination check only for validated deals, no check needed for cancelled deals
						Logging.info("No nomination on the dispatch deal. ");
						continue;
					}
					userTableTriggeredDeal = currentSession.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName());
					oldDealTbl  = currentSession.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName() + " WHERE deal_num = " + dealNum);
					isBuyDispatch= true;
					
				} else if (insType == EnumInsType.FxInstrument.getValue()){
					dealDetail = getFxDealDetail(dealNum);
					int buySell = dealTypeTbl.getInt("buy_sell", 0);
					if(buySell == EnumBuySell.Sell.getValue()) {
						userTableTriggeredDeal = currentSession.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName());
						oldDealTbl  = currentSession.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName() + " WHERE deal_num = " + dealNum);
						isBuyDispatch= false;
					}else{
						userTableTriggeredDeal = currentSession.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName());
						oldDealTbl  = currentSession.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName() + " WHERE deal_num = " + dealNum);
						isBuyDispatch= true;
					}
				} else {
					throw new Exception("The operation service is not implemented for the instrument.");
				}
				
//				int customId = dealDetail.getInt("customer_id", 0);
//				String metalTypes = getMetalTypes(dealDetail);
				
				
				if(tranStatus == EnumTranStatus.Validated.getValue()) {
					Table newDealTbl= oldDealTbl.cloneStructure();

					newDealTbl.select(dealDetail, "deal_num, volume->volume_in_toz, volume->volume_left_in_toz, customer_id, metal_type", "[In.deal_num]>0");
					newDealTbl.setColumnValues("match_status", "N");
					
					if(oldDealTbl.getRowCount() <= 0){						
						userTableTriggeredDeal.insertRows(newDealTbl);
					} else {
						if(insType == EnumInsType.FxInstrument.getValue()){
							userTableTriggeredDeal.updateRows(newDealTbl, "deal_num");	//Each FX deal has only one metal type which can be updated in user table with its deal num
						}else{//CommPhysical
							userTableTriggeredDeal.updateRows(newDealTbl, "deal_num, metal_type");	
						}
					}
					newDealTbl.dispose();

				}
				dealDetail.dispose();	
				
				if(tranStatus == EnumTranStatus.Cancelled.getValue()) {					
					//Set the match status on the cancelled sell deal to E
					Table cancelledDealTbl= oldDealTbl.cloneStructure();
					cancelledDealTbl.addRow();
					cancelledDealTbl.setInt("deal_num", 0, dealNum);
					cancelledDealTbl.select(oldDealTbl, "*", "[In.deal_num] == [Out.deal_num]");
					cancelledDealTbl.setColumnValues("match_status", "E");						

					String matchStatus;
					String linkedUserTableName = "";
					String strBuyDispatchOrSell = "";

					String strLinkedDealBuyDispatchOrSell = "";
					if(isBuyDispatch){
						matchStatus = HelpUtil.retrieveMatchStatus(currentSession, dealNum, ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName());	
						linkedUserTableName = ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName();
						strBuyDispatchOrSell = "buy";
						strLinkedDealBuyDispatchOrSell = "sell";
					}else{
						matchStatus = HelpUtil.retrieveMatchStatus(currentSession, dealNum, ApUserTable.USER_TABLE_ADVANCED_PRICING_SELL_DEALS.getName());	
						linkedUserTableName = ApUserTable.USER_TABLE_ADVANCED_PRICING_BUY_DISPATCH_DEALS.getName();
						strBuyDispatchOrSell = "sell";
						strLinkedDealBuyDispatchOrSell = "buy";
					}
					
					if("N".equalsIgnoreCase(matchStatus)) {
							// The deal with status not matched, no further calculation needed
					} else { 
						//if matchStatus is M or P update the linked buy deals in user-table
						UserTable userTableLinked = currentSession.getIOFactory().getUserTable(linkedUserTableName);
						
						Table oldTblLink  = currentSession.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName() 
								+ " WHERE " + strBuyDispatchOrSell + "_deal_num = " + dealNum);
						

						String linkedDealNums = printLinkedDealNums(oldTblLink, strLinkedDealBuyDispatchOrSell);
						
						Table oldTblLinkedDeals  = currentSession.getIOFactory().runSQL("SELECT * FROM " + linkedUserTableName + " WHERE deal_num in (" + linkedDealNums + ")");
						Table linkedDealTbl= oldTblLinkedDeals.cloneStructure();
						linkedDealTbl.select(oldTblLink, strLinkedDealBuyDispatchOrSell + "_deal_num->deal_num, match_volume, metal_type", "[In." + strBuyDispatchOrSell + "_deal_num] == " + dealNum);
						
						
						linkedDealTbl.select(oldTblLinkedDeals, "*", "[In.deal_num] == [Out.deal_num] AND [In.metal_type] == [Out.metal_type]");
						linkedDealTbl.calcColumn("volume_left_in_toz", "volume_left_in_toz + match_volume");
						linkedDealTbl.removeColumn("match_volume");
						for(int i = 0; i < linkedDealTbl.getRowCount(); i++){
							double volume = Math.abs(linkedDealTbl.getDouble("volume_in_toz", i));
							double volumeLeft = Math.abs(linkedDealTbl.getDouble("volume_left_in_toz", i));
							String linkedDealMatchStatus = HelpUtil.identifyMatchStatus(volume, volumeLeft);
							linkedDealTbl.setString("match_status", i, linkedDealMatchStatus);
							linkedDealTbl.setDate("match_date", i, null);
						}
						
						userTableLinked.updateRows(linkedDealTbl, "deal_num, metal_type");	
						
						reverseSettlementOnLinkedDeal(oldTblLink, strLinkedDealBuyDispatchOrSell);
						
						oldTblLinkedDeals.dispose();
						oldTblLink.dispose();
						linkedDealTbl.dispose();	
						userTableLinked.dispose();
					}
					if(insType == EnumInsType.FxInstrument.getValue()){
						userTableTriggeredDeal.updateRows(cancelledDealTbl, "deal_num");	
					}else{//CommPhysical
						userTableTriggeredDeal.updateRows(cancelledDealTbl, "deal_num, metal_type");	
					}
					userTableTriggeredDeal.dispose();
					oldDealTbl.dispose();
					userTableTriggeredDeal.dispose();
					cancelledDealTbl.dispose();
				}
				
				tran.dispose();

				Logging.info(this.getClass().getName() + " ended\n");
			}
		} catch (Exception e) {
			String reason = String.format("PostProcess FAILED %s CAUSE:%s", null != tran ? tran.getTransactionId() : -888, this.getClass().getSimpleName(),
					e.getLocalizedMessage());
			Logging.error(reason);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
		
		}finally{
			Logging.close();
		}
		
	}
	
	private void reverseSettlementOnLinkedDeal(Table tblLink, String strLinkedDealBuyDispatchOrSell) throws Exception {
		if("sell".equals(strLinkedDealBuyDispatchOrSell)){
			for(int i = 0; i < tblLink.getRowCount(); i++) {				
//				int buy_deal_num = tblLink.getInt("buy_deal_num", i);
				double match_volume = tblLink.getDouble("match_volume", i);
				double settle_amount = tblLink.getDouble("settle_amount", i);
				long sell_event_num = tblLink.getLong("sell_event_num", i);
				Date match_date = tblLink.getDate("match_date", i);
				int sell_deal_num = tblLink.getInt("sell_deal_num", i);				
				double amount_used = settle_amount *(-1);				

				if(sell_event_num != 0){
					settleSplitUtil = new SettleSplitUtil((Context) currentSession);

					Table work_table = settleSplitUtil.getSplitWorkTable();
			            // Get all of the Cash Delivery Events

					Table events_table = settleSplitUtil.retrieveSplitEventData(sell_deal_num);
					events_table.sort("event_num", true);
					int lastRowNum = events_table.getRowCount()-1;
  				
					Long orig_event_num = events_table.getLong("event_num", lastRowNum);
					int fx_sell_tran_num = events_table.getInt("tran_num", lastRowNum);
						
						//Split the last row into 2 events (- matched_amount, rest)
					String select_where = "[In.event_num] ==" + orig_event_num;
					work_table.select(events_table,
									"event_num->orig_event_num, event_type->event_type, pymt_type->cflow_type, event_position->Qty, event_date, int_settle_id->Int Settle, ext_settle_id->Ext Settle",
									select_where);

					int numrows = work_table.getRowCount();
					if (numrows == 1) {
					

					double amount = work_table.getDouble("Qty", 0);
					double amount_left = amount - amount_used;

					// If event settlement amount == used amount, no split needed
					// If event settlement amount < used amount, ERROR!
					if (amount_left < 0.01) {
						String msg = "ERROR: Please check matched settle amount in user-table "
								+ ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK
								.name()
						+ " for sell deal "
						+ sell_deal_num
						+ ". The returned amount is not valid.";
						Logging.error(msg);					
						throw new Exception(msg);
					}	

					Table sub_table = work_table.cloneData();
					// sub_table.copyData(work_table);
					sub_table.setDouble("Qty", 0, amount_used);

					// Add a new row as the same original event data in the work_table
					sub_table.addRow();
					sub_table.copyRowData(work_table, 0, 1);
					// work_table.copyRowAdd(1, sub_table);
					sub_table.setDouble("Qty", 1, amount_left);

					settleSplitUtil.splitEvent(sub_table, orig_event_num);

					sub_table.dispose();

			        events_table.dispose();

			        work_table.dispose();

		            long newEventNum = settleSplitUtil.getMatchingEventNum(fx_sell_tran_num, orig_event_num);

					//Change event info Matched Deal Num to -1 on original matched event
	                settleSplitUtil.saveMatchedDealNumOnEvent(sell_event_num, -1); 
	                
					//Save event info on new reverse event
	                settleSplitUtil.saveMatchedInfoOnEvent(newEventNum, -1, (-1)*match_volume, match_date); //TODO check
	                
		            settleSplitUtil.addReverseEventRowInUserTable(sell_event_num, newEventNum);
				
					}
				}
			}
		}		
	}

	private Table getLinkedDeals(Table oldTblLink, String strLinkedDealBuyDispatchOrSell) {
		TableFactory tf = currentSession.getTableFactory();
		Table linkedDeals = tf.createTable();
		linkedDeals.selectDistinct(oldTblLink, strLinkedDealBuyDispatchOrSell + "_deal_num->deal_num", "[In." + strLinkedDealBuyDispatchOrSell + "_deal_num] > 0");
		return linkedDeals;
	}
	
	private String printLinkedDealNums(Table oldTblLink, String strLinkedDealBuyDispatchOrSell) {
		String linkedDealNums = "";
		Table linkedDeals = getLinkedDeals(oldTblLink, strLinkedDealBuyDispatchOrSell);
		linkedDealNums = linkedDeals.getInt("deal_num", 0)+"";
		if(linkedDeals.getRowCount() > 1) {
			for(int i = 1; i<linkedDeals.getRowCount(); i++){						
				int linkedDealNum = linkedDeals.getInt("deal_num", i);
				linkedDealNums = linkedDealNums + "," + linkedDealNum;
			}						
		}		
		linkedDeals.dispose();
		return linkedDealNums;
	}

	private Table getDealType(int tranNum) {
		String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.buy_sell, ab.ins_type, ab.tran_status"
				+ "\nFROM ab_tran ab"
				+ "\nWHERE "
				+ "\nab.tran_num =" + tranNum 
				+ "\n AND ab.current_flag = 1"; 
		Table dealData = currentSession.getIOFactory().runSQL(sql);
		return dealData;
	}

	/**
	 * Gets Information for dispatch deals: deal volume, unit etc. If the dispatch deal has not been nominated, return nothing.
	 * @param session
	 * @param dealNum
	 * @return
	 */
	private Table getDispatchDealDetail(int dealNum) {
		//Column volume shows nominated volume

		String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.external_bunit customer_id, p.ins_num, c.id_number metal_type, "
				+ "\n csh.unit, SUM(csh.total_quantity) volume"
				+ "\nFROM ab_tran ab"
				+ "\n INNER JOIN parameter p ON p.ins_num = ab.ins_num "
				+ "\n INNER JOIN idx_def idx ON p.proj_index = idx.index_id AND idx.db_status = 1  "
				+ "\n INNER JOIN idx_subgroup idxs ON idxs.id_number = idx.idx_subgroup "
				+ "\n INNER JOIN currency c ON c.name = idxs.code "
				+ "\n INNER JOIN comm_schedule_header csh ON csh.ins_num = p.ins_num AND csh.param_seq_num= p.param_seq_num "
				+ " AND csh.volume_type = " + EnumVolume.Nominated.getValue()
				+ "\nWHERE "
				+ "\nab.deal_tracking_num =" + dealNum 
				+ "\n AND ab.current_flag = 1 AND ab.tran_status = " + EnumTranStatus.Validated.getValue() 
				+ "\n AND p.settlement_type = " + EnumSettleType.Physical.getValue()
				+ "\n GROUP BY ab.deal_tracking_num, ab.external_bunit, p.ins_num, c.id_number, csh.unit"
				;
		Table dealData = currentSession.getIOFactory().runSQL(sql);
		return dealData;	
	}

	/**
	 * Gets Information for FX deals: deal volume, unit etc. 
	 * @param session
	 * @param dealNum deal tracking number
	 * @return
	 */
	private Table getFxDealDetail(int dealNum) {

		String sql = "\nSELECT ab.deal_tracking_num deal_num, ab.external_bunit customer_id, vol.metal_type, "
				+ "\nvol.volume, vol.unit "
				+ "\nFROM ab_tran ab" 
				+ "\n INNER JOIN (SELECT IIF(unit1 = 0, unit2, unit1) AS unit, "
				+ "\n		             IIF(unit1 = 0, c_amt, d_amt) AS volume, "
				+ "\n		             IIF(unit1 = 0, ccy2, ccy1) AS metal_type, "
				+ "\n		             tran_num "
				+ "\n		      FROM   fx_tran_aux_data fx) AS vol "
				+ "\n		      ON vol.tran_num = ab.tran_num "
				+ "\n WHERE "
				+ "\n ab.deal_tracking_num =" + dealNum
				+ "\n AND ab.toolset =" + EnumToolset.Fx.getValue()
				+ "\n AND ab.current_flag = 1 AND ab.tran_status = " + EnumTranStatus.Validated.getValue();
				;
		Table dealData = currentSession.getIOFactory().runSQL(sql);
		return dealData;
	} 
	
	private void updateMatchStatusToE(String userTblToUpdate, int dealNum) {
		UserTable userTableTriggeredDeal = currentSession.getIOFactory().getUserTable(userTblToUpdate);
		Table tblDealToExcluded  = currentSession.getIOFactory().runSQL("SELECT * FROM " + userTblToUpdate + " WHERE deal_num = " + dealNum);
		tblDealToExcluded.setColumnValues(tblDealToExcluded.getColumnId("match_status"), "E");
		userTableTriggeredDeal.updateRows(tblDealToExcluded, "deal_num, metal_type");
	}

	/**
	 * Initial plug-in log by retrieving logging settings from constants repository.
	 * @param class1 
	 * @param context
	 */
	private void init(String pluginName)  {	
		try {
			String abOutdir = Util.getEnv("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPOSITORY_CONTEXT, 
					CONST_REPOSITORY_SUBCONTEXT);
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", pluginName + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, 
						CONST_REPOSITORY_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			Logging.info(pluginName + " started");
		} catch (OException e) {
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
		}
	}
}
