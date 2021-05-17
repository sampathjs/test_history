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
public class GenericActionHandlerController extends AbstractEJMController {
    protected final EjmServiceConnector ejmServiceConnector;
    
    public GenericActionHandlerController(EndurConnector endurConnector, EjmServiceConnector ejmConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
        this.ejmServiceConnector = ejmConnector;
    }

    @ApiOperation("notify Endur of executed Action")
    @PostMapping("genericActionHandler/response")
    public String postGenericAction(
              @ApiParam(value = "actionId", example = "ABCDEFGH1234567890", required = true) @RequestParam String actionId) {
    	GenericAction[] actions = endurConnector.get("/generic_action?actionId={account}",
                  GenericAction[].class,
                  actionId);
    	if (actions != null && actions.length == 1) {
    		try {
    			String result = ejmServiceConnector.post (actions[0].actionConsumer() + "?actionId={actionId}", 
        				String.class, actionId);
    		} catch (Exception ex) {
    			throw new RuntimeException ("Internal error while processing the request");
    		}
    		return genResponse(new String[] {actions[0].responseMessage()}, String[].class);
       } else {
    		return genResponse(new String[] {"The link you have followed is no longer valid"}, String[].class);
   	   }
    }
}
