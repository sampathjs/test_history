package com.matthey.pmm.tradebooking.processors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthey.pmm.tradebooking.TransactionConverter;
import com.matthey.pmm.tradebooking.TransactionItemsListExecutor;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.matthey.pmm.tradebooking.items.TransactionItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.LicenseType;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public class FileProcessor {

    private static Logger logger = null;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TransactionItemsListExecutor executor = new TransactionItemsListExecutor();

    private final Session session;

    private Transaction newDeal = null;

    private final int runId;
    private final int dealCounter;

    private boolean executeDebugCommands;

    private static Logger getLogger() {
        if (logger == null) {
            logger = LogManager.getLogger(FileProcessor.class);
        }
        return logger;
    }

    public FileProcessor(final Session session, final ConstRepository constRepo, final int runId, final int dealCounter) {

        this.session = session;
        this.runId = runId;
        this.dealCounter = dealCounter;
        try {
            executeDebugCommands = Boolean.parseBoolean(constRepo.getStringValue("executeDebugCommands", "false"));
            LicenseType[] types = session.getUser().getLicenseTypes();
            for (int i=0; i < types.length; i++) {
            	if (types[i].getName().equalsIgnoreCase("Server")) {
                    getLogger().info("Running as server user - skipping debug commands"); 
                    executeDebugCommands = false;
            	}
            }
        } catch (Exception ex) {
            getLogger().error("Could not read or parse Const Repo entry " + constRepo.getContext()
                    + "\\" + constRepo.getSubcontext() + "\\executeDebugCommands that is expected to contain the String"
                    + " values 'true' or 'false'. Defaulting to false");
            executeDebugCommands = false;
        }
    }

    public boolean processFile(String fullPath) {

        val logTable = new LogTable(session, runId, dealCounter, fullPath);
        try {
            Optional<Transaction> tran = Optional.of(fullPath)
                    .flatMap(this::toTransctionTo)
                    .flatMap(toTransactionItemsList(session, fullPath, logTable))
                    .flatMap(toTransaction(fullPath));
            if (tran.isPresent()) {
            	tran.get().close();
            }
            return tran.isPresent();
        } catch (Throwable t) {
            getLogger().error("Error while executing action plan (booking trade) for transaction in file '" + fullPath + "': " + t.toString());
            StringWriter sw = new StringWriter(4000);
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            getLogger().error(sw.toString());
            return false;
        } finally {
            getLogger().info("Persisting log table to database");
            logTable.persistToDatabase();
            getLogger().info("Successfully persisted log table to database");
            if (executeDebugCommands) {
                logTable.showLogTableToUser();
            }
        }
    }

    public int getLatestDealTrackingNum() {
        return newDeal != null ? newDeal.getDealTrackingId() : -1;
    }

    private Optional<TransactionTo> toTransctionTo(String fullPath) {
        try {
            getLogger().info("Reading input file '" + fullPath + "' and converting it to JSON Transaction Object");
            val transactionTo = mapper.readValue(Paths.get(fullPath).toFile(), TransactionTo.class);
            getLogger().info("Successfully read input file '" + fullPath + "'. Conversion succeeded");
            return Optional.of(transactionTo);
        } catch (IOException e) {
            String message = (e instanceof JsonParseException || e instanceof JsonMappingException) ?
                    "Error while parsing JSON context of file '" + fullPath + "': " + e.toString() :
                    "Error while reading file '" + fullPath + "': " + e.toString();
            getLogger().error(message);
            StringWriter sw = new StringWriter(4000);
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            getLogger().error(sw.toString());
            return Optional.empty();
        }
    }

    private Function<TransactionTo, Optional<List<? extends TransactionItem<?, ?, ?, ?>>>> toTransactionItemsList(Session session,
                                                                                                                  String fullPath,
                                                                                                                  LogTable logTable) {
        return transactionTo -> {
            val converter = new TransactionConverter(session, logTable, executeDebugCommands);
            try {
                getLogger().info("Converting parsed JSON object");
                val transactionItems = Optional.of(transactionTo).map(converter);
                getLogger().info("Converted parsed JSON object. ");
                return transactionItems;
            } catch (Throwable t) {
                getLogger().error("Error while generating action plan for transaction in file '" + fullPath + "': " + t.toString());
                StringWriter sw = new StringWriter(4000);
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                getLogger().error(sw.toString());
                logTable.persistToDatabase();
                return Optional.empty();
            }
        };
    }

    private Function<List<? extends TransactionItem<?, ?, ?, ?>>, Optional<Transaction>> toTransaction(String fullPath) {
        return transactionItems -> {
            try {
                getLogger().info("Executing action plan to book deal");
                val result = Optional.of(transactionItems)
                        .map(executor)
                        // this is not really nice...
                        .map(transaction -> {
                            newDeal = transaction;
                            return transaction;
                        });
                getLogger().info("Successfully executed action plan");
                return result;
            } catch (Throwable t) {
                getLogger().error("Error while executing action plan (booking trade) for transaction in file '" + fullPath + "': " + t.toString());
                StringWriter sw = new StringWriter(4000);
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                getLogger().error(sw.toString());
                return Optional.empty();
            }
        };
    }
}
