package com.matthey.pmm.metal.rentals.service;

import com.matthey.pmm.metal.rentals.Account;
import com.matthey.pmm.metal.rentals.Region;
import com.matthey.pmm.metal.rentals.data.DataCache;
import com.matthey.pmm.metal.rentals.document.DocumentGeneratingResultSender;
import com.matthey.pmm.metal.rentals.document.DocumentGenerator;
import com.matthey.pmm.metal.rentals.interest.Interest;
import com.matthey.pmm.metal.rentals.interest.InterestCalculationParameters;
import com.matthey.pmm.metal.rentals.interest.InterestCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

@RestController
public class MetalRentalsController {

    private static final Logger logger = LoggerFactory.getLogger(MetalRentalsController.class);

    private final WebsiteUserService websiteUserService;
    private final DataCache dataCache;
    private final InterestCalculator interestCalculator;
    private final DocumentGenerator documentGenerator;
    private final DocumentGeneratingResultSender documentGeneratingResultSender;

    public MetalRentalsController(WebsiteUserService websiteUserService,
                                  DataCache dataCache,
                                  InterestCalculator interestCalculator,
                                  DocumentGenerator documentGenerator,
                                  DocumentGeneratingResultSender documentGeneratingResultSender) {
        this.websiteUserService = websiteUserService;
        this.dataCache = dataCache;
        this.interestCalculator = interestCalculator;
        this.documentGenerator = documentGenerator;
        this.documentGeneratingResultSender = documentGeneratingResultSender;
    }

    @GetMapping("/users")
    Set<SimplifiedWebsiteUser> retrieveUserList() {
        return websiteUserService.getAllUsersForUI();
    }

    @GetMapping("/users/current")
    Principal retrieveLoginStatus(Principal principal) {
        return principal;
    }

    @PostMapping("/users/{username}/password")
    void updateUser(@PathVariable String username) {
        logger.info("resetting password for user {}", username);
        websiteUserService.resetPassword(username);
    }

    @GetMapping("/interest_rates/{region}")
    Map<String, Double> retrieveInterestRates(@PathVariable Region region) {
        logger.info("retrieving interest rates with region {}", region);
        return dataCache.getInterestRates().get(region);
    }

    @PutMapping("/interest_rates/{region}")
    void updateInterestRates(Principal principal,
                             @PathVariable Region region,
                             @RequestBody Map<String, Double> interestRates) {
        logger.info("updating interest rates with region {} and interest rates {}", region, interestRates);
        dataCache.getInterestRates().update(principal.getName(), region, interestRates);
    }

    @GetMapping("/average_prices/{region}")
    Map<String, Map<String, Double>> retrieveAveragePrices(@PathVariable Region region) {
        logger.info("retrieving average prices with region {}", region);
        return dataCache.getAveragePrices().getMetalPrices(region);
    }

    @GetMapping("/accounts/{region}")
    Map<String, Set<Account>> retrieveAccounts(@PathVariable Region region) {
        logger.info("retrieving accounts with region {}", region);
        return dataCache.getAccounts().asGroups(region);
    }

    @PutMapping("/interests/{region}")
    Map<String, List<Interest>> calculateInterests(@PathVariable Region region,
                                                   @RequestBody InterestCalculationParameters parameters) {
        logger.info("calculateInterests started with region {} and parameters {}", region, parameters);
        var dailyBalances = dataCache.getDailyBalances().get(region);
        var numOfDays = dataCache.getStatementPeriods().getPeriod(region).numOfDays;
        return interestCalculator.calculateAllInterests(dailyBalances, parameters, numOfDays);
    }

    @PutMapping("/documents/{region}")
    void generateDocuments(Principal principal,
                           @PathVariable Region region,
                           @RequestBody Map<String, List<Interest>> interests,
                           @RequestParam("statements") boolean generateStatements,
                           @RequestParam("deals") boolean bookCashDeals) {
        logger.info("generateDocuments started with region {}, interests {}, need statements? {}, need deal booking? {}",
                    region,
                    interests,
                    generateStatements,
                    bookCashDeals);
        // these are long running tasks so the call is returned immediately and an email will be sent after completion
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                var user = principal.getName();
                var runs = documentGenerator.generateDocuments(interests,
                                                               generateStatements,
                                                               bookCashDeals,
                                                               region,
                                                               user);
                documentGeneratingResultSender.send(runs, websiteUserService.getUser(user).email());
            } catch (Exception e) {
                logger.error("error occurred during generating documents: {}", e.getMessage(), e);
            }
        });
    }
}
