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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.matthey.pmm.ejm.service.EJMService.API_PREFIX;

@SuppressWarnings("deprecation")
@Api(tags = {"GenericActionHandler"}, description = "APIs for handling Endur events triggered outside of Endur")
@RestController
@RequestMapping(API_PREFIX)
public class GenericActionHandlerController extends AbstractEJMController {
    
    private static final Logger logger = LoggerFactory.getLogger(GenericActionHandlerController.class);
    
    public GenericActionHandlerController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }

    @ApiOperation("notify Endur of executed Action")
    @PostMapping("genericActionHandler/response")
    public String postGenericAction(
              @ApiParam(value = "actionId", example = "ABCDEFGH1234567890", required = true) @RequestParam String actionId) {
    	GenericAction[] actions = endurConnector.get("/generic_action?actionId={actionId}",
                  GenericAction[].class,
                  actionId);
    	if (actions != null && actions.length == 1) {
    		try {
    			String result = endurConnector.post (actions[0].actionConsumer() + "?actionId={actionId}", 
        				String.class, actionId);
    			logger.info ("Result of post: " + result);
    			if (result != null && result.startsWith("Error:")) {
    				return genResponse(new String[]{result}, String.class);
    			}
    		} catch (Exception ex) {
    			logger.error ("Error while sending post to consumer '" + actions[0].actionConsumer() +
    					"' using actionId: '" + actionId + "':  " + ex.toString());
    			for (StackTraceElement ste : ex.getStackTrace()) {
    				logger.error (ste.toString());
    			}
    			throw new RuntimeException ("Internal error while processing the request");
    		}
    		return genResponse(new String[]{actions[0].responseMessage()}, String.class);
       } else {
    		return genResponse(new String[]{"The link you have followed is no longer valid" }, String.class);
   	   }
    }
}
