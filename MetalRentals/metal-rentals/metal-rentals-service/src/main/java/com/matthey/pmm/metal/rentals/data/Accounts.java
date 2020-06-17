package com.matthey.pmm.metal.rentals.data;

import com.google.common.collect.Maps;
import com.matthey.pmm.metal.rentals.Account;
import com.matthey.pmm.metal.rentals.EndurConnector;
import com.matthey.pmm.metal.rentals.Region;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class Accounts {

    private final Map<String, Account> map;
    private final Map<Region, Map<String, Set<Account>>> groups;

    public Accounts(EndurConnector endurConnector) {
        var accounts = Arrays.asList(endurConnector.get("/accounts", Account[].class));
        map = Maps.uniqueIndex(accounts, Account::name);
        groups = accounts.stream().collect(groupingBy(Accounts::getRegion, groupingBy(Account::group, toSet())));
    }

    public static Region getRegion(Account account) {
        return Region.fromInternalBU(account.holder());
    }

    public Map<String, Account> asMap() {
        return this.map;
    }

    public Map<String, Set<Account>> asGroups(Region region) {
        return this.groups.get(region);
    }
}
