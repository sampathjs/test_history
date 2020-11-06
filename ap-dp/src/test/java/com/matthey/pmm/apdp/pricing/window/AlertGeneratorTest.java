package com.matthey.pmm.apdp.pricing.window;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AlertGeneratorTest {
    
    @Test
    public void html_content_is_generated() {
        Set<CheckResult> checkResults = ImmutableSet.of(ImmutableCheckResult.builder()
                                                                .pricingType("AP")
                                                                .dealNum("11")
                                                                .customer("Customer1")
                                                                .dealDate("2020-09-15")
                                                                .expiryDate("2020-10-15")
                                                                .numOfDaysToExpiry(0)
                                                                .unmatchedVolume(111.111)
                                                                .build(),
                                                        ImmutableCheckResult.builder()
                                                                .pricingType("DP")
                                                                .dealNum("22")
                                                                .customer("Customer2")
                                                                .dealDate("2020-09-15")
                                                                .expiryDate("2020-10-15")
                                                                .numOfDaysToExpiry(1)
                                                                .unmatchedVolume(222)
                                                                .build(),
                                                        ImmutableCheckResult.builder()
                                                                .pricingType("AP")
                                                                .dealNum("33")
                                                                .customer("Customer1")
                                                                .dealDate("2020-09-15")
                                                                .expiryDate("2020-10-15")
                                                                .numOfDaysToExpiry(2)
                                                                .unmatchedVolume(0.11)
                                                                .build());
        AlertGenerator sut = new AlertGenerator(this::assertSubjectAndContent);
        sut.generateAndSendAlert(LocalDate.of(2020, 10, 1), LocalDateTime.of(2020, 10, 1, 11, 11), checkResults);
    }
    
    private void assertSubjectAndContent(String subject, String content) {
        assertThat(subject).isEqualTo("AP DP Deals Expiring 2020-10-01");
        String expected = getExpectedFromFile();
        assertThat(content).isEqualTo(expected);
    }
    
    private String getExpectedFromFile() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("expected-alert.html")) {
            assert inputStream != null;
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}