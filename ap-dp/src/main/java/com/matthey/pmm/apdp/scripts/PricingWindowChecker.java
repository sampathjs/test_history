package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.matthey.pmm.apdp.pricing.window.AlertGenerator;
import com.matthey.pmm.apdp.pricing.window.CheckResult;
import com.matthey.pmm.apdp.pricing.window.CheckResultCreator;
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
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.matthey.pmm.ScriptHelper.fromDate;
import static com.matthey.pmm.ScriptHelper.getTradingDate;
import static com.matthey.pmm.apdp.Metals.METAL_NAMES;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Currency;
import static com.olf.openrisk.staticdata.EnumReferenceTable.Party;
import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
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
        Set<UnmatchedDeal> deals = retrieveUnmatchedDeals(context);
        logger.info("unmatched deals: {}", deals);
        
        StaticDataFactory staticDataFactory = context.getStaticDataFactory();
        Function<Integer, String> customerNameGetter = id -> staticDataFactory.getName(Party, id);
        CheckResultCreator checkResultCreator = new CheckResultCreator(currentDate, customerNameGetter, pricingWindows);
        Set<CheckResult> results = deals.stream()
                .map(checkResultCreator::from)
                .filter(checkResultCreator::needAlert)
                .collect(toSet());
        logger.info("expiring deals: {}", deals);
        
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
    
    private Set<UnmatchedDeal> retrieveUnmatchedDeals(Context context) {
        //language=TSQL
        String sql = "SELECT deal_num, volume_left_in_toz\n" +
                     "    FROM (SELECT * FROM user_jm_ap_buy_dispatch_deals UNION SELECT * FROM user_jm_ap_sell_deals) deals\n" +
                     "    WHERE match_status IN ('P', 'N')";
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
        int customerId = transaction.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
        String pricingType = transaction.getField("Pricing Type").getValueAsString();
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
}
