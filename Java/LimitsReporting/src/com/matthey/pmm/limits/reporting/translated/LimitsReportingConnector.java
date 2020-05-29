package com.matthey.pmm.limits.reporting.translated;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.olf.embedded.application.Context;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.jm.logging.Logging;

public class LimitsReportingConnector {
    private final Context context;
	private MetalBalances metalBalances;
    
    public LimitsReportingConnector (final Context context) {
    	this.context = context;
    	this.metalBalances = null;
    }

    public int getLeaseLimit() {
    	try (Table limitsReportingLease = context.getIOFactory().getUserTable("USER_limits_reporting_lease").retrieveTable()) {
    		return limitsReportingLease.getInt("value", 0);
    	}
    }

    public List<DealingLimit> getDealingLimits () {
    	List<DealingLimit> dealingLimits = new ArrayList<>();
    	
    	try (Table limitsTable = context.getIOFactory().getUserTable("USER_limits_reporting_dealing").retrieveTable()) {
        	for (int row=limitsTable.getRowCount()-1; row >= 0; row--) {
        		DealingLimit dl = new DealingLimit(
        				  limitsTable.getString("limit_type", row)
        				, limitsTable.getString("desk", row)
        				, limitsTable.getString("metal", row)
        				, limitsTable.getInt("limit", row)
        				);
        		dealingLimits.add(dl);
        	}
        	return dealingLimits;    		
    	}
    }    
    
    public List<LiquidityLimit> getLiquidityLimits () {
    	List<LiquidityLimit> liquidityLimits = new ArrayList<>();
    	
    	try (Table limitsTable = context.getIOFactory().getUserTable("USER_limits_reporting_liquidity").retrieveTable()) {
        	for (int row=limitsTable.getRowCount()-1; row >= 0; row--) {
        		LiquidityLimit dl = new LiquidityLimit(
        				  limitsTable.getString("metal", row)
        				, limitsTable.getInt("lower_limit", row)
        				, limitsTable.getInt("upper_limit", row)
        				, limitsTable.getInt("max_liability", row)
        				);
        		liquidityLimits.add(dl);
        	}
        	return liquidityLimits;    		
    	}
    }
    
    public LocalDateTime getRunDate () {
        return LocalDateTime.fromDateFields(context.getTradingDate());
    }

    public double getGbpUsdRate (LocalDateTime date) {
    	StaticDataFactory staticDataFactory = context.getStaticDataFactory();
    	Currency gbp = (Currency)staticDataFactory.getReferenceObject(EnumReferenceObject.Currency, "GBP");
    	Currency usd = (Currency)staticDataFactory.getReferenceObject(EnumReferenceObject.Currency, "USD");
    	Market market = context.getMarket();
    	
    	// look for max 30 days back to find FX rates.
    	for (int i=0; i < 30; i++) {
            try {
                market.loadClose(date.minusDays(i).toDate());
            } catch (Exception ex) {
            	continue;
            }
            break;
    	}
        return market.getFXSpotRate(gbp, usd, date.toDate());
    }
    
    public Map<String, Double> getMetalPrices() {
    	final String sql = 
    			"            SELECT substring(id.index_name, 1, 3) AS metal,\n" + 
    			"                   ihp.price\n" + 
    			"                FROM idx_historical_prices ihp\n" + 
    			"                         INNER JOIN (SELECT MAX(last_update) AS last_update, index_id, ref_source\n" + 
    			"                                         FROM idx_historical_prices\n" + 
    			"                                         GROUP BY index_id, ref_source) AS latest_prices\n" + 
    			"                                    ON ihp.index_id = latest_prices.index_id AND ihp.last_update = latest_prices.last_update AND ihp.ref_source = latest_prices.ref_source\n" + 
    			"                         JOIN ref_source rs\n" + 
    			"                              ON ihp.ref_source = rs.id_number\n" + 
    			"                         JOIN idx_def id\n" + 
    			"                              ON ihp.index_id = id.index_version_id\n" + 
    			"                WHERE (id.index_name = 'XAG.USD' AND rs.name = 'LBMA Silver')\n" + 
    			"                   OR (id.index_name = 'XAU.USD' AND rs.name = 'LBMA PM')\n" + 
    			"                   OR (id.index_name IN ('XOS.USD', 'XRU.USD', 'XPD.USD', 'XPT.USD', 'XIR.USD', 'XRH.USD') AND\n" + 
    			"                       rs.name = 'JM NY Opening')\n";
    	Map<String, Double> metalPrices = new HashMap<String, Double>();
    	try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
    		for (int row=sqlResult.getRowCount()-1; row >= 0; row--) {
    			metalPrices.put(sqlResult.getString("metal", row), sqlResult.getDouble("price", row));
    		}
    	}
    	return metalPrices;
    }
    
    public MetalBalances getMetalBalances () {
    	if (this.metalBalances != null) {
    		return this.metalBalances;
    	}
    	
    	HashBasedTable<String, String, String> result = HashBasedTable.create();
        Table rawTable = runReport("Metals Balance Sheet - Combined", "Output_01");
        TableFormatter tableFormatter = rawTable.getFormatter();
        for (int row = rawTable.getRowCount()-1; row >=0; row--) {
        	String line = rawTable.getDisplayString(0, row);        		
        	for (int column=rawTable.getColumnCount()-1; column >= 0; column--) {
        		result.put(line, tableFormatter.getColumnTitle(column), rawTable.getDisplayString(column, row));
        	}
        }
        this.metalBalances = new MetalBalances(result);
        return this.metalBalances;
    }

    public List<BalanceLine> getBalanceLines () {
    	List<BalanceLine> balanceLine = new ArrayList<>();
    	
    	try (Table balanceLineTable = context.getIOFactory().getUserTable("USER_limits_reporting_balance").retrieveTable()) {
        	for (int row=balanceLineTable.getRowCount()-1; row >= 0; row--) {
        		BalanceLine bl = new BalanceLine(
        				  balanceLineTable.getString("balance_line", row)
        				, balanceLineTable.getString("purpose", row)
        				);
        		balanceLine.add(bl);
        	}
        	return balanceLine;    		
    	}
    }  
    
    public ImmutableTable<String, String, Double> getClosingPositions () {
    	Builder<String, String, Double> resultBuilder = new ImmutableTable.Builder<String, String, Double>();
    	Table rawTable = runReport("PMM Closing Position by Metal and BU", "TableOutput");
    	for (int row=rawTable.getRowCount()-1; row>=0; row--) {
    		try {
				resultBuilder.put(
						 rawTable.getString("bunit", row)
						,rawTable.getString("metal_ccy", row)
						,NumberFormat.getInstance().parse(rawTable.getString("closing_volume", row)).doubleValue());
			} catch (ParseException e) {
				throw new RuntimeException ("Error parsing string in report output for Report Builder definition PMM Closing Position by Metal and BU:"
						+ "\nCan't parse row " + row + " column 'closing_volume' value '" + rawTable.getString("closing_volume", row));
			}
    	}
    	return resultBuilder.build();
    }
    
    public Map<String, Double> getUnhedgedAndRefiningGainsPositions() {
    	final String sql = 
    			"            SELECT c.name AS metal, sum(settle_amount) AS position\n" + 
    			"                FROM ab_tran_event_settle abes\n" + 
    			"                         JOIN ab_tran_event abe\n" + 
    			"                              ON abes.event_num = abe.event_num\n" + 
    			"                         JOIN ab_tran ab\n" + 
    			"                              ON abe.tran_num = ab.tran_num\n" + 
    			"                         JOIN trans_status ts\n" + 
    			"                              ON ab.tran_status = ts.trans_status_id\n" + 
    			"                         JOIN account a\n" + 
    			"                              ON abes.int_account_id = a.account_id\n" + 
    			"                         JOIN USER_limits_reporting_account ra\n" + 
    			"                              ON a.account_name = ra.account_name\n" + 
    			"                         JOIN currency c\n" + 
    			"                              ON abes.currency_id = c.id_number\n" + 
    			"                WHERE ts.name IN ('Validated', 'Matured')\n" + 
    			"                GROUP BY c.name\n"
    			;
    	Map<String, Double> metalPositions = new HashMap<String, Double>();
    	try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
    		for (int row=sqlResult.getRowCount()-1; row >= 0; row--) {
    			metalPositions.put(sqlResult.getString("metal", row), sqlResult.getDouble("position", row));
    		}
    	}
    	return metalPositions;
    }

    public List<LeaseDeal> getLeaseDeals() {
    	List<LeaseDeal> leaseDeals = new ArrayList<LeaseDeal> ();
    	TradingFactory tradingFactory = context.getTradingFactory();
    	try (Query query = tradingFactory.getQueries().getQuery("Limits Reporting - Lease Deals");
    	    Transactions transactions = tradingFactory.retrieveTransactions(query)) {
    		for (Transaction tran : transactions) {
    			leaseDeals.add (new LeaseDeal(
    					 	 tran.retrieveField(EnumTranfField.TranNum, 0).getValueAsString()
    					 	,LocalDateTime.fromDateFields(tran.retrieveField(EnumTranfField.StartDate, 0).getValueAsDate())
    					 	,tran.retrieveField(EnumTranfField.Notnl, 0).getValueAsDouble()
    					 	,tran.retrieveField(EnumTranfField.CcyConvRate).getValueAsDouble()));
    		}
    	}	
    	return leaseDeals;
    }
    
    public String getPreviousBreachDates(
    		final String runType,
    		final LocalDateTime runDate,
    		final String liquidityBreachLimit,
    		final String desk,
    		final String metal) {
    	String runDateString = ISODateTimeFormat.date().print(runDate);
    	String sql = "                SELECT breach_dates\n" + 
    			"                    FROM USER_limits_reporting_result r1\n" + 
    			"                             JOIN (SELECT max(update_time) AS latest_update_time, metal\n" + 
    			"                                       FROM USER_limits_reporting_result\n" + 
    			"                                       WHERE run_date < '" + runDateString + "'\n" + 
    			"                                       GROUP BY run_type, metal) r2\n" + 
    			"                                  ON r1.metal = r2.metal AND r1.update_time = r2.latest_update_time\n" + 
    			"                    WHERE run_type = '" + runType + "'\n" + 
    			"                      AND liquidity_breach_limit = '" + liquidityBreachLimit + "'\n" + 
    			"                      AND desk = '" + desk + "'\n" + 
    			"                      AND r1.metal = '" + metal + "'\n";
        Logging.debug("getPreviousBreachDates SQL: " + sql);
        try (Table table = context.getIOFactory().runSQL(sql)) {
        	if (table != null && table.getRowCount() > 0) {
        		return table.getString(0, 0);
        	}
        	return null;
        }
    }

    public List<RunResult> getIntradayBreaches () {
    	List<RunResult> runResults = new ArrayList<>();
    	String sql = 
    			"            SELECT *\n" + 
    			"                FROM USER_limits_reporting_result\n" + 
    			"                WHERE run_type = 'Intraday Desk'\n" + 
    			"                  AND breach = 1";
    	try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
    		for (int row=sqlResult.getRowCount()-1; row>=0; row--) {
    			runResults.add(fromResultTableRow(sqlResult, row, true));
    		}
    	}
    	return runResults;
    }
    
    public List<RunResult> getEodBreaches () {
    	List<RunResult> runResults = new ArrayList<>();
    	String sql = 
    			" 				 SELECT r1.*\n" + 
    			"                FROM USER_limits_reporting_result r1\n" + 
    			"                         JOIN (SELECT max(update_time) AS latest_update_time,\n" + 
    			"                                      run_date,\n" + 
    			"                                      run_type,\n" + 
    			"                                      desk,\n" + 
    			"                                      metal\n" + 
    			"                                   FROM USER_limits_reporting_result\n" + 
    			"                                   WHERE breach = 1 \n" + 
    			"                                     AND run_type <> 'Intraday Desk'\n" + 
    			"                                   GROUP BY run_date, run_type, desk, metal) r2\n" + 
    			"                              ON r1.run_date = r2.run_date AND r1.run_type = r2.run_type AND r1.desk = r2.desk AND\n" + 
    			"                                 r1.metal = r2.metal AND r1.update_time = r2.latest_update_time";
        Logging.debug("breaches SQL: " + sql);
        try (Table sqlResult = context.getIOFactory().runSQL(sql);) {
    		for (int row=sqlResult.getRowCount()-1; row>=0; row--) {
    			runResults.add(fromResultTableRow(sqlResult, row, false));
    		}
        }
        return runResults;
    }
    
    private RunResult fromResultTableRow (Table table, int rowNum, boolean needRunTime) {
    	LocalDateTime runTime = needRunTime?
    		LocalDateTime.parse(table.getString("update_time",rowNum) + "Z", ISODateTimeFormat.dateTime()):
    		LocalDateTime.fromDateFields(table.getDate("run_date", rowNum));
    	String runType = table.getString("run_type", rowNum);
        String desk = table.getString("desk", rowNum);
    	String metal = table.getString("metal", rowNum);
        int liquidityLowerLimit = table.getInt("liquidity_lower_limit", rowNum);
        int liquidityUpperLimit = table.getInt("liquidity_upper_limit", rowNum);
        int liquidityMaxLiability = table.getInt("liquidity_max_liability", rowNum);
        int positionLimit = table.getInt("position_limit", rowNum);
        boolean breach = table.getInt("breach", rowNum) > 0;
        String liquidityBreachLimit = table.getString("liquidity_breach_limit", rowNum);
        double currentPosition = table.getDouble("current_position", rowNum);
        int liquidityDiff = table.getInt("liquidity_diff", rowNum);
        double breachTOz = table.getDouble("breach_toz", rowNum);
        double breachGBP = table.getDouble("breach_gbp", rowNum);
        boolean critical = table.getInt("critical", rowNum) > 0;
        List<String> breachDates = Arrays.asList(table.getString("breach_dates", rowNum).split(RunResult.DATE_SEPARATOR));
        return new RunResult(runTime, runType, desk, metal, 
        		liquidityLowerLimit, liquidityUpperLimit, liquidityMaxLiability, 
        		positionLimit, breach, liquidityBreachLimit, currentPosition,
        		liquidityDiff, breachTOz, breachGBP, critical, breachDates);
    }

    public void saveRunResult (final RunResult result) {
    	try (UserTable userTable = context.getIOFactory().getUserTable("USER_limits_reporting_result");
    		 Table appendedDataTable = userTable.getTableStructure()) {
    		appendedDataTable.addRow();
    		appendedDataTable.setDate("run_date", 0, getRunDate().toDate());
    		appendedDataTable.setString("run_type", 0, result.getRunType());
    		appendedDataTable.setString("desk", 0, result.getDesk());
			appendedDataTable.setString("metal", 0, result.getMetal());
    		appendedDataTable.setInt("liquidity_lower_limit", 0, result.getLiquidityLowerLimit());
    		appendedDataTable.setInt("liquidity_upper_limit", 0,  result.getLiquidityUpperLimit());
			appendedDataTable.setInt("liquidity_max_liability", 0, result.getLiquidityMaxLiability());
    	    appendedDataTable.setInt("position_limit", 0,  result.getPositionLimit());
    	    appendedDataTable.setInt("breach", 0, result.isBreach()?1:0);
   			appendedDataTable.setString("liquidity_breach_limit", 0, result.getLiquidityBreachLimit());
   			appendedDataTable.setDouble("current_position", 0, result.getCurrentPosition());
   			appendedDataTable.setInt("liquidity_diff", 0 , result.getLiquidityDiff());
   			appendedDataTable.setDouble("breach_toz", 0, result.getBreachTOz());
   			appendedDataTable.setDouble("breach_gbp", 0, result.getBreachGBP());
   			appendedDataTable.setInt("critical", 0, result.isCritical()?1:0);
   	    	StringBuilder sb = formatBreachDates(result);
   			appendedDataTable.setString("breach_dates", 0, sb.toString());
   			appendedDataTable.setString("update_time", 0, ISODateTimeFormat.dateTime().print(LocalDateTime.now()));
    		userTable.insertRows(appendedDataTable);
    	}
    }

	private StringBuilder formatBreachDates(final RunResult result) {
		List<String> breachDates = result.getBreachDates(this, result.isBreach(), 
				result.getRunType(), 
				result.getLiquidityBreachLimit(), result.getDesk(), result.getMetal());
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String breachDate : breachDates) {
			if (!first) {
				sb.append(RunResult.DATE_SEPARATOR);
			} else {
				first = false;
			}
			sb.append(breachDate);
		}
		return sb;
	}
    
	public Set<String> getEmails(final String runType) {
		Set<String> emails = new HashSet<>();
		String sql = 
				"                SELECT DISTINCT(p.email)\n" + 
				"                    FROM personnel p\n" + 
				"                             JOIN personnel_functional_group pf\n" + 
				"                                  ON p.id_number = pf.personnel_id\n" + 
				"                             JOIN functional_group f\n" + 
				"                                  ON f.id_number = pf.func_group_id\n" + 
				"                    WHERE f.name = 'EOD Limits Reporting - " + runType + "'";
        Logging.debug("getEmails SQL: " + sql);
		try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
			for (int row = sqlResult.getRowCount()-1; row >= 0; row--) {
				emails.add(sqlResult.getString("email", row));
			}
		}
		return emails;
	}
    
    private Table runReport(final String name, final String reportOutput) {
    	try {
            com.olf.openjvs.Table output = com.olf.openjvs.Table.tableNew();
            ReportBuilder reportBuilder = ReportBuilder.createNew(name);
            reportBuilder.setOutputTable(output);
            reportBuilder.runReportOutput(reportOutput);
            Table ocTable = context.getTableFactory().fromOpenJvs(output);
            // comment extract of "fromOpenJvs":
            // Memory will be freed when the dispose() method for both objects is called. See dispose.
            // so we need to dispose both the JVS and OC table.
            output.dispose();
            return ocTable;    		
    	} catch (OException ex) {
    		throw new RuntimeException ("Error running report '" + name + "', output '" + reportOutput + "'");
    	}
    }
}