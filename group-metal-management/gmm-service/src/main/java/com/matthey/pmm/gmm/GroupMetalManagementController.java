package com.matthey.pmm.gmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.FRIDAY;

@RestController
public class GroupMetalManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupMetalManagementController.class);
    
    private final WebsiteUserService websiteUserService;
    private final EndurConnector endurConnector;
    
    public GroupMetalManagementController(WebsiteUserService websiteUserService, EndurConnector endurConnector) {
        this.websiteUserService = websiteUserService;
        this.endurConnector = endurConnector;
    }
    
    @GetMapping("/users")
    Set<WebsiteUser> retrieveUserList() {
        return websiteUserService.getAllUsersForUI();
    }
    
    @GetMapping("/login_user")
    Principal retrieveLoginStatus(Principal principal) {
        return principal;
    }
    
    @PostMapping("/users/{username}/password")
    void updateUser(@PathVariable String username) {
        logger.info("resetting password for user {}", username);
        websiteUserService.resetPassword(username);
    }
    
    @GetMapping("/units")
    Set<String> retrieveUnits() {
        return Set.of("gms", "mgs", "kgs", "lbs", "TOz");
    }
    
    @GetMapping("/metals")
    Set<String> retrieveMetals() {
        return Set.of("XOS", "XRU", "XIR", "XPT", "XPD", "XRH");
    }
    
    @GetMapping("/basis_of_assumptions")
    Set<String> retrieveBasisOfAssumptions() {
        return Set.of("Contractual", "Average", "Estimate");
    }
    
    @GetMapping("/balance_dates")
    List<String> retrieveBalanceDates() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(14);
        return startDate.datesUntil(endDate)
                .filter(date -> date.getDayOfWeek() == FRIDAY || date.lengthOfMonth() == date.getDayOfMonth())
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/groups")
    Group[] retrieveGroups(Principal principal) {
        Integer userId = websiteUserService.getUser(principal.getName()).id();
        return endurConnector.get("/groups/?user={userId}", Group[].class, userId);
    }
    
    @GetMapping("/customers")
    String[] retrieveCustomers() {
        return endurConnector.get("/customers", String[].class);
    }
    
    @GetMapping("/forecasts")
    Forecast[] retrieveForecasts() {
        return endurConnector.get("/forecasts", Forecast[].class);
    }
    
    @PostMapping("/forecasts")
    void saveForecasts(@RequestBody Forecast forecast) {
        endurConnector.post("/forecasts", forecast);
    }
}
