package com.matthey.openlink.pnl;

import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class CVaR_Details_Report extends CVaR_ReportEngine {

	
	@Override
	protected void generateOutputTableFormat(Table output) throws OException 
	{
		output.addCol("counterparty", COL_TYPE_ENUM.COL_INT);
		output.addCol("maturity_bucket", COL_TYPE_ENUM.COL_INT);
		output.addCol("maturity_bucket_label", COL_TYPE_ENUM.COL_STRING);
		output.addCol("using_pos_cvar", COL_TYPE_ENUM.COL_INT);
		output.addCol("cvar_direction", COL_TYPE_ENUM.COL_STRING);
		
		output.addCol("cvar_xag", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xau", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xir", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xos", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xpd", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xpt", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xrh", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("cvar_xru", COL_TYPE_ENUM.COL_DOUBLE);		
		
		output.addCol("position_xag", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xau", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xir", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xos", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xpd", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xpt", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xrh", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_xru", COL_TYPE_ENUM.COL_DOUBLE);		
		
		Map<Integer, MetalDefinition> metals = getMetals();
		for (MetalDefinition metal : metals.values())
		{
			s_metalPosColumnOffsets.put(metal.m_metalID, output.getColNum("position_" + metal.m_lowercaseLabel));
			s_metalCVaRColumnOffsets.put(metal.m_metalID, output.getColNum("cvar_" + metal.m_lowercaseLabel));
		}
	}
	
	Map<Integer, Integer> s_metalPosColumnOffsets = new HashMap<Integer, Integer>();
	Map<Integer, Integer> s_metalCVaRColumnOffsets = new HashMap<Integer, Integer>();	

	@Override
	protected void populateOutputTable(Table output) throws OException 
	{
		Map<Integer, MatBucketDefinition> matBuckets = getMaturityBuckets();
		Map<Integer, MetalDefinition> metals = getMetals();		
		
		for (SummaryPositionData counterpartyData : m_counterpartySummaryData.values())
		{
			double posCVaR = counterpartyData.m_summaryData.m_posCVaR;
			double negCVaR = counterpartyData.m_summaryData.m_negCVaR;
			
			boolean usePosCVaR = (posCVaR > negCVaR);
			
			for (MatBucketDefinition bucket : matBuckets.values())
			{
				output.addRow();
				int newRow = output.getNumRows();
				
				output.setInt("counterparty", newRow, counterpartyData.m_counterparty);
				output.setInt("maturity_bucket", newRow, bucket.m_bucketID);
				output.setString("maturity_bucket_label", newRow, bucket.m_bucketLabel);
				output.setInt("using_pos_cvar", newRow, usePosCVaR ? 1 : 0);
				output.setString("cvar_direction", newRow, usePosCVaR ? CVAR_DIRECTION_POSITIVE : CVAR_DIRECTION_NEGATIVE);
				
				for (MetalDefinition metal : metals.values())
				{					
					GroupingCriteria group = new GroupingCriteria(counterpartyData.m_counterparty, metal.m_metalID, bucket.m_bucketID);
					
					if (counterpartyData.m_cVaRDetailsMap.containsKey(group))
					{
						PositionData posData = counterpartyData.m_cVaRDetailsMap.get(group);
						
						output.setDouble(s_metalPosColumnOffsets.get(metal.m_metalID), newRow, posData.m_position);
						output.setDouble(s_metalCVaRColumnOffsets.get(metal.m_metalID), newRow, usePosCVaR ? posData.m_posCVaR : posData.m_negCVaR);
					}
				}
			}
		}
	}



	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "counterparty", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "using_pos_cvar", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
	}
}
