package com.olf.jm.storageDealManagement.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;














import javax.persistence.EnumType;

import com.olf.openjvs.OException;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

public class StorageDeal {
	
	private Session session;
	
	private int dealTrackingNum;
	private int tranNum;
	
	private String reference;
	private Date startDate;
	private Date maturityDate;
	private int locationId;
	private String locationName;
	private int idxSubgroup;
	private String metalName;
	
	public StorageDeal(int storageDealTrackingNum) {
		session = Application.getInstance().getCurrentSession();		
		dealTrackingNum = storageDealTrackingNum;		
	}

	public StorageDeal(TableRow storageDeal) {
		session = Application.getInstance().getCurrentSession();		
		dealTrackingNum = storageDeal.getInt("deal_tracking_num");	
		tranNum = storageDeal.getInt("tran_num");	
		reference = storageDeal.getString("reference");
		startDate = storageDeal.getDate("start_date");
		maturityDate = storageDeal.getDate("maturity_date");
		locationId = storageDeal.getInt("location_id");
		locationName = storageDeal.getString("location_name");
		idxSubgroup = storageDeal.getInt("idx_subgroup");
		metalName = storageDeal.getString("metal_name");		
	}
	
	@Override
	public String toString() {
		StringBuffer output = new StringBuffer();
		output.append("Storage Deal { ");		
		output.append("Reference [").append(reference).append("] " );
		output.append("Start Date [").append(startDate).append("] " );
		output.append("Maturity Date [").append(maturityDate).append("] " );
		output.append("Location Id [").append(locationId).append("] " );
		output.append("Location Name [").append(locationName).append("] " );
		output.append("Idx Subgroup [").append(idxSubgroup).append("] " );
		output.append("Metal Name  [").append(metalName).append("] " );
		output.append("Deal Tracking Num  [").append(dealTrackingNum).append("] " );
		output.append("Tran Num  [").append(tranNum).append("] " );
		output.append(" }");
		return output.toString();
	}
	
	public String getMetal() {
		return metalName;
	}
	
	public String getLocation() {
		return locationName;
	}
	
	public int getDealTrackingNumber() {
		return dealTrackingNum;
	}
	
	public Transaction generateNextStoreDeal(String duration) {
		
		CalendarFactory cf = session.getCalendarFactory();
		
		SymbolicDate startSymbolicDate = cf.createSymbolicDate("0cd");
		Date newStartDate = startSymbolicDate.evaluate(maturityDate, true);
		
		SymbolicDate endSymbolicDate = cf.createSymbolicDate(duration);
		Date newEndDate = endSymbolicDate.evaluate(newStartDate, true);

		// Check to see if a deal exists if so return it else create a new one
		String sql = DbHelper.buildSqlCommStoreAfterDate(session, locationName, metalName, maturityDate);
		Table deals = DbHelper.runSql(session, sql);
		
		// Check whether we have any unlinked deals, if so, the start date needs to mirror the earliest unlinked element
		newStartDate = getUnlinkedEarliestStartDate(newStartDate);
					
		if(deals.getRowCount() == 0) {
			// no deals create a new one
			return bookFollowOnDeal(newStartDate, newEndDate);
		} 
		
		return validateExistingDeals(deals, newStartDate, newEndDate);

	}
	
	private Transaction bookFollowOnDeal(Date newStartDate, Date newEndDate) {
		
		TradingFactory tf  = session.getTradingFactory();
		
		PluginLog.info("Creatint new storage deal for loaction " + locationName + " metal " + metalName + " start " + newStartDate + " end " + newEndDate);
		
		Transaction newTran;
		try {
			
			//newTran = tf.retrieveTransactionByDeal(dealTrackingNum).clone();
			newTran = tf.createTransactionFromTemplate(tranNum);	
			
			// Set the start and end dates the first physical leg
			for (Leg leg : newTran.getLegs()) {
				if (leg.isPhysicalCommodity()) {
					leg.setValue(EnumLegFieldId.StartDate, newStartDate);
					leg.setValue(EnumLegFieldId.MaturityDate, newEndDate);
					break;
				}
			}
			
			newTran.process(EnumTranStatus.Validated);
			PluginLog.info("Created new storage deal " + newTran.getDealTrackingId());
			
			return newTran;
		} catch (Exception e) {
			String errorMessage = "Error creating new storage deal. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);				
		}
	}
	
	private Transaction validateExistingDeals(Table deals, Date newStartDate, Date newEndDate) {
		// If one than 1 entry error as there should only be the current month and the
		// next month.
		if(deals.getRowCount() > 1) {
			String errorMessage = "Error creating new storage deal. Expecting 0 or 1 deals but found " 
					+ deals.getRowCount() + " for metal " + metalName + " location " + locationName 
					+ " maturity date " + maturityDate;
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		Date loadedStartDate = deals.getDate("start_date",0);
		Date loadedMaturityDate = deals.getDate("maturity_date",0);
		
		//if(loadedStartDate.compareTo(newStartDate) != 0 || loadedMaturityDate.compareTo(newEndDate) != 0) {
		if (loadedMaturityDate.compareTo(newEndDate) != 0) {
			String errorMessage = "Error creating new storage deal. Deal found but start / end dates not alligned. " 
					+ " expected start date " + newStartDate + " found " + loadedStartDate 
					+ " expected maturity date " + newEndDate + " found " + loadedMaturityDate;
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		int dealTrackingNum = deals.getInt("deal_tracking_num", 0);
		PluginLog.info("Using existing storage deal " + dealTrackingNum);
		
		TradingFactory tf = session.getTradingFactory();
		Transaction tranaction = tf.retrieveTransactionByDeal(dealTrackingNum);
		
		return tranaction;
	}
	
	public List<Inventory> getLinkedReceiptBatches() {
		
		String sql =  DbHelper.getLinkedReceiptBatchesSql(dealTrackingNum);
		
//				" SELECT distinct csdc_i.delivery_id as delivery_id, csh_i.location_id as location_id \n" 
//					+ " FROM comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i, ab_tran ab_i \n" 
//					+ " WHERE  csh_i.delivery_id = csdc_i.delivery_id \n"
//					+ " AND csh_i.bav_flag = 1 \n" 
//					+ " AND csh_i.total_quantity > 1.0 \n"
//					+ " AND csdc_i.batch_id > 0 \n"
//					+ " AND ab_i.ins_num = csh_i.ins_num\n"
//					+ " AND ab_i.deal_tracking_num = " + dealTrackingNum + "\n"
//					+ " AND ab_i.current_flag = 1\n"
//					+ " AND ab_i.ins_sub_type = 9204\n"
//					+ " AND 0=(select count (*) \n"
//					+ "        from comm_sched_deliv_deal csdd2 \n"
//					+ "        where csdd2.delivery_id = csdc_i.delivery_id \n"
//					+ "        and deal_num <> 6)\n"
//					+ " AND 1=(select count (*) "
//					+ "		   from comm_sched_deliv_deal csdd3 "
//					+ "        where csdc_i.source_delivery_id <> csdc_i.delivery_id "
//					+ "        and csdc_i.source_delivery_id <> 0 "
//					+ "        and csdd3.delivery_id = csdc_i.source_delivery_id "
//					+ "        and deal_num <> 6)";
		
		
		Table linkedReceiptBatches = DbHelper.runSql(session, sql);
		
		List<Inventory> batches = new ArrayList<Inventory>();
		
		int deliveryIdColNum = linkedReceiptBatches.getColumnId("delivery_id");
		int locationIdColNum = linkedReceiptBatches.getColumnId("location_id");
		int batchIdColNum = linkedReceiptBatches.getColumnId("batch_id");
		for(int rowId = 0; rowId < linkedReceiptBatches.getRowCount(); rowId++) {
			batches.add( new Inventory(linkedReceiptBatches.getInt(deliveryIdColNum, rowId),
					linkedReceiptBatches.getInt(locationIdColNum, rowId),
					linkedReceiptBatches.getInt(batchIdColNum, rowId)));	
		}

		return batches;
		
	}
	
	public List<Inventory> getUnLinkedReceiptBatches() {
		/*
		 */
		 String sql = DbHelper.getUnLinkedReceiptBatchesSql(dealTrackingNum);
//				  " select distinct csdc_i.source_delivery_id as delivery_id, csh_i.location_id as location_id \n"
//		 		+ " from comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i , ab_tran ab_i \n"
//		 		+ " where csh_i.delivery_id = csdc_i.delivery_id \n"
//		 		+ " and csh_i.bav_flag = 1 \n"
//		 		+ " and csh_i.total_quantity > 1.0 \n"
//		 		+ " and csdc_i.batch_id > 0 \n"
//		 		+ " and ab_i.ins_num = csh_i.ins_num \n"
//		 		+ " and ab_i.current_flag = 1 \n"
//		 		+ " and ab_i.tran_status in (3) \n"
//		 		+ " and ab_i.deal_tracking_num = " + dealTrackingNum + " \n" 
//		 		+ " and ab_i.ins_sub_type = 9204 \n"
//		 		+ " and 0=(select count (*) \n"
//		 		+ "        from comm_sched_deliv_deal csdd2 \n"
//		 		+ "        where csdd2.delivery_id = csdc_i.delivery_id \n"
//		 		+ "        and deal_num <> 6) \n"
//		 		+ " and 0=(select count (*) \n"
//		 		+ "        from comm_sched_deliv_deal csdd3 \n"
//		 		+ "        where csdc_i.source_delivery_id <> csdc_i.delivery_id \n"
//		 		+ "        and csdc_i.source_delivery_id <> 0 \n"
//		 		+ "        and csdd3.delivery_id = csdc_i.source_delivery_id \n"
//		 		+ "        and deal_num <> 6)";

			Table unlinkedReceiptBatches = DbHelper.runSql(session, sql);
			
			List<Inventory> batches = new ArrayList<Inventory>();

			int deliveryIdColNum = unlinkedReceiptBatches.getColumnId("delivery_id");
			int locationIdColNum = unlinkedReceiptBatches.getColumnId("location_id");
			int batchIdColNum = unlinkedReceiptBatches.getColumnId("batch_id");
			for(int rowId = 0; rowId < unlinkedReceiptBatches.getRowCount(); rowId++) {
				batches.add( new Inventory(unlinkedReceiptBatches.getInt(deliveryIdColNum, rowId),
						unlinkedReceiptBatches.getInt(locationIdColNum, rowId),
						unlinkedReceiptBatches.getInt(batchIdColNum, rowId)));
			}
			return batches;
		
	}
	
	
	/**
	 * @description Checks the earliest movement date on any of the unlinked deals
	 * @param 		newStartDate  
	 * @return		newStartDate;
	 */
	private Date getUnlinkedEarliestStartDate(Date newStartDate) {

        String sql 	= " select distinct min(csdc_i.movement_date) AS start_date \n"
		 			+ " from comm_sched_delivery_cmotion csdc_i, comm_schedule_header csh_i , ab_tran ab_i \n"
			 		+ " where csh_i.delivery_id = csdc_i.delivery_id \n"
			 		+ " and csh_i.bav_flag = 1 \n"
			 		+ " and csh_i.total_quantity > 1.0 \n"
			 		+ " and csdc_i.batch_id > 0 \n"
			 		+ " and ab_i.ins_num = csh_i.ins_num \n"
			 		+ " and ab_i.current_flag = 1 \n"
			 		+ " and ab_i.tran_status in (3) \n"
			 		+ " and ab_i.deal_tracking_num = " + dealTrackingNum + " \n" 
			 		+ " and ab_i.ins_sub_type = 9204 \n"
			 		+ " and 0=(select count (*) \n"
			 		+ "        from comm_sched_deliv_deal csdd2 \n"
			 		+ "        where csdd2.delivery_id = csdc_i.delivery_id \n"
			 		+ "        and deal_num <> 6) \n"
			 		+ " and 0=(select count (*) \n"
			 		+ "        from comm_sched_deliv_deal csdd3 \n"
			 		+ "        where csdc_i.source_delivery_id <> csdc_i.delivery_id \n"
			 		+ "        and csdc_i.source_delivery_id <> 0 \n"
			 		+ "        and csdd3.delivery_id = csdc_i.source_delivery_id \n"
			 		+ "        and deal_num <> 6) \n"
			 		+ " order by 1 asc";

		Table startDates = DbHelper.runSql(session, sql);
		if (startDates.getRowCount() == 0) return newStartDate;
		Date startDate = startDates.getDate("start_date", 0);
		if (startDate != null && startDate.before(newStartDate) == true) newStartDate = startDate;
		startDates.dispose();
		return newStartDate;
	}
}
