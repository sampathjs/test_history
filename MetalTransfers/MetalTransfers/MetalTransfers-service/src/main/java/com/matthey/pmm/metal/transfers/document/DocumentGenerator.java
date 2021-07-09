package com.matthey.pmm.metal.transfers.document;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.Run;
import com.matthey.pmm.metal.transfers.data.DataCache;
import com.matthey.pmm.metal.transfers.interest.Interest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Component
public class DocumentGenerator {

    public static final Map<String, String> METAL_NAMES = Map.of("XPT",
                                                                 "Platinum",
                                                                 "XPD",
                                                                 "Palladium",
                                                                 "XRH",
                                                                 "Rhodium",
                                                                 "XAU",
                                                                 "Gold",
                                                                 "XAG",
                                                                 "Silver",
                                                                 "XIR",
                                                                 "Iridium",
                                                                 "XOS",
                                                                 "Osmium",
                                                                 "XRU",
                                                                 "Ruthenium");
    private static final Logger logger = LoggerFactory.getLogger(DocumentGenerator.class);

    private final DataCache dataCache;
    private final InterestBooker interestBooker;
    private final StatementGenerator statementGenerator;
    private final StatementSender statementSender;

    public DocumentGenerator(DataCache dataCache,
                             InterestBooker interestBooker,
                             StatementGenerator statementGenerator,
                             StatementSender statementSender) {
        this.dataCache = dataCache;
        this.interestBooker = interestBooker;
        this.statementGenerator = statementGenerator;
        this.statementSender = statementSender;
    }

    public static String formatDate(LocalDate date) {
        return DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(date);
    }

    public DocumentGeneratingResult generateDocuments(Map<String, List<Interest>> interests,
                                                      boolean generateStatements,
                                                      boolean bookCashDeals,
                                                      Region region,
                                                      String user) {
        logger.info("generate statements: {}; book cash deals: {}; all interests: {}",
                    generateStatements,
                    bookCashDeals,
                    interests);
        var resultBuilder = ImmutableDocumentGeneratingResult.builder();
        try {
            var filteredInterests = ImmutableMap.copyOf(Maps.transformValues(interests,
                                                                             interestSet -> Objects.requireNonNull(
                                                                                     interestSet)
                                                                                     .stream()
                                                                                     .filter(this::isNeeded)
                                                                                     .collect(toList())));
            var period = dataCache.getStatementPeriods().getPeriod(region);
            var statementMonth = period.yearMonth;

            if (generateStatements) {
                var statementGeneratingRuns = statementGenerator.generate(filteredInterests,
                                                                          dataCache.getParties(),
                                                                          dataCache.getStatementPeriods()
                                                                                  .getPeriod(region).endDate,
                                                                          statementMonth,
                                                                          user);
                resultBuilder.addAllStatementGeneratingRuns(statementGeneratingRuns);
                var successfulRuns = statementGeneratingRuns.stream().filter(Run::isSuccessful).collect(toList());
                var statementEmailingRuns = statementSender.sendStatements(dataCache.getPartyContacts(),
                                                                           successfulRuns,
                                                                           statementMonth,
                                                                           user);
                resultBuilder.addAllStatementEmailingRuns(statementEmailingRuns);
                logger.info("statement generation and emailing are finished");
            } else {
                resultBuilder.addAllStatementGeneratingRuns(List.of());
                resultBuilder.addAllStatementEmailingRuns(List.of());
            }
            if (bookCashDeals) {
                var allInterests = filteredInterests.values().stream().flatMap(List::stream).collect(toList());
                var cashDealBookingRuns = interestBooker.book(allInterests,
                                                              region,
                                                              statementMonth,
                                                              period.endDate,
                                                              user);
                resultBuilder.addAllCashDealBookingRuns(cashDealBookingRuns);
                resultBuilder.isInvoiceGeneratingOk(interestBooker.generateInvoices(region));
                logger.info("cash deal booking and invoicing are finished");
            } else {
                resultBuilder.addAllCashDealBookingRuns(List.of());
                resultBuilder.isInvoiceGeneratingOk(true);
            }
        } catch (Exception e) {
            logger.error("failed to complete document generation: {}", e.getMessage(), e);
        }
        return resultBuilder.build();
    }

    private boolean isNeeded(Interest interest) {
        return Math.abs(interest.value()) > 0.1;
    }
}
