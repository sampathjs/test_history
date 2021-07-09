package com.matthey.pmm.metal.transfers.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.ImmutableInterestRate;
import com.matthey.pmm.metal.transfers.InterestRate;
import com.matthey.pmm.metal.transfers.Region;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class InterestRates {

    private static final Logger logger = LoggerFactory.getLogger(InterestRates.class);

    private final EndurConnector endurConnector;
    private final String apiUrl = "/interest_rates/{indexName}";
    private final List<String> metals;
    private final Map<Region, String> interestRatesIndexes;
    private final Map<Region, Map<String, Double>> interestRates;

    public InterestRates(EndurConnector endurConnector, List<String> metals, Map<Region, String> interestRatesIndexes) {
        this.endurConnector = endurConnector;
        this.metals = metals;
        this.interestRatesIndexes = interestRatesIndexes;
        this.interestRates = ImmutableMap.copyOf(Maps.transformValues(this.interestRatesIndexes,
                                                                      this::retrieveFromEndur));
        logger.info("interest rates for all the region: {}", this.interestRates);
    }

    private Map<String, Double> retrieveFromEndur(String index) {
        logger.info("retrieving interest rates from {}", index);
        var interestRates = endurConnector.get(apiUrl, InterestRate[].class, index);
        return Arrays.stream(interestRates)
                .filter(interestRate -> metals.contains(interestRate.metal()))
                .collect(toMap(InterestRate::metal, InterestRate::rate));
    }

    public Map<String, Double> get(Region region) {
        logger.info("get interest rates for {}", region);
        checkRegion(region);
        return interestRates.get(region);
    }

    public void update(String user, Region region, Map<String, Double> interestRates) {
        logger.info("update interest rates for {}", region);
        checkRegion(region);
        var index = interestRatesIndexes.get(region);

        var filtered = interestRates.keySet()
                .stream()
                .filter(metals::contains)
                .map(metal -> ImmutableInterestRate.builder().metal(metal).rate(interestRates.get(metal)).build())
                .collect(toSet());
        endurConnector.put(apiUrl + "?user={user}", filtered, index, user);
    }

    private void checkRegion(Region region) {
        checkArgument(interestRatesIndexes.containsKey(region), "invalid region for interest rates: " + region);
    }
}
