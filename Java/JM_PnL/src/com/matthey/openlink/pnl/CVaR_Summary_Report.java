package com.matthey.openlink.pnl;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class CVaR_Summary_Report extends CVaR_ReportEngine {

	
	@Override
	protected void generateOutputTableFormat(Table output) throws OException 
	{
		output.addCol("counterparty", COL_TYPE_ENUM.COL_INT);
		output.addCol("using_pos_cvar", COL_TYPE_ENUM.COL_INT);
		output.addCol("position", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_direction", COL_TYPE_ENUM.COL_STRING);
		
		output.addCol("cvar_limit", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("limit_usage", COL_TYPE_ENUM.COL_DOUBLE);		
		output.addCol("in_breach", COL_TYPE_ENUM.COL_INT);		
	}

	@Override
	protected void populateOutputTable(Table output) throws OException 
	{
		for (SummaryPositionData counterpartyData : m_counterpartySummaryData.values())
		{
			double posCVaR = counterpartyData.m_summaryData.m_posCVaR;
			double negCVaR = counterpartyData.m_summaryData.m_negCVaR;
			
			boolean usePosCVaR = (posCVaR > negCVaR);
			
			output.addRow();
			int newRow = output.getNumRows();

			output.setInt("counterparty", newRow, counterpartyData.m_counterparty);
			output.setInt("using_pos_cvar", newRow, usePosCVaR ? 1 : 0);
			output.setDouble("position", newRow, counterpartyData.m_summaryData.m_position);
			output.setDouble("cvar", newRow, usePosCVaR ? posCVaR : negCVaR);
			output.setString("cvar_direction", newRow, usePosCVaR ? CVAR_DIRECTION_POSITIVE : CVAR_DIRECTION_NEGATIVE);
		}
		
		// Enrich position limits per counterparty from DB 
		enrichPositionLimits(output);
		
		// Calculate limit usage and whether we are in breach
		for (int row = 1; row <= output.getNumRows(); row++)
		{
			double position = output.getDouble("cvar", row);
			double limit = output.getDouble("cvar_limit", row);
			double limitUsage = (Math.abs(limit) > 0.001) ? Math.abs(position / limit) : Math.abs(position);
			
			if (Math.abs(position) > Math.abs(limit))
			{
				output.setInt("in_breach", row, 1);
			}
			
			output.setDouble("limit_usage", row, limitUsage);
		}			
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "counterparty", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "using_pos_cvar", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
		regRefConversion(output, "in_breach", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
	}

	private void enrichPositionLimits(Table output) throws OException
	{
		int ret = 0;
		Table tblLimitData = Table.tableNew();
		
		ret = DBaseTable.execISql(tblLimitData, 
				"select rsk.exp_line_id, SUM(rsk.limit) cvar_limit " + 
				"from credit_exposure_view cev, rsk_limit rsk " +
				"where cev.credit_expdef_name = 'Credit VaR' and cev.exp_line_id = rsk.exp_line_id and " +
				"rsk.start_date <= " + OCalendar.today() + " and " +
				"rsk.end_date >= " + OCalendar.today() + " and " +
				"rsk.status = 1 " +
				"group by rsk.exp_line_id");

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query to select limit data");
		}   
		
		Table tblInfoData = Table.tableNew();
		
		ret = DBaseTable.execISql(tblInfoData, 
				"select rc.* from credit_exposure_view cev, rsk_criteria rc " + 
				"where cev.credit_expdef_name = 'Credit VaR' and cev.exp_line_id = rc.exp_line_id");

		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query to select limit data");
		}  		
		
		tblLimitData.select(tblInfoData, "criteria_value(counterparty)", "exp_line_id EQ $exp_line_id AND criteria_type EQ 4");
		
		output.select(tblLimitData, "cvar_limit", "counterparty EQ $counterparty");
		
		tblLimitData.destroy();
		tblInfoData.destroy();		
	}	
}
