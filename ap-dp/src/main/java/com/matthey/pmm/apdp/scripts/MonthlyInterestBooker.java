package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.matthey.pmm.apdp.interests.DailyInterest;
import com.matthey.pmm.apdp.interests.ImmutableDailyInterest;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.text.StringSubstitutor;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.ScriptHelper.fromDate;
import static com.matthey.pmm.ScriptHelper.getTradingDate;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Party;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@ScriptCategory(EnumScriptCategory.Generic)
public class MonthlyInterestBooker extends EnhancedGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(MonthlyInterestBooker.class);
    
    @Override
    protected void run(Context context, ConstTable constTable) {
        LocalDate currentDate = getTradingDate(context);
        LocalDate startDate = currentDate.minusMonths(1).withDayOfMonth(1);
        LocalDate endDate = currentDate.withDayOfMonth(1).minusDays(1);
        Map<String, List<DailyInterest>> interests = getInterests(context, startDate, endDate).stream()
                .collect(groupingBy(DailyInterest::customer));
        for (String customer : interests.keySet()) {
            double monthlyInterest = interests.get(customer).stream().mapToDouble(DailyInterest::interest).sum();
            if (Math.abs(monthlyInterest) > 0.01) {
                bookCashDeal(context, customer, monthlyInterest);
            }
        }
    }
    
    private Set<DailyInterest> getInterests(Context context, LocalDate startDate, LocalDate endDate) {
        //language=TSQL
        String sqlTemplate = "SELECT customer_id, run_date, interest_charge\n" +
                             "    FROM USER_jm_ap_dp_interest_charges\n" +
                             "    WHERE effective = 'Y'\n" +
                             "      AND run_date BETWEEN '${startDate}' AND '${endDate}'";
        Map<String, Object> variables = ImmutableMap.of("startDate", startDate, "endDate", endDate);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving interests:{}{}", System.lineSeparator(), sql);
        
        IOFactory ioFactory = context.getIOFactory();
        try (Table rawData = ioFactory.runSQL(sql)) {
            TableFormatter tableFormatter = rawData.getFormatter();
            tableFormatter.setColumnFormatter("customer_id", tableFormatter.createColumnFormatterAsRef(Party));
            rawData.exportCsv(Paths.get(ioFactory.getReportDirectory(), "AP DP Monthly Interest.csv").toString(), true);
            return rawData.getRows()
                    .stream()
                    .map(row -> ImmutableDailyInterest.builder()
                            .customer(row.getCell("customer_id").getDisplayString())
                            .date(fromDate(row.getDate("run_date")))
                            .interest(row.getDouble("interest_charge"))
                            .build())
                    .collect(toSet());
        }
    }
    
    private void bookCashDeal(Context context, String customer, double monthlyInterest) {
        logger.info("booking transaction for {} with interest {}", customer, monthlyInterest);
        TradingFactory tradingFactory = context.getTradingFactory();
        try (Instrument ins = tradingFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, "USD");
             Transaction cash = tradingFactory.createTransaction(ins)) {
            LocalDate settleDate = getTradingDate(context).plusMonths(1).withDayOfMonth(1).minusDays(1);
            cash.setValue(EnumTransactionFieldId.CashflowType, "Interest");
            cash.setValue(EnumTransactionFieldId.ReferenceString, "AP/DP Interest " + settleDate);
            cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, "JM PMM HK");
            cash.setValue(EnumTransactionFieldId.InternalPortfolio, "HK Fees");
            cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, customer);
            cash.setValue(EnumTransactionFieldId.SettleDate, java.sql.Date.valueOf(settleDate));
            cash.setValue(EnumTransactionFieldId.Position, monthlyInterest);
            cash.process(EnumTranStatus.Validated);
            logger.info("cash transaction #{} booked for {}", cash.getTransactionId(), customer);
        }
    }
}
