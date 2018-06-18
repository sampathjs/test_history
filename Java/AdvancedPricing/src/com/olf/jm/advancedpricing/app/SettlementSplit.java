package com.olf.jm.advancedpricing.app;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.model.EventInfoField;
import com.olf.jm.advancedpricing.persistence.SettleSplitUtil;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumDealEventType;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.Generic })
public class SettlementSplit extends AbstractGenericScript{
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
	
	private SettleSplitUtil settleSplitUtil;

	
	/**
	 * Settlement Split for BO invoice
	 * {@inheritDoc}
	 */
	public Table execute(final Context context, final EnumScriptCategory category, final ConstTable table) {
		
		Table events_table, work_table, sub_table;
        int numrows;
        long orig_event_num;
        int fx_sell_tran_num;

        String select_where;
        
       try{
        
			init (context, this.getClass().getSimpleName());
			
			Table tblLink  = context.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName() + " WHERE sell_event_num = 0");
			
			tblLink.sort("sell_deal_num, buy_deal_num", true);

			if(tblLink.getRowCount() <= 0) {
				//no matching, no need to update user-tables
				PluginLog.info(this.getClass().getName() + " no new matched advanced pricing deals found.");
			} else {
	
				for(int i = 0; i < tblLink.getRowCount(); i++){
					int buy_deal_num = tblLink.getInt("buy_deal_num", i);
					int sell_deal_num = tblLink.getInt("sell_deal_num", i);
					double match_volume = tblLink.getDouble("match_volume", i);
					double settle_amount = tblLink.getDouble("settle_amount", i);
					long sell_event_num = tblLink.getLong("sell_event_num", i);
					Date match_date = tblLink.getDate("match_date", i);
					
					//If sell_event_num == 0 which is a new matching and needs to be start with settlement split
					if(sell_event_num == 0){

	      				settleSplitUtil = new SettleSplitUtil(context); 

						events_table = settleSplitUtil.retrieveSplitEventData(sell_deal_num);
						events_table.sort("event_num", true);
						int lastRowNum = events_table.getRowCount()-1;


      				orig_event_num = events_table.getLong("event_num", lastRowNum);
      				fx_sell_tran_num = events_table.getInt("tran_num", lastRowNum);
      				
      				work_table = settleSplitUtil.getSplitWorkTable();   				
      				
      				select_where = "[In.event_num] ==" + orig_event_num;
                    work_table.select(events_table, 
                    		"event_num->orig_event_num, event_type->event_type, pymt_type->cflow_type, event_position->Qty, event_date, int_settle_id->Int Settle, ext_settle_id->Ext Settle", select_where);                    
                    
                numrows = work_table.getRowCount();
                if (numrows != 1){
                    continue;
                }    
                
                
                double amount = work_table.getDouble("Qty", 0);
                double amount_used = settle_amount;
                double amount_left = amount - amount_used; 

                //If event settlement amount == used amount, no split needed
                //If event settlement amount < used amount, ERROR!   
                if(amount_left < -0.01) {
                	PluginLog.error("ERROR: Please check matched settle amount in user-table " 
                + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.name() + " for sell deal " + sell_deal_num 
                + ". The matched settle amount is over the deal amount.");
//                	throw new Exception("ERROR: Please check matched settle amount in user-table " 
//                + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.name() + " for sell deal " + sell_deal_num 
//                + ". The matched settle amount is over the deal amount.");
                	continue;
                }
                
//                if(amount_left >-0.01 && amount_left < 0.01){
//                	PluginLog.info("The sell deal " + sell_deal_num + " has been fully matched. No Settlement Split needed.");
//                	settleSplitUtil.saveMatchedInfoOnEvent(orig_event_num, buy_deal_num, match_volume, match_date);
//                	settleSplitUtil.updateEventNumInUserTable(orig_event_num, userTableLink, tblLink, i);
//                    continue;
//                }                    
                
                sub_table = work_table.cloneData();
//                sub_table.copyData(work_table);
                sub_table.setDouble("Qty", 0, amount_used);
                
                //Add a new row as the same original event data in the work_table
                sub_table.addRow();
                sub_table.copyRowData(work_table, 0, 1);
                sub_table.setDouble("Qty", 1, amount_left);

                settleSplitUtil.splitEvent(sub_table, orig_event_num);

                sub_table.dispose();                        

                work_table.dispose();
                
	            long newEventNum = settleSplitUtil.getMatchingEventNum(fx_sell_tran_num, orig_event_num);
	            settleSplitUtil.saveMatchedInfoOnEvent(newEventNum, buy_deal_num, match_volume, match_date);
	            settleSplitUtil.updateEventNumInUserTable(newEventNum, tblLink, i);
				}

            }  // Loop for each deal

			}
			PluginLog.info(this.getClass().getName() + " ended\n");

			return null;
       }catch (Throwable t)  {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw t;
		}
    }
    
//    /**
//     * Insert sell_event_num in user table USER_jm_ap_buy_sell_link
//     * @param event_num
//     * @param tblLink
//     * @throws OException 
//     */
//    private void updateEventNumInUserTable(Session session, long event_num, UserTable userTableLink, Table tblLink, int dealRowNum) {
//		tblLink.setLong("sell_event_num", dealRowNum, event_num); 
//		tblLink.setDate("last_update",dealRowNum, session.getServerTime());
//		userTableLink.updateRows(tblLink, "buy_deal_num, sell_deal_num");		
//	}



    
    
//    private void setEventInfo(Session session, Table eventInfoTbl, String eventInfoName, String value) {
//    	int row = eventInfoTbl.find(eventInfoTbl.getColumnId("type_name"), eventInfoName, 0);
//    			//unsortedFindString("type_name", eventInfoName,SEARCH_CASE_ENUM.CASE_SENSITIVE);
//    	if(row<=0){
//    		eventInfoTbl.addRow();
//    		eventInfoTbl.setInt("type_id", eventInfoTbl.getRowCount()-1, getEventInfoTypeId(session, eventInfoName));
//    		eventInfoTbl.setString("type_name", eventInfoTbl.getRowCount()-1,eventInfoName);
//    		eventInfoTbl.setString("value", eventInfoTbl.getRowCount()-1, value);
//    		eventInfoTbl.setInt("data_type", eventInfoTbl.getRowCount()-1,0);
//        } else {
//        	eventInfoTbl.setString("value", row, value);
//        }		
//	}
//
//	private int getEventInfoTypeId(Session session, String eventInfoName) {
//		String sql = "\nSELECT type_id "
//					+ "\nFROM tran_event_info_types "
//					+ "\nWHERE "
//					+ "\n type_name = '" + eventInfoName + "'"; 
//			//AND LEFTER OUTER JOIN with event info 
//
//		    Table eventInfoTypeTbl = session.getIOFactory().runSQL(sql);
//			int typeId = eventInfoTypeTbl.getInt("type_id", 0);
//		return typeId;
//	}
	


	
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
			PluginLog.info(pluginName + " started.");
		} catch (OException e) {
			PluginLog.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		}
	}



}
