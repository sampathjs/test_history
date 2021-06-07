package com.matthey.pmm.toms.service;


import org.joda.time.LocalDate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.olf.openrisk.application.Session;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("MVCPathVariableInspection") // the paths are actually defined in AbstractEndurConnectorController
@RestController
public class TomsController {
    private final Session session;

    public TomsController(Session session) {
        this.session = session;
    }
    
    
    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }
}
