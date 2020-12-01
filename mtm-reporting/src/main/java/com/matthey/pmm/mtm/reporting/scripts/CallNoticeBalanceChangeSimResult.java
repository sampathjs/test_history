package com.matthey.pmm.mtm.reporting.scripts;

import com.matthey.pmm.EnhancedSimulationResult;
import com.matthey.pmm.mtm.reporting.pnl.BalanceChangeCalculator;
import com.matthey.pmm.mtm.reporting.pnl.CallNotice;
import com.matthey.pmm.mtm.reporting.pnl.ImmutableCallNotice;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

import java.util.Map;

import static com.olf.embedded.application.EnumScriptCategory.SimResult;
import static java.util.stream.Collectors.toMap;

@ScriptCategory(SimResult)
public class CallNoticeBalanceChangeSimResult extends EnhancedSimulationResult {
    
    @Override
    protected void run(Session session,
                       Scenario scenario,
                       RevalResult revalResult,
                       Transactions transactions,
                       RevalResults prerequisites,
                       RevalResults priorResults,
                       SimResults priorSimResults,
                       EnumSplitMethod splitMethod) {
        String baseCcy = scenario.getCurrency().getName();
        Map<String, Double> fxRates = retrieveFxRates(prerequisites);
        BalanceChangeCalculator calculator = new BalanceChangeCalculator(revalResult.getName(), baseCcy, fxRates);
        transactions.forEach(transaction -> {
            CallNotice callNotice = genCallNotice(transaction, prerequisites, priorResults);
            revalResult.addValue(transaction, transaction.getLeg(0), calculator.calc(callNotice));
        });
    }
    
    private Map<String, Double> retrieveFxRates(RevalResults prerequisites) {
        Table table = prerequisites.getResultTable(EnumResultType.Fx).asTable();
        return table.getRows().stream().collect(toMap(row -> row.getString("label"), row -> row.getDouble("result")));
    }
    
    private CallNotice genCallNotice(Transaction transaction, RevalResults prerequisites, RevalResults priorResults) {
        int dealNum = transaction.getDealTrackingId();
        int tranNum = transaction.getTransactionId();
        String paymentCcy = transaction.retrieveField(EnumTranfField.Currency).getDisplayString();
        String notionalCcy = transaction.retrieveField(EnumTranfField.NotnlCurrency).getDisplayString();
        double currNotional = getCurrentNotional(prerequisites, dealNum);
        double prevNotional = getCurrentNotional(priorResults, dealNum);
        return ImmutableCallNotice.builder()
                .tranNum(tranNum)
                .paymentCcy(paymentCcy)
                .notionalCcy(notionalCcy)
                .currentNotional(currNotional)
                .previousNotional(prevNotional)
                .build();
    }
    
    private double getCurrentNotional(RevalResults revalResults, int dealNum) {
        return revalResults == null ? 0 : revalResults.getResultAsDouble(dealNum, EnumResultType.CurrentNotional);
    }
}
