package com.olf.jm.pricewebservice.app;

import java.util.List;

import com.olf.jm.pricewebservice.model.Pair;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-04-13	V1.0	jwaechter	- Initial Version
 */

/**
 * This plugin checks if an index relevant for the TPM PriceWebService data is being processed.
 * It has to be executed as a PreProcess market index plugin (supposed to be the OPS triggering
 * the TPM and control whether the post process service should run or not)
 * The same checks are applied at the start of the TPM.
 * @author jwaechter
 * @version 1.0
 */
public class PriceWebPreProcessCheck implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init (context);
			PluginLog.info("PriceWebPreProcessCheck starts");
			process(context);
			PluginLog.info("PriceWebPreProcessCheck ended successfully");
		} catch (Throwable t) {
			String message = "Error in pre process check: " + t.getMessage();
			for (StackTraceElement ste : t.getStackTrace()) {
				message += "\n" + ste.toString();
			}
			PluginLog.error(message);
			throw t;
		}
	}

	private void process(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		int universal = argt.getInt("universal", 1);
		int close = argt.getInt("close", 1);
		Table indexList = argt.getTable("index_list", 1);
		int indexId = indexList.getInt("index_id", 1);
		ODateTime dateSavedFor = argt.getDateTime("date", 1);

		if (universal != 0) {
			PluginLog.info("Saving universal - not relevant for Price Web Service");
			returnt.setInt("do_not_run_post_process", 1, 1);
			return;
		}

		List<Pair<String, Integer>> relevantDatasets = DBHelper.getRelevantClosingDatasetTypes();
		List<Pair<String, Integer>> relevantIndices = DBHelper.getRelevantIndices();
		
		boolean isRelevantClosingDataset=false;
		for (Pair<String, Integer> relDataset : relevantDatasets) {
			if (relDataset.getRight() == close) {
				isRelevantClosingDataset = true;
				break;
			}
		}
		if (!isRelevantClosingDataset) {			
			PluginLog.info("Closing Dataset #" + close + " not relevant for Price Web Service");
			returnt.setInt("do_not_run_post_process", 1, 1);
			return;
		}
		
		boolean isRelevantIndex=false;
		for (Pair<String, Integer> relIndex : relevantIndices) {
			if (relIndex.getRight() == indexId) {
				isRelevantIndex = true;
				break;
			}
		}
		if (!isRelevantIndex) {			
			PluginLog.info("Index #" + indexId + " not relevant for Price Web Service");
			returnt.setInt("do_not_run_post_process", 1, 1);
			return;
		}
		
		if (dateSavedFor.getDate() != Util.getTradingDate()) {
			PluginLog.info("Trading date " + OCalendar.formatDateInt(Util.getTradingDate()) +
					" is not equal to date index data saved for (" + 
					OCalendar.formatDateInt(dateSavedFor.getDate()) + ")");
			returnt.setInt("do_not_run_post_process", 1, 1);
			return;
		}
	}

	private void init(IContainerContext context) throws OException {
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, 
				DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
