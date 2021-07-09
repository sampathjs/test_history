package com.matthey.pmm.metal.transfers.data;

import com.google.common.collect.Maps;
import com.matthey.pmm.metal.transfers.ClosingPrice;
import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.Region;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.averagingDouble;

public class AveragePrices {

    private static final Logger logger = LoggerFactory.getLogger(AveragePrices.class);

    private final EndurConnector endurConnector;
    private final StatementPeriods statementPeriods;
    private final Map<Region, Map<String, Map<String, Double>>> metalPrices;
    private final double usdCnyRate;

    public AveragePrices(EndurConnector endurConnector,
                         List<String> metals,
                         List<String> currencies,
                         StatementPeriods statementPeriods) {
        this.endurConnector = endurConnector;
        this.statementPeriods = statementPeriods;
        usdCnyRate = retrieveAveragePrice("FX_USD.CNY", Region.CN);
        metalPrices = Maps.toMap(List.of(Region.values()),
                                 region -> Maps.toMap(region == Region.CN ? List.of("CNY") : currencies,
                                                      currency -> retrieveAveragePrices(metals, currency, region)));
    }

    private static String getRefSource(String indexName, Region region) {
        if (indexName.endsWith("CNY")) {
            return "BOC";
        } else if (indexName.startsWith("XAU")) {
            return "LBMA PM";
        } else if (indexName.startsWith("XAG")) {
            return "LBMA Silver";
        } else {
            return region.equals(Region.CN) ? "JM HK Opening" : "JM London Opening";
        }
    }

    private Double retrieveAveragePrice(String indexName, Region region) {
        var refSource = getRefSource(indexName, region);
        logger.info("retrieving prices for index name: {}; ref source: {}", indexName, refSource);
        var period = statementPeriods.getPeriod(region);
        String apiUrl = "/closing_prices/{indexName}/{refSource}?startDate={startDate}&&endDate={endDate}";
        var prices = endurConnector.get(apiUrl,
                                        ClosingPrice[].class,
                                        indexName,
                                        refSource,
                                        period.startDate.toString(),
                                        period.endDate.toString());
        return Arrays.stream(prices).collect(averagingDouble(ClosingPrice::price));
    }

    private Map<String, Double> retrieveAveragePrices(List<String> metals, String currency, Region region) {
        return Maps.toMap(metals,
                          metal -> currency.equals("CNY")
                                   ? retrieveAveragePrice(metal + "." + "USD", Region.CN) *
                                     usdCnyRate
                                   : retrieveAveragePrice(metal + "." + currency, region));
    }

    public Map<String, Map<String, Double>> getMetalPrices(Region region) {
        return metalPrices.get(region);
    }

    public double getUsdCnyRate() {
        return usdCnyRate;
    }
}
