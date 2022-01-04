package com.matthey.pmm.connector.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.olf.openrisk.application.Session;

@RestController
@RequestMapping("/shared")
public class EndurConnectorSharedController {
	
    private static final Logger logger = LogManager.getLogger(EndurConnectorSharedController.class);


    private final Session session;

    public EndurConnectorSharedController(Session session) {
        this.session = session;
    }
    
    @PostMapping("emailConfirmation/response")
    public String postEmailConfirmationAction(@RequestParam String actionId) {
    	return new EmailConfirmationActionProcessor(session).processPost(actionId);
    }
}
