package com.olf.jm.SapInterface;

import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentColumns;
import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentsAuxSubTables;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/**
 * The Class DealBookingHook.
 * Connex hook used to add comments to deals beeing booked as part of the coverage interface.
 * Plugin is set via the environment variable CX_TRANF_PLUGIN_HOOK_FULL_NAME=com.olf.jm.SapInterface.DealBookingHook
 * 
 * Note this is a JVS implementation not OC!
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class DealBookingHook implements IScript {

	/** The const rep. */
	private ConstRepository constRep;
	
	/** The Constant TRADE_BUILDER_AUX_COLUMN_NAME defining the column name that holds 
	 * the aux data table. */
	private static final String TRADE_BUILDER_AUX_COLUMN_NAME = "oad:auxData";

	/** The Constant CONTEXT using to initialise the const repository. */
	private static final String CONTEXT = "Connex";

	/** The Constant SUBCONTEXT using to initialise the const repository. */
	private static final String SUBCONTEXT = "DealBookingHook";
	
	
	/* (non-Javadoc)
	 * @see com.olf.openjvs.IScript#execute(com.olf.openjvs.IContainerContext)
	 */
	@Override
	public final void execute(final IContainerContext context) throws OException {
		// Initialise the logging and const repository.
		try {
			init();
		} catch (Exception e) {
			throw new OException(e.getMessage());
		}
		PluginLog.debug("In Deal Booking Hook");
		Table argt = context.getArgumentsTable();
		
		
		// Extract the transaction from the argument table
		Transaction tran = argt.getTran("tran", 1);
		if (tran == null) {
			// Invalid transaction
			String errorMessage = "Error setting deal comments. No transaction present.";
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		}
		
		Table origArgt = argt.getTable("orig_argt", 1);
		if (origArgt == null || Table.isTableValid(origArgt) == 0) {
			// Invalid argument table
			String errorMessage = "Error setting deal comments. No argument data present.";
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);			
		}
		
		if (origArgt.getColNum("tb:tradeBuilder") < 0) {
			PluginLog.info("Currently processing a trade book related deal and skip SAP related logic");
			// we are currently processing a deal from the trade book interface and skip SAP logic.
			return;
		}
		
		Table tradeBuilder = origArgt.getTable("tb:tradeBuilder", 1);
		if (tradeBuilder == null || Table.isTableValid(tradeBuilder) == 0) {
			// Invalid argument table
			String errorMessage = "Error setting deal comments. Argument does not contain a traidbuilder object.";
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);			
		}		
		Table auxData = tradeBuilder.getTable(TRADE_BUILDER_AUX_COLUMN_NAME, 1);
		
		if (auxData != null
			&&	Table.isTableValid(auxData) == 1 
			&&	auxData.getColNum(EnumCommentsAuxSubTables.COMMENTS.getTableName()) > 0) {
			Table comments = auxData.getTable(EnumCommentsAuxSubTables.COMMENTS.getTableName(), 1);
			addDealComments(tran, comments);
		}
		
		PluginLog.debug("Finished Deal Booking Hook");

	}
	
	/**
	 * Adds the deal comments.
	 *
	 * @param tran the tran
	 * @param comments the comments
	 * @throws OException the o exception
	 */
	private void addDealComments(final Transaction tran, final Table comments)
			throws OException {
		try {
			tran.loadDealComments();
			
			for (int commentId = 1; commentId <= comments.getNumRows(); commentId++) {
		
				int numberOfComments = tran.getNumRows(0,
						TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt());

				int newComment = 0;

				boolean matchFound = false;
				if (numberOfComments > 0) {
					for (int row = 0; row < numberOfComments; row++) {
						String commentType = comments.getString(EnumCommentColumns.COMMENT_TYPE.getColumnName(), commentId);
						if (tran.getField(TRANF_FIELD.TRANF_DEAL_COMMENTS_NOTE_TYPE.toInt(),
								0, "", row).equals(commentType)) {
							newComment = row;
							matchFound = true;
							break;
						}
					}
				} 
				if (!matchFound) {
					// No existing comment found so add a new one.
					tran.addDealComment();
					newComment = tran.getNumRows(0,
							TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt()) - 1;
				}

				String comment = comments.getString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), commentId);
				String commentType = comments.getString(EnumCommentColumns.COMMENT_TYPE.getColumnName(), commentId);
				tran.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_COMMENT.toInt(), 0, "", comment, newComment);
				tran.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_NOTE_TYPE.toInt(), 0, "", commentType, newComment);
			}
		} catch (OException e) {
			PluginLog.error("Error setting deal commetns. " + e.getMessage());
			throw e;
		}

	}
	
	/**
	 * Inits the logging classes and const repository.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	

}
