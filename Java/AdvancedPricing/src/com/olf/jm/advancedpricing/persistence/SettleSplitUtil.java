package com.olf.jm.advancedpricing.persistence;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.jm.advancedpricing.model.EventInfoField;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumDealEventType;
import com.openlink.util.logging.PluginLog;
 
 public class SettleSplitUtil {
     
     private Context currentContext;
     
     public SettleSplitUtil(Context context) {
           currentContext = context;
     }

//     public SettleSplitUtil(Session session) {
//    	 currentContext = session.getConnexFactory().
//	}
     
     public Table retrieveSplitEventData(int sell_deal_num) {
 		String sql ="\nSELECT ate.tran_num, atsv.event_position, atsv.delivery_type, atsv.event_date, ate.event_type, ate.pymt_type,"
 					+ "atsv.event_num, atsv.int_settle_id, atsv.ext_settle_id, atsv.settle_currency "
 					+ "\nFROM ab_tran_event ate "
 					+ "\nJOIN ab_tran at ON at.tran_num = ate.tran_num AND at.current_flag  = 1 AND at.tran_status = 3 "
 					+ "\nJOIN ab_tran_settle_view atsv "
 					+ "\nON ate.tran_num = atsv.tran_num AND ate.event_num = atsv.event_num " 
// 					+ "\nLEFT OUTER JOIN ab_tran_event_info atei "
// 					+ "\nON ate.event_num = atei.event_num AND atei.type_id = " 
// 					+ getEventInfoTypeId(EventInfoField.MATCHED_DEAL_NUM.getName())
 					+ "\nWHERE "
 					+ "\natsv.deal_tracking_num  = " + sell_deal_num 
 					+ "\n AND ate.event_type =  " +  EnumDealEventType.CashSettle.getValue() //Cash Settlement 14 
 					+ "\n AND ate.unit = 0 " // Unit = 'Currency'
 					+ "\n AND atsv.nostro_flag != 2 ";
// 					+ "AND atei.value is null";
 			
 		Table events_table = currentContext.getIOFactory().runSQL(sql);
 		return events_table;
     }

	public long getMatchingEventNum(int fx_sell_tran_num, long orig_event_num) {
		long matchedEventNum; 
		// get new event number for this matching
        String sql = "\nSELECT event_num "
					+ "\nFROM ab_tran_event "
					+ "\nWHERE "
					+ "tran_num = " + fx_sell_tran_num + " AND unit = 0  AND event_type = "
					+  EnumDealEventType.CashSettle.getValue() + " AND event_xref_num = " + orig_event_num;
        Table events_table= currentContext.getIOFactory().runSQL(sql);              
        events_table.sort("event_num", true);
        matchedEventNum = events_table.getLong(0, 0);
        events_table.dispose();
		return matchedEventNum;
	}

	public Table getSplitWorkTable() {
           com.olf.openjvs.Table  workTable = null;
           
       try {
           workTable = com.olf.openjvs.Table.tableNew();
                 Ref.settleSPLITInitTable(workTable);
           } catch (OException e) {
                 // TODO error handling
                 e.printStackTrace();
           }
       
       return currentContext.getTableFactory().fromOpenJvs(workTable);
     }
     
     public void splitEvent(Table subTable, long origEventNum) { 
           try {
                 com.olf.openjvs.Table  workTable = currentContext.getTableFactory().toOpenJvs(subTable);
                 int retval = Ref.settleSPLIT(origEventNum, workTable);
           } catch (OException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
           }
     }

	public Table loadEventInfo(long event_num) {
		 com.olf.openjvs.Table eventInfoTbl = null;

		 try {
		     eventInfoTbl =  com.olf.openjvs.Transaction.loadEventInfo(event_num);
       } catch (OException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
       }
		 return currentContext.getTableFactory().fromOpenJvs(eventInfoTbl);
	}
	
	public void saveMatchedDealNumOnEvent(long event_num, int matchedDealNum) {       	
		Table eventInfoTbl;
        eventInfoTbl = loadEventInfo(event_num);
        setEventInfo(eventInfoTbl, EventInfoField.MATCHED_DEAL_NUM.getName(), matchedDealNum+"");        
        int retval = saveEventInfo(event_num , eventInfoTbl);
    }
	
	public void saveMatchedInfoOnEvent(long event_num, int matchedDealNum, double MatchedPosition, Date matchDate) {       	
		Table eventInfoTbl;
        eventInfoTbl = loadEventInfo(event_num);
        setEventInfo(eventInfoTbl, EventInfoField.MATCHED_DEAL_NUM.getName(), matchedDealNum+"");
        DecimalFormat df3 = new DecimalFormat("###.###");
		double roundedMatchedPosition = Double.valueOf(df3.format(MatchedPosition));
        setEventInfo(eventInfoTbl, EventInfoField.MATCHED_POSITION.getName(), roundedMatchedPosition+"");
        
		SimpleDateFormat format1 = new SimpleDateFormat("dd-MMM-yyyy");
		String MetalValueDate = format1.format(matchDate);
        setEventInfo(eventInfoTbl, EventInfoField.METAL_VALUE_DATE.getName(), MetalValueDate);
        
        int retval = saveEventInfo(event_num , eventInfoTbl);
    }
	
    private void setEventInfo(Table eventInfoTbl, String eventInfoName, String value) {
    	int row = eventInfoTbl.find(eventInfoTbl.getColumnId("type_name"), eventInfoName, 0);
    			//unsortedFindString("type_name", eventInfoName,SEARCH_CASE_ENUM.CASE_SENSITIVE);
    	if(row<=0){
    		eventInfoTbl.addRow();
    		eventInfoTbl.setInt("type_id", eventInfoTbl.getRowCount()-1, getEventInfoTypeId(eventInfoName));
    		eventInfoTbl.setString("type_name", eventInfoTbl.getRowCount()-1,eventInfoName);
    		eventInfoTbl.setString("value", eventInfoTbl.getRowCount()-1, value);
    		eventInfoTbl.setInt("data_type", eventInfoTbl.getRowCount()-1,0);
        } else {
        	eventInfoTbl.setString("value", row, value);
        }		
	}

	public int saveEventInfo(long event_num, Table eventInfoTbl) {
		int retval = 0;
		try {
			com.olf.openjvs.Table  workTable = currentContext.getTableFactory().toOpenJvs(eventInfoTbl);
			retval = com.olf.openjvs.Transaction.saveEventInfo(event_num, workTable);
		} catch (OException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
      }
		return retval;
	}
	
	public int getEventInfoTypeId(String eventInfoName) {
		String sql = "\nSELECT type_id "
					+ "\nFROM tran_event_info_types "
					+ "\nWHERE "
					+ "\n type_name = '" + eventInfoName + "'"; 
			//AND LEFTER OUTER JOIN with event info 

		    Table eventInfoTypeTbl = currentContext.getIOFactory().runSQL(sql);
			int typeId = eventInfoTypeTbl.getInt("type_id", 0);
		return typeId;
	}
	
    /**
     * Insert sell_event_num in user table USER_jm_ap_buy_sell_link
     * @param event_num
     * @param tblLink
     * @throws OException 
     */
    public void updateEventNumInUserTable(long event_num, Table tblLink, int dealRowNum) {
		UserTable userTableLink = currentContext.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName());
		tblLink.setLong("sell_event_num", dealRowNum, event_num); 
		tblLink.setDate("last_update",dealRowNum, currentContext.getServerTime());
		userTableLink.updateRows(tblLink, "buy_deal_num, sell_deal_num");		
	}
    
    /**
     * Insert sell_event_num in user table USER_jm_ap_buy_sell_link
     * @param event_num
     * @param tblLink
     * @throws OException 
     */
    public void addReverseEventRowInUserTable(long matched_event_num, long reverse_event_num) {
		UserTable userTableLink = currentContext.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName());
		Table eventData  = currentContext.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName() 
				+ " WHERE sell_event_num = " + matched_event_num);
		eventData.setLong("sell_event_num", 0, reverse_event_num); 
		double settleAmount = eventData.getDouble("settle_amount", 0);
		eventData.setDouble("settle_amount", 0, settleAmount*(-1)); 
		eventData.setInt("invoice_doc_num", 0, 0); 
		eventData.setString("invoice_status", 0, ""); 
		eventData.setDate("last_update",0 , currentContext.getServerTime());
		userTableLink.insertRows(eventData);
	}
}