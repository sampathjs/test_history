package com.matthey.openlink.pnl;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 * 2020-03-15           fernaI01 - added code to bring back last traded metal
 */

public abstract class PnlReportPositionLimitBase extends PNL_ReportEngine {
	
	String relevantRiskDefinitions = "'Position by Metal and BU', 'Position by Metal Global'";
	public static final double EPSILON = 0.000001d;
	
	protected void generateOutputTableFormat(Table output) throws OException {
		output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		output.addCol("date", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("closing_volume", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("closing_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("closing_price", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("position_limit", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("limit_usage", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("in_breach", COL_TYPE_ENUM.COL_INT);
	}

	@Override
	protected void registerConversions(Table output) throws OException {
		regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		regRefConversion(output, "in_breach", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
		
		regDateConversion(output, "date");
	}
	
	@Override
	protected void populateOutputTable(Table output) throws OException {
		int reportCloseDate = reportDate + 1;
		Logging.info("PNL_Report_Summary::populateOutputTable called.\n");
		
		Table openPosData = m_positionHistory.getOpenPositionsForDates(reportDate, reportCloseDate);
		Table dealDetailsData = m_positionHistory.getPositionData();
		
		try {
			int dealRows = dealDetailsData.getNumRows();
			for (int row = 1; row <= dealRows; row++) {
				int bunit = dealDetailsData.getInt("bunit", row);
				int metalCcy = dealDetailsData.getInt("metal_ccy", row);
				
				// Check that either the selected Business Unit Set is empty, or contains this BUnit
				if (intBUSet.isEmpty() || intBUSet.contains(bunit)) {
					// Check this is a "Metal" as opposed to a foreign currency
					if (MTL_Position_Utilities.isPreciousMetal(metalCcy)) {
						// If all criteria match, add to output					
						output.addRow();
						int outRow = output.getNumRows();
									
						output.setInt("bunit", outRow, bunit);
						output.setInt("metal_ccy", outRow, metalCcy);
					}				
				}			
			}		
			
			output.select(openPosData, "open_volume (closing_volume), open_value (closing_value), open_price (closing_price)", 
					"bunit EQ $bunit AND metal_ccy EQ $metal_ccy AND open_date EQ " + reportCloseDate);
			
			List<COG_PNL_Grouping> missingKeys = new ArrayList<COG_PNL_Grouping>();
			List<COG_PNL_Grouping> relevantMetalAndBunitList = m_positionHistory.inititalizeAllMetalAndBUnitList();

			if (m_positionHistory.getDealHistoryMap() != null  && !m_positionHistory.getDealHistoryMap().isEmpty()) {
				for (COG_PNL_Grouping key: relevantMetalAndBunitList) {
					if (m_positionHistory.getDealHistoryMap().get(key) != null && m_positionHistory.getDealHistoryMap().get(key).isEmpty()) {
						missingKeys.add(key);
					}
				}
			}
			
			if (!missingKeys.isEmpty()) {
				Logging.info("Missing keys size :"+missingKeys.size());
				processTheMissingMetals(missingKeys, output);
			}
			
			
			
		} finally {
			if (Table.isTableValid(openPosData) == 1) {
				openPosData.destroy();	
			}
			if (Table.isTableValid(dealDetailsData) == 1) {
				dealDetailsData.destroy();	
			}
		}
		
		// Create summary rows per metal, aggregated across all BU's
		Table summaryData = output.cloneTable();
		
		try {
			summaryData.select(output, "DISTINCT, metal_ccy", "metal_ccy GT -1");
			summaryData.select(output, "SUM, closing_volume, closing_value", "metal_ccy EQ $metal_ccy");
			
			int rows = summaryData.getNumRows();
			for (int row = 1; row <= rows; row++) {
				// Set price as value \ volume by metal, unless volume is zero
				double volume = summaryData.getDouble("closing_volume", row);
				double value = summaryData.getDouble("closing_value", row);
				double price = (Math.abs(volume) > 0.0001) ? value/volume : 0.0;
				
				summaryData.setDouble("closing_price", row, price);
			}
			summaryData.copyRowAddAll(output);
			
		} finally {
			if (Table.isTableValid(summaryData) == 1) {
				summaryData.destroy();	
			}
		}
		
		// Set report dates
		output.setColValInt("date", reportDate);		
		enrichPositionLimits(output);
		
		int rows = output.getNumRows();
		for (int row = 1; row <= rows; row++) {
			double position = output.getDouble("closing_volume", row);
			double limit = output.getDouble("position_limit", row);
			double limitUsage = (Math.abs(limit) > 0.001) ? Math.abs(position / limit) : Math.abs(position);
			
			if (Math.abs(position) > Math.abs(limit)) {
				output.setInt("in_breach", row, 1);
			}
			output.setDouble("limit_usage", row, limitUsage);
		}				
	}
	
	private void enrichPositionLimits(Table output) throws OException {
		int ret = 0;
		Table tblInfoData = Util.NULL_TABLE;
		Table tblLimitData = Table.tableNew();
		
		try {
			ret = DBaseTable.execISql(tblLimitData, 
					"SELECT rsk.exp_line_id, SUM(rsk.limit) position_limit " + 
					"FROM risk_exposure_view rev, rsk_limit rsk " +
					"WHERE rev.risk_expdef_name IN (" + relevantRiskDefinitions + ") AND rev.exp_line_id = rsk.exp_line_id AND " +
					"rsk.start_date <= " + OCalendar.today() + " AND " +
					"rsk.end_date >= " + OCalendar.today() + " AND " +
					"rsk.status = 1 " +
					"GROUP BY rsk.exp_line_id");

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException("Unable to run query to select limit data");
			}   
			
			tblInfoData = Table.tableNew();
			ret = DBaseTable.execISql(tblInfoData, 
					"SELECT rc.* FROM risk_exposure_view rev, rsk_criteria rc " + 
					"WHERE rev.exp_line_id = rc.exp_line_id AND " +
						"(rev.risk_expdef_name = 'Position by Metal AND BU' OR " +
						"(rev.risk_expdef_name = 'Position by Metal Global' AND criteria_type = 12))");

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException("Unable to run query to select limit data");
			}  		
			
			tblLimitData.select(tblInfoData, "criteria_value(bunit)", "exp_line_id EQ $exp_line_id AND criteria_type EQ 1");
			tblLimitData.select(tblInfoData, "criteria_value(metal_ccy)", "exp_line_id EQ $exp_line_id AND criteria_type EQ 12");
			
			output.select(tblLimitData, "position_limit", "bunit EQ $bunit AND metal_ccy EQ $metal_ccy");
			
		} finally {
			if (Table.isTableValid(tblLimitData) == 1) {
				tblLimitData.destroy();	
			}
			if (Table.isTableValid(tblInfoData) == 1) {
				tblInfoData.destroy();	
			}
		}
	}
	private void processTheMissingMetals (List<COG_PNL_Grouping> missingKeys,Table output)throws OException {
		Table openPositionResults = Util.NULL_TABLE;
		try {

			for (COG_PNL_Grouping missingKey : missingKeys) {
				Integer bUnit= missingKey.m_bunit;
				Integer metalCcy = missingKey.m_metalCcyGroup;

				openPositionResults = m_positionHistory.populateTheOutputFromOpenTradingPosition(bUnit, metalCcy);
				if (Table.isTableValid(openPositionResults)== 0 || openPositionResults.getNumRows() < 1 ) {
					continue;
				}

				Double openPrice= openPositionResults.getDouble("open_price", 1);
				Double openValue= openPositionResults.getDouble("open_value",1);
				Double openVolume= openPositionResults.getDouble("open_volume", 1);
				
				boolean blnIsZero= Math.abs(Double.compare(openValue, BigDecimal.ZERO.doubleValue())) < EPSILON 
									&& Math.abs(Double.compare(openVolume, BigDecimal.ZERO.doubleValue())) < EPSILON;
				
				if (blnIsZero == false) {
					
					Logging.info("Adding the values in the output table for bunit :" + bUnit + " and metal :"+metalCcy);
					int intRowNum = output.addRow();
					output.setInt("bunit",intRowNum,bUnit);
					output.setInt("metal_ccy",intRowNum,metalCcy);
					output.setInt("date",intRowNum,reportDate);
					output.setDouble("closing_volume",intRowNum,openVolume);
					output.setDouble("closing_value",intRowNum,openValue);
					output.setDouble("closing_price",intRowNum,openPrice);
				}
			}
		} catch(Exception e) {
			Logging.error("Failed to fetch the data from open trading position table " + e.getMessage());
		} finally{ 
			if (Table.isTableValid(openPositionResults) == 1) {
				openPositionResults.destroy();
			}
		}
	}	
}