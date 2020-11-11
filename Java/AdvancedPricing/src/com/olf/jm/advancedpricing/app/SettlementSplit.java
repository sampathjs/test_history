package com.olf.jm.advancedpricing.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.persistence.SettleSplitUtil;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import java.util.Date;

/*
 * History:
 * 2017-07-04	V1.0	sma	- initial version
 * 2018-05-14   V1.1    sma - Set match_date as event_date in settlement split table, remove block when settlement amount left <-0.01
 */
@ScriptCategory({EnumScriptCategory.Generic})
public class SettlementSplit extends AbstractGenericScript {
    
    /**
     * The Constant CONST_REPOSITORY_CONTEXT.
     */
    private static final String CONST_REPOSITORY_CONTEXT = "Util";
    
    /**
     * The Constant CONST_REPOSITORY_SUBCONTEXT.
     */
    private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
    
    
    /**
     * Settlement Split for BO invoice
     * {@inheritDoc}
     */
    public Table execute(final Context context, final EnumScriptCategory category, final ConstTable table) {
        
        Table events_table, work_table, sub_table;
        int numRows;
        long orig_event_num;
        int fx_sell_tran_num;
        
        String select_where;
        
        try {
            
            init(this.getClass().getSimpleName());
            
            Table tblLink = context.getIOFactory()
                    .runSQL("SELECT * FROM " +
                            ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName() +
                            " WHERE sell_event_num = 0");
            
            tblLink.sort("sell_deal_num, buy_deal_num", true);
            
            if (tblLink.getRowCount() <= 0) {
                //no matching, no need to update user-tables
                Logging.info(this.getClass().getName() + " no new matched advanced pricing deals found.");
            } else {
                
                for (int i = 0; i < tblLink.getRowCount(); i++) {
                    int buy_deal_num = tblLink.getInt("buy_deal_num", i);
                    int sell_deal_num = tblLink.getInt("sell_deal_num", i);
                    double match_volume = tblLink.getDouble("match_volume", i);
                    double settle_amount = tblLink.getDouble("settle_amount", i);
                    long sell_event_num = tblLink.getLong("sell_event_num", i);
                    Date match_date = tblLink.getDate("match_date", i);
                    
                    //If sell_event_num == 0 which is a new matching and needs to be start with settlement split
                    if (sell_event_num == 0) {
                        
                        SettleSplitUtil settleSplitUtil = new SettleSplitUtil(context);
                        
                        events_table = settleSplitUtil.retrieveSplitEventData(sell_deal_num);
                        events_table.sort("event_num", true);
                        int lastRowNum = events_table.getRowCount() - 1;
                        
                        
                        orig_event_num = events_table.getLong("event_num", lastRowNum);
                        fx_sell_tran_num = events_table.getInt("tran_num", lastRowNum);
                        
                        work_table = settleSplitUtil.getSplitWorkTable();
                        
                        select_where = "[In.event_num] ==" + orig_event_num;
                        work_table.select(events_table,
                                          "event_num->orig_event_num,  event_date->event_date, event_type->event_type, pymt_type->cflow_type, event_position->Qty, int_settle_id->Int Settle, ext_settle_id->Ext Settle",
                                          select_where);
                        
                        numRows = work_table.getRowCount();
                        if (numRows != 1) {
                            continue;
                        }
                        double amount = work_table.getDouble("Qty", 0);
                        double amount_left = amount - settle_amount;
                        
                        sub_table = work_table.cloneData();
                        sub_table.setDouble("Qty", 0, settle_amount);
                        
                        //Add a new row as the same original event data in the work_table
                        sub_table.addRow();
                        sub_table.copyRowData(work_table, 0, 1);
                        sub_table.setDouble("Qty", 1, amount_left);
                        
                        //Set match_date as event_date in settlement split table
                        sub_table.setDate("event_date", 0, match_date);
                        sub_table.setDate("event_date", 1, match_date);
                        
                        settleSplitUtil.splitEvent(sub_table, orig_event_num);
                        
                        sub_table.dispose();
                        
                        work_table.dispose();
                        
                        long newEventNum = settleSplitUtil.getMatchingEventNum(fx_sell_tran_num, orig_event_num);
                        settleSplitUtil.saveMatchedInfoOnEvent(newEventNum, buy_deal_num, match_volume, match_date);
                        settleSplitUtil.updateEventNumInUserTable(newEventNum, tblLink, i);
                    }
                    
                }  // Loop for each deal
                
            }
            Logging.info(this.getClass().getName() + " ended\n");
            
            return null;
        } catch (Throwable t) {
            Logging.error(t.toString());
            for (StackTraceElement ste : t.getStackTrace()) {
                Logging.error(ste.toString());
            }
            throw t;
        } finally {
            Logging.close();
        }
    }
    
    private void init(String pluginName) {
        try {
            Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Logging.info(pluginName + " started.");
    }
}
