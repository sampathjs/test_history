package com.olf.jm.metalstransfer.opservice;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PreProcessingInfo;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.metalstransfer.model.ConfigurationItem;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Comment;
import com.olf.openrisk.trading.Comments;
import com.olf.openrisk.trading.EnumCommentFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-04-12	V1.0	jwaechter	- Intitial Version
 */

/**
 * This plugin blocks booking of metal strategies in case they are missing comments of type 
 * "From Account" or "To Account" or both.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckStrategyComments extends AbstractTradeProcessListener {

	private static final String COMMENT_TYPE_FROM_ACCOUNT = "From Account";
	private static final Object COMMENT_TYPE_TO_ACCOUNT = "To Account";

	@Override
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {		
		try {
			initLogging ();
			StringBuilder errorMessage = new StringBuilder();
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				errorMessage.append(process(ppi));
			}
			if (errorMessage.length() > 0) {
				return PreProcessResult.failed(errorMessage.toString());
			} else {
				return PreProcessResult.succeeded();				
			}
			
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw t;
		}
	}
	
	private StringBuilder process(PreProcessingInfo<EnumTranStatus> ppi) {
		// Assuming there are no offset transactions.
		Transaction strategy = ppi.getTransaction(); 
		PluginLog.info("Processing strategy having deal tracking #" + strategy.getDealTrackingId() );
		int fromAccountCommentCount = 0;
		int toAccountCommentCount = 0;
		Comments comments = strategy.getComments();
		for (Comment c : comments) {
			if (c.getValueAsString(EnumCommentFieldId.Type).equals(COMMENT_TYPE_FROM_ACCOUNT)) {
			    String comment = c.getValueAsString(EnumCommentFieldId.Comments);
			    String commentML = c.getValueAsString(EnumCommentFieldId.CommentsMultiLine);
			    if (comment!= null && comment.trim().length() > 0) {
					fromAccountCommentCount++;			    	
			    } else if (commentML!= null && commentML.trim().length() > 0) {
					fromAccountCommentCount++;			    	
			    }			    
			}
			if (c.getValueAsString(EnumCommentFieldId.Type).equals(COMMENT_TYPE_TO_ACCOUNT)) {
			    String comment = c.getValueAsString(EnumCommentFieldId.Comments);
			    String commentML = c.getValueAsString(EnumCommentFieldId.CommentsMultiLine);
			    if (comment!= null && comment.trim().length() > 0) {
					toAccountCommentCount++;			    	
			    } else if (commentML!= null && commentML.trim().length() > 0) {
					toAccountCommentCount++;			    	
			    }
			}
		}
		StringBuilder errorMessage = new StringBuilder();
		if (fromAccountCommentCount == 0) {
			errorMessage.append("\nMissing comment of type '").append(COMMENT_TYPE_FROM_ACCOUNT);
			errorMessage.append("' or empty comment");
		}
		if (toAccountCommentCount == 0) {
			errorMessage.append("\nMissing comment of type '").append(COMMENT_TYPE_TO_ACCOUNT);
			errorMessage.append("' or empty comment");
		}
		if (fromAccountCommentCount > 1) {
			errorMessage.append("\nMore than one comment of type '").append(COMMENT_TYPE_FROM_ACCOUNT);
			errorMessage.append("' or empty comment");
		}
		if (toAccountCommentCount > 1) {
			errorMessage.append("\nMore than one comment of type '").append(COMMENT_TYPE_TO_ACCOUNT);
			errorMessage.append("'");
		}
		if (errorMessage.length() > 0) {
			errorMessage.insert(0, "Error(s) processing transaction:");
		}
		return errorMessage;
	}

	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging()  {
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_LEVEL.getValue();
		String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
		
		try {
			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		} 
	}
}
