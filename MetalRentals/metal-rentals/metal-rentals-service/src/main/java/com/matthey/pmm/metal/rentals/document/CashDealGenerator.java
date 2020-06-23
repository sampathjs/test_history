package com.matthey.pmm.metal.rentals.document;

import com.matthey.pmm.metal.rentals.CashDeal;
import com.matthey.pmm.metal.rentals.ImmutableCashDeal;
import com.matthey.pmm.metal.rentals.Region;
import com.matthey.pmm.metal.rentals.data.DataCache;
import com.matthey.pmm.metal.rentals.interest.Interest;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.matthey.pmm.metal.rentals.document.DocumentGenerator.METAL_NAMES;

@Component
public class CashDealGenerator {

    private final DataCache dataCache;

    public CashDealGenerator(DataCache dataCache) {
        this.dataCache = dataCache;
    }

    public CashDeal generate(Interest interest, Region region) {
        var internalPortfolio = dataCache.getFeePortfolios().get(interest.holder());
        checkNotNull(internalPortfolio, "no portfolio for the holder of %s: %s", interest.account(), interest.holder());
        var externalPortfolio = dataCache.getFeePortfolios().get(interest.owner());
        return ImmutableCashDeal.builder()
                .currency(interest.currency())
                .cashflowType("Metal Rentals - " + METAL_NAMES.get(interest.metal()).toUpperCase())
                .internalBU(interest.holder())
                .internalPortfolio(internalPortfolio)
                .externalBU(interest.owner())
                .settleDate(dataCache.getStatementPeriods().getPeriod(region).settleDate.toString())
                .position(interest.value())
                .externalPortfolio(externalPortfolio)
                .fxRate(region == Region.CN ? dataCache.getAveragePrices().getUsdCnyRate() : null)
                .build();
    }
}
