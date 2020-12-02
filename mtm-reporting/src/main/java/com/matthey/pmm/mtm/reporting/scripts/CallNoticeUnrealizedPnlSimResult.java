package com.matthey.pmm.mtm.reporting.scripts;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedSimulationResult;
import com.matthey.pmm.mtm.reporting.pnl.UnrealizedPnlAdjuster;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.trading.Transactions;

import static com.olf.embedded.application.EnumScriptCategory.SimResult;

@ScriptCategory(SimResult)
public class CallNoticeUnrealizedPnlSimResult extends EnhancedSimulationResult {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(CallNoticeUnrealizedPnlSimResult.class);
    
    @Override
    protected void run(Session session,
                       Scenario scenario,
                       RevalResult revalResult,
                       Transactions transactions,
                       RevalResults prerequisites,
                       RevalResults priorResults,
                       SimResults priorSimResults,
                       EnumSplitMethod splitMethod) {
        UnrealizedPnlAdjuster adjuster = new UnrealizedPnlAdjuster(revalResult.getName());
        transactions.forEach(transaction -> {
            int dealNum = transaction.getDealTrackingId();
            double original = getTranOrCumResult(prerequisites, dealNum, adjuster.getDependentResultName());
            double prior = getTranOrCumResult(priorResults, dealNum, adjuster.getResultName());
            double change = getTranOrCumResult(prerequisites, dealNum, adjuster.getBalanceChangeResultName());
            double increment = getTranOrCumResult(prerequisites, dealNum, adjuster.getPnlResultName());
            double result = adjuster.adjust(prior, original, increment, change);
            logger.info(
                    "unrealized pnl adjustment for deal {}: prior -> {}; original -> {}; increment -> {}; change -> {}; result -> {}",
                    dealNum,
                    prior,
                    original,
                    increment,
                    change,
                    result);
            revalResult.addValue(transaction, transaction.getLeg(0), result);
        });
    }
}
