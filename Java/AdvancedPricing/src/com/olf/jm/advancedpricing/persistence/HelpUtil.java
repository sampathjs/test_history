/*
 * File updated 05/02/2021, 17:53
 */

package com.olf.jm.advancedpricing.persistence;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-12	V1.0	lma 	- initial version
 */
/**
 * Class contains static methods or calculation logic to get the unit conversion and the match status etc.  
 * @author sma
 * @version 1.0
 */
public class HelpUtil {	
	private static final String INTERCHANGE_TABLE_NAME = "Amended Deals to Excluded";

	/**
	 * Get the conversion factor from the database table unit_conversion
	 * @param srcUnit source unit
	 * @param destUnit destination unit
	 * @param session
	 * @return conversion factor
	 */
	public static double unitConversion(int srcUnit, int destUnit, Session session) {
		double convFactor;
		if(srcUnit == destUnit) {
			convFactor = 1.0;
		} else {
			String sql = "\nSELECT src_unit_id, dest_unit_id, factor "
					+ "\nFROM unit_conversion "
					+ "\nWHERE src_unit_id = " + srcUnit
					+ " AND dest_unit_id = " + destUnit
					;
			Table conversion = session.getIOFactory().runSQL(sql);
			convFactor = conversion.getDouble("factor", 0);
			conversion.dispose();
		}
		return convFactor;
	}
	
	/**
	 * Retrieve the matchStatus in a user-table for the giving deal tracking number
	 * @param session
	 * @param dealNum deal tracking number
	 * @param userTable a user-table contains deal_num, match_status columns.
	 * @return
	 */
	public static String retrieveMatchStatus(Session session, int dealNum, String userTable) {
		Table dealTable  = session.getIOFactory().runSQL("SELECT * FROM " + userTable + " WHERE deal_num = " + dealNum);
		String matchStatus = "N";
		if(dealTable.getRowCount() > 0){
			for(int i = 0; i<dealTable.getRowCount(); i++){	
				//For dispatch deals with multiply metal types, if one metal type has been matched, return match status P/M
				if (!dealTable.getString("match_status", i).equalsIgnoreCase("N")) {
					matchStatus = dealTable.getString("match_status", i);
				}				
			}
		}
		
		return matchStatus;
	}
	
	/**
	 * Identify the match status on the current deal based on volume and the left volume: 
	 * N for Not Matched, P for Partially Matched, M for Fully Matched and E for Excluded
	 * @param volume volume on the current deal
	 * @param volumeLeft the left volume on the current deal
	 * @return N for Not Matched, P for Partially Matched, M for Fully Matched
	 * @throws Exception deal volume < volumeLeft, should not happen. 
	 * Wrongly matched, please check the linked deals and matching criteria.
	 */
	public static String identifyMatchStatus(double volume, double volumeLeft) throws Exception {
		String matchStatus = "";
		double diff = volume - volumeLeft;
		if( Math.abs(diff) < 0.000001){
			matchStatus = "N";
		} else if (diff  >= 0.000001){
			matchStatus = "P";
		} else{ //volume < volumeLeft, should not happen
			Logging.error("Wrongly matched, please check the linked deals and matching criteria.");
			throw new Exception("Wrongly matched, please check the linked deals and matching criteria.");
		}  	
		
		if(Math.abs(volumeLeft) < 0.000001) {
			matchStatus = "M";
		}
		
		return matchStatus;
	}
	
	public static Table getFxDealDetail(Session session, int dealNum) {

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
		Table dealData = session.getIOFactory().runSQL(sql);
		return dealData;
	}
	

	/**
	 * Retrieves the table for interchange with Post Process. 
	 * @param session
	 * @param clientDataTable
	 * @return may not be disposed
	 */
	public static Table getInterchangeTable (Session session, Table clientDataTable) {
		if (!clientDataTable.isValidColumn(INTERCHANGE_TABLE_NAME)) {
			clientDataTable.addColumn(INTERCHANGE_TABLE_NAME, EnumColType.Table);
			if (clientDataTable.getRowCount() == 0) {
				clientDataTable.addRow();
			}
			Table interchangeTable = setupInterchangeTable(session);
			clientDataTable.setTable(INTERCHANGE_TABLE_NAME, 0, interchangeTable);
		}
		return clientDataTable.getTable(INTERCHANGE_TABLE_NAME, 0);
	}

	private static Table setupInterchangeTable(Session session) {
		Table postProcessTable;
		postProcessTable = session.getTableFactory().createTable("Pre/Post Process Interchange table");
		postProcessTable.addColumn ("deal_tracking_id", EnumColType.Int);
		postProcessTable.addColumn ("ap_user_table_name", EnumColType.String);
		return postProcessTable;
	}
	
	/**
	 * To prevent instantiation
	 */
	public HelpUtil () {
		
	}
}
