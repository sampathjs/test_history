package com.olf.jm.metalutilisationstatement.reportbuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.market.EnumBmo;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/**
 * 
 * Report Builder data source. Retrieves the closing fx rates for the requested date range. Will also return average rate across the date
 * range if required by setting the use_average parameter to 'Yes'.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 30-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class SpotFxRatesUSD extends AbstractGenericScript {

    private static SimpleDateFormat SDF = new SimpleDateFormat("dd-MMM-yyyy");

    @Override
    public Table execute(Session session, ConstTable argt) {
        try {
            Logging.init(session, this.getClass(), "Metals Utilisation Statement", "Spot FX Rates USD");
            return process(session, argt);
        }
        catch (ParseException e) {
            Logging.error("Error while retrieving fx rates", e);
            throw new RuntimeException(e);
        }
        catch (RuntimeException e) {
            Logging.error("Error while retrieving fx rates", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }
    
    /**
     * Main processing method.
     * 
     * @param session Current session
     * @param argt Session table
     * @return fx rates
     * @throws ParseException
     */
    private Table process(Session session, ConstTable argt) throws ParseException {

        // Get the start/end dates and use average parameters
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        boolean useAverage = false;

        Table parameters = argt.getTable("PluginParameters", 0);
        int rowId = parameters.find(parameters.getColumnId("parameter_name"), "fx_start_date", 0);
        if (rowId >= 0) {
            start.setTime(SDF.parse(parameters.getString("parameter_value", rowId)));
        }
        rowId = parameters.find(parameters.getColumnId("parameter_name"), "fx_end_date", 0);
        if (rowId >= 0) {
            end.setTime(SDF.parse(parameters.getString("parameter_value", rowId)));
        }
        rowId = parameters.find(parameters.getColumnId("parameter_name"), "use_average", 0);
        if (rowId >= 0) {
            useAverage = "yes".equalsIgnoreCase(parameters.getString("parameter_value", rowId));
        }

        // Get the rates for the date range
        try (Table fxRates = getFxSpotRatesForDateRange(session, start, end)) {
            // Check if average requested
            if (useAverage) {
                Logging.info("Calculating average fx rates over the date range of %1$td-%1$tb-%1$tY to %2$td-%2$tb-%2$tY", start, end);
                fxRates.setColumnValues("date", end.getTime());
                try (Table avgFxRates = fxRates.calcByGroup("date, from_ccy, from_ccy_id", "rate", EnumColumnOperation.Average)) {
                    avgFxRates.setColumnName(3, "rate"); // calcByGroup changes column name
                    return avgFxRates.cloneData();
                }
            }
            return fxRates.cloneData();
        }
    }

    /**
     * Get the fx rates for the date range.
     * 
     * @param session Current session
     * @param startDate Start date
     * @param endDate End date
     * @return fx rates
     */
    private Table getFxSpotRatesForDateRange(Session session, Calendar startDate, Calendar endDate) {
        Calendar closingDate = (Calendar) startDate.clone();
        Logging.info("Getting fx spot rates for date range %1$td-%1$tb-%1$tY to %2$td-%2$tb-%2$tY", startDate, endDate);
        try (Market market = session.getMarket();
             Table fxRates = session.getTableFactory().createTable();
             Table fxInvertedRates = session.getTableFactory().createTable()) {

            do {
                getFxSpotRatesForDate(market, closingDate.getTime(), fxRates, fxInvertedRates);
                closingDate.add(Calendar.DATE, 1);
            } while (closingDate.before(endDate) || closingDate.equals(endDate));

            removeBadRates(fxRates);

            addCurrencyId(session, fxRates);

            fxRates.convertColumns("DateTime[date]");

            return fxRates.cloneData();
        }
    }

    /**
     * Get the fx rates for the closing date.
     * 
     * @param market Market
     * @param date Closing date
     * @param fxRates Table to populate with standard rates
     * @param fxInvertedRates Table to populate with inverted rates
     */
    private void getFxSpotRatesForDate(Market market, Date date, Table fxRates, Table fxInvertedRates) {
        try (Table mktFxRates = market.getFXSpotRateTableByBmo(date, EnumBmo.Mid)) {
            fxRates.select(mktFxRates, "SpotDate->date, Commodity->from_ccy,  SpotRate->rate", "[In.Terms] == 'USD'");
            fxInvertedRates.select(mktFxRates, "SpotDate->date, Terms->from_ccy,  SpotRate->rate", "[In.Commodity] == 'USD'");
            mergeInvertedRates(fxRates, fxInvertedRates);
            TableRow row = fxRates.addRow();
            row.setValues(new Object[] {date, "USD", 1.0});
            fxInvertedRates.clearData();
        }
    }

    /**
     * Merge the inverted rates into the main fx rates table switching the to/from currency and 'inverting' the rate.
     * 
     * @param fxRates Main fx table
     * @param fxInvertedRates Inverted rates table
     */
    private void mergeInvertedRates(Table fxRates, Table fxInvertedRates) {
        for (TableRow row : fxInvertedRates.getRows()) {
            TableRow newRow = fxRates.addRow();
            newRow.setValues(new Object[] {
                    row.getDate("date"),
                    row.getString("from_ccy"),
                    1 / row.getDouble("rate")
            });
        }
    }

    /**
     * Remove any 'bad' rates i.e. those that are less than 0.000001 as standard or when inverted.
     * 
     * @param fxRates FX rates table
     */
    private void removeBadRates(Table fxRates) {
        // Add a row id to ensure we remove the correct row
        fxRates.addColumn("rowid", EnumColType.Int);
        for (TableRow row : fxRates.getRows()) {
            fxRates.setInt("rowid", row.getNumber(), row.getNumber());
        }

        // Clone main table as cannot delete rows from table being iterated over
        try (Table clone = fxRates.cloneData()) {
            for (TableRow row : clone.getRows()) {
                double rate = row.getDouble("rate");
                if (rate == 0.0 || rate < 0.000001 || (1 / rate) < 0.000001) {
                    // Rate is bad so find row in main table and remove it
                    int rowid = row.getInt("rowid");
                    rowid = fxRates.findSorted(fxRates.getColumnId("rowid"), rowid, 0);
                    fxRates.removeRow(rowid);
                }
            }
        }
        fxRates.removeColumn("rowid");
    }

    /**
     * Add the currency id for the currencies.
     * 
     * @param session Current session
     * @param fxRates FX rate table
     */
    private void addCurrencyId(Session session, Table fxRates) {
        try (Table refTable = session.getStaticDataFactory().getTable(EnumReferenceTable.Currency)) {
               fxRates.select(refTable, "id->from_ccy_id", "[In.label] == [Out.from_ccy]");
           }
    }
}
