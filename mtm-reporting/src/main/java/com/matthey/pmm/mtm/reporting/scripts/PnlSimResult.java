package com.matthey.pmm.mtm.reporting.scripts;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedSimulationResult;
import com.matthey.pmm.ScriptHelper;
import com.matthey.pmm.mtm.reporting.pnl.PnlCalculator;
import com.matthey.pmm.mtm.reporting.pnl.TranResultSet;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.simulation.EnumConfiguration;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimResults;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;

import java.time.LocalDate;
import java.util.Date;
import java.util.function.BiFunction;

import static com.olf.embedded.application.EnumScriptCategory.SimResult;

@ScriptCategory(SimResult)
public class PnlSimResult extends EnhancedSimulationResult {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(PnlSimResult.class);
    
    @Override
    protected void run(Session session,
                       Scenario scenario,
                       RevalResult revalResult,
                       Transactions transactions,
                       RevalResults prerequisites,
                       RevalResults priorResults,
                       SimResults priorSimResults,
                       EnumSplitMethod splitMethod) {
        LocalDate currentDate = getCurrentDate(session, scenario);
        logger.info("current date: {}", currentDate);
        String yearStartDate = getYearStartDate();
        logger.info("year start date: {}", yearStartDate);
        
        for (Transaction transaction : transactions) {
            int dealNum = transaction.getDealTrackingId();
            logger.info("deal num: {}", dealNum);
            
            BiFunction<TranResultSet, String, Double>
                    tranResultRetriever
                    = (tranResultSet, resultName) -> getTranOrCumResult(tranResultSet == TranResultSet.CURRENT
                                                                        ? prerequisites
                                                                        : priorResults, dealNum, resultName);
            PnlCalculator calculator = new PnlCalculator(tranResultRetriever,
                                                         revalResult.getName(),
                                                         yearStartDate,
                                                         currentDate);
            double result = calculator.calc();
            logger.info("result for deal num {}: {}", dealNum, result);
            revalResult.addValue(transaction, transaction.getLeg(0), result);
        }
    }
    
    private LocalDate getCurrentDate(Session session, Scenario scenario) {
        Date currentDate = scenario.getValueAsDate(EnumConfiguration.Date, "", "Scenario Date");
        return ScriptHelper.fromDate(currentDate == null ? session.getTradingDate() : currentDate);
    }
    
    private String getYearStartDate() {
        try {
            return new ConstRepository("MtmReporting", "UDSR").getStringValue("YearStartDate");
        } catch (Exception e) {
            logger.warn("cannot retrieve year start date from configuration, use default year start date instead");
            return "01-01";
        }
    }
}
