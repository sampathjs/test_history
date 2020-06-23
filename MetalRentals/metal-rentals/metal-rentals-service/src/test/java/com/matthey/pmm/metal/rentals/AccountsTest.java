package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.data.Accounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.matthey.pmm.metal.rentals.TestUtils.genAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsTest {

    @Mock
    private EndurConnector endurConnector;

    @Test
    public void get_account_map() {
        var accounts = new Account[]{genAccount("Account 1", null, false),
                                     genAccount("Account 2", null, false),
                                     genAccount("Account 3", null, true)};
        when(endurConnector.get(any(String.class), eq(Account[].class))).thenReturn(accounts);

        var sut = new Accounts(endurConnector);
        var actual = sut.asMap();

        verify(endurConnector, times(1)).get("/accounts", Account[].class);
        assertThat(actual).containsOnlyKeys("Account 1", "Account 2", "Account 3");
    }

    @Test
    public void group_accounts_for_group_borrowings() {
        var accounts = new Account[]{genAccount("JM REFINING UK@PMM UK-ROY", null, false),
                                     genAccount("JM REFINING UK@PMM UK-ROY/ING", null, false),
                                     genAccount("JM REFINING UK@PMM UK-ROY/GRA", null, false),
                                     genAccount("JM NM AUSTRALIA@PMM UK-ROY", null, false),
                                     genAccount("SHANGHAI CST@PMM CN", null, true)};
        when(endurConnector.get(any(String.class), eq(Account[].class))).thenReturn(accounts);

        var sut = new Accounts(endurConnector);
        var actualForNonCN = sut.asGroups(Region.NonCN);
        var actualForCN = sut.asGroups(Region.CN);

        verify(endurConnector, times(1)).get("/accounts", Account[].class);
        assertThat(actualForNonCN).containsOnlyKeys("JM REFINING UK@PMM UK-ROY", "JM NM AUSTRALIA@PMM UK-ROY");
        assertThat(actualForNonCN.get("JM REFINING UK@PMM UK-ROY")).hasSize(3);
        assertThat(actualForNonCN.get("JM NM AUSTRALIA@PMM UK-ROY")).hasSize(1);
        assertThat(actualForCN).containsOnlyKeys("SHANGHAI CST@PMM CN");
        assertThat(actualForCN.get("SHANGHAI CST@PMM CN")).hasSize(1);
    }

    @Test
    public void group_accounts_for_internal_borrowings() {
        var accounts = new Account[]{genAccount("INV BORROWING - UK@PMM US-VF", "UK-US", false),
                                     genAccount("INV BORROWING-UK@PMM HK-HK", "UK-HK", false)};
        when(endurConnector.get(any(String.class), eq(Account[].class))).thenReturn(accounts);

        var sut = new Accounts(endurConnector);
        var actual = sut.asGroups(Region.NonCN);

        verify(endurConnector, times(1)).get("/accounts", Account[].class);
        assertThat(actual).containsOnlyKeys("UK-US", "UK-HK");
        assertThat(actual.get("UK-US")).hasSize(1);
        assertThat(actual.get("UK-HK")).hasSize(1);
    }
}