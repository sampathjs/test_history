package com.matthey.pmm.limits.reporting;

import static com.olf.embedded.application.EnumScriptCategory.Generic;

import com.matthey.pmm.limits.reporting.translated.EmailSender;
import com.matthey.pmm.limits.reporting.translated.LimitsReportingConnector;
import com.matthey.pmm.limits.reporting.translated.LimitsReportingEODChecker;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/* 
 * Important change: now uses the java classes instead of the kotlin classes
 *
 */

@ScriptCategory(Generic)
public class LimitsReportingEODWrapper extends AbstractGenericScript {
	public static final String CONST_REPO_CONTEXT = "LimitsReporting"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "EOD"; // sub context of constants repository
	
	private String templateDir;
	
    @Override
    public Table execute(Context context, ConstTable table) {
    	init(context);
        LimitsReportingConnector connector = new LimitsReportingConnector(context);
        EmailSender emailSender = new EmailSender();
        new LimitsReportingEODChecker(connector, emailSender, templateDir).run();
        Logging.close();
        return null;
    }

	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(final Session session) {
		try {
			String abOutdir = session.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			templateDir = constRepo.getStringValue("freemarkerTemplateDirectory", abOutdir + "/freemarker-templates");
			
			try {
				Logging.init( this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		Logging.info("\n\n********************* Start of new run ***************************");		
	}    
}
