package com.matthey.openlink.reporting.udsr;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.market.Element;
import com.olf.openrisk.market.Elements;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.ForwardCurve;
import com.olf.openrisk.market.Index;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.trading.Transactions;

import com.olf.openrisk.application.Debug;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class JM_CVaR_Index_Info extends AbstractSimulationResult2 
{
	TableFactory 		tf;
	CalendarFactory 	cf;
	
	private final String SVAR_PREFIX = "sVaR_";
	private final String SVAR_SEPARATOR = "_";
	private final String MIN_PX_PREFIX = "min";
	private final String SPOT_GPT_NAME = "Spot";

	@Override
	public void calculate(Session session, Scenario scenario,
			RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) 
	{	
		tf = session.getTableFactory();
		cf = session.getCalendarFactory();	
		
		Table returnVal = createOutputTable();		
		
		for ( ReferenceChoice r : session.getMarket().getIndexChoices())
		{
			String shockIdxName = r.getName();			
			if (!shockIdxName.startsWith(SVAR_PREFIX))
				continue;			
								
			// Convert the name into original price index
			int nextSeparator = shockIdxName.substring(SVAR_PREFIX.length()).indexOf(SVAR_SEPARATOR);			
			if (nextSeparator == -1)
				continue;
			String origName = shockIdxName.substring(SVAR_PREFIX.length() + nextSeparator + 1);
			
			String minPxIdxName = MIN_PX_PREFIX + origName;
			
			session.getDebug().printLine("Found " + shockIdxName + " - " + origName + " - " + minPxIdxName);
			
			try 
			{
				Index shockIdx = session.getMarket().getIndex(shockIdxName);
				Index origIdx = session.getMarket().getIndex(origName);
				Index minPxIdx = session.getMarket().getIndex(minPxIdxName);
				
				origIdx.recalculate();
				minPxIdx.recalculate();
				
				double origPrice = origIdx.getActiveGridPoints().getGridPoint(SPOT_GPT_NAME).getValue(EnumGptField.EffInput);
				double minPrice = minPxIdx.getActiveGridPoints().getGridPoint(SPOT_GPT_NAME).getValue(EnumGptField.EffInput);
				
				double maxNegativeShock = Math.max(origPrice - minPrice, 0.0);
				
				int metal = origIdx.getBoughtCurrency().getId();
				
				int row = returnVal.addRows(1);									

				returnVal.setInt("shock_factor_index", row, shockIdx.getId());		
				returnVal.setInt("original_index", row, origIdx.getId());
				returnVal.setInt("min_price_index", row, minPxIdx.getId());	
				
				returnVal.setDouble("current_spot_price", row, origPrice);
				returnVal.setDouble("min_spot_price", row, minPrice);
				returnVal.setDouble("max_negative_shock", row, maxNegativeShock);
				
				returnVal.setInt("metal", row, metal);					
			}	
			catch (Exception e)
			{
				session.getDebug().printLine(e.getMessage());
			}
		}
		
		revalResult.setTable(returnVal);					
	}
	
	@Override
    public void format(final Session session, final RevalResult revalResult) 
	{
		Table returnVal = revalResult.getTable();
		TableFormatter formatter = returnVal.getFormatter();
		
		// Set column titles
		formatter.setColumnTitle("shock_factor_index", "Shock Factor Index");
		formatter.setColumnTitle("original_index", "Original Index");
		formatter.setColumnTitle("min_price_index", "Min Price Index");
		formatter.setColumnTitle("current_spot_price", "Current Spot Price");
		formatter.setColumnTitle("min_spot_price", "Min Spot Price");
		formatter.setColumnTitle("max_negative_shock", "Max Negative Shock");
		formatter.setColumnTitle("metal", "Metal");
		
		// Set column formatting 
		formatter.setColumnFormatter("shock_factor_index", formatter.createColumnFormatterAsRef(EnumReferenceTable.Index));
		formatter.setColumnFormatter("original_index", formatter.createColumnFormatterAsRef(EnumReferenceTable.Index));
		formatter.setColumnFormatter("min_price_index", formatter.createColumnFormatterAsRef(EnumReferenceTable.Index));
		formatter.setColumnFormatter("metal", formatter.createColumnFormatterAsRef(EnumReferenceTable.Currency));		
	}	
	
	protected Table createOutputTable()
	{		
		Table data = tf.createTable("JM CVaR Index Info");
		
		data.addColumn("shock_factor_index", EnumColType.Int);
		data.addColumn("original_index", EnumColType.Int);
		data.addColumn("min_price_index", EnumColType.Int);
		data.addColumn("current_spot_price", EnumColType.Double);
		data.addColumn("min_spot_price", EnumColType.Double);
		data.addColumn("max_negative_shock", EnumColType.Double);
		data.addColumn("metal", EnumColType.Int);		
		
		return data;
	}
}
