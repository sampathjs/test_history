package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.InterestRate;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.data.InterestRates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestRatesTest {

    @Mock
    private EndurConnector endurConnector;

    private InterestRates sut;

    @BeforeEach
    public void setUp() {
        var rates = new InterestRate[]{ImmutableInterestRate.of("XAU", 1.11),
                                       ImmutableInterestRate.of("XAG", 2.22),
                                       ImmutableInterestRate.of("XPT", 3.33)};
        when(endurConnector.get(anyString(), eq(InterestRate[].class), anyString())).thenReturn(rates);

        sut = new InterestRates(endurConnector, List.of("XAU", "XAG"), Map.of(Region.NonCN, "test index"));
    }

    @Test
    public void only_get_interest_rates_for_declared_metals() {
        var actual = sut.get(Region.NonCN);
        verify(endurConnector, times(1)).get("/interest_rates/{indexName}", InterestRate[].class, "test index");
        assertThat(actual).hasSize(2);
        assertThat(actual).containsAllEntriesOf(Map.of("XAU", 1.11, "XAG", 2.22));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void only_update_interest_rates_for_declared_metals() {
        sut.update("test_user", Region.NonCN, Map.of("XAU", 1.11, "XAG", 2.22, "XPT", 3.33));
        var captor = ArgumentCaptor.forClass(Set.class);
        verify(endurConnector, times(1)).put(eq("/interest_rates/{indexName}?user={user}"),
                                             captor.capture(),
                                             eq("test index"),
                                             eq("test_user"));
        assertThat(captor.getValue()).containsOnly(ImmutableInterestRate.of("XAU", 1.11),
                                                   ImmutableInterestRate.of("XAG", 2.22));
    }

    @Test
    public void throw_error_when_region_is_invalid() {
        var errMsg = "invalid region for interest rates: CN";
        assertThatIllegalArgumentException().isThrownBy(() -> sut.get(Region.CN)).withMessage(errMsg);
        assertThatIllegalArgumentException().isThrownBy(() -> sut.update("", Region.CN, Map.of())).withMessage(errMsg);
    }
}