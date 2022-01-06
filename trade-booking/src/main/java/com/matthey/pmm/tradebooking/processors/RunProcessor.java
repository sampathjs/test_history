package com.matthey.pmm.tradebooking.processors;

import java.util.ArrayList;
import java.util.List;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

import ch.qos.logback.classic.Logger;

public class RunProcessor {
	private final Logger logger;
	private final Session session;
	private final List<String> filesToProcess;
	private final int runId;
	private int dealCounter=0;
	
	public RunProcessor (final Session session, final Logger logger, final List<String> filesToProcess) {
		this.session = session;
		this.logger = logger;
		this.filesToProcess = new ArrayList<>(filesToProcess);
		this.runId = getMaxCurrentRunId() + 1;
	}
	
	public void processRun () {
    	for (String fileNameToProcess : filesToProcess) {
    		FileProcessor fileProcessor = new FileProcessor(session, constRepo, logger);
    		logger.info("Now processing file' " + fileProcessor + "'");
    		fileProcessor.processFile(fileNameToProcess);
    		logger.info("Processing of ' " + fileNameToProcess + "' finished successfully");
    	}
	}
}
