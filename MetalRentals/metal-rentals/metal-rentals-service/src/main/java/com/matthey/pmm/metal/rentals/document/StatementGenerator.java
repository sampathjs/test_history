package com.matthey.pmm.metal.rentals.document;

import com.google.common.collect.Maps;
import com.matthey.pmm.metal.rentals.EndurConnector;
import com.matthey.pmm.metal.rentals.ImmutableStatementGeneratingRun;
import com.matthey.pmm.metal.rentals.Party;
import com.matthey.pmm.metal.rentals.StatementGeneratingRun;
import com.matthey.pmm.metal.rentals.interest.Interest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.matthey.pmm.metal.rentals.PropertyChecker.checkAndReturn;
import static com.matthey.pmm.metal.rentals.RunResult.Failed;
import static com.matthey.pmm.metal.rentals.RunResult.Skipped;
import static com.matthey.pmm.metal.rentals.RunResult.Successful;
import static com.matthey.pmm.metal.rentals.document.DocumentGenerator.METAL_NAMES;
import static com.matthey.pmm.metal.rentals.document.DocumentGenerator.formatDate;
import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
import static java.util.stream.Collectors.toList;

@Component
public class StatementGenerator {

    private static final Logger logger = LoggerFactory.getLogger(StatementGenerator.class);
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.00");

    private final DocumentTemplate documentTemplate;
    private final String statementRootDir;
    private final EndurConnector endurConnector;

    public StatementGenerator(DocumentTemplate documentTemplate,
                              @Value("${statement.root.dir}") String statementRootDir,
                              EndurConnector endurConnector) {
        this.documentTemplate = documentTemplate;
        this.statementRootDir = checkAndReturn(statementRootDir,
                                               Files.isDirectory(Path.of(statementRootDir)),
                                               "statement root dir");
        this.endurConnector = endurConnector;
    }

    public List<StatementGeneratingRun> generate(Map<String, List<Interest>> interests,
                                                 Map<String, Party> parties,
                                                 LocalDate statementDate,
                                                 String statementMonth,
                                                 String user) {
        var statementDir = getStatementDir(statementMonth);
        var runTime = LocalDateTime.now(ZoneOffset.UTC).toString();
        ForkJoinPool customThreadPool = new ForkJoinPool(33);
        var runs = sneak(() -> customThreadPool.submit(() -> interests.entrySet()
                .parallelStream()
                .map(entry -> generateStatement(entry.getKey(),
                                                entry.getValue(),
                                                parties,
                                                statementDate,
                                                statementDir,
                                                runTime,
                                                statementMonth,
                                                user))
                .filter(Objects::nonNull)
                .collect(toList())).get());
        endurConnector.saveRuns("/runs/statement_generating?user={user}", runs, user);
        return runs;
    }

    private Path getStatementDir(String statementMonth) {
        var path = Paths.get(statementRootDir, statementMonth);
        if (!Files.exists(path)) {
            var created = path.toFile().mkdirs();
            checkState(created, "failed to create report folder: {}", path);
        }
        return path;
    }

    private StatementGeneratingRun generateStatement(String group,
                                                     List<Interest> interests,
                                                     Map<String, Party> parties,
                                                     LocalDate statementDate,
                                                     Path statementDir,
                                                     String runTime,
                                                     String statementMonth,
                                                     String user) {
        if (interests.isEmpty()) {
            return null;
        }

        var statementPath = statementDir.resolve(group + ".PDF");

        var holder = interests.stream().map(Interest::holder).distinct().collect(onlyElement());
        var owner = interests.stream().map(Interest::owner).distinct().collect(onlyElement());
        var successfulRun = ImmutableStatementGeneratingRun.builder()
                .user(user)
                .runTime(runTime)
                .statementMonth(statementMonth)
                .result(Successful)
                .party(owner)
                .accountGroup(group)
                .statementPath(statementPath.toString())
                .build();

        try {
            logger.info("generating statement: group -> {}, interests -> {}", group, interests);

            if (Files.isRegularFile(statementPath)) {
                return successfulRun.withResult(Skipped);
            }

            Map<String, Object> variables = Maps.newHashMap();
            variables.put("holder_address", parties.get(holder).address());
            variables.put("owner_address", parties.get(owner).address());
            variables.put("current_date", formatDate(statementDate));
            variables.put("ccy", interests.stream().map(Interest::currency).distinct().collect(onlyElement()));
            variables.put("sum", NUMBER_FORMAT.format(interests.stream().mapToDouble(Interest::value).sum()));
            variables.put("account", interests.stream().map(Interest::account).collect(toList()));
            variables.put("metal", interests.stream().map(Interest::metal).map(METAL_NAMES::get).collect(toList()));
            variables.put("unit", interests.stream().map(Interest::unit).collect(toList()));
            variables.put("avg_balance",
                          interests.stream()
                                  .map(Interest::averageBalance)
                                  .map(NUMBER_FORMAT::format)
                                  .collect(toList()));
            variables.put("avg_price",
                          interests.stream().map(Interest::averagePrice).map(NUMBER_FORMAT::format).collect(toList()));
            variables.put("interest_rate",
                          interests.stream()
                                  .map(Interest::interestRate)
                                  .map(r -> r * 100)
                                  .map(NUMBER_FORMAT::format)
                                  .collect(toList()));
            variables.put("value",
                          interests.stream().map(Interest::value).map(NUMBER_FORMAT::format).collect(toList()));
            documentTemplate.generateDocument(variables, statementPath.toString());

            return successfulRun;
        } catch (Exception e) {
            logger.error("error occurred when generating statement: " + e.getMessage(), e);
            return successfulRun.withResult(Failed);
        }
    }
}
