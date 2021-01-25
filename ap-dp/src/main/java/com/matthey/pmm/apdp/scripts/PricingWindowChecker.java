package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.matthey.pmm.apdp.pricing.window.AlertGenerator;
import com.matthey.pmm.apdp.pricing.window.CheckResult;
import com.matthey.pmm.apdp.pricing.window.CheckResultCreator;
import com.matthey.pmm.apdp.pricing.window.DeferredPositionKey;
import com.matthey.pmm.apdp.pricing.window.ImmutableDeferredPositionKey;
import com.matthey.pmm.apdp.pricing.window.ImmutablePricingWindow;
import com.matthey.pmm.apdp.pricing.window.ImmutablePricingWindowKey;
import com.matthey.pmm.apdp.pricing.window.ImmutableUnmatchedDeal;
import com.matthey.pmm.apdp.pricing.window.PricingWindow;
import com.matthey.pmm.apdp.pricing.window.PricingWindowKey;
import com.matthey.pmm.apdp.pricing.window.UnmatchedDeal;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.text.StringSubstitutor;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.matthey.pmm.ScriptHelper.fromDate;
import static com.matthey.pmm.ScriptHelper.getTradingDate;
import static com.matthey.pmm.apdp.Metals.METAL_NAMES;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Currency;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Instruments;
import static com.olf.openrisk.staticdata.EnumReferenceTable.OffsetTranType;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Party;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Portfolio;
import static com.olf.openrisk.staticdata.EnumReferenceTable.TransStatus;
import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@ScriptCategory(EnumScriptCategory.Generic)
public class PricingWindowChecker extends EnhancedGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(PricingWindowChecker.class);
    
    @Override
    protected void run(Context context, ConstTable constTable) {
        LocalDate currentDate = getTradingDate(context);
        logger.info("current date: {}", currentDate);
        Map<PricingWindowKey, Integer> pricingWindows = retrievePricingWindows(context);
        logger.info("pricing windows: {}", pricingWindows);
        Map<DeferredPositionKey, Double> deferredPosition = retrieveDeferredPosition(context);
        logger.info("deferred position: {}", deferredPosition);
        Set<UnmatchedDeal> dpUnmatchedDeals = retrieveDPUnmatchedDeals(context, deferredPosition);
        logger.info("DP unmatched deals: {}", dpUnmatchedDeals);
        Set<UnmatchedDeal> apUnmatchedDeals = retrieveAPUnmatchedDeals(context);
        logger.info("AP unmatched deals: {}", apUnmatchedDeals);
        Set<UnmatchedDeal> deals = Sets.union(apUnmatchedDeals, dpUnmatchedDeals);
        
        StaticDataFactory staticDataFactory = context.getStaticDataFactory();
        Function<Integer, String> customerNameGetter = id -> staticDataFactory.getName(Party, id);
        CheckResultCreator checkResultCreator = new CheckResultCreator(currentDate, customerNameGetter, pricingWindows);
        Set<CheckResult> results = deals.stream()
                .map(checkResultCreator::from)
                .filter(checkResultCreator::needAlert)
                .collect(toSet());
        logger.info("expiring deals: {}", results);
        
        saveCheckResult(context, results, currentDate);
        
        Set<String> emails = retrieveEmails(context);
        logger.info("email addresses for the alert: {}", emails);
        BiConsumer<String, String> emailSender = (subject, htmlContent) -> sendEmail(subject, htmlContent, emails);
        new AlertGenerator(emailSender).generateAndSendAlert(currentDate, LocalDateTime.now(), results);
    }
    
    private void saveCheckResult(Context context, Set<CheckResult> results, LocalDate currentDate) {
        try (UserTable userTable = context.getIOFactory().getUserTable("USER_jm_ap_dp_reporting");
             Table newRows = userTable.retrieveTable().cloneStructure()) {
            for (CheckResult result : results) {
                TableRow newRow = newRows.addRow();
                newRow.getCell("run_date").setString(currentDate.toString());
                newRow.getCell("pricing_type").setString(result.pricingType());
                newRow.getCell("deal_num").setInt(Integer.parseInt(result.dealNum()));
                newRow.getCell("customer").setString(result.customer());
                newRow.getCell("deal_date").setString(result.dealDate());
                newRow.getCell("expiry_date").setString(result.expiryDate());
                newRow.getCell("days_to_expiry").setInt(result.numOfDaysToExpiry());
                newRow.getCell("open_toz").setDouble(result.unmatchedVolume());
            }
            newRows.setColumnValues("last_update", new Date());
            userTable.insertRows(newRows);
        }
    }
    
    private Map<PricingWindowKey, Integer> retrievePricingWindows(Context context) {
        try (Table rawData = context.getIOFactory().getUserTable("USER_jm_ap_dp_pricingwindow").retrieveTable()) {
            return rawData.getRows()
                    .stream()
                    .map(row -> ImmutablePricingWindow.builder()
                            .pricingWindowKey(ImmutablePricingWindowKey.of(row.getInt("customer_id"),
                                                                           row.getString("pricing_type"),
                                                                           row.getInt("metal_ccy_id")))
                            .numOfDays(row.getInt("pricing_window"))
                            .build())
                    .collect(toMap(PricingWindow::pricingWindowKey, PricingWindow::numOfDays));
        }
    }
    
    private Set<UnmatchedDeal> retrieveAPUnmatchedDeals(Context context) {
        //language=TSQL
        String sql = "SELECT deal_num, volume_left_in_toz\n" +
                     "    FROM (SELECT * FROM user_jm_ap_buy_dispatch_deals UNION SELECT * FROM user_jm_ap_sell_deals) deals\n" +
                     "    WHERE match_status IN ('P', 'N')";
        logger.info("sql for retrieving ap unmatched deals:{}{}", System.lineSeparator(), sql);
        try (Table result = context.getIOFactory().runSQL(sql)) {
            Map<Integer, Double> unmatchedDeals = result.getRows()
                    .stream()
                    .collect(toMap(row -> row.getInt("deal_num"), row -> row.getDouble("volume_left_in_toz")));
            return unmatchedDeals.entrySet()
                    .stream()
                    .map(entry -> fromTransaction(context.getTradingFactory().retrieveTransaction(entry.getKey()),
                                                  entry.getValue(),
                                                  context.getStaticDataFactory()))
                    .collect(toSet());
        }
    }
    
    private UnmatchedDeal fromTransaction(Transaction transaction,
                                          double unmatchedVolume,
                                          StaticDataFactory staticDataFactory) {
        int dealNum = transaction.getDealTrackingId();
        LocalDate dealDate = fromDate(transaction.getValueAsDate(EnumTransactionFieldId.StartDate));
        String pricingType = transaction.getField("Pricing Type").getValueAsString();
        int customerId = pricingType.equals("DP") &&
                         transaction.getValueAsString(EnumTransactionFieldId.InstrumentType).equals("COMM-PHYS")
                         ? staticDataFactory.getId(Party, transaction.getField("Consignee").getValueAsString())
                         : transaction.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
        EnumToolset toolset = transaction.getToolset();
        int metalId = EnumToolset.Fx.equals(toolset)
                      ? transaction.getValueAsInt(EnumTransactionFieldId.FxBaseCurrency)
                      : staticDataFactory.getId(Currency, METAL_NAMES.get(getMetalForDispatch(transaction)));
        return ImmutableUnmatchedDeal.builder()
                .dealNum(dealNum)
                .dealDate(dealDate)
                .pricingWindowKey(ImmutablePricingWindowKey.of(customerId, pricingType, metalId))
                .unmatchedVolume(unmatchedVolume)
                .build();
    }
    
    private String getMetalForDispatch(Transaction transaction) {
        return transaction.getLeg(1).getValueAsString(EnumLegFieldId.CommoditySubGroup);
    }
    
    private Set<String> retrieveEmails(Context context) {
        //language=TSQL
        String sql = "SELECT DISTINCT(p.email)\n" +
                     "    FROM personnel p\n" +
                     "             JOIN personnel_functional_group pf\n" +
                     "                  ON p.id_number = pf.personnel_id\n" +
                     "             JOIN functional_group f\n" +
                     "                  ON f.id_number = pf.func_group_id\n" +
                     "    WHERE f.name = 'AP DP Reporting'\n";
        try (Table result = context.getIOFactory().runSQL(sql)) {
            return result.getRows().stream().map(row -> row.getString(0)).collect(toSet());
        }
    }
    
    private void sendEmail(String subject, String htmlContent, Set<String> emails) {
        final String host = System.getenv("AB_EMAIL_SMTP_HOST");
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        final InternetAddress fromAddress = sneak(() -> new InternetAddress("apdp.reporting@matthey.com"));
        
        for (String email : emails) {
            try {
                MimeMessage message = new MimeMessage(Session.getDefaultInstance(properties));
                message.setFrom(fromAddress);
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                message.setSubject(subject);
                MimeBodyPart mimeBodyPart = new MimeBodyPart();
                mimeBodyPart.setContent(htmlContent, "text/html");
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(mimeBodyPart);
                message.setContent(multipart);
                Transport.send(message);
            } catch (Exception e) {
                logger.error("error occurred when sending email to {}", email, e);
            }
        }
    }
    
    private Map<DeferredPositionKey, Double> retrieveDeferredPosition(Context context) {
        //language=TSQL
        String sqlTemplate = "SELECT n.currency_id, n.position, a.holder_id\n" +
                             "    FROM nostro_account_position n\n" +
                             "             JOIN account a\n" +
                             "                  ON n.account_id = a.account_id AND a.account_name LIKE 'PMM HK DEFERRED%'\n" +
                             "    WHERE n.portfolio_id = ${hkPhysical}\n";
        int hkPhysical = context.getStaticDataFactory().getId(Portfolio, "HK Physical");
        Map<String, Object> variables = ImmutableMap.of("hkPhysical", hkPhysical);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving deferred position:{}{}", System.lineSeparator(), sql);
        try (Table result = context.getIOFactory().runSQL(sql)) {
            TableFormatter tableFormatter = result.getFormatter();
            tableFormatter.setColumnFormatter("currency_id", tableFormatter.createColumnFormatterAsRef(Currency));
            tableFormatter.setColumnFormatter("holder_id", tableFormatter.createColumnFormatterAsRef(Party));
            return result.getRows().stream().collect(toMap(this::fromRow, row -> Math.abs(row.getDouble("position"))));
        }
    }
    
    private DeferredPositionKey fromRow(TableRow row) {
        return ImmutableDeferredPositionKey.of(row.getCell("holder_id").getDisplayString(),
                                               row.getCell("currency_id").getDisplayString());
    }
    
    private Set<UnmatchedDeal> retrieveDPUnmatchedDeals(Context context,
                                                        Map<DeferredPositionKey, Double> deferredPosition) {
        //language=TSQL
        String sqlTemplate = "SELECT t.deal_tracking_num,\n" +
                             "       t.currency,\n" +
                             "       t.position,\n" +
                             "       t.trade_date,\n" +
                             "       c.value AS customer\n" +
                             "    FROM ab_tran t\n" +
                             "             JOIN ab_tran_info_view pt\n" +
                             "                  ON t.tran_num = pt.tran_num AND pt.type_name = 'Pricing Type'\n" +
                             "             JOIN ab_tran_info_view c\n" +
                             "                  ON t.tran_num = c.tran_num AND c.type_name = 'Consignee'\n" +
                             "    WHERE t.ins_type = ${commPhys}\n" +
                             "      AND t.tran_status = ${validated}\n" +
                             "      AND t.offset_tran_type = ${originalOffset}\n" +
                             "      AND pt.value = 'DP'\n" +
                             "    ORDER BY t.deal_tracking_num DESC";
        StaticDataFactory staticDataFactory = context.getStaticDataFactory();
        int commPhys = staticDataFactory.getId(Instruments, "COMM-PHYS");
        int validated = staticDataFactory.getId(TransStatus, "Validated");
        int originalOffset = staticDataFactory.getId(OffsetTranType, "Original Offset");
        Map<String, Object> variables = ImmutableMap.of("commPhys",
                                                        commPhys,
                                                        "validated",
                                                        validated,
                                                        "originalOffset",
                                                        originalOffset);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving DP deals:{}{}", System.lineSeparator(), sql);
        try (Table result = context.getIOFactory().runSQL(sql)) {
            TableFormatter tableFormatter = result.getFormatter();
            tableFormatter.setColumnFormatter("currency", tableFormatter.createColumnFormatterAsRef(Currency));
            
            Map<DeferredPositionKey, List<UnmatchedDeal>> deals = deferredPosition.keySet()
                    .stream()
                    .collect(toMap(identity(), key -> new ArrayList<>()));
            for (TableRow row : result.getRows()) {
                String customer = row.getString("customer");
                String metal = row.getCell("currency").getDisplayString();
                DeferredPositionKey key = ImmutableDeferredPositionKey.of(customer, metal);
                if (!deferredPosition.containsKey(key)) {
                    continue;
                }
                double position = deferredPosition.get(key);
                double matchedVolume = deals.get(key).stream().mapToDouble(UnmatchedDeal::unmatchedVolume).sum();
                if (position > matchedVolume) {
                    int dealNum = row.getInt("deal_tracking_num");
                    LocalDate dealDate = fromDate(row.getDate("trade_date"));
                    int customerId = staticDataFactory.getId(Party, row.getString("customer"));
                    int metalId = row.getInt("currency");
                    double dealPosition = Math.abs(row.getDouble("position"));
                    UnmatchedDeal deal = ImmutableUnmatchedDeal.builder()
                            .dealNum(dealNum)
                            .dealDate(dealDate)
                            .pricingWindowKey(ImmutablePricingWindowKey.of(customerId, "DP", metalId))
                            .unmatchedVolume(Math.min(dealPosition, position - matchedVolume))
                            .build();
                    deals.get(key).add(deal);
                }
            }
            
            return deals.values().stream().flatMap(List::stream).collect(toSet());
        }
    }
}
