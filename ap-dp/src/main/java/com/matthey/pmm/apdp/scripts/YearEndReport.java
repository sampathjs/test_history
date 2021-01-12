package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.matthey.pmm.apdp.reports.AccountBalanceDetails;
import com.matthey.pmm.apdp.reports.CustomerDetails;
import com.matthey.pmm.apdp.reports.ImmutableAccountBalanceDetails;
import com.matthey.pmm.apdp.reports.ImmutableCustomerDetails;
import com.matthey.pmm.apdp.reports.ReportGenerator;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import org.apache.commons.text.StringSubstitutor;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import static com.matthey.pmm.ScriptHelper.fromDate;
import static com.matthey.pmm.ScriptHelper.getTradingDate;
import static com.matthey.pmm.apdp.Metals.METAL_NAMES;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Account;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Currency;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Party;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@ScriptCategory(EnumScriptCategory.Generic)
public class YearEndReport extends EnhancedGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(YearEndReport.class);
    
    @Override
    protected void run(Context context, ConstTable constTable) {
        LocalDate currentDate = getTradingDate(context);
        LocalDate startDate = LocalDate.of(currentDate.getYear() - (currentDate.getMonthValue() < 4 ? 1 : 0), 4, 1);
        LocalDate endDate = startDate.plusYears(1).minusDays(1);
        logger.info("reporting period: {} - {}", startDate, endDate);
        
        Map<String, Map<LocalDate, Double>> metalPrices = getMetalPrices(context, startDate, endDate);
        Map<LocalDate, Double> hkdFxRates = getClosingPrices(startDate, endDate, "FX_HKD.USD", "BLOOMBERG", context);
        
        Set<AccountBalanceDetails> allAccountBalanceDetails = getAllAccountBalanceDetails(startDate, endDate, context);
        Set<CustomerDetails> customerDetailsSet = allAccountBalanceDetails.stream()
                .collect(groupingBy(AccountBalanceDetails::customer))
                .entrySet()
                .stream()
                .map(entry -> toCustomerDetails(entry.getKey(), entry.getValue(), metalPrices, hkdFxRates))
                .collect(toSet());
        
        String path = Paths.get(context.getIOFactory().getReportDirectory(), "AP DP Year End Report.xlsx").toString();
        new ReportGenerator(sort(allAccountBalanceDetails), new ArrayList<>(customerDetailsSet), path).generate();
    }
    
    private List<AccountBalanceDetails> sort(Set<AccountBalanceDetails> allAccountBalanceDetails) {
        return allAccountBalanceDetails.stream()
                .sorted(Comparator.comparingLong(AccountBalanceDetails::eventNum))
                .collect(toList());
    }
    
    private CustomerDetails toCustomerDetails(String customer,
                                              List<AccountBalanceDetails> detailsList,
                                              Map<String, Map<LocalDate, Double>> closingPrices,
                                              Map<LocalDate, Double> hkdFxRates) {
        ToDoubleFunction<AccountBalanceDetails> priceGetter = d -> closingPrices.get(d.metal()).get(d.eventDate());
        ToDoubleFunction<AccountBalanceDetails> fxGetter = d -> hkdFxRates.get(d.eventDate());
        
        return ImmutableCustomerDetails.builder()
                .customer(customer)
                .deferredValueUsd(sum(detailsList,
                                      AccountBalanceDetails::isDeferred,
                                      d -> d.settleAmount() * priceGetter.applyAsDouble(d)))
                .pricedValueUsd(sum(detailsList,
                                    AccountBalanceDetails::isPriced,
                                    d -> d.settleAmount() * priceGetter.applyAsDouble(d)))
                .deferredAmount(sum(detailsList,
                                    AccountBalanceDetails::isDeferred,
                                    AccountBalanceDetails::settleAmount))
                .pricedAmount(sum(detailsList, AccountBalanceDetails::isPriced, AccountBalanceDetails::settleAmount))
                .deferredValueHkd(sum(detailsList,
                                      AccountBalanceDetails::isDeferred,
                                      d -> d.settleAmount() * priceGetter.applyAsDouble(d) * fxGetter.applyAsDouble(d)))
                .pricedValueHkd(sum(detailsList,
                                    AccountBalanceDetails::isPriced,
                                    d -> d.settleAmount() * priceGetter.applyAsDouble(d) * fxGetter.applyAsDouble(d)))
                .build();
    }
    
    private double sum(Collection<AccountBalanceDetails> details,
                       Predicate<AccountBalanceDetails> filter,
                       ToDoubleFunction<AccountBalanceDetails> mapper) {
        return details.stream().filter(filter).mapToDouble(mapper).sum();
    }
    
    private Set<AccountBalanceDetails> getAllAccountBalanceDetails(LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Context context) {
        //language=TSQL
        String sqlTemplate = "SELECT s.event_num,\n" +
                             "       s.accounting_date,\n" +
                             "       s.currency_id,\n" +
                             "       s.int_account_id,\n" +
                             "       s.settle_amount,\n" +
                             "       s.actual_amount,\n" +
                             "       s.ext_bunit_id\n" +
                             "    FROM ab_tran_event_settle s\n" +
                             "             JOIN account a\n" +
                             "                  ON s.int_account_id = a.account_id\n" +
                             "    WHERE a.account_name LIKE 'PMM HK DEFERRED%'\n" +
                             "      AND s.ext_account_id <> 20007\n" +
                             "      AND s.accounting_date BETWEEN '${startDate}' AND '${endDate}'";
        Map<String, Object> variables = ImmutableMap.of("startDate", startDate, "endDate", endDate);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving account balances:{}{}", System.lineSeparator(), sql);
        
        try (Table rawData = context.getIOFactory().runSQL(sql)) {
            TableFormatter tableFormatter = rawData.getFormatter();
            tableFormatter.setColumnFormatter("currency_id", tableFormatter.createColumnFormatterAsRef(Currency));
            tableFormatter.setColumnFormatter("int_account_id", tableFormatter.createColumnFormatterAsRef(Account));
            tableFormatter.setColumnFormatter("ext_bunit_id", tableFormatter.createColumnFormatterAsRef(Party));
            
            return rawData.getRows()
                    .stream()
                    .map(row -> ImmutableAccountBalanceDetails.builder()
                            .eventNum(row.getLong("event_num"))
                            .eventDate(fromDate(row.getDate("accounting_date")))
                            .metal(row.getCell("currency_id").getDisplayString())
                            .internalAccount(row.getCell("int_account_id").getDisplayString())
                            .settleAmount(row.getDouble("settle_amount"))
                            .actualAmount(row.getDouble("actual_amount"))
                            .customer(row.getCell("ext_bunit_id").getDisplayString())
                            .build())
                    .collect(toSet());
        }
    }
    
    Map<String, Map<LocalDate, Double>> getMetalPrices(Context context, LocalDate startDate, LocalDate endDate) {
        Function<String, Map<LocalDate, Double>> pricesGetter = metal -> getClosingPrices(startDate,
                                                                                          endDate,
                                                                                          metal + ".USD",
                                                                                          "JM HK Closing",
                                                                                          context);
        return METAL_NAMES.values().stream().collect(toMap(identity(), pricesGetter));
    }
    
    private Map<LocalDate, Double> getClosingPrices(LocalDate startDate,
                                                    LocalDate endDate,
                                                    String indexName,
                                                    String refSource,
                                                    Context context) {
        //language=TSQL
        String sqlTemplate = "SELECT ihp.reset_date, ihp.price\n" +
                             "    FROM idx_historical_prices ihp\n" +
                             "             JOIN idx_def id\n" +
                             "                  ON ihp.index_id = id.index_version_id\n" +
                             "             JOIN ref_source rs\n" +
                             "                  ON ihp.ref_source = rs.id_number\n" +
                             "    WHERE id.index_name = '${indexName}'\n" +
                             "      AND reset_date BETWEEN '${startDate}' AND '${endDate}'\n" +
                             "      AND rs.name = '${refSource}'";
        Map<String, Object> variables = ImmutableMap.of("startDate",
                                                        startDate,
                                                        "endDate",
                                                        endDate,
                                                        "indexName",
                                                        indexName,
                                                        "refSource",
                                                        refSource);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving closing prices:{}{}", System.lineSeparator(), sql);
        
        try (Table rawData = context.getIOFactory().runSQL(sql)) {
            return rawData.getRows()
                    .stream()
                    .collect(toMap(row -> fromDate(row.getDate("reset_date")),
                                   row -> row.getDouble("price"),
                                   (p1, p2) -> p2));
        }
    }
}
