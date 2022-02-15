package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.matthey.pmm.apdp.reports.AccountBalanceDetails;
import com.matthey.pmm.apdp.reports.CustomerDetails;
import com.matthey.pmm.apdp.reports.FXSellDeal;
import com.matthey.pmm.apdp.reports.ImmutableAccountBalanceDetails;
import com.matthey.pmm.apdp.reports.ImmutableCustomerDetails;
import com.matthey.pmm.apdp.reports.ImmutableFXSellDeal;
import com.matthey.pmm.apdp.reports.ReportGenerator;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.text.StringSubstitutor;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import static com.olf.openrisk.staticdata.EnumReferenceTable.TransStatus;
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
        Map<String, List<FXSellDeal>> fxSellDeals = getAllFXSellDeals(startDate, endDate, context, hkdFxRates);
        
        Set<AccountBalanceDetails> allAccountBalanceDetails = getAllAccountBalanceDetails(startDate,
                                                                                          endDate,
                                                                                          context,
                                                                                          metalPrices,
                                                                                          hkdFxRates);
        Set<CustomerDetails> customerDetailsSet = allAccountBalanceDetails.stream()
                .collect(groupingBy(AccountBalanceDetails::customer))
                .entrySet()
                .stream()
                .map(entry -> toCustomerDetails(entry.getKey(), entry.getValue(), fxSellDeals.get(entry.getKey())))
                .collect(toSet());
        
        String path = Paths.get(context.getIOFactory().getReportDirectory(), "AP DP Year End Report.xlsx").toString();
        new ReportGenerator(sort(allAccountBalanceDetails),
                            new ArrayList<>(customerDetailsSet),
                            flattenAndSort(fxSellDeals),
                            path).generate();
    }
    
    private List<AccountBalanceDetails> sort(Set<AccountBalanceDetails> allAccountBalanceDetails) {
        return allAccountBalanceDetails.stream()
                .sorted(Comparator.comparingLong(AccountBalanceDetails::eventNum))
                .collect(toList());
    }
    
    private List<FXSellDeal> flattenAndSort(Map<String, List<FXSellDeal>> fxSellDeals) {
        return fxSellDeals.values()
                .stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(FXSellDeal::externalBU).thenComparing(FXSellDeal::dealNum))
                .collect(toList());
    }
    
    private CustomerDetails toCustomerDetails(String customer,
                                              List<AccountBalanceDetails> detailsList,
                                              List<FXSellDeal> fxSellDeals) {
        return ImmutableCustomerDetails.builder()
                .customer(customer)
                .deferredValueUsd(sum(detailsList, AccountBalanceDetails::isDeferred, AccountBalanceDetails::usdValue))
                .pricedValueUsd(fxSellDeals.stream().mapToDouble(FXSellDeal::usdAmount).sum())
                .deferredAmount(sum(detailsList,
                                    AccountBalanceDetails::isDeferred,
                                    AccountBalanceDetails::settleAmount))
                .pricedAmount(sum(detailsList, AccountBalanceDetails::isPriced, AccountBalanceDetails::settleAmount))
                .deferredValueHkd(sum(detailsList, AccountBalanceDetails::isDeferred, AccountBalanceDetails::hkdValue))
                .pricedValueHkd(fxSellDeals.stream().mapToDouble(FXSellDeal::hkdAmount).sum())
                .build();
    }
    
    private double sum(Collection<AccountBalanceDetails> details,
                       Predicate<AccountBalanceDetails> filter,
                       ToDoubleFunction<AccountBalanceDetails> mapper) {
        return details.stream().filter(filter).mapToDouble(mapper).sum();
    }
    
    private Set<AccountBalanceDetails> getAllAccountBalanceDetails(LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Context context,
                                                                   Map<String, Map<LocalDate, Double>> closingPrices,
                                                                   Map<LocalDate, Double> hkdFxRates) {
        //language=TSQL
        String sqlTemplate = "SELECT nadv.deal_tracking_num,\n" +
                             "       nadv.event_num,\n" +
                             "       nadv.event_date,\n" +
                             "       nadv.ohd_position,\n" +
                             "       nadv.ohd_applied_amount,\n" +
                             "       nadv.account_id,\n" +
                             "       nadv.currency_id,\n" +
                             "       a.holder_id\n" +
                             "    FROM nostro_account_detail_view nadv\n" +
                             "             JOIN account a\n" +
                             "                  ON nadv.account_id = a.account_id\n" +
                             "    WHERE a.account_name LIKE 'PMM HK DEFERRED%'\n" +
                             "      AND nadv.nostro_flag = 1\n" +
                             "      AND nadv.event_date BETWEEN '${startDate}' AND '${endDate}'\n";
        Map<String, Object> variables = ImmutableMap.of("startDate", startDate, "endDate", endDate);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving account balances:{}{}", System.lineSeparator(), sql);
        
        try (Table rawData = context.getIOFactory().runSQL(sql)) {
            TableFormatter tableFormatter = rawData.getFormatter();
            tableFormatter.setColumnFormatter("currency_id", tableFormatter.createColumnFormatterAsRef(Currency));
            tableFormatter.setColumnFormatter("account_id", tableFormatter.createColumnFormatterAsRef(Account));
            tableFormatter.setColumnFormatter("holder_id", tableFormatter.createColumnFormatterAsRef(Party));
            
            return rawData.getRows().stream().map(row -> {
                String metal = row.getCell("currency_id").getDisplayString();
                LocalDate eventDate = fromDate(row.getDate("event_date"));
                return ImmutableAccountBalanceDetails.builder()
                        .dealNum(row.getInt("deal_tracking_num"))
                        .eventNum(row.getLong("event_num"))
                        .eventDate(eventDate)
                        .metal(metal)
                        .internalAccount(row.getCell("account_id").getDisplayString())
                        .settleAmount(row.getDouble("position"))
                        .actualAmount(row.getDouble("applied_amount"))
                        .metalPrice(closingPrices.get(metal).get(eventDate))
                        .hkdFxRate(hkdFxRates.get(eventDate))
                        .customer(row.getCell("holder_id").getDisplayString())
                        .build();
            }).collect(toSet());
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
    
    private Map<String, List<FXSellDeal>> getAllFXSellDeals(LocalDate startDate,
                                                            LocalDate endDate,
                                                            Context context,
                                                            Map<LocalDate, Double> hkdFxRates) {
        //language=TSQL
        String sqlTemplate = "SELECT t.tran_num\n" +
                             "    FROM ab_tran t\n" +
                             "             JOIN ab_tran_info_view i\n" +
                             "                  ON t.tran_num = i.tran_num AND i.type_name = 'Pricing Type'\n" +
                             "    WHERE t.toolset = ${fxToolset}\n" +
                             "      AND t.buy_sell = ${sell}\n" +
                             "      AND i.value = 'DP'\n" +
                             "      AND t.current_flag = 1\n" +
                             "      AND t.tran_status IN (${validated}, ${matured})\n" +
                             "      AND t.trade_date BETWEEN '${startDate}' AND '${endDate}'\n";
        StaticDataFactory staticDataFactory = context.getStaticDataFactory();
        int fxToolset = staticDataFactory.getId(EnumReferenceTable.Toolsets, "FX");
        int sell = staticDataFactory.getId(EnumReferenceTable.BuySell, "Sell");
        int validated = staticDataFactory.getId(TransStatus, "Validated");
        int matured = staticDataFactory.getId(TransStatus, "Matured");
        Map<String, Object> variables = new HashMap<>();
        variables.put("startDate", startDate);
        variables.put("endDate", endDate);
        variables.put("validated", validated);
        variables.put("fxToolset", fxToolset);
        variables.put("sell", sell);
        variables.put("matured", matured);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving closing prices:{}{}", System.lineSeparator(), sql);
        
        Currency hkd = staticDataFactory.getReferenceObject(Currency.class, "HKD");
        Currency usd = staticDataFactory.getReferenceObject(Currency.class, "USD");
        double defaultHKDFxRate = 1 / context.getMarket().getFXSpotRate(hkd, usd);
        try (Table deals = context.getIOFactory().runSQL(sql)) {
            return deals.getRows().stream().map(row -> {
                int tranNum = row.getInt(0);
                Transaction transaction = context.getTradingFactory().retrieveTransactionById(tranNum);
                LocalDate settleDate = fromDate(transaction.getValueAsDate(EnumTransactionFieldId.SettleDate));
                
                return ImmutableFXSellDeal.builder()
                        .dealNum(transaction.getDealTrackingId())
                        .reference(transaction.getValueAsString(EnumTransactionFieldId.ReferenceString))
                        .externalBU(transaction.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit))
                        .metal(transaction.getValueAsString(EnumTransactionFieldId.FxBaseCurrency))
                        .tradeDate(fromDate(transaction.getValueAsDate(EnumTransactionFieldId.TradeDate)))
                        .settleDate(settleDate)
                        .position(transaction.getValueAsDouble(EnumTransactionFieldId.FxDealtAmount))
                        .price(transaction.getField("Trade Price").getValueAsDouble())
                        .hkdFxRate(hkdFxRates.getOrDefault(settleDate, defaultHKDFxRate))
                        .build();
            }).collect(groupingBy(FXSellDeal::externalBU));
        }
    }
}
