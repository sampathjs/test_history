package com.matthey.openlink.pnl;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.ROW_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

public abstract class PnlReportPositionLimitBase extends PNL_ReportEngine
{
	String relevantRiskDefinitions = "'Position by Metal and BU', 'Position by Metal Global'";
	
	protected void generateOutputTableFormat(Table output) throws OException
	{
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
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		regRefConversion(output, "in_breach", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
		
		regDateConversion(output, "date");
	}
	
	@Override
	protected void populateOutputTable(Table output) throws OException
	{
		int reportCloseDate = reportDate + 1;
		
		PluginLog.info("PNL_Report_Summary::populateOutputTable called.\n");
		OConsole.message("PNL_Report_Summary::populateOutputTable called.\n");
		
		Table openPosData = m_positionHistory.getOpenPositionsForDates(reportDate, reportCloseDate);
		Table dealDetailsData = m_positionHistory.getPositionData();
		
		for (int row = 1; row <= dealDetailsData.getNumRows(); row++)
		{	
			int bunit = dealDetailsData.getInt("bunit", row);
			int metalCcy = dealDetailsData.getInt("metal_ccy", row);
			
			// Check that either the selected Business Unit Set is empty, or contains this BUnit
			if (intBUSet.isEmpty() || intBUSet.contains(bunit))
			{
				// Check this is a "Metal" as opposed to a foreign currency
				if (MTL_Position_Utilities.isPreciousMetal(metalCcy))
				{
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
		
		// Create summary rows per metal, aggregated across all BU's
		Table summaryData = output.cloneTable();
		
		summaryData.select(output, "DISTINCT, metal_ccy", "metal_ccy GT -1");
		summaryData.select(output, "SUM, closing_volume, closing_value", "metal_ccy EQ $metal_ccy");
		for (int row = 1; row <= summaryData.getNumRows(); row++)
		{
			// Set price as value \ volume by metal, unless volume is zero
			double volume = summaryData.getDouble("closing_volume", row);
			double value = summaryData.getDouble("closing_value", row);
			double price = (Math.abs(volume) > 0.0001) ? value/volume : 0.0;
			
			summaryData.setDouble("closing_price", row, price);
		}
		
		summaryData.copyRowAddAll(output);
		summaryData.destroy();
		
		// Set report dates
		output.setColValInt("date", reportDate);		
		
		enrichPositionLimits(output);
		
		for (int row = 1; row <= output.getNumRows(); row++)
		{
			double position = output.getDouble("closing_volume", row);
			double limit = output.getDouble("position_limit", row);
			double limitUsage = (Math.abs(limit) > 0.001) ? Math.abs(position / limit) : Math.abs(position);
			
			if (Math.abs(position) > Math.abs(limit))
			{
				output.setInt("in_breach", row, 1);
			}
			
			output.setDouble("limit_usage", row, limitUsage);
		}				
		
		// output.viewTable();
	}
	
	private void enrichPositionLimits(Table output) throws OException
	{
		int ret = 0;
		Table tblLimitData = Table.tableNew();
		
		ret = DBaseTable.execISql(tblLimitData, 
				"select rsk.exp_line_id, SUM(rsk.limit) position_limit " + 
				"from risk_exposure_view rev, rsk_limit rsk " +
				"where rev.risk_expdef_name in (" + relevantRiskDefinitions + ") and rev.exp_line_id = rsk.exp_line_id and " +
				"rsk.start_date <= " + OCalendar.today() + " and " +
				"rsk.end_date >= " + OCalendar.today() + " and " +
				"rsk.status = 1 " +
				"group by rsk.exp_line_id");

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
		{
			throw new RuntimeException("Unable to run query to select limit data");
		}   
		
		Table tblInfoData = Table.tableNew();
		
		ret = DBaseTable.execISql(tblInfoData, 
				"select rc.* from risk_exposure_view rev, rsk_criteria rc " + 
				"where rev.exp_line_id = rc.exp_line_id and " +
					"(rev.risk_expdef_name = 'Position by Metal and BU' OR " +
					"(rev.risk_expdef_name = 'Position by Metal Global' AND criteria_type = 12))");

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
		{
			throw new RuntimeException("Unable to run query to select limit data");
		}  		
		
		tblLimitData.select(tblInfoData, "criteria_value(bunit)", "exp_line_id EQ $exp_line_id AND criteria_type EQ 1");
		tblLimitData.select(tblInfoData, "criteria_value(metal_ccy)", "exp_line_id EQ $exp_line_id AND criteria_type EQ 12");
		
		output.select(tblLimitData, "position_limit", "bunit EQ $bunit AND metal_ccy EQ $metal_ccy");
		
		tblLimitData.destroy();
		tblInfoData.destroy();		
	}
}