package com.matthey.pmm.tradebooking.processors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthey.pmm.tradebooking.TransactionConverter;
import com.matthey.pmm.tradebooking.TransactionItemsListExecutor;
import com.matthey.pmm.tradebooking.items.TransactionItem;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class FileProcessor {	
	private static Logger logger = null;

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
            getLogger().error("Could not read or parse Const Repo entry " + this.constRepo.getContext()
                    + "\\" + this.constRepo.getSubcontext() + "\\executeDebugCommands that is expected to contain the String"
                    + " values 'true' or 'false'. Defaulting to false");
            executeDebugCommands = false;
        }
    }

    public int getLatestDealTrackingNum() {
        return newDeal != null ? newDeal.getDealTrackingId() : -1;
    }

    public boolean processFile(String fullPath) {
    	try {
            logTable = new LogTable(session, runId, dealCounter, fullPath);
            newDeal = null;

            ObjectMapper mapper = new ObjectMapper();
            TransactionTo transaction = null;
            try {
            	getLogger().info("Reading input file '" + fullPath + "' and converting it to JSON Transaction Object");
                transaction = mapper.readValue(Paths.get(fullPath).toFile(), TransactionTo.class);
            	getLogger().info("Successfully read input file '" + fullPath + "'. Conversion succeeded");
            } catch (IOException e) {
                String message = (e instanceof JsonParseException || e instanceof JsonMappingException) ?
                        "Error while parsing JSON context of file '" + fullPath + "': " + e.toString() :
                        "Error while reading file '" + fullPath + "': " + e.toString();
                getLogger().error(message);
        		StringWriter sw = new StringWriter(4000);
        		PrintWriter pw = new PrintWriter(sw);
        		e.printStackTrace(pw);
        		logger.error(sw.toString());
                return false;
            }
            TransactionConverter converter = new TransactionConverter(logTable);
            TransactionItemsListExecutor executor = new TransactionItemsListExecutor();
            List<? extends TransactionItem<?, ?, ?, ?>> transactionAsList;
            try {
                getLogger().info("Converting parsed JSON object");
                transactionAsList = converter.apply(session, transaction);
                getLogger().info("Converted parsed JSON object. ");
            } catch (Throwable t) {
                getLogger().error("Error while generating action plan for transaction in file '" + fullPath + "': " + t.toString());
        		StringWriter sw = new StringWriter(4000);
        		PrintWriter pw = new PrintWriter(sw);
        		t.printStackTrace(pw);
        		logger.error(sw.toString());
                logTable.persistToDatabase();
                return false;
            }
            try {
                getLogger().info("Executing action plan to book deal");
                newDeal = executor.apply(transactionAsList);
                getLogger().info("Successfully executed action plan");
            } catch (Throwable t) {
                getLogger().error("Error while executing action plan (booking trade) for transaction in file '" + fullPath + "': " + t.toString());
        		StringWriter sw = new StringWriter(4000);
        		PrintWriter pw = new PrintWriter(sw);
        		t.printStackTrace(pw);
        		logger.error(sw.toString());
                return false;
            }    		
    	} finally {
            getLogger().info("Persisting log table to database");
            logTable.persistToDatabase();
            getLogger().info("Successfully persisted log table to database");
            if (executeDebugCommands) {
                logTable.showLogTableToUser();
            }    		
    	}
        return true;
    }
}
