package com.matthey.pmm.connector.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestAlreadyExists;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestErrorWhileProcessing;
import com.matthey.pmm.connector.service.exception.TradeBookingRequestFileOperationException;
import com.matthey.pmm.tradebooking.app.TradeBookingMain;
import com.matthey.pmm.tradebooking.processors.RunProcessor;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.openlink.util.constrepository.ConstRepository;

import ch.qos.logback.classic.Logger;

@RestController
@RequestMapping("/shared")
public class EndurConnectorSharedController {
    private static final Logger logger = EndurLoggerFactory.getLogger(TradeBookingMain.class);

	private static final String CONTEXT = "EndurConnectorShared";
	private static final String SUBCONTEXT = "TradeBooking";
	private static final String VAR_BASE_DIR = "BaseDirectory"; 
    
    private final Session session;

    
    public EndurConnectorSharedController(Session session) {
        this.session = session;
    }
    
    @PostMapping("/tradeBooking")
    public int postTradeBookingRequest(@RequestParam String clientName,
    		@RequestParam String fileName,
    		@RequestParam boolean overwrite,
    		@RequestBody String tradeBookingActionPlan) {
        ConstRepository constRepo = initConstRepo(CONTEXT, SUBCONTEXT); 
        File inputFile = saveFileToLocalFolder(clientName, fileName, overwrite, constRepo);
        RunProcessor rp = new RunProcessor(session,logger, constRepo, clientName, Arrays.asList(inputFile.getPath()));
        try {
        	rp.processRun(); 
        } catch (Exception ex) {
        	throw new TradeBookingRequestErrorWhileProcessing("Error while processing the trade book request: " + ex.toString());
        }
        Map<String, Integer> bookedDealNums = rp.getBookedDealTrackingNums();
        if (bookedDealNums.isEmpty()) {
        	throw new TradeBookingRequestErrorWhileProcessing("No trade has been booked durign the trade booking request");
        }
        for (Map.Entry<String, Integer> entry : bookedDealNums.entrySet()) {
        	return entry.getValue();
        }
        return -1;
    }

	private File saveFileToLocalFolder(String clientName, String fileName, boolean overwrite, ConstRepository constRepo) {
		String baseDir;
		try {
			baseDir = constRepo.getStringValue(VAR_BASE_DIR, session.getSystemSetting("AB_OUTDIR"));
		} catch (OException e) {
			throw new RuntimeException ("Internal error while processing Trade Booking Request: Const Repository Operation Failed");
		}
        
        File inputFile = new File(baseDir, fileName);
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
        	logger.error(msg + ": " + ex.toString());
        	for (StackTraceElement ste : ex.getStackTrace()) {
            	logger.error(ste.toString());        		
        	}
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
