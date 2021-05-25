package com.matthey.pmm.gmm;

import com.olf.openrisk.application.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class GroupMetalManagementController {
    
    private final Session session;
    
    public GroupMetalManagementController(Session session) {
        this.session = session;
    }
    
    @GetMapping("/support_emails")
    public Set<String> getSupportEmails() {
        return new SupportEmailRetriever(session).retrieve();
    }
    
    @GetMapping("/users")
    public Set<WebsiteUser> getWebsiteUsers() {
        return new WebsiteUsersUpdater(session).retrieve();
    }
    
    @PostMapping("/users")
    public void updateUser(@RequestBody WebsiteUser user) {
        new WebsiteUsersUpdater(session).update(user);
    }
    
    @GetMapping("/groups")
    Set<Group> retrieveGroups(@RequestParam Integer userId) {
        return new GroupRetriever(session).retrieve(userId);
    }
    
    @GetMapping("/customers")
    Set<String> retrieveCustomers() {
        return new CustomerRetriever(session).retrieve();
    }
    
    @GetMapping("/forecasts")
    Set<Forecast> retrieveForecasts() {
        return new ForecastUpdater(session).retrieve();
    }
    
    @PostMapping("/forecasts")
    void saveForecasts(@RequestBody Forecast forecast) {
        new ForecastUpdater(session).save(forecast);
    }
}
