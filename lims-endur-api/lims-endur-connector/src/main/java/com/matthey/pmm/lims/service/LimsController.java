package com.matthey.pmm.lims.service;

import com.olf.openrisk.application.Session;
import com.matthey.pmm.lims.data.SampleUpdater;
import com.matthey.pmm.lims.data.ResultUpdater;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lims")
public class LimsController {

    private final Session session;

    public LimsController(Session session) {
        this.session = session;
    }

    @PostMapping("/Sample1")
    String newLimsSample(@RequestParam String batchId, @RequestParam String sampleNumber, @RequestParam String product) {
      return new SampleUpdater(session).updateTable(batchId, sampleNumber, product);
    }
    	
    @PostMapping("/Results")
    String newLimsResult(@RequestParam String result) {
      return new ResultUpdater(session).updateTable(result);
    }

}
