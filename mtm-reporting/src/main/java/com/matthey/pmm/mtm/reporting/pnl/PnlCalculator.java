package com.matthey.pmm.mtm.reporting.pnl;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;

import java.time.LocalDate;
import java.util.function.BiFunction;

public class PnlCalculator {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(PnlCalculator.class);
    
    private final BiFunction<TranResultSet, String, Double> tranResultRetriever;
    private final String resultName;
    private final String yearStartDate;
    private final LocalDate currentDate;
    
    public PnlCalculator(BiFunction<TranResultSet, String, Double> tranResultRetriever,
                         String resultName,
                         String yearStartDate,
                         LocalDate currentDate) {
        this.tranResultRetriever = tranResultRetriever;
        this.resultName = resultName;
        this.yearStartDate = yearStartDate;
        this.currentDate = currentDate;
        logger.info("result name: {}", resultName);
    }
    
    public double calc() {
        switch (getTranResultType(resultName)) {
            case UNREALIZED_PNL:
                return calcUnrealizedPnl(resultName);
            case CUSTOM_CUMULATIVE_PNL:
                return calcCumulativeResult(resultName);
            default:
                return tranResultRetriever.apply(TranResultSet.CURRENT, getCoreResultName(resultName));
        }
    }
    
    private TranResultType getTranResultType(String resultName) {
        if (resultName.matches("JM( Base)? Unrealized P&L")) {
            return TranResultType.UNREALIZED_PNL;
        }
        if (resultName.matches("(JM.+Unrealized P&L.+)|(JM.+Realized P&L YTD)")) {
            return TranResultType.CUSTOM_CUMULATIVE_PNL;
        }
        return TranResultType.OTHER;
    }
    
    private double calcUnrealizedPnl(String resultName) {
        String coreResultName = getCoreResultName(resultName);
        String dependencyResultName = getUnrealizedPnlDependency(resultName);
        double coreResult = tranResultRetriever.apply(TranResultSet.CURRENT, coreResultName);
        double dependencyResult = tranResultRetriever.apply(TranResultSet.CURRENT, dependencyResultName);
        logger.info("{}: {}; {}: {}", coreResultName, coreResult, dependencyResultName, dependencyResult);
        return coreResult - dependencyResult;
    }
    
    private String getCoreResultName(String resultName) {
        return resultName.replaceAll("\\AJM ", "");
    }
    
    private String getUnrealizedPnlDependency(String resultName) {
        return resultName.contains("Base") ? "CallNot Base P&L Adjustment" : "CallNot Balance Change";
    }
    
    private double calcCumulativeResult(String resultName) {
        String dependency = resultName.replaceAll(" (MTD|YTD|LTD)", "");
        double prevResult = tranResultRetriever.apply(TranResultSet.PRIOR, resultName);
        double dependencyResult = tranResultRetriever.apply(TranResultSet.CURRENT, dependency);
        logger.info("{}: {}; {}: {}", resultName, prevResult, dependency, dependencyResult);
        return needResetResult(resultName) ? dependencyResult : prevResult + dependencyResult;
    }
    
    private boolean needResetResult(String resultName) {
        return (resultName.endsWith("MTD") && currentDate.getDayOfMonth() == 1) ||
               (resultName.endsWith("YTD") && currentDate.toString().endsWith(yearStartDate));
    }
}
