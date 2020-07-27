package com.matthey.pmm.ejm;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.matthey.pmm.ejm.service.EJMService.API_PREFIX;

@SuppressWarnings("deprecation")
@Api(tags = {"Documents"}, description = "APIs for retrieving paths of statements & specifications")
@RestController
@RequestMapping(API_PREFIX)
public class DocumentsController extends AbstractEJMController {

    public DocumentsController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }

    @Cacheable({"statements"})
    @ApiOperation("originally ejmStatements")
    @GetMapping("/statements")
    public String getStatement(
            @ApiParam(value = "Account Number", example = "13119/01", required = true) @RequestParam String account,
            @ApiParam(value = "Year", example = "2020", required = true) @RequestParam int year,
            @ApiParam(value = "Month", example = "February", required = true) @RequestParam String month,
            @ApiParam(value = "Type", example = "MATURED SUMMARY", required = true) @RequestParam String type) {
        var statements = endurConnector.get("/statements?account={account}&&year={year}&&month={month}&&type={type}",
                                            Statement[].class,
                                            account,
                                            year,
                                            month,
                                            type);
        return genResponse(statements, Statement.class);
    }

    @Cacheable({"specifications"})
    @ApiOperation("originally ejmSpecification")
    @GetMapping("/specifications")
    public String getSpecification(
            @ApiParam(value = "Trade Ref", example = "760783", required = true) @RequestParam String tradeRef,
            @ApiParam(value = "Spec Type", defaultValue = "Batch Spec")
            @RequestParam(required = false, defaultValue = "Batch Spec") String type) {
        var specifications = endurConnector.get("/specifications?tradeRef={tradeRef}&&type={type}",
                                                Specification[].class,
                                                tradeRef,
                                                type);
        return genResponse(specifications, Specification.class);
    }
}
