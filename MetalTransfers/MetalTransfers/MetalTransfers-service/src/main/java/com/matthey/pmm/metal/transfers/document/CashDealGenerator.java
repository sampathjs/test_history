package com.matthey.pmm.metal.transfers.document;

import com.matthey.pmm.metal.transfers.CashDeal;
import com.matthey.pmm.metal.transfers.ImmutableCashDeal;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.data.DataCache;
import com.matthey.pmm.metal.transfers.interest.Interest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.matthey.pmm.metal.transfers.document.DocumentGenerator.METAL_NAMES;
import static com.matthey.pmm.metal.transfers.document.DocumentGenerator.formatDate;

@Component
public class CashDealGenerator {

    private final DataCache dataCache;
    private final List<String> cnZeroVatBUList;

    public CashDealGenerator(DataCache dataCache, @Value("${cn.zero.vat.bu.list}") List<String> cnZeroVatBUList) {
        this.dataCache = dataCache;
        this.cnZeroVatBUList = cnZeroVatBUList;
    }

    public CashDeal generate(Interest interest, Region region, LocalDate statementDate) {
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
                .statementDate(formatDate(statementDate))
                .position(interest.value())
                .isCnAndHasVat(region == Region.CN && !cnZeroVatBUList.contains(interest.owner()))
                .externalPortfolio(externalPortfolio)
                .fxRate(region == Region.CN ? dataCache.getAveragePrices().getUsdCnyRate() : null)
                .build();
    }
}
