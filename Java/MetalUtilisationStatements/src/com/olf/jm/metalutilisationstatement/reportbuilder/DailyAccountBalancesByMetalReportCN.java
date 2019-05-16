package com.olf.jm.metalutilisationstatement.reportbuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap; 

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalutilisationstatement.MetalsUtilisationConstRepository;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.EnumQueryResultTable;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/**
 * 
 * Report Builder data source. Executes the Nostro and Vostro account balance reports for each day within the requested date range and
 * combines into a single data table. The data is enriched with additional data. 
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 30-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 21-Jul-2016 |               | J. Waechter     | Fixed defect in method runReport: now copying table                             |
 * |     |             |               |                 | as it might have been destroyed    
 * | 003 | 05-Feb-2018 |               | S. Ma           | FIX for multiply nostro accounts                                            |
 *   003 | 03-Jan-2019 |               | Kiran.Babu      | Created for Shanghai updates
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class DailyAccountBalancesByMetalReportCN extends AbstractGenericScript {

    private static SimpleDateFormat SDF1 = new SimpleDateFormat("dd-MMM-yyyy");
    private static SimpleDateFormat SDF2 = new SimpleDateFormat("yyyyMMdd");
    private Session _session;
    private Table accountTable;
    /**
     * Plugin entry method.
     */
    @Override
    public Table execute(Session session, ConstTable table) {
        this._session = session;
    	try {
            Logging.init(session, getClass(), "Metals Utilisation Statement", "Daily Account Balances By Metal");
            intiAccountTable();
            Table parameters = table.getTable("PluginParameters", 0);
            try (Table output = runReportDateRange(session, parameters)) {
                removeNonMetalBalances(session, output);
                addPartyDetails(session, output);
                output.convertColumns("DateTime[report_date]");
                return filterOutput(output, parameters);
            }
        }
        catch (RuntimeException | ParseException e) {
            Logging.error("", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        finally {
			accountTable.dispose();
            Logging.close();
        }
    }

    /**
     * Run the Nostro/Vostro daily account balance reports for each day in the date range requested within the report parameters.
     * 
     * @param session Current session
     * @param parameters Report parameters
     * @return Table with collected daily balances
     * @throws ParseException for invalid dates
     */
    private Table runReportDateRange(Session session, Table parameters) throws ParseException {
        HashMap<String, String> runParameters = new HashMap<>();
        
        // Get start/end date range
        Calendar start = getDateParameter(parameters, "start_date");
        Calendar end = getDateParameter(parameters, "end_date");

        MetalsUtilisationConstRepository constants = new MetalsUtilisationConstRepository();
        String vostroReportName = constants.getVostroAccountBalanceReportCN();
        String nostroReportName = constants.getNostroAccountBalanceReportCN();

        Logging.info("Running '" + vostroReportName + "' and '" + nostroReportName + "' "
                + "reports for date range %1$td-%1$tb-%1$tY to %2$td-%2$tb-%2$tY", start, end);
        
        // Execute Nostro/Vostro account balance reports for each date in the date range and collect results into single table
        try (Table output = session.getTableFactory().createTable()) {
            do {
                runParameters.put("ReportDate", SDF1.format(start.getTime()));
                Table vostro = runReport(session, vostroReportName, runParameters);
                Table nostro = runReport(session, nostroReportName, runParameters);
                if (vostro != null && vostro.getRowCount() > 0){
                    output.select(vostro, "*", "[In.account_id] > 0");
                }
                if (nostro != null && nostro.getRowCount() > 0){
                    output.select(nostro, "*", "[In.account_id] > 0");
                }
                start.add(Calendar.DATE, 1);
            } while (start.before(end) || start.equals(end));
    
            return output.cloneData();
        }
    }

    /**
     * Get the date parameter value.
     * 
     * @param parameters Report parameters
     * @param parameterName Date parameter name
     * @return date
     * @throws ParseException
     */
    private Calendar getDateParameter(Table parameters, String parameterName) throws ParseException {
        Calendar date = Calendar.getInstance();
        int row = parameters.find(parameters.getColumnId("parameter_name"), parameterName, 0);
        if (row >= 0) {
            try {
                date.setTime(SDF1.parse(parameters.getString("parameter_value", row)));
            }
            catch (ParseException e) {
                date.setTime(SDF2.parse(parameters.getString("parameter_value", row)));
            }
        }
        return date;
    }

    /**
     * Run the given Report Builder report.
     * 
     * @param reportName Report name
     * @return Report output data
     */
    private Table runReport(Session session, String reportName, HashMap<String, String> parameters) {

        parameters.put("report_name", reportName);
        ReportParameters rptParams = new ReportParameters(session, parameters);
        
        GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
        generator.generate();

        return generator.getResults().cloneData();
    }

    /**
     * Remove from the output table any account balances that are not metal balances.
     * 
     * @param session Current session
     * @param output Report output table
     */
    private void removeNonMetalBalances(Session session, Table output) {
        try (Table metal = session.getIOFactory().runSQL("SELECT id_number, precious_metal FROM currency WHERE precious_metal = 1");
             Table clone = output.cloneStructure()) {
            output.select(metal, "precious_metal", "[In.id_number] == [Out.currency_id] AND [In.precious_metal] == 1");
            clone.addColumn("precious_metal", EnumColType.Int);
            clone.select(output, "*", "[In.precious_metal] == 1");
            output.clearData();
            output.appendRows(clone);
            output.removeColumn("precious_metal");
        }
    }

    /**
     * Add account party details including party id and party preferred currency.
     * 
     * @param session Current session
     * @param output Output table
     */
    private void addPartyDetails(Session session, Table output) {
        try (QueryResult qr = session.getIOFactory().createQueryResult(EnumQueryResultTable.Plugin)) {
             qr.add(output.getColumnValuesAsInt("account_id"));
             try (Table result = session.getIOFactory().runSQL(
                  "\n SELECT pac.account_id"
                + "\n      , pac.party_id"
                + "\n      , ccy.id_number AS party_preferred_currency_id"
                + "\n      , ccy.name AS party_preferred_currency_name"
                + "\n   FROM party_account   pac"
                + "\n   JOIN party_info_view piv ON (piv.party_id = pac.party_id)"
                + "\n   JOIN currency        ccy ON (ccy.name = piv.value)"
                + "\n   JOIN account acc ON acc.account_id = pac.account_id  and ( acc.business_unit_owner  = pac.party_id or acc.business_unit_owner  = 0)"
                + "\n   JOIN " + qr.getDatabaseTableName() + " qr ON (qr.query_result = pac.account_id)"
                + "\n  WHERE piv.type_name = 'Preferred Currency'"
                + "\n    AND qr.unique_id = " + qr.getId())) {

                 output.select(result, "*", "[In.account_id] == [Out.account_id]");
            }
        }
    }

    /**
     * Look for other report parameters that are in additional to the date range parameters that are to be used to further filter the
     * report output.
     *  
     * @param output Report output table
     * @param parameters Report parameters
     * @return filtered output table
     */
    private Table filterOutput(Table output, Table parameters) {
        Table filtered = output.cloneStructure();
        StringBuilder where = new StringBuilder();
        // Iterate over parameters
        try {
			
        	for (TableRow row : parameters.getRows()) {
        		String colName = row.getString("parameter_name");
        		// Is parameter name a column in the output table?
        		if (output.isValidColumn(colName)) {
        			// Ignore start_date and end_date parameters
        			if (!colName.endsWith("_date")) {
        				String value = row.getString("parameter_value").trim();
        				if(colName.equals("account_id"))
        				{
        					value = this.translateAccountId(value);
        				}
        				if (!value.isEmpty()) {
        					where.append(" AND [In." + colName + "] == ");
        					switch (row.getInt("parameter_type")) {
        					case 20: // Int
        						where.append(value);
        						break;
        						
        					case 5: // String
        						where.append("'" + value + "'");
        						break;
        						
        					default:
        						break;
        					}
        				}
        			}
        		}
        	}
        	where.trimToSize();
        	if (where.length() > 0) {
        		// Filter out using where clause removing the leading ' AND '
        		filtered.select(output, "*", where.substring(5).toString());
        	}
        	else {
        		filtered = output.cloneData();
        	}
		} catch (Throwable e) {
			 Logging.error("", e);
		}
        return filtered;
    }
    
    /*
    public String translateAccountId (String accountName)
    {
    	String ret = "";
    	String sql = "select account_id from account where account_name = '"+accountName+"'";
    	try (Table result = _session.getIOFactory().runSQL(sql))
    	{
    		ret = Integer.toString(result.getInt(0, 0));			
    		
		} catch (Exception e) {
			Logging.info("Exception in the translateAccountID block "+e.getLocalizedMessage());
		}
    	return ret;
    	
    }
    */
    
    public String translateAccountId (String accountName)
    {
    	String ret = "";
    	try
    	{
    		int rowId = accountTable.find(0, accountName, 0);
    		ret = Integer.toString(accountTable.getInt(1, rowId));			
    		
		} catch (Exception e) {
			Logging.info("Exception in the translateAccountID block "+e.getLocalizedMessage());
		}
    	return ret;
    	
    }

    private void intiAccountTable()
    {
        	String sql = "select account_name , account_id from account";
        	try 
        	{
        		accountTable = _session.getIOFactory().runSQL(sql);
        		
    		} catch (Exception e) {
    			Logging.info("Exception in loading account table "+e.getLocalizedMessage());
    		}
        	accountTable.sortStable(0, true);
    	
    }
}
