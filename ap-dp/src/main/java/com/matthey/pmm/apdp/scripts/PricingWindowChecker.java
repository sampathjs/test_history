package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
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
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;

@ScriptCategory(EnumScriptCategory.Generic)
public class PricingWindowChecker extends EnhancedGenericScript {
    
    public static final Map<String, String> METAL_NAMES = ImmutableMap.of("Platinum",
                                                                          "XPT",
                                                                          "Palladium",
                                                                          "XPD",
                                                                          "Rhodium",
                                                                          "XRH");
    
    private static final Logger logger = EndurLoggerFactory.getLogger(PricingWindowChecker.class);
    
    @Override
    protected void run(Context context, ConstTable constTable) {
        LocalDate currentDate = getCurrentDate(context);
        logger.info("current date: {}", currentDate);
        Map<PricingWindowKey, Integer> pricingWindows = retrievePricingWindows(context);
        logger.info("pricing windows: {}", pricingWindows);
        Set<UnmatchedDeal> deals = retrieveUnmatchedDeals(context);
        logger.info("unmatched deals: {}", deals);
        
        StaticDataFactory staticDataFactory = context.getStaticDataFactory();
        Function<Integer, String> customerNameGetter = id -> staticDataFactory.getName(EnumReferenceTable.Party, id);
        CheckResultCreator checkResultCreator = new CheckResultCreator(currentDate, customerNameGetter, pricingWindows);
        Set<CheckResult> results = deals.stream()
                .map(checkResultCreator::from)
                .filter(checkResultCreator::needAlert)
                .collect(Collectors.toSet());
        logger.info("expiring deals: {}", deals);
        
        Set<String> emails = retrieveEmails(context);
        logger.info("email addresses for the alert: {}", emails);
        BiConsumer<String, String> emailSender = (subject, htmlContent) -> sendEmail(subject, htmlContent, emails);
        new AlertGenerator(emailSender).generateAndSendAlert(currentDate, LocalDateTime.now(), results);
    }
    
    private Map<PricingWindowKey, Integer> retrievePricingWindows(Context context) {
        try (Table rawData = context.getIOFactory().getUserTable("USER_jm_ap_dp_pricingwindow").retrieveTable()) {
            return rawData.getRows()
                    .stream()
                    .map(row -> ImmutablePricingWindow.builder()
                            .pricingWindowKey(ImmutablePricingWindowKey.of(Integer.parseInt(row.getString("customer_id")),
                                                                           row.getString("pricing_type"),
                                                                           row.getInt("metal_ccy_id")))
                            .numOfDays(row.getInt("pricing_window"))
                            .build())
                    .collect(Collectors.toMap(PricingWindow::pricingWindowKey, PricingWindow::numOfDays));
        }
    }
    
    private Set<UnmatchedDeal> retrieveUnmatchedDeals(Context context) {
        //language=TSQL
        String sql = "SELECT deal_num\n" +
                     "    FROM (SELECT * FROM user_jm_ap_buy_dispatch_deals UNION SELECT * FROM user_jm_ap_sell_deals) deals\n" +
                     "    WHERE match_status IN ('P', 'N')";
        try (Table result = context.getIOFactory().runSQL(sql)) {
            Set<Integer> dealNums = result.getRows().stream().map(row -> row.getInt(0)).collect(Collectors.toSet());
            return dealNums.stream()
                    .map(deal -> fromTransaction(context.getTradingFactory().retrieveTransaction(deal),
                                                 context.getStaticDataFactory()))
                    .collect(Collectors.toSet());
        }
    }
    
    private UnmatchedDeal fromTransaction(Transaction transaction, StaticDataFactory staticDataFactory) {
        int dealNum = transaction.getDealTrackingId();
        LocalDate dealDate = fromDate(transaction.getValueAsDate(EnumTransactionFieldId.StartDate));
        int customerId = transaction.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
        String pricingType = transaction.getField("Pricing Type").getValueAsString();
        EnumToolset toolset = transaction.getToolset();
        int metalId = EnumToolset.Fx.equals(toolset)
                      ? transaction.getValueAsInt(EnumTransactionFieldId.FxBaseCurrency)
                      : staticDataFactory.getId(EnumReferenceTable.Currency,
                                                METAL_NAMES.get(transaction.getLeg(1)
                                                                        .getValueAsString(EnumLegFieldId.CommoditySubGroup)));
        return ImmutableUnmatchedDeal.builder()
                .dealNum(dealNum)
                .dealDate(dealDate)
                .pricingWindowKey(ImmutablePricingWindowKey.of(customerId, pricingType, metalId))
                .build();
    }
    
    private LocalDate getCurrentDate(Context context) {
        return fromDate(context.getTradingDate());
    }
    
    private LocalDate fromDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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
            return result.getRows().stream().map(row -> row.getString(0)).collect(Collectors.toSet());
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
