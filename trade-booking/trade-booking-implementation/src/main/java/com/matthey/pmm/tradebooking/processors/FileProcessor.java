package com.matthey.pmm.tradebooking.processors;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthey.pmm.tradebooking.TransactionConverter;
import com.matthey.pmm.tradebooking.TransactionItemsListExecutor;
import com.matthey.pmm.tradebooking.items.TransactionItem;
import com.matthey.pmm.tradebooking.TransactionTo;
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
	private static  Logger logger = null;

    private final Session session;

    private Transaction newDeal = null;

    private final int runId;
    private final int dealCounter;

    private final ConstRepository constRepo;
    private boolean executeDebugCommands;
    private LogTable logTable;
    
	private static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger(FileProcessor.class);
		}
		return logger;
	}

    public FileProcessor(final Session session, final ConstRepository constRepo,
                         final int runId, final int dealCounter) {
        this.session = session;
        this.constRepo = constRepo;
        this.runId = runId;
        this.dealCounter = dealCounter;
        try {
            executeDebugCommands = Boolean.parseBoolean(this.constRepo.getStringValue("executeDebugCommands", "false"));
        } catch (Exception ex) {
            getLogger().error("Could not read or parse Const Repso entry " + this.constRepo.getContext()
                    + "\\" + this.constRepo.getSubcontext() + "\\executeDebugCommands that is expected to contain the String"
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
            getLogger().error(message);
            for (StackTraceElement ste : e.getStackTrace()) {
                getLogger().error(ste.toString());
            }
        }
        TransactionConverter converter = new TransactionConverter(logTable);
        TransactionItemsListExecutor executor = new TransactionItemsListExecutor();
        List<? extends TransactionItem<?, ?, ?, ?>> transactionAsList;
        try {
            transactionAsList = converter.apply(session, transaction);
            getLogger().info(transactionAsList.stream().map(x -> x.toString()).collect(Collectors.joining("\n")));
        } catch (Throwable t) {
            getLogger().error("Error while generating action plan for transaction in file '" + fullPath + "': " + t.toString());
            for (StackTraceElement ste : t.getStackTrace()) {
                getLogger().error(ste.toString());
            }
            throw t;
        }
        try {
            newDeal = executor.apply(transactionAsList);
        } catch (Throwable t) {
            getLogger().error("Error while executing action plan (booking trade) for transaction in file '" + fullPath + "': " + t.toString());
            for (StackTraceElement ste : t.getStackTrace()) {
                getLogger().error(ste.toString());
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
