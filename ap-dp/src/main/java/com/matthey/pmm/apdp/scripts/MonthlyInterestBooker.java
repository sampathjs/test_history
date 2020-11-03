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
import com.olf.openrisk.staticdata.EnumReferenceTable;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ScriptCategory(EnumScriptCategory.Generic)
public class MonthlyInterestBooker extends EnhancedGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(MonthlyInterestBooker.class);
    
    @Override
    protected void run(Context context, ConstTable constTable) {
        LocalDate currentDate = getCurrentDate(context);
        LocalDate startDate = currentDate.minusMonths(1).withDayOfMonth(1);
        LocalDate endDate = currentDate.withDayOfMonth(1).minusDays(1);
        Map<String, List<DailyInterest>> interests = getInterests(context, startDate, endDate).stream()
                .collect(Collectors.groupingBy(DailyInterest::customer));
        for (String customer : interests.keySet()) {
            double monthlyInterest = interests.get(customer).stream().mapToDouble(DailyInterest::interest).sum();
            if (monthlyInterest < 0) {
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
        logger.info("sql for retrieving interests: " + System.lineSeparator() + sql);
        try (Table rawData = context.getIOFactory().runSQL(sql)) {
            TableFormatter tableFormatter = rawData.getFormatter();
            tableFormatter.setColumnFormatter("customer_id",
                                              tableFormatter.createColumnFormatterAsRef(EnumReferenceTable.Party));
            return rawData.getRows()
                    .stream()
                    .map(row -> ImmutableDailyInterest.builder()
                            .customer(row.getCell("customer_id").getDisplayString())
                            .date(fromDate(row.getDate("run_date")))
                            .interest(row.getDouble("interest_charge"))
                            .build())
                    .collect(Collectors.toSet());
        }
    }
    
    private void bookCashDeal(Context context, String customer, double monthlyInterest) {
        logger.info("booking transaction for {} with interest {}", customer, monthlyInterest);
        TradingFactory tradingFactory = context.getTradingFactory();
        try (Instrument ins = tradingFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, "USD");
             Transaction cash = tradingFactory.createTransaction(ins)) {
            LocalDate settleDate = getCurrentDate(context).plusMonths(1).withDayOfMonth(1).minusDays(1);
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
    
    private LocalDate getCurrentDate(Context context) {
        return fromDate(context.getTradingDate());
    }
    
    private LocalDate fromDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
