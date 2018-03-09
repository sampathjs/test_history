package com.matthey.openlink.reporting.udsr;

import java.util.HashMap;
import java.util.Vector;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openjvs.OException;
import com.olf.openrisk.calendar.SymbolicDate;

import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.ConstField;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class JM_CVaR_Data extends AbstractSimulationResult2 
{
	TableFactory 		tf;
	CalendarFactory 	cf;
	SimulationFactory	sf;
	Session				_session;
	
	private static final double s_stdConfidenceFactor = 1.96;
	private static final String s_extLentityField = "External Legal Entity";
	private static final String s_paymentDateField = "Payment Date";
	private static final String s_settleDateField = "Settle Date";
	
	private class CreditIndexData
	{
		int m_ID;
		int m_metal;
		double m_maxNegativeShock;
		HashMap<Integer, CreditGptData> m_gptMap = new HashMap<Integer, CreditGptData>();		
	}
	
	private class CreditGptData
	{
		int m_gptID;
		double m_dailyVol;
	}
	
	@Override
	public void calculate(Session session, Scenario scenario,
			RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) 
	{	
		tf = session.getTableFactory();
		cf = session.getCalendarFactory();	
		sf = session.getSimulationFactory();
		
		_session = session;	
		
		ConstTable deltaData = prerequisites.getGeneralResultTable(EnumResultType.TranGptDeltaByLeg, 0, 0, 0);
		ConstTable varRawData = prerequisites.getGeneralResultTable(EnumResultType.VaRGridPointRawData, 0, 0, 0);
		
		ResultType idxInfoResult = sf.createResultType("JM Credit VaR Index Info");
		ConstTable idxInfoData = prerequisites.getGeneralResultTable(idxInfoResult, 0, 0, 0);
		
		if ((deltaData == null) || (deltaData.getRowCount() == 0))
		{
			_session.getDebug().printLine("JM_CVaR_Data:: No Delta data found, skipping calculations.");
			return;
		}
		if ((varRawData == null) || (varRawData.getRowCount() == 0))
		{
			_session.getDebug().printLine("JM_CVaR_Data:: No VaR Gpt Data data found, skipping calculations.");
			return;
		}	
		if ((idxInfoData == null) || (idxInfoData.getRowCount() == 0))
		{
			_session.getDebug().printLine("JM_CVaR_Data:: No Credit VaR Index Info data found, skipping calculations.");
			return;
		}			
		
				
		// Create a map of index and gpt-level properties
		HashMap<Integer, CreditIndexData> creditIndexMap = parseIndexDataMap(idxInfoData, varRawData);
			
		// Populate Credit VaR data values
		Table creditVaRData = createCreditVaRData(deltaData, creditIndexMap, transactions);
		
		processMaturityBuckets(creditVaRData);
		
		revalResult.setTable(creditVaRData);					
	}
	
	private static String[] s_maturityBucketNames = null;
	private static String[] s_maturityBucketEndDates = null;
	private static Integer[] s_maturityBucketIDs = null;
	private static boolean s_initMaturityBuckets = false;
	
	private static boolean initMaturityBuckets(Session session)
	{
		Table matBuckets = null;
		
		for (UserTable t : session.getIOFactory().getUserTables())
		{
			if (t.getName().equalsIgnoreCase("USER_jm_cvar_maturity_buckets"))
			{
				matBuckets = t.retrieveTable();
				break;
			}
		}
		
		if (matBuckets == null)
		{
			session.getDebug().printLine("Could not find USER_jm_cvar_maturity_buckets data table.");
			return false;
		}
		
		s_maturityBucketNames = new String[matBuckets.getRowCount()];
		s_maturityBucketEndDates =  new String[matBuckets.getRowCount()];
		s_maturityBucketIDs = new Integer[matBuckets.getRowCount()];
		
		for (TableRow r : matBuckets.getRows())
		{
			int offset = r.getInt("id") - 1;
			
			s_maturityBucketNames[offset] = r.getString("label");
			s_maturityBucketEndDates[offset] = r.getString("symbolic_length");
			s_maturityBucketIDs[offset] = r.getInt("id");
		}
		
		s_initMaturityBuckets = true;
		
		return true;
	}
	
	private void processMaturityBuckets(Table creditVaRData)
	{
		if (!s_initMaturityBuckets)
		{
			// Attempt to initialise maturity buckets; exit if failed to initialise
			if (!initMaturityBuckets(_session))
				return;
		}
		
		Vector<Integer> bucketDates = new Vector<Integer>();
		
		for (int i = 0; i < s_maturityBucketEndDates.length; i++)
		{
			SymbolicDate sd = cf.createSymbolicDate(s_maturityBucketEndDates[i]);
			
			bucketDates.add(cf.getJulianDate(sd.evaluate()));
		}
		
		for (int row = 0; row < creditVaRData.getRowCount(); row++)
		{
			int matDate = creditVaRData.getInt("maturity_date", row);
			
			for (int i = 0; i < bucketDates.size(); i++)
			{
				if (matDate <= bucketDates.get(i))
				{
					creditVaRData.setInt("maturity_bucket", row, s_maturityBucketIDs[i]);
					break;
				}
			}
			
		}
	}
	
	private Table createCreditVaRData(ConstTable deltaData, HashMap<Integer, CreditIndexData> creditIndexMap, Transactions transactions)
	{
		int today = cf.getJulianDate(_session.getTradingDate());
				
		Table creditVaRData = createOutputTable();
		
		for (int row = 0; row < deltaData.getRowCount(); row++)
		{
			int dealNum = deltaData.getInt("deal_num", row);
			int dealLeg = deltaData.getInt("deal_leg", row);
			int dealPdc = deltaData.getInt("deal_pdc", row);
			int indexID = deltaData.getInt("index", row);
			int gptID = deltaData.getInt("gpt_id", row);
			double delta = deltaData.getDouble("delta", row);
			
			// Skip exposure to non-shock VaR curves
			if (!creditIndexMap.containsKey(indexID))
				continue;
			
			CreditIndexData idxData = creditIndexMap.get(indexID);
			
			// Skip un-recognised gridpoints
			if (!idxData.m_gptMap.containsKey(gptID))
				continue;
			
			CreditGptData gptData = idxData.m_gptMap.get(gptID);
			
			Transaction trn = transactions.getTransaction(dealNum);
			
			if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
			{
				_session.getDebug().printLine("Processing transaction: " + dealNum);
			}
						
			// Skip exposure from deals that do not contribute to Credit VaR -
			// anything other than FX, ComSwap or LoanDepo toolset deal
			EnumToolset toolset = trn.getToolset();
			if ((toolset == EnumToolset.Fx) || (toolset == EnumToolset.ComSwap) || (toolset == EnumToolset.Loandep))
			{
				// NO-OP, fall through to the logic below
			}
			else
			{
				continue;
			}
			
			int counterparty = 0;
			
			try
			{
				counterparty = trn.getConstFields().getField(s_extLentityField).getValueAsInt();	
			}
			catch (Exception e)
			{
				_session.getDebug().printLine("Failed to retrieve External LE for transaction: " + trn.getDealTrackingId() + " - " + e.toString());
			}				
			
			int paymentDate = today;
			
			try
			{
				if ((dealLeg < trn.getLegCount()) && (dealPdc < trn.getLeg(dealLeg).getProfiles().getCount()))
				{
					paymentDate = trn.getLeg(dealLeg).getProfile(dealPdc).getConstFields().getField(s_paymentDateField).getValueAsInt();
				}
				else
				{
					paymentDate = trn.getConstFields().getField(s_settleDateField).getValueAsInt();
				}
			}
			catch (Exception e)
			{
				_session.getDebug().printLine("Failed to retrieve payment date for transaction: " + trn.getDealTrackingId() + " - " + e.toString());
			}			
						
			int timeToMaturity = _session.getMarket().getIndex(indexID).getHolidaySchedules(true).getGoodBusinessDayCount(cf.getDate(today), cf.getDate(paymentDate), false);
			
			if (timeToMaturity < 0)
			{
				timeToMaturity = 0;
			}
			
			double posShock = s_stdConfidenceFactor * Math.sqrt((double)timeToMaturity) * gptData.m_dailyVol;
			double negShock = Math.min(posShock, idxData.m_maxNegativeShock);
			
			double posShockContribution = (gptData.m_dailyVol > 0) ?
					posShock * delta / gptData.m_dailyVol :
					0;
			
			double negShockContribution = (gptData.m_dailyVol > 0) ?
					- negShock * delta / gptData.m_dailyVol :
					0;
			
			int outRow = creditVaRData.addRows(1);	
			
			creditVaRData.setInt("deal_num", outRow, dealNum);
			creditVaRData.setInt("deal_leg", outRow, dealLeg);
			creditVaRData.setInt("deal_pdc", outRow, dealPdc);
			creditVaRData.setInt("exposure_index", outRow, indexID);
			
			creditVaRData.setDouble("delta", outRow, delta);
			
			creditVaRData.setDouble("confidence_factor", outRow, s_stdConfidenceFactor);			
			
			creditVaRData.setInt("metal", outRow, idxData.m_metal);
			creditVaRData.setInt("counterparty", outRow, counterparty);
			creditVaRData.setInt("maturity_date", outRow, paymentDate);
			creditVaRData.setInt("time_to_maturity", outRow, timeToMaturity);
			
			creditVaRData.setDouble("daily_volatility", outRow, gptData.m_dailyVol);
			creditVaRData.setDouble("max_negative_shock", outRow, idxData.m_maxNegativeShock);
			
			creditVaRData.setDouble("pos_shock", outRow, posShock);
			creditVaRData.setDouble("neg_shock", outRow, negShock);

			creditVaRData.setDouble("pos_shock_cvar", outRow, posShockContribution);
			creditVaRData.setDouble("neg_shock_cvar", outRow, negShockContribution);			
		}
	
		return creditVaRData;
	}
	
	/*
	 * 
	 */
	private HashMap<Integer, CreditIndexData> parseIndexDataMap(ConstTable idxInfoData, ConstTable varRawData)
	{
		HashMap<Integer, CreditIndexData> creditIndexMap = new HashMap<Integer, CreditIndexData>();		
		
		// Iterate over all entries in VaR Raw Data to retrieve a list of relevant indexes and gridpoints with appropriate daily vol
		for (int row = 0; row < varRawData.getRowCount(); row++)
		{
			int indexID = varRawData.getInt("index_id", row);
			int gptID = varRawData.getInt("gpt_id", row);
			double dailyVol = varRawData.getDouble("gpt_sigma", row);
			
			if (!creditIndexMap.containsKey(indexID))
			{
				CreditIndexData idxData = new CreditIndexData();
				idxData.m_ID = indexID;
				creditIndexMap.put(indexID, idxData);
			}
			
			CreditIndexData idxData = creditIndexMap.get(indexID);
			
			if (!idxData.m_gptMap.containsKey(gptID))
			{
				CreditGptData gptData = new CreditGptData();
				gptData.m_gptID = gptID;
				gptData.m_dailyVol = dailyVol;
				idxData.m_gptMap.put(gptID, gptData);
			}			
		}
		
		// Iterate over all entries in JM Credit VaR Index Info to retrieve relevant index properties
		for (int row = 0; row < idxInfoData.getRowCount(); row++)
		{
			int indexID = idxInfoData.getInt("shock_factor_index", row);
			int metal = idxInfoData.getInt("metal", row);
			double maxNegShock = idxInfoData.getDouble("max_negative_shock", row);
			
			if (creditIndexMap.containsKey(indexID))
			{
				CreditIndexData idxData = creditIndexMap.get(indexID);
				
				idxData.m_maxNegativeShock = maxNegShock;
				idxData.m_metal = metal;
			}
		}
		
		return creditIndexMap;
	}
	
	@Override
    public void format(final Session session, final RevalResult revalResult) 
	{
		Table returnVal = revalResult.getTable();
		TableFormatter formatter = returnVal.getFormatter();
		
		// Set column titles
		
		formatter.setColumnTitle("deal_num", "Deal Number");
		formatter.setColumnTitle("deal_leg", "Deal Leg");
		formatter.setColumnTitle("deal_pdc", "Deal Profile");
		
		formatter.setColumnTitle("counterparty", "Counterparty");
		formatter.setColumnTitle("exposure_index", "Exposure Index");
		formatter.setColumnTitle("metal", "Metal");
		
		formatter.setColumnTitle("delta", "Exposure");
		
		formatter.setColumnTitle("pos_shock", "Positive Shock");
		formatter.setColumnTitle("neg_shock", "Negative Shock");
		
		formatter.setColumnTitle("pos_shock_cvar", "Positive Shock\nCVaR Contribution");
		formatter.setColumnTitle("neg_shock_cvar", "Negative Shock\nCVaR Contribution");
		
		formatter.setColumnTitle("maturity_date", "Maturity Date");
		formatter.setColumnTitle("time_to_maturity", "Time To Maturity");
		formatter.setColumnTitle("maturity_bucket", "Maturity Bucket");
		
		formatter.setColumnTitle("confidence_factor", "Confidence Factor");
		formatter.setColumnTitle("daily_volatility", "Daily Volatility");
		formatter.setColumnTitle("max_negative_shock", "Max Negative Shock");
		
		// Set column formatting
		
		formatter.setColumnFormatter("counterparty", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));		
		formatter.setColumnFormatter("exposure_index", formatter.createColumnFormatterAsRef(EnumReferenceTable.Index));
		formatter.setColumnFormatter("metal", formatter.createColumnFormatterAsRef(EnumReferenceTable.Currency));
		
		formatter.setColumnFormatter("delta", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		
		formatter.setColumnFormatter("pos_shock", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("neg_shock", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );

		formatter.setColumnFormatter("pos_shock_cvar", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("neg_shock_cvar", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		
		formatter.setColumnFormatter("maturity_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		
		formatter.setColumnFormatter("confidence_factor", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("daily_volatility", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("max_negative_shock", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		
		for (UserTable t : session.getIOFactory().getUserTables())
		{
			if (t.getName().equalsIgnoreCase("USER_jm_cvar_maturity_buckets"))
			{				
				ReferenceChoices choices = session.getStaticDataFactory().createReferenceChoices(t.retrieveTable(), "JM CVaR Maturity Buckets");				
				formatter.setColumnFormatter("maturity_bucket", formatter.createColumnFormatterAsRef(choices));	
				break;
			}
		}		
    }	
	
	protected Table createOutputTable()
	{		
		Table data = tf.createTable("JM CVaR Data");
		
		data.addColumn("deal_num", EnumColType.Int);
		data.addColumn("deal_leg", EnumColType.Int);
		data.addColumn("deal_pdc", EnumColType.Int);
		data.addColumn("counterparty", EnumColType.Int);
		data.addColumn("exposure_index", EnumColType.Int);
		data.addColumn("metal", EnumColType.Int);
		
		data.addColumn("delta", EnumColType.Double);
		
		data.addColumn("maturity_date", EnumColType.Int);
		data.addColumn("time_to_maturity", EnumColType.Int);
		
		data.addColumn("maturity_bucket", EnumColType.Int);
		
		data.addColumn("confidence_factor", EnumColType.Double);
		data.addColumn("daily_volatility", EnumColType.Double);
		data.addColumn("max_negative_shock", EnumColType.Double);
		data.addColumn("pos_shock", EnumColType.Double);
		data.addColumn("neg_shock", EnumColType.Double);
		
		data.addColumn("pos_shock_cvar", EnumColType.Double);
		data.addColumn("neg_shock_cvar", EnumColType.Double);
		
		return data;
	}
}
