package com.matthey.openlink.reporting.udsr;

/*
 *History: 
 * 2020-06-15	V1.0    GuptaN02 - Fix days passed this Month } Problem 2894
 */

import java.util.Calendar;

import com.matthey.openlink.reporting.util.PnLUtils;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedules;
import com.olf.openrisk.io.DatabaseTable;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.trading.Transactions;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class JM_Interest_PNL_Data extends AbstractSimulationResult2 
{
	Session _session = null;
	
	@Override
	public void calculate(Session session, Scenario scenario,
			RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) 
	{	
		_session = session;
	
		ResultType jmRawPnlDataResType = _session.getSimulationFactory().getResultType("JM Raw PNL Data");
		
		Table jmRawPnlData = prerequisites.getGeneralResultTable(jmRawPnlDataResType, 0, 0, 0).asTable();
		Table output = jmRawPnlData.cloneData();
		output.setName("JM Interest PNL Data");
				
		output.addColumn("accrued_pnl", EnumColType.Double);
		output.addColumn("accrued_pnl_this_month", EnumColType.Double);	
		
		output.addColumn("days_passed", EnumColType.Int);
		output.addColumn("days_passed_this_month", EnumColType.Int);
		output.addColumn("total_days", EnumColType.Int);
		
		jmRawPnlData.copyTo(output);
				
		generateAccruedPnl(output);
		
		revalResult.setTable(output);
	}
	
	private void generateAccruedPnl(Table data)
	{	
		CalendarFactory cf = _session.getCalendarFactory();
		int today = cf.getJulianDate(_session.getTradingDate());
		
        Calendar som = Calendar.getInstance();  
        som.setTime(_session.getTradingDate());  
        som.set(Calendar.DAY_OF_MONTH, 1);        
		int somJd = cf.getJulianDate(som.getTime());    
        
        Calendar eom = Calendar.getInstance();  
        eom.setTime(_session.getTradingDate());  
        eom.add(Calendar.MONTH, 1);  
        eom.set(Calendar.DAY_OF_MONTH, 1);  
        eom.add(Calendar.DATE, -1);  
        int eomJd = cf.getJulianDate(eom.getTime());		        
        
		for (int row = data.getRowCount() - 1; row >= 0; row--)
		{
			int pnlType = data.getInt("pnl_type", row);
			
			if ((pnlType != PriceComponentType.INTEREST_PNL) && (pnlType != PriceComponentType.FUNDING_INTEREST_PNL))
			{
				// continue
				data.removeRow(row);
				continue;
			}

			// This row is of interest - calculate the accrued PNL
			double pnl = data.getDouble("value", row);
			int sd = data.getInt("accrual_start_date", row);
			int ed = data.getInt("accrual_end_date", row);
			int metalCcy = data.getInt("group", row);
			
			int fxIndex = PnLUtils.getIndexForCcy(_session, metalCcy);
			
			HolidaySchedules hs = _session.getMarketFactory().getMarket().getIndex(fxIndex).getHolidaySchedules(true);
			int totalDays = hs.getGoodBusinessDayCount(cf.getDate(sd), cf.getDate(ed), true);			
			
			double accruedRatio = 0.0, accruedInCurrentMonthRatio = 0.0;
			
			// Calculate total accrued ratio so far
			int daysPassed = 0;
				
			if (sd > today)
			{
				accruedRatio = 0.0;
			}	
			else if (ed <= today)
			{
				accruedRatio = 1.0;
				daysPassed = totalDays;
			}		
			else
			{
				daysPassed = hs.getGoodBusinessDayCount(cf.getDate(sd), cf.getDate(today), true);		
				
				accruedRatio = (totalDays > 0) ? ((double) daysPassed / (double) (totalDays)) : 1.0;
			}		
			
			// Calculate total accrued ratio for the current month
			int daysPassedThisMonth = 0;
			
			//Days Passed this month is minimum of Settle Date(ed), Today
			int startOfPeriod = Math.max(sd, somJd);
			int endOfPeriod = Math.min(ed, today);
			
			if (startOfPeriod <= endOfPeriod)
			{
				daysPassedThisMonth = hs.getGoodBusinessDayCount(cf.getDate(startOfPeriod), cf.getDate(endOfPeriod), true);
						
				accruedInCurrentMonthRatio = (totalDays > 0) ? ((double) daysPassedThisMonth / (double) (totalDays)) : 1.0;
			}
			
			data.setDouble("accrued_pnl", row, pnl * accruedRatio);
			data.setDouble("accrued_pnl_this_month", row, pnl * accruedInCurrentMonthRatio);
			
			data.setInt("days_passed", row, daysPassed);
			data.setInt("days_passed_this_month", row, daysPassedThisMonth);
			data.setInt("total_days", row, totalDays);			
		}
	}
	
	@Override
    public void format(final Session session, final RevalResult revalResult) 
	{
		Table returnVal = revalResult.getTable();
		TableFormatter formatter = returnVal.getFormatter();
		
		returnVal.sort("deal_num, deal_leg, deal_pdc, pnl_type, deal_reset_id, date", true);
		
		// Set column titles
		
		formatter.setColumnTitle("deal_num", "Deal Number");
		formatter.setColumnTitle("deal_leg", "Deal Leg");
		formatter.setColumnTitle("deal_pdc", "Deal Profile");
		formatter.setColumnTitle("deal_reset_id", "Deal Reset ID");
		formatter.setColumnTitle("pnl_type", "PnL Type");
		formatter.setColumnTitle("date", "Date");
		formatter.setColumnTitle("int_bu", "Int Business Unit");
		formatter.setColumnTitle("group", "Group");
		
		formatter.setColumnTitle("volume", "Volume");
		formatter.setColumnTitle("price", "Price");
		formatter.setColumnTitle("value", "Value");		
		
		formatter.setColumnTitle("accrual_start_date", "Accrual Start Date");	
		formatter.setColumnTitle("accrual_end_date", "Accrual End Date");	
		
		formatter.setColumnTitle("days_passed", "Days Passed");	
		formatter.setColumnTitle("days_passed_this_month", "Days Passed This Month");	
		formatter.setColumnTitle("total_days", "Days - Total");	
	
		// Set column formatting
		
		formatter.setColumnFormatter("int_bu", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));		
		formatter.setColumnFormatter("group", formatter.createColumnFormatterAsRef(EnumReferenceTable.Currency));
		
		formatter.setColumnFormatter("volume", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("price", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 5) );
		formatter.setColumnFormatter("value", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );		
		
		formatter.setColumnFormatter("date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("accrual_start_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("accrual_end_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));

		// Interest P&L - specific logic
		
		formatter.setColumnFormatter("accrued_pnl", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("accrued_pnl_this_month", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );

		formatter.setColumnTitle("accrued_pnl", "Accrued PnL");
		formatter.setColumnTitle("accrued_pnl_this_month", "Accrued PnL This Month");
		
		
		DatabaseTable table = session.getIOFactory().getUserTable("USER_JM_pnl_types");		
		ReferenceChoices choices = session.getStaticDataFactory().createReferenceChoices(table.retrieveTable(), "JM PNL Types");
				
		formatter.setColumnFormatter("pnl_type", formatter.createColumnFormatterAsRef(choices));
		
	}		
}
