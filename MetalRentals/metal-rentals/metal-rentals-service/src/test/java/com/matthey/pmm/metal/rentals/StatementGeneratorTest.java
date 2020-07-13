package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.document.DocumentTemplate;
import com.matthey.pmm.metal.rentals.document.StatementGenerator;
import com.matthey.pmm.metal.rentals.interest.ImmutableInterest;
import com.matthey.pmm.metal.rentals.interest.Interest;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.matthey.pmm.metal.rentals.RunResult.Failed;
import static com.matthey.pmm.metal.rentals.RunResult.Skipped;
import static com.matthey.pmm.metal.rentals.RunResult.Successful;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatementGeneratorTest {

    private final Map<String, Party> parties = Map.of("owner", genParty("owner"), "holder", genParty("holder"));
    @Mock
    DocumentTemplate documentTemplate;
    @Mock
    EndurConnector endurConnector;
    @TempDir
    Path statementRootDir;

    @SuppressWarnings("unchecked")
    @Test
    public void generate_statement_with_two_interests() {
        var sut = new StatementGenerator(documentTemplate, statementRootDir.toString(), endurConnector);
        var runs = sut.generate(Map.of("group", List.of(genInterest(), genInterest())),
                                parties,
                                LocalDate.of(2019, 11, 11),
                                "2019-10",
                                "test user");
        var statementPath = statementRootDir.toString() + "\\2019-10\\group.PDF";
        assertThat(runs).hasSize(1);
        var run = runs.get(0);
        assertThat(run.party()).isEqualTo("owner");
        assertThat(run.statementPath()).isEqualTo(statementPath);
        assertThat(run.accountGroup()).isEqualTo("group");
        assertThat(run.user()).isEqualTo("test user");
        assertThat(run.statementMonth()).isEqualTo("2019-10");
        assertThat(run.isSuccessful()).isTrue();
        verify(endurConnector, times(1)).saveRuns("/runs/statement_generating?user={user}", runs, "test user");
        var captor = ArgumentCaptor.forClass(Map.class);
        verify(documentTemplate, times(1)).generateDocument(captor.capture(), eq(statementPath));
        var variables = captor.getValue();
        assertThat(variables.get("current_date")).isEqualTo("11-Nov-2019");
        assertThat(variables.get("unit")).isEqualTo(Lists.newArrayList("TOz", "TOz"));
        assertThat(variables.get("ccy")).isEqualTo("USD");
        assertThat(variables.get("metal")).isEqualTo(Lists.newArrayList("Gold", "Gold"));
        assertThat(variables.get("interest_rate")).isEqualTo(Lists.newArrayList("0.09", "0.09"));
        assertThat(variables.get("holder_address")).isEqualTo("address");
        assertThat(variables.get("owner_address")).isEqualTo("address");
        assertThat(variables.get("sum")).isEqualTo("-2.09");
        assertThat(variables.get("avg_price")).isEqualTo(Lists.newArrayList("136.79", "136.79"));
        assertThat(variables.get("value")).isEqualTo(Lists.newArrayList("-1.05", "-1.05"));
        assertThat(variables.get("account")).isEqualTo(Lists.newArrayList("account", "account"));
    }

    @Test
    public void do_not_recreate_folder_if_already_exist() {
        var sut = new StatementGenerator(documentTemplate, statementRootDir.toString(), endurConnector);
        sut.generate(Map.of("group", List.of(genInterest())), parties, LocalDate.now(), "2019-10", "test user");
        sut.generate(Map.of("group", List.of(genInterest())), parties, LocalDate.now(), "2019-10", "test user");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void skip_generating_if_exist() throws IOException {
        var sut = new StatementGenerator(documentTemplate, statementRootDir.toString(), endurConnector);
        statementRootDir.resolve("2019-10").toFile().mkdirs();
        statementRootDir.resolve("2019-10/group.PDF").toFile().createNewFile();
        var runs = sut.generate(Map.of("group", List.of(genInterest())),
                                parties,
                                LocalDate.now(),
                                "2019-10",
                                "test user");
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).result()).isEqualTo(Skipped);
    }

    @Test
    public void generate_two_statements() {
        var sut = new StatementGenerator(documentTemplate, statementRootDir.toString(), endurConnector);
        var runs = sut.generate(Map.of("group1", List.of(genInterest()), "group2", List.of(genInterest())),
                                parties,
                                LocalDate.of(2019, 11, 11),
                                "2019-10",
                                "test user");
        verify(documentTemplate, times(1)).generateDocument(anyMap(),
                                                            eq(statementRootDir.toString() + "\\2019-10\\group1.PDF"));
        verify(documentTemplate, times(1)).generateDocument(anyMap(),
                                                            eq(statementRootDir.toString() + "\\2019-10\\group2.PDF"));
        assertThat(runs).hasSize(2);
        assertThat(runs.stream().map(Run::runTime).collect(toSet())).hasSize(1);
        assertThat(runs.stream().map(Run::statementMonth).collect(toSet())).containsOnly("2019-10");
        assertThat(runs.stream().map(Run::user).collect(toSet())).containsOnly("test user");
        assertThat(runs.stream().map(Run::isSuccessful).collect(toSet())).containsOnly(true);
    }

    @Test
    public void do_not_generate_statement_when_no_interest() {
        var sut = new StatementGenerator(documentTemplate, statementRootDir.toString(), endurConnector);
        var runs = sut.generate(Map.of("group", List.of()), parties, LocalDate.of(2019, 11, 11), "", "");
        verify(documentTemplate, never()).generateDocument(anyMap(), anyString());
        assertThat(runs).isEmpty();
    }

    @Test
    public void generate_other_statements_even_one_fails() {
        lenient().doThrow(RuntimeException.class)
                .when(documentTemplate)
                .generateDocument(anyMap(), eq(statementRootDir.toString() + "\\2019-10\\group1.PDF"));

        var sut = new StatementGenerator(documentTemplate, statementRootDir.toString(), endurConnector);
        var result = sut.generate(Map.of("group1", List.of(genInterest()), "group2", List.of(genInterest())),
                                  parties,
                                  LocalDate.of(2019, 11, 11),
                                  "2019-10",
                                  "");
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(Run::result).collect(Collectors.toSet())).containsOnly(Successful, Failed);
    }

    private Party genParty(String name) {
        return ImmutableParty.builder()
                .name(name)
                .address("address")
                .telephone("1111111")
                .vatNumber("GB111111")
                .build();
    }

    private Interest genInterest() {
        return ImmutableInterest.builder()
                .group("group")
                .account("account")
                .metal("XAU")
                .unit("TOz")
                .currency("USD")
                .averageBalanceInTOz(123.123)
                .averageBalance(100d)
                .averagePriceForTOz(111.1)
                .interestRate(0.0009)
                .numOfDays(31)
                .daysOfYear(365)
                .owner("owner")
                .holder("holder")
                .build();
    }
}