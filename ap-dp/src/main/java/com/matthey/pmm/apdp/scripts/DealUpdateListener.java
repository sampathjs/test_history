package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedTradeProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@ScriptCategory({EnumScriptCategory.OpsSvcTrade})
public class DealUpdateListener extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(DealUpdateListener.class);
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table clientData) {
    }
    
    @Override
    protected PreProcessResult check(Context context,
                                     EnumTranStatus targetStatus,
                                     PreProcessingInfo<EnumTranStatus>[] infoArray,
                                     Table clientData) {
        if (targetStatus == EnumTranStatus.Cancelled) {
            return PreProcessResult.succeeded();
        }
        
        Function<PreProcessingInfo<EnumTranStatus>, Integer> dealIdGetter = i -> i.getTransaction().getDealTrackingId();
        Set<Integer> deals = Arrays.stream(infoArray).map(dealIdGetter).collect(toSet());
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
        String dealRange = dealsToCheck.stream().map(String::valueOf).collect(joining(","));
        Map<String, Object> variables = ImmutableMap.of("deals", dealRange);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving matched deals:{}{}", System.lineSeparator(), sql);
        
        try (Table result = context.getIOFactory().runSQL(sql)) {
            return result.getRows().stream().map(row -> row.getInt(0)).collect(toSet());
        }
    }
}
