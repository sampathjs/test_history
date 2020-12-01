package com.matthey.pmm.mtm.reporting.pnl;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class UnrealizedPnlAdjuster {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(UnrealizedPnlAdjuster.class);
    
    private final String dependentResultName;
    private final String balanceChangeResultName;
    
    public UnrealizedPnlAdjuster(String resultName) {
        checkArgument(resultName.matches("CallNot (Base )*Unrealized P&L.*"), "unsupported result: " + resultName);
        dependentResultName = resultName.replaceAll("CallNot ", "");
        balanceChangeResultName = resultName.replaceAll("Unrealized P&L.*", "Balance Change");
        logger.info("result name: {}; unrealized pnl result it depends on: {}; balance change result it depends on: {}",
                    resultName,
                    dependentResultName,
                    balanceChangeResultName);
    }
    
    public String getDependentResultName() {
        return dependentResultName;
    }
    
    public String getBalanceChangeResultName() {
        return balanceChangeResultName;
    }
    
    public double adjust(double original, double change) {
        return original - change;
    }
}
