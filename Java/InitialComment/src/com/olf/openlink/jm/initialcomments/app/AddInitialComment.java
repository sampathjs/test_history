package com.olf.openlink.jm.initialcomments.app;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.Comment;
import com.olf.openrisk.trading.Comments;
import com.olf.openrisk.trading.EnumCommentFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-10-06	V1.0	jwaechter	- Initial Version
 * 2016-10-07	V1.1	jwaechter	- removed spelling mistake out of class name
 */

/**
 * This plugin adds a new. empty comment of type {@value #COMMENT_TYPE} 
 * to the transaction being processed in case there is no other
 * existing comment. Those comments are automatically removed
 * in case the user does not enter data into them manually
 * while processing the transaction.
 * @author jwaechter
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class AddInitialComment extends AbstractTransactionListener {
	public static final String COMMENT_TYPE = "Invoice";
	public static final String CONST_REPO_CONTEXT = "BackOffice";
	public static final String CONST_REPO_SUBCONTEXT = "InitialComment";
	
	@Override
    public void notify(final Context context, final Transaction tran) {
		try {
			init(context);
			PluginLog.info ("Plugin processing transactions #" + tran.getTransactionId());
			Comments comments = tran.getComments();
			if (comments == null) {
				throw new RuntimeException ("Comments object is null");
			}
			if (comments.size() > 0) {
				PluginLog.info("There are already existing comments for transaction "
						+ tran.getTransactionId() + ". Skipping.");
				return;
			}
			Comment c = comments.addItem();
			c.setValue(EnumCommentFieldId.Type, COMMENT_TYPE);
	} catch (Throwable t) {
			PluginLog.error("Error executing plugin " + this.getClass()
					+ ": " + t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		}
    }

	private void init(final Session session) {
		try {
			ConstRepository repo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			String abOutDir = session.getSystemSetting("AB_OUTDIR");
			String logFile = repo.getStringValue("logFile", this.getClass().getName() + ".log");
			String logDir = repo.getStringValue("logDir", abOutDir + "\\error_logs");
			String logLevel = repo.getStringValue("logLevel", "Error");
            PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException ("Could not retrieve settings from ConstantsRepository", e);
		}
	}
}
