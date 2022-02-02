package com.matthey.pmm.connector.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestAlreadyExists;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestErrorWhileProcessing;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestFileOperationException;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestIllegalJsonData;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.matthey.pmm.tradebooking.processors.RunProcessor;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.openlink.util.constrepository.ConstRepository;

@RestController
@RequestMapping("/shared")
public class EndurConnectorSharedController {
	private static final Logger logger = LogManager.getLogger(EndurConnectorSharedController.class);
	
	private static final String CONTEXT = "EndurConnectorShared";
	private static final String SUBCONTEXT = "TradeBooking";
	private static final String VAR_BASE_DIR = "BaseDirectory"; 
    
    private final Session session;

    @Autowired
    @Qualifier ("PrettyFormatter")
    private ObjectMapper prettyFormatter;

    
    public EndurConnectorSharedController(Session session) {
        this.session = session;
    }
    
    @PostMapping("/tradeBookingJson")
    public int postTradeBookingRequest(@RequestParam String clientName,
    		@RequestParam String fileName,
    		@RequestParam boolean overwrite,
    		@RequestBody TransactionTo tradeBookingActionPlan) {
    	try {
    		logger.info("/tradeBookingJson endpoint called with clientName='" + clientName  
    				+ "', fileName='" + fileName + "', overwrite=" + overwrite + " and tradeBookingActionPlan = \n" + tradeBookingActionPlan.toString());
        	String asTextFile = prettyFormatter.writeValueAsString(tradeBookingActionPlan);
        	int dealTrackingNumOfBookedDeal = postTradeBookingRequest (clientName, fileName, overwrite, asTextFile);
        	return dealTrackingNumOfBookedDeal;
    	} catch (JsonProcessingException ex) {
    		String msg = "Error while convertig JSON object for formatted text: " + ex.toString();
    		logger.error (msg);
    		StringWriter sw = new StringWriter(4000);
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(sw.toString());
            throw new TradeBookingRequestIllegalJsonData (msg);
    	}
    }
    
    @PostMapping("/tradeBooking")
    public int postTradeBookingRequest(@RequestParam String clientName,
    		@RequestParam String fileName,
    		@RequestParam boolean overwrite,
    		@RequestBody String tradeBookingActionPlan) {
        ConstRepository constRepo = initConstRepo(CONTEXT, SUBCONTEXT); 
        File inputFile = saveFileToLocalFolder(clientName, fileName, tradeBookingActionPlan, overwrite, constRepo);
        RunProcessor rp = new RunProcessor(session, constRepo, clientName, Arrays.asList(inputFile.getPath()));
        try {
        	boolean success = rp.processRun();
        	if (!success) {
        		throw new RuntimeException ("Trade Booking Processor Failed to Book Trade");
        	}
        } catch (Exception ex) {
        	String msg = "Error while processing trade book request: " + ex.toString();
    		logger.error (msg);
            StringWriter sw = new StringWriter(4000);
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(sw.toString());
        	throw new TradeBookingRequestErrorWhileProcessing(msg);
        }
        Map<String, Integer> bookedDealNums = rp.getBookedDealTrackingNums();
        if (bookedDealNums.isEmpty()) {
        	throw new TradeBookingRequestErrorWhileProcessing("No trade has been booked during the trade booking request");
        }
        for (Map.Entry<String, Integer> entry : bookedDealNums.entrySet()) {
        	return entry.getValue();
        }
        return -1;
    }

	private File saveFileToLocalFolder(String clientName, String fileName, String fileContent, boolean overwrite, ConstRepository constRepo) {
		String baseDir;
		try {
			baseDir = constRepo.getStringValue(VAR_BASE_DIR, session.getSystemSetting("AB_OUTDIR") + "/IncomingTrades/");
		} catch (OException e) {
			throw new RuntimeException ("Internal error while processing Trade Booking Request: Const Repository Operation Failed");
		}
        File inputDirectory = new File(baseDir, clientName);
        File inputFile = new File(inputDirectory, fileName);

        inputFile.getParentFile().mkdirs();
        if (inputFile.exists() && !overwrite) {
        	throw new TradeBookingRequestAlreadyExists("The file '" + fileName + "' has already been submitted"
        			+ " for client '" + clientName + "' and is waiting for being processed and the overwrite flag is set to false");
        } 
        try {
            Files.write(inputFile.toPath(), fileName.getBytes(StandardCharsets.UTF_8), 
            		StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException ex) {
        	String msg = "Internal error while processing Trade Booking Request: File Operation Exception";
    		logger.error (msg);
            StringWriter sw = new StringWriter(4000);
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(sw.toString());
        	throw new TradeBookingRequestFileOperationException(msg);
        }
		return inputFile;
	}

	private ConstRepository initConstRepo(String context, String subContext){
		ConstRepository constRepo;
		try {
			constRepo = new ConstRepository(context, subContext);
			return constRepo;
		} catch (OException e) {
			throw new RuntimeException ("Error initialising ConstRepository");
		}
	}
}
