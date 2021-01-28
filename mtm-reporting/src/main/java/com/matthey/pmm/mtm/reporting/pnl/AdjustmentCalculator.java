package com.matthey.pmm.mtm.reporting.pnl;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toMap;

public class AdjustmentCalculator {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(AdjustmentCalculator.class);
    
    private final BiFunction<TranResultSet, String, Double> tranResultRetriever;
    private final Map<String, Double> fxRates;
    private final String resultName;
    private final String paymentCcy;
    private final String notionalCcy;
    private final String baseCcy;
    
    public AdjustmentCalculator(String resultName,
                                Map<String, Double> fxRates,
                                BiFunction<TranResultSet, String, Double> tranResultRetriever,
                                String paymentCcy,
                                String notionalCcy,
                                String baseCcy) {
        this.resultName = resultName;
        this.tranResultRetriever = tranResultRetriever;
        this.paymentCcy = paymentCcy;
        this.notionalCcy = notionalCcy;
        this.baseCcy = baseCcy;
        
        checkArgument(resultName.matches("(CallNot Balance Change)|(CallNot Base P&L Adjustment)"),
                      "unsupported result: " + resultName);
        checkArgument(fxRates.containsKey(paymentCcy), "there is no fx rate for " + paymentCcy);
        checkArgument(fxRates.containsKey(notionalCcy), "there is no fx rate for " + notionalCcy);
        checkArgument(fxRates.containsKey(baseCcy), "there is no fx rate for " + baseCcy);
        
        // the fx rates retrieved from Endur is reversed compared to common practise, e.g. USD/XPD rather than XPD/USD
        this.fxRates = fxRates.entrySet().stream().collect(toMap(Entry::getKey, entry -> 1d / entry.getValue()));
        
        logger.info("result name: {}", resultName);
    }
    
    public double calc() {
        if (resultName.equals("CallNot Balance Change")) {
            String notionalResultName = "Current Notional Result";
            double currentNotional = tranResultRetriever.apply(TranResultSet.CURRENT, notionalResultName);
            double prevNotional = tranResultRetriever.apply(TranResultSet.PRIOR, notionalResultName);
            double diff = currentNotional - prevNotional;
            logger.info("current notional: {}; prev notional: {}; diff: {}", currentNotional, prevNotional, diff);
            double fxRate = fxRates.get(notionalCcy) / fxRates.get(paymentCcy);
            logger.info("fx rate {}/{}: {}", notionalCcy, paymentCcy, fxRate);
            double balanceChange = diff * fxRate;
            logger.info("balance change for call notice: {}", balanceChange);
            return balanceChange;
        } else {
            double balanceChange = tranResultRetriever.apply(TranResultSet.CURRENT, "CallNot Balance Change");
            logger.info("balance change: {}", balanceChange);
            double fxRate = fxRates.get(paymentCcy) / fxRates.get(baseCcy);
            logger.info("fx rate {}/{}: {}", paymentCcy, baseCcy, fxRate);
            double basePnlAdjustment = balanceChange * fxRate;
            logger.info("base P&L adjustment for call notice: {}", basePnlAdjustment);
            return basePnlAdjustment;
        }
    }
}
