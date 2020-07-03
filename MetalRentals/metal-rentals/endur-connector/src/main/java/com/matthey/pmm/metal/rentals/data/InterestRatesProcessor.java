package com.matthey.pmm.metal.rentals.data;

import com.google.common.collect.Lists;
import com.matthey.pmm.metal.rentals.ImmutableInterestRate;
import com.matthey.pmm.metal.rentals.InterestRate;
import com.matthey.pmm.metal.rentals.RunResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.GridPoints;
import com.olf.openrisk.market.Index;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InterestRatesProcessor {

    private static final Logger logger = LogManager.getLogger(InterestRatesProcessor.class);
    private static final String REQUEST_TABLE = "USER_metal_rentals_rates";
    private static final String USER_COL = "run_user";
    private static final String ID_COL = "run_time";
    private static final String INDEX_COL = "index_name";
    private static final String RESULT_COL = "result";
    private static final List<String> metals = Lists.newArrayList("XPT",
                                                                  "XPD",
                                                                  "XRH",
                                                                  "XAU",
                                                                  "XAG",
                                                                  "XIR",
                                                                  "XOS",
                                                                  "XRU");

    private final Session session;

    public InterestRatesProcessor(Session session) {
        this.session = session;
    }

    public Set<InterestRate> retrieve(String indexName) {
        Index index = getIndexWithUniversalDataset(indexName);
        HashSet<InterestRate> interestRates = new HashSet<>();
        for (TableRow row : index.getOutputTable().getRows()) {
            InterestRate interestRate = ImmutableInterestRate.of(row.getString("Name"), row.getDouble("Mid"));
            interestRates.add(interestRate);
        }
        return interestRates;
    }

    public Index getIndexWithUniversalDataset(String indexName) {
        Market market = session.getMarket();
        market.refresh(true, true);
        Index index = market.getIndex(indexName);
        index.loadUniversal();
        return index;
    }

    public void recordRequest(String user, String indexName, Set<InterestRate> interestRates) {
        try (UserTable updateRequests = session.getIOFactory().getUserTable(REQUEST_TABLE);
             Table changes = updateRequests.retrieveTable().cloneStructure()) {
            TableRow row = changes.addRow();
            row.getCell(USER_COL).setString(user);
            DateTime runTime = new DateTime(DateTimeZone.UTC);
            row.getCell(ID_COL).setString(runTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            row.getCell(INDEX_COL).setString(indexName);
            for (InterestRate interestRate : interestRates) {
                row.getCell(interestRate.metal()).setDouble(interestRate.rate());
            }
            updateRequests.insertRows(changes);
        }
    }

    public void update() {
        try (UserTable updateRequests = session.getIOFactory().getUserTable(REQUEST_TABLE);
             Table changes = updateRequests.retrieveTable().cloneStructure()) {
            Table requestTable = updateRequests.retrieveTable();
            requestTable.sort(ID_COL);
            int rowId = requestTable.getRowCount() - 1;
            changes.addRow();
            changes.copyRowData(requestTable, rowId, 0);

            try {
                String indexName = requestTable.getString(INDEX_COL, rowId);
                Index index = getIndexWithUniversalDataset(indexName);
                GridPoints gridPoints = index.getGridPoints();
                for (String metal : metals) {
                    gridPoints.getGridPoint(metal)
                            .getInputField(EnumGptField.UserInput)
                            .setValue(requestTable.getDouble(metal, rowId));
                }
                index.saveUniversal();
                changes.setString(RESULT_COL, 0, RunResult.Successful.name());
            } catch (Exception e) {
                logger.error("failed to update interest rates: " + e.getMessage());
                changes.setString(RESULT_COL, 0, RunResult.Failed.name());
            } finally {
                updateRequests.updateRows(changes, ID_COL);
            }
        }
    }
}
