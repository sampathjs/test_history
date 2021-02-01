package com.matthey.pmm.mtm.reporting.scripts;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedSimulationResult;
import com.matthey.pmm.mtm.reporting.pnl.AdjustmentCalculator;
import com.matthey.pmm.mtm.reporting.pnl.TranResultSet;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

import java.util.Map;
import java.util.function.BiFunction;

import static com.olf.embedded.application.EnumScriptCategory.SimResult;
import static java.util.stream.Collectors.toMap;

@ScriptCategory(SimResult)
public class CallNoticeAdjustmentSimResult extends EnhancedSimulationResult {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(CallNoticeAdjustmentSimResult.class);
    
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
        Map<String, Double> fxRates = retrieveFxRates(revalResult.getName().equals("CallNot Base P&L Adjustment")
                                                      ? priorResults
                                                      : prerequisites);
        for (Transaction transaction : transactions) {
            if (!revalResult.isApplicable(transaction)) {
                continue;
            }
            
            int dealNum = transaction.getDealTrackingId();
            logger.info("deal num: {}", dealNum);
            
            if (fxRates == null) {
                logger.info("fx rate not available, set result as 0");
                revalResult.addValue(transaction, transaction.getLeg(0), 0);
                continue;
            }
            
            String paymentCcy = transaction.retrieveField(EnumTranfField.Currency).getDisplayString();
            String notionalCcy = transaction.retrieveField(EnumTranfField.NotnlCurrency).getDisplayString();
            BiFunction<TranResultSet, String, Double>
                    tranResultRetriever
                    = (tranResultSet, resultName) -> getTranOrCumResult(tranResultSet == TranResultSet.CURRENT
                                                                        ? prerequisites
                                                                        : priorResults, dealNum, resultName);
            AdjustmentCalculator calculator = new AdjustmentCalculator(revalResult.getName(),
                                                                       fxRates,
                                                                       tranResultRetriever,
                                                                       paymentCcy,
                                                                       notionalCcy,
                                                                       baseCcy);
            double result = calculator.calc();
            revalResult.addValue(transaction, transaction.getLeg(0), result);
        }
    }
    
    private Map<String, Double> retrieveFxRates(RevalResults revalResults) {
        if (revalResults == null) {
            return null;
        }
        ConstTable table = revalResults.getResultTable(EnumResultType.Fx);
        return table == null
               ? null
               : table.asTable()
                       .getRows()
                       .stream()
                       .collect(toMap(row -> row.getString("label"), row -> row.getDouble("result")));
    }
}
