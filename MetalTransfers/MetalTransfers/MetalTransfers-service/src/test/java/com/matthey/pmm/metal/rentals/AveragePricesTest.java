package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.metal.transfers.ClosingPrice;
import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.data.AveragePrices;
import com.matthey.pmm.metal.transfers.data.StatementPeriods;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AveragePricesTest {

    @Mock
    private EndurConnector endurConnector;

    @Test
    public void calculate_average_price_per_currency_per_metal() {
        final var XAU_USD = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-08", 1562.11),
                                               ImmutableClosingPrice.of("2019-11-11", 1563.22),
                                               ImmutableClosingPrice.of("2019-11-30", 1564.33)};
        final var XAU_GBP = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-01", 1195.00),
                                               ImmutableClosingPrice.of("2019-11-02", 1196.00),
                                               ImmutableClosingPrice.of("2019-11-03", 1197.00)};
        final var XAG_USD = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-08", 18.22),
                                               ImmutableClosingPrice.of("2019-11-11", 18.44)};
        final var XAG_GBP = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-11", 14.00),
                                               ImmutableClosingPrice.of("2019-11-22", 15.00)};
        final var XPT_USD = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-11", 738.00)};
        final var XPT_GBP = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-11", 692.00)};
        final var CNY_USD = new ClosingPrice[]{ImmutableClosingPrice.of("2019-11-30", 7.00)};
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XAU.USD"),
                                eq("LBMA PM"),
                                eq("2019-11-01"),
                                eq("2019-11-30"))).thenReturn(XAU_USD);
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XAU.USD"),
                                eq("LBMA PM"),
                                eq("2019-10-26"),
                                eq("2019-11-25"))).thenReturn(new ClosingPrice[]{XAU_USD[2]});
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XAU.GBP"),
                                eq("LBMA PM"),
                                eq("2019-11-01"),
                                eq("2019-11-30"))).thenReturn(XAU_GBP);
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XAG.USD"),
                                eq("LBMA Silver"),
                                eq("2019-11-01"),
                                eq("2019-11-30"))).thenReturn(XAG_USD);
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XAG.USD"),
                                eq("LBMA Silver"),
                                eq("2019-10-26"),
                                eq("2019-11-25"))).thenReturn(new ClosingPrice[]{});
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XAG.GBP"),
                                eq("LBMA Silver"),
                                eq("2019-11-01"),
                                eq("2019-11-30"))).thenReturn(XAG_GBP);
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XPT.USD"),
                                eq("JM London Opening"),
                                eq("2019-11-01"),
                                eq("2019-11-30"))).thenReturn(XPT_USD);
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XPT.USD"),
                                eq("JM HK Opening"),
                                eq("2019-10-26"),
                                eq("2019-11-25"))).thenReturn(new ClosingPrice[]{});
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("XPT.GBP"),
                                eq("JM London Opening"),
                                eq("2019-11-01"),
                                eq("2019-11-30"))).thenReturn(XPT_GBP);
        when(endurConnector.get(anyString(),
                                eq(ClosingPrice[].class),
                                eq("FX_USD.CNY"),
                                eq("BOC"),
                                eq("2019-10-26"),
                                eq("2019-11-25"))).thenReturn(CNY_USD);

        var sut = new AveragePrices(endurConnector,
                                    List.of("XAU", "XAG", "XPT"),
                                    List.of("USD", "GBP"),
                                    new StatementPeriods(LocalDate.of(2019, 12, 1)));
        var actual = sut.getMetalPrices(Region.NonCN);
        var expected = Map.of("USD",
                              Map.of("XAU", 1563.22, "XAG", 18.33, "XPT", 738.00),
                              "GBP",
                              Map.of("XAU", 1196.00, "XAG", 14.50, "XPT", 692.00));

        assertThat(actual).containsOnlyKeys("USD", "GBP");
        assertThat(actual.get("USD")).hasSize(3);
        assertThat(actual.get("USD")).containsAllEntriesOf(expected.get("USD"));
        assertThat(actual.get("GBP")).hasSize(3);
        assertThat(actual.get("GBP")).containsAllEntriesOf(expected.get("GBP"));

        var actualCN = sut.getMetalPrices(Region.CN);
        var expectedCN = Map.of("CNY", Map.of("XAU", 1564.33 * 7.00, "XAG", 0.0, "XPT", 0.0));
        assertThat(actualCN).containsOnlyKeys("CNY");
        assertThat(actualCN.get("CNY")).hasSize(3);
        assertThat(actualCN.get("CNY")).containsAllEntriesOf(expectedCN.get("CNY"));
        assertThat(sut.getUsdCnyRate()).isEqualTo(7.00);

        verifyEndurConnectorCall("XAU.USD", "LBMA PM", "2019-11-01", "2019-11-30");
        verifyEndurConnectorCall("XAU.USD", "LBMA PM", "2019-10-26", "2019-11-25");
        verifyEndurConnectorCall("XAU.GBP", "LBMA PM", "2019-11-01", "2019-11-30");
        verifyEndurConnectorCall("XAG.USD", "LBMA Silver", "2019-11-01", "2019-11-30");
        verifyEndurConnectorCall("XAG.USD", "LBMA Silver", "2019-10-26", "2019-11-25");
        verifyEndurConnectorCall("XAG.GBP", "LBMA Silver", "2019-11-01", "2019-11-30");
        verifyEndurConnectorCall("XPT.USD", "JM London Opening", "2019-11-01", "2019-11-30");
        verifyEndurConnectorCall("XPT.USD", "JM HK Opening", "2019-10-26", "2019-11-25");
        verifyEndurConnectorCall("XPT.GBP", "JM London Opening", "2019-11-01", "2019-11-30");
        verifyEndurConnectorCall("FX_USD.CNY", "BOC", "2019-10-26", "2019-11-25");
    }

    private void verifyEndurConnectorCall(String indexName, String refSource, String startDate, String endDate) {
        verify(endurConnector, times(1)).get(eq(
                "/closing_prices/{indexName}/{refSource}?startDate={startDate}&&endDate={endDate}"),
                                             eq(ClosingPrice[].class),
                                             eq(indexName),
                                             eq(refSource),
                                             eq(startDate),
                                             eq(endDate));
    }
}