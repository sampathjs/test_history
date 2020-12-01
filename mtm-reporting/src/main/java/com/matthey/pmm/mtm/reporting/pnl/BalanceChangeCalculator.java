package com.matthey.pmm.mtm.reporting.pnl;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toMap;

public class BalanceChangeCalculator {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(BalanceChangeCalculator.class);
    
    private final Map<String, Double> fxRates;
    private final Optional<String> baseCcy;
    
    // the fx rates retrieved from Endur is reversed compared to common practise, e.g. USD/XPD rather than XPD/USD
    public BalanceChangeCalculator(String resultName, String baseCcy, Map<String, Double> fxRates) {
        checkArgument(resultName.matches("CallNot (Base )?Balance Change"), "unsupported result: " + resultName);
        this.fxRates = fxRates.entrySet().stream().collect(toMap(Entry::getKey, entry -> 1d / entry.getValue()));
        this.baseCcy = resultName.contains("Base") ? Optional.of(baseCcy) : Optional.empty();
        this.baseCcy.ifPresent(ccy -> checkArgument(fxRates.containsKey(ccy), "there is no fx rate for " + ccy));
        logger.info("result name: {}; base ccy: {}, fx rates: {}", resultName, this.baseCcy, this.fxRates);
    }
    
    public double calc(CallNotice callNotice) {
        double diff = callNotice.currentNotional() - callNotice.previousNotional();
        logger.info("notional difference for call notice {}: current -> {}; previous -> {}; diff -> {}",
                    callNotice.tranNum(),
                    callNotice.currentNotional(),
                    callNotice.previousNotional(),
                    diff);
        String targetCcy = baseCcy.orElse(callNotice.paymentCcy());
        checkArgument(fxRates.containsKey(targetCcy), "there is no fx rate for " + targetCcy);
        String notionalCcy = callNotice.notionalCcy();
        checkArgument(fxRates.containsKey(notionalCcy), "there is no fx rate for " + notionalCcy);
        double fxRate = fxRates.get(notionalCcy) / fxRates.get(targetCcy);
        logger.info("fx rate {}/{}: {}", notionalCcy, targetCcy, fxRate);
        double balanceChange = diff * fxRate;
        logger.info("balance change for call notice {}: {}", callNotice.tranNum(), balanceChange);
        return balanceChange;
    }
}
