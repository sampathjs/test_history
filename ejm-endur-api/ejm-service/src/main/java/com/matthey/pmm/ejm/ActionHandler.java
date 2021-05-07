package com.matthey.pmm.ejm;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.matthey.pmm.ejm.service.EJMService.API_PREFIX;

@SuppressWarnings("deprecation")
@Api(tags = {"GenericActionHandler"}, description = "APIs for handling Endur events triggered outside of Endur")
@RestController
@RequestMapping(API_PREFIX)
public class ActionHandler extends AbstractEJMController {

    public ActionHandler(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }

    @ApiOperation("notify Endur of executed Action")
    @PostMapping("genericActionHandler/response")
    public String postGenericAction(
              @ApiParam(value = "actionId", example = "ABCDEFGH1234567890", required = true) @RequestParam String actionId) {
       if (actionId != null && actionId.startsWith ("accept")) {
    	   return "You have accepted the deal confirmation";
       }
       if (actionId != null && actionId.startsWith ("dispute")) {
    	   return "You have disputed the deal confirmation";
       }
       throw new RuntimeException ("Mock: neither dispute nor confirm");

    }
}
