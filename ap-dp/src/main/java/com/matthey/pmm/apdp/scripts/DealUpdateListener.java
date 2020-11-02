package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedTradeProcessListener;
import com.matthey.pmm.apdp.DealDetails;
import com.matthey.pmm.apdp.ImmutableDealDetails;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ScriptCategory({EnumScriptCategory.OpsSvcTrade})
public class DealUpdateListener extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(DealUpdateListener.class);
    private static final Map<Boolean, String> dealTables = ImmutableMap.of(true,
                                                                           "USER_jm_ap_sell_deals",
                                                                           false,
                                                                           "USER_jm_ap_buy_dispatch_deals");
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table clientData) {
        if (!succeed) {
            return;
        }
        for (int tranNum : dealInfo.getTransactionIds()) {
            DealDetails dealDetails = retrieveDealDetails(tranNum);
            String dealTable = dealTables.get(dealDetails.isSell());
            
            if (dealDetails.isCancelled()) {
                removeDeal(dealDetails, dealTable);
            } else {
                if (isDealInTable(tranNum, dealTable)) {
                    updateDeal(dealDetails, dealTable);
                } else {
                    insertDeal(dealDetails, dealTable);
                }
                
                String otherDealTable = dealTables.get(!dealDetails.isSell());
                if (isDealInTable(tranNum, otherDealTable)) {
                    removeDeal(dealDetails, otherDealTable);
                }
            }
        }
    }
    
    private void removeDeal(DealDetails dealDetails, String dealTable) {
        Optional<DealDetails> linkedDeal = retrieveLinkedDeal(dealDetails.dealNum());
        linkedDeal.ifPresent(deal -> {
            double volume = dealDetails.volumeInToz();
            DealDetails updatedLinkedDeal = ((ImmutableDealDetails) deal).withVolumeLeftInToz(deal.volumeLeftInToz() +
                                                                                              volume);
            updateDeal(updatedLinkedDeal, dealTables.get(deal.isSell()));
            removeLink(dealDetails.dealNum(), deal.dealNum());
        });
        DealDetails updatedDealDetails = ((ImmutableDealDetails) dealDetails).withMatchStatus("E");
        updateDeal(updatedDealDetails, dealTable);
    }
    
    private void removeLink(int buyDealNum, int sellDealNum) {
    
    }
    
    private Optional<DealDetails> retrieveLinkedDeal(int dealNum) {
        return Optional.of(ImmutableDealDetails.builder()
                                   .dealNum(-1)
                                   .volumeInToz(0d)
                                   .volumeLeftInToz(0d)
                                   .matchStatus("N")
                                   .matchDate(LocalDate.now())
                                   .customerId(-1)
                                   .metalType(-1)
                                   .isSell(true)
                                   .isCancelled(true)
                                   .build());
    }
    
    private void updateDeal(DealDetails dealDetails, String tableName) {
    
    }
    
    private DealDetails retrieveDealDetails(int tranNum) {
        return ImmutableDealDetails.builder()
                .dealNum(-1)
                .volumeInToz(0d)
                .volumeLeftInToz(0d)
                .matchStatus("N")
                .matchDate(LocalDate.now())
                .customerId(-1)
                .metalType(-1)
                .isSell(true)
                .isCancelled(true)
                .build();
    }
    
    @Override
    protected PreProcessResult check(Context context,
                                     EnumTranStatus targetStatus,
                                     PreProcessingInfo<EnumTranStatus>[] infoArray,
                                     Table clientData) {
        if (targetStatus == EnumTranStatus.Cancelled) {
            return PreProcessResult.succeeded();
        }
        
        Set<Integer> deals = Arrays.stream(infoArray)
                .map(item -> item.getTransaction().getDealTrackingId())
                .collect(Collectors.toSet());
        Set<Integer> matchedDeals = retrieveMatchedDeals(context, deals);
        logger.info("deals have been matched: {}", matchedDeals);
        return matchedDeals.isEmpty()
               ? PreProcessResult.succeeded()
               : PreProcessResult.failed("the following deals have been matched, please cancel them and rebook:" +
                                         StringUtils.join(matchedDeals, ","));
    }
    
    private Set<Integer> retrieveMatchedDeals(Context context, Set<Integer> dealsToCheck) {
        //language=TSQL
        String sqlTemplate = "SELECT buy_deal_num AS deal_num\n" +
                             "    FROM USER_jm_ap_buy_sell_link\n" +
                             "    WHERE buy_deal_num IN (${deals})\n" +
                             "UNION\n" +
                             "SELECT sell_deal_num AS deal_num\n" +
                             "    FROM USER_jm_ap_buy_sell_link\n" +
                             "    WHERE sell_deal_num IN (${deals})";
        Map<String, Object> variables = ImmutableMap.of("deals",
                                                        dealsToCheck.stream()
                                                                .map(String::valueOf)
                                                                .collect(Collectors.joining(",")));
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving matched deals: " + System.lineSeparator() + sql);
        try (Table result = context.getIOFactory().runSQL(sql)) {
            return result.getRows().stream().map(row -> row.getInt(0)).collect(Collectors.toSet());
        }
    }
    
    private boolean isDealInTable(int tranNum, String tableName) {
        return false;
    }
    
    private void insertDeal(DealDetails dealDetails, String tableName) {
    
    }
}
