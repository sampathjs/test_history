package com.matthey.pmm.tradebooking.processors;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthey.pmm.transaction.TransactionConverter;
import com.matthey.pmm.transaction.TransactionItemsListExecutor;
import com.matthey.pmm.transaction.TransactionTo;
import com.matthey.pmm.transaction.items.TransactionItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class FileProcessor {	
	private static final Logger logger = LogManager.getLogger(FileProcessor.class);

    private final Session session;

    private Transaction newDeal = null;

    private final int runId;
    private final int dealCounter;

    private final ConstRepository constRepo;
    private boolean executeDebugCommands;
    private LogTable logTable;
    private int currentLine;

    public FileProcessor(final Session session, final ConstRepository constRepo,
                         final int runId, final int dealCounter) {
        this.session = session;
        this.constRepo = constRepo;
        this.runId = runId;
        this.dealCounter = dealCounter;
        try {
            executeDebugCommands = Boolean.parseBoolean(constRepo.getStringValue("executeDebugCommands", "false"));
        } catch (Exception ex) {
            logger.error("Could not read or parse Const Repso entry " + constRepo.getContext()
                    + "\\" + constRepo.getSubcontext() + "\\executeDebugCommands that is expected to contain the String"
                    + " values 'true' or 'false'. Defaulting to false");
            executeDebugCommands = false;
        }
    }

    public int getLatestDealTrackingNum() {
        return newDeal != null ? newDeal.getDealTrackingId() : -1;
    }

    public boolean processFile(String fullPath) {
        logTable = new LogTable(session, runId, dealCounter);
        newDeal = null;

        ObjectMapper mapper = new ObjectMapper();
        TransactionTo transaction = null;
        try {
            transaction = mapper.readValue(Paths.get(fullPath).toFile(), TransactionTo.class);
        } catch (IOException e) {
            String message = (e instanceof DatabindException) ?
                    "Error while parsing JSON context of file '" + fullPath + "': " + e.toString() :
                    "Error while reading file '" + fullPath + "': " + e.toString();
            logger.error(message);
            for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
            }
        }
        TransactionConverter converter = new TransactionConverter(logTable);
        TransactionItemsListExecutor executor = new TransactionItemsListExecutor();
        List<? extends TransactionItem<?, ?, ?, ?>> transactionAsList;
        try {
            transactionAsList = converter.apply(session, transaction);
            logger.info(transactionAsList.stream().map(x -> x.toString()).collect(Collectors.joining("\n")));
        } catch (Throwable t) {
            logger.error("Error while generating action plan for transaction in file '" + fullPath + "': " + t.toString());
            for (StackTraceElement ste : t.getStackTrace()) {
                logger.error(ste.toString());
            }
            throw t;
        }
        try {
            executor.apply(transactionAsList);
        } catch (Throwable t) {
            logger.error("Error while executing action plan (booking trade) for transaction in file '" + fullPath + "': " + t.toString());
            for (StackTraceElement ste : t.getStackTrace()) {
                logger.error(ste.toString());
            }
            throw t;
        }

        logTable.persistToDatabase();
        if (executeDebugCommands) {
            logTable.showLogTableToUser();
        }
        return true;
    }
}
