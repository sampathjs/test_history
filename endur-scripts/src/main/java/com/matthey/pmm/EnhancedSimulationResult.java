package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.trading.Transactions;

import static com.olf.embedded.application.EnumScriptCategory.SimResult;

@SuppressWarnings("unused")
@ScriptCategory(SimResult)
public abstract class EnhancedSimulationResult extends AbstractSimulationResult2 {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(EnhancedSimulationResult.class);
    
    @Override
    public void calculate(Session session,
                          Scenario scenario,
                          RevalResult revalResult,
                          Transactions transactions,
                          RevalResults prerequisites,
                          RevalResults priorResults,
                          SimResults priorSimResults,
                          EnumSplitMethod splitMethod) {
        EndurLoggerFactory.runScriptWithLogging("simulation result",
                                                this.getClass(),
                                                logger,
                                                () -> run(session,
                                                          scenario,
                                                          revalResult,
                                                          transactions,
                                                          prerequisites,
                                                          priorResults,
                                                          priorSimResults,
                                                          splitMethod));
    }
    
    protected abstract void run(final Session session,
                                final Scenario scenario,
                                final RevalResult revalResult,
                                final Transactions transactions,
                                final RevalResults prerequisites,
                                final RevalResults priorResults,
                                final SimResults priorSimResults,
                                final EnumSplitMethod splitMethod);
    
    protected double getTranOrCumResult(RevalResults revalResults, int dealNum, String resultName) {
        try {
            return revalResults.getResultAsDouble(dealNum, resultName);
        } catch (Exception e) {
            logger.warn("unable to get result {} for deal {} - returns 0 instead", resultName, dealNum);
            return 0;
        }
    }
}
