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
@Api(tags = {"Events"}, description = "APIs for relevant data of events")
@RestController
@RequestMapping(API_PREFIX)
public class EventsController extends AbstractEJMController {

    public EventsController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }

    @Cacheable({"events/daily_summary"})
    @ApiOperation("originally ejmDailyAccountBalance")
    @GetMapping("/events/daily_summary")
    public String getDailyAccountBalances(
            @ApiParam(value = "Account Number", example = "13119/01", required = true) @RequestParam String account,
            @ApiParam(value = "Metal Code", example = "XPT", required = true) @RequestParam String metal,
            @ApiParam(value = "Start Date", example = "01-May-2018", required = true) @RequestParam String startDate,
            @ApiParam(value = "End Date", example = "31-May-2018", required = true) @RequestParam String endDate) {
        var dailyAccountBalances = endurConnector.get(
                "/events/daily_summary?account={account}&&metal={metal}&&startDate={startDate}&&endDate={endDate}",
                DailyAccountBalance[].class,
                account,
                metal,
                startDate,
                endDate);
        return genResponse(dailyAccountBalances, DailyAccountBalance.class);
    }

    @Cacheable({"events/BS"})
    @ApiOperation("originally ejmTransactionDetailBS")
    @GetMapping("/events/BS")
    public String getEventsForBS(
            @ApiParam(value = "Account Number", example = "13354/02", required = true) @RequestParam String account,
            @ApiParam(value = "Trade Ref", example = "758954", required = true) @RequestParam String tradeRef) {
        var transactions = endurConnector.get("/events/BS?account={account}&&tradeRef={tradeRef}",
                                              BSTransaction[].class,
                                              account,
                                              tradeRef);
        return genResponse(transactions, BSTransaction.class);
    }

    @Cacheable({"events/DTR"})
    @ApiOperation("originally ejmTransactionDetailDTR")
    @GetMapping("/events/DTR")
    public String getEventsForDTR(
            @ApiParam(value = "Account Number", example = "13354/02", required = true) @RequestParam String account,
            @ApiParam(value = "Trade Ref", example = "764785", required = true) @RequestParam String tradeRef) {
        var transactions = endurConnector.get("/events/DTR?account={account}&&tradeRef={tradeRef}",
                                              DTRTransaction[].class,
                                              account,
                                              tradeRef);
        return genResponse(transactions, DTRTransaction.class);
    }

    @Cacheable({"events"})
    @ApiOperation("originally ejmTransactionListing")
    @GetMapping("/events")
    public String getEvents(
            @ApiParam(value = "Account Number", example = "12781/02", required = true) @RequestParam String account,
            @ApiParam(value = "Metal Code", example = "XPT", required = true) @RequestParam String metal,
            @ApiParam(value = "Start Date", example = "01-Apr-2018", required = true) @RequestParam String startDate,
            @ApiParam(value = "End Date", example = "30-Apr-2018", required = true) @RequestParam String endDate) {
        var transactions = endurConnector.get(
                "/events?account={account}&&metal={metal}&&startDate={startDate}&&endDate={endDate}",
                Transaction[].class,
                account,
                metal,
                startDate,
                endDate);
        return genResponse(transactions, Transaction.class);
    }

    @Cacheable({"events/specifications"})
    @ApiOperation("originally ejmSpecificationSummary")
    @GetMapping("/events/specifications")
    public String getEventsForSpec(
            @ApiParam(value = "Account Number", example = "12781/02", required = true) @RequestParam String account,
            @ApiParam(value = "Metal Code", example = "XPT", required = true) @RequestParam String metal,
            @ApiParam(value = "Start Date", example = "03-Jun-2018", required = true) @RequestParam String startDate,
            @ApiParam(value = "End Date", example = "03-Jun-2020", required = true) @RequestParam String endDate) {
        var specificationSummaries = endurConnector.get(
                "/events/specifications?account={account}&&metal={metals}&&startDate={startDate}&&endDate={endDate}",
                SpecificationSummary[].class,
                account,
                metal,
                startDate,
                endDate);
        return genResponse(specificationSummaries, SpecificationSummary.class);
    }
}
