package com.matthey.pmm.mtm.reporting.pnl;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class UnrealizedPnlAdjuster {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(UnrealizedPnlAdjuster.class);
    
    private final String resultName;
    private final String pnlResultName;
    private final String dependentResultName;
    private final String balanceChangeResultName;
    
    public UnrealizedPnlAdjuster(String resultName) {
        checkArgument(resultName.matches("CallNot (Base )*Unrealized P&L.*"), "unsupported result: " + resultName);
        this.resultName = resultName;
        this.pnlResultName = resultName.replaceAll(" (MTD|YTD|LTD)", "");
        this.dependentResultName = resultName.replaceAll("CallNot ", "");
        this.balanceChangeResultName = resultName.replaceAll("Unrealized P&L.*", "Balance Change");
        logger.info("result name: {}; unrealized pnl result it depends on: {}; balance change result it depends on: {}",
                    resultName,
                    dependentResultName,
                    balanceChangeResultName);
    }
    
    public String getResultName() {
        return resultName;
    }
    
    public String getPnlResultName() {
        return pnlResultName;
    }
    
    public String getDependentResultName() {
        return dependentResultName;
    }
    
    public String getBalanceChangeResultName() {
        return balanceChangeResultName;
    }
    
    public double adjust(double prior, double original, double increment, double change) {
        return resultName.matches(".+(MTD|YTD|LTD)") ? prior + increment : original - change;
    }
}
