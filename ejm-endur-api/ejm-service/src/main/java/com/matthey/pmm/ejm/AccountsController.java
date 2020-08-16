package com.matthey.pmm.ejm;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.matthey.pmm.ejm.service.EJMService.API_PREFIX;

@SuppressWarnings("deprecation")
@Api(tags = {"Accounts"}, description = "APIs for relevant data of accounts")
@RestController
@RequestMapping(API_PREFIX)
public class AccountsController extends AbstractEJMController {

    public AccountsController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }

    @Cacheable({"account_balances"})
    @ApiOperation("originally ejmAccountBalance")
    @GetMapping("/account_balances")
    public String getAccountBalances(
            @ApiParam(value = "Account Number", example = "12917/01", required = true) @RequestParam String account,
            @ApiParam(value = "Reporting Date - use current date if missing", example = "06-Dec-2018")
            @RequestParam(required = false) String date) {
        var accountBalances = endurConnector.get("/account_balances?account={account}&&date={date}",
                                                 AccountBalance[].class,
                                                 account,
                                                 ObjectUtils.defaultIfNull(date,
                                                                           DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                                                                                   .format(LocalDate.now())));
        return genResponse(accountBalances, AccountBalance.class);
    }

    @Cacheable({"accounts"})
    @ApiOperation("originally ejmAccountExists")
    @GetMapping("/accounts")
    public String getAccount(
            @ApiParam(value = "Account Number", example = "12917/01", required = true) @RequestParam String account) {
        var accountDetail = endurConnector.get("/accounts?account={account}", Account[].class, account);
        return genResponse(accountDetail, Account.class);
    }
}
