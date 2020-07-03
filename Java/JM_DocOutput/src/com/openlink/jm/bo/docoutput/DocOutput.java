package com.openlink.jm.bo.docoutput;

import java.io.File;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.STLDOC_OUTPUT_TYPES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2020-03-25	V1.1	YadavP03	- memory leaks, remove console print & formatting changes
 */

class DocOutput extends BaseClass //implements IScript
{
	/* ************** ConstRepo *********************************************** */

	// configuration thru ConstRepo
	private final String CONSTREPO_STRVAR_STLDOC_STATUS_CANCELLED   = "Cancelled Status";
	private final String CONSTREPO_STRVAR_STLDOC_STATUS_CANCELLED_D = ""; // default
	private String _constRepo_StldocStatusCancelled = CONSTREPO_STRVAR_STLDOC_STATUS_CANCELLED_D;

	private final String CONSTREPO_STRVAR_CANCELLATION_FILESUFFIX   = "Cancelled Suffix";
	private final String CONSTREPO_STRVAR_CANCELLATION_FILESUFFIX_D = "C"; // default
	private String _constRepo_CancellationFilesuffix = CONSTREPO_STRVAR_CANCELLATION_FILESUFFIX_D;

	private final String CONSTREPO_STRVAR_STLDOC_INFO_EXPFILEPATH   = "Export File Path";
	private final String CONSTREPO_STRVAR_STLDOC_INFO_EXPFILEPATH_D = ""; // default
	private String _constRepo_StldocInfoExpFilepath = CONSTREPO_STRVAR_STLDOC_INFO_EXPFILEPATH_D;

	private final String CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS     = "Maximum Wait Seconds";
	private final int CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS_D      = 20; // default
	private int _constRepo_OutputMaxWaitSeconds = CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS_D;

	final String VAR_SMTP_SERVER = "SMTP Server"; // accessed by extending wMail class, defaults to ""

	// common ConstRepo for DocsOutput
	private final String CONSTREPO_DOCSOUTPUT_CONTEXT = "SCBO";
	private final String CONSTREPO_DOCSOUTPUT_SUBCONTEXT = "DocsOutput";
	private ConstRepository _constRepoDocsOutput = null;

	// additional ConstRepo for context specific use
	private String _constRepoSpecificContext = null;
	private String _constRepoSpecificSubcontext = null;
	private ConstRepository _constRepoSpecific = null;

	/* **************** argt ************************************************** */

	// provide argt to extending classes
	Table argt = null;

	// name of argt column holding the Process Data table
	protected final String PROCESS_DATA = "process_data";
	// embedded tables in Process Data table
	protected final String OUTPUT_DATA = "output_data";
	// for determining the current Output Type
	private final String OUTPUT_TYPE_ID = "output_type_id";
	// key/value pairs in Output Data table
	protected final String OUTPUT_PARAM_NAME  = "outparam_name",
						   OUTPUT_PARAM_VALUE = "outparam_value";

	/* **************** global ************************************************ */

	// can we already access PluginLog as it was initialized for this run?
	private boolean _isPluginLogInitialized = false; // if not initialized, we'll print to OConsole

	@Override
	public void execute(IContainerContext context) throws OException
	{
		argt = context.getArgumentsTable();
		_constRepoDocsOutput = new ConstRepository(CONSTREPO_DOCSOUTPUT_CONTEXT, CONSTREPO_DOCSOUTPUT_SUBCONTEXT);
		_constRepoSpecific   = getSpecificConstRepo();
		if (_constRepoSpecific != null)
		{
//			_constRepoSpecificContext = _constRepoSpecific.getContext();
//			_constRepoSpecificSubcontext = _constRepoSpecific.getSubcontext();
		}

		// default log settings
		String logLevel = "Warn", 
			   logFile  = getClass().getSimpleName() + ".log",
			   logDir   = null;

		logLevel = tryRetrieveSettingFromConstRepo("logLevel", logLevel, false);
		logFile  = tryRetrieveSettingFromConstRepo("logFile", logFile, true);
		logDir   = tryRetrieveSettingFromConstRepo("logDir", logDir, true);

		try
		{
			if (logDir == null)
				PluginLog.init(logLevel);
			else
			{
				File dir = new File(logDir);
				if (!dir.exists()) dir.mkdirs(); dir = null; 
				PluginLog.init(logLevel, logDir, logFile);
			}

			_isPluginLogInitialized = true;
		}
		catch (Exception e) { }

		_constRepo_CancellationFilesuffix = tryRetrieveSettingFromConstRepo(CONSTREPO_STRVAR_CANCELLATION_FILESUFFIX, _constRepo_CancellationFilesuffix, false);
		_constRepo_OutputMaxWaitSeconds   = tryRetrieveSettingFromConstRepo(CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS,   _constRepo_OutputMaxWaitSeconds);
		_constRepo_StldocInfoExpFilepath  = tryRetrieveSettingFromConstRepo(CONSTREPO_STRVAR_STLDOC_INFO_EXPFILEPATH, _constRepo_StldocInfoExpFilepath, false);
		_constRepo_StldocStatusCancelled  = tryRetrieveSettingFromConstRepo(CONSTREPO_STRVAR_STLDOC_STATUS_CANCELLED, _constRepo_StldocStatusCancelled, false);

		try { process(context); }
		catch (Throwable t) { PluginLog.error(t.toString()); }

		PluginLog.exitWithStatus();
	}

	private final String _constRepoShort = "%s/%s/%s";
	final String tryRetrieveSettingFromConstRepo(String variableName, String defaultValue, boolean enhanceEnvVars)
	{
		final String enhEnvVars = enhanceEnvVars ? "[%]" : "";
		String value = defaultValue, context = null, subcontext = null, msg;

		// get the DocOutput variable's value first and keep its source
		{
			context = CONSTREPO_DOCSOUTPUT_CONTEXT; subcontext = CONSTREPO_DOCSOUTPUT_SUBCONTEXT;
			try
			{
				value = tryRetrieveSettingFromConstRepo(variableName, value, enhanceEnvVars, _constRepoDocsOutput, context, subcontext, enhEnvVars);
			}
			catch (Throwable t)
			{
				msg = String.format(_constRepoShort + "%s - failed", context, subcontext, variableName, enhEnvVars);
				if (_isPluginLogInitialized) PluginLog.debug(msg);
				context = null; subcontext = null;
			}
		}

		// improve with specific value as far as available
		if (_constRepoSpecific != null)
		{
			context = _constRepoSpecificContext; subcontext = _constRepoSpecificSubcontext;
			try
			{
				value = tryRetrieveSettingFromConstRepo(variableName, value, enhanceEnvVars, _constRepoSpecific, context, subcontext, enhEnvVars);
			}
			catch (Throwable t)
			{
				msg = String.format(_constRepoShort + "%s - failed", context, subcontext, variableName, enhEnvVars);
				if (_isPluginLogInitialized) PluginLog.debug(msg);
				context = null; subcontext = null;
			}
		}

		msg = String.format(_constRepoShort+" = '%s'", context, subcontext, variableName, value);
		if (_isPluginLogInitialized) PluginLog.debug(msg);

		return value;
	}
	private String tryRetrieveSettingFromConstRepo(String variableName, String defaultValue, boolean enhanceEnvVars, ConstRepository constRepo, String context, String subcontext, String enhEnvVars) throws OException
	{
		String s = defaultValue, enh, msg;
		msg = String.format(_constRepoShort+"%s (%s)...", context, subcontext, variableName, enhEnvVars, defaultValue);
		if (_isPluginLogInitialized) PluginLog.debug(msg);

		s = constRepo.getStringValue(variableName, s);
		if (enhanceEnvVars && s.contains("%"))
		{
			enh = constRepo.getStringValue(variableName, s, enhanceEnvVars);
			msg = String.format("Default value is '%s' - Retrieved raw value '%s' - Enhanced value is '%s'", defaultValue, s, enh);
			if (_isPluginLogInitialized) PluginLog.debug(msg);
			s = enh;
		}
		return s;
	}

	final int tryRetrieveSettingFromConstRepo(String variableName, int defaultValue)
	{
		int value = defaultValue; String context = null, subcontext = null, msg;

		// get the DocOutput variable's value first and keep its source
		{
			context = CONSTREPO_DOCSOUTPUT_CONTEXT; subcontext = CONSTREPO_DOCSOUTPUT_SUBCONTEXT;
			try
			{
				value = tryRetrieveSettingFromConstRepo(variableName, value, _constRepoDocsOutput, context, subcontext);
			}
			catch (Throwable t)
			{
				msg = String.format(_constRepoShort + " - failed", context, subcontext, variableName);
				if (_isPluginLogInitialized) PluginLog.debug(msg);
				context = null; subcontext = null;
			}
		}

		// improve with specific value as far as available
		if (_constRepoSpecific != null)
		{
			context = _constRepoSpecificContext; subcontext = _constRepoSpecificSubcontext;
			try
			{
				value = tryRetrieveSettingFromConstRepo(variableName, value, _constRepoSpecific, context, subcontext);
			}
			catch (Throwable t)
			{
				msg = String.format(_constRepoShort + " - failed", context, subcontext, variableName);
				if (_isPluginLogInitialized) PluginLog.debug(msg);
				context = null; subcontext = null;
			}
		}

		msg = String.format(_constRepoShort+" = '%s'", context, subcontext, variableName, value);
		if (_isPluginLogInitialized) PluginLog.debug(msg);

		return value;
	}
	private int tryRetrieveSettingFromConstRepo(String variableName, int defaultValue, ConstRepository constRepo, String context, String subcontext) throws OException
	{
		int i = defaultValue; String msg;
		msg = String.format(_constRepoShort+" (%s)...", context, subcontext, variableName, defaultValue);
		if (_isPluginLogInitialized) PluginLog.debug(msg);

		i = constRepo.getIntValue(variableName, i);
		return i;
	}

	ConstRepository getSpecificConstRepo()
	{
		return null;
	}

	protected void process(IContainerContext context) throws OException
	{
		DocOutput_Base output = null;
		try
		{
			int outputTypeId = context.getArgumentsTable().getTable(PROCESS_DATA, 1).getInt(OUTPUT_TYPE_ID, 1);
			switch (STLDOC_OUTPUT_TYPES_ENUM.fromInt(outputTypeId))
			{
				// load classes without requiring their existence within compiling, non existing classes (ie not 
				// licensed features for a specific client) will cause a ClassNotFoundException which we catch
				case STLDOC_OUTPUT_TYPE_CRYSTAL:
					output = (DocOutput_Base)Class.forName("com.openlink.jm.bo.docoutput.DocOutput_Crystal", false, getClass().getClassLoader()).newInstance();
					break;
				case STLDOC_OUTPUT_TYPE_OPENLINK_DOC:
					output = (DocOutput_Base)Class.forName("com.openlink.jm.bo.docoutput.DocOutput_DMS", false, getClass().getClassLoader()).newInstance();
					break;

				default:
					throw new java.lang.ClassNotFoundException();
			}
		}
		catch (java.lang.ClassNotFoundException e)
		{
			int outputTypeId = context.getArgumentsTable().getTable(PROCESS_DATA, 1).getInt(OUTPUT_TYPE_ID, 1);
			PluginLog.warn("Not supported: "+STLDOC_OUTPUT_TYPES_ENUM.fromInt(outputTypeId).name()+" (Id:"+outputTypeId+")");
		}
		catch (Throwable t)
		{
			throw new OException("Couldn't determine Output Type - Is this script running as an Output Script?");
		}

		if (output != null)
		{
			int docType = context.getArgumentsTable().getTable(PROCESS_DATA, 1).getInt("doc_type", 1);
			int docTypeInvoice = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, "Invoice");
			if (isCancellationDoc())
				output.setCancellationHandlingParams(true, _constRepo_CancellationFilesuffix, _constRepo_StldocStatusCancelled,
						docType == docTypeInvoice
				);

			// do the Output Type specific work
			PluginLog.debug ("Before process");
			output.process(context);
			PluginLog.debug ("After process");

			if (!output.isDocumentPreviewed)
			{
				// additional actions on really created documents, eq mailing
				if (isSendMailRequested() && output.isDocumentForMail)
					try
					{
						PluginLog.debug ("Before sending email");
						sendMail(output);
						PluginLog.debug ("After sending email");
					}
					catch (Throwable t)
					{
						PluginLog.error("Mail failed\n"+t.toString());
					}

				if (output.isDocumentExported)
				{
					try
					{
						PluginLog.debug ("Before setting output filename");
						int ret = StlDoc.setOutputFilename(output.documentExportPath);
						PluginLog.debug ("After setting output filename");
						if (ret != OLF_RETURN_SUCCEED)
							throw new OException("StlDoc.setOutputFilename returned "+ret);
					}
					catch (Throwable t)
					{
						PluginLog.warn(t.getMessage());
					}

					String strStldocInfoType_DocFilePath = tryRetrieveSettingFromConstRepo("Document File Path", "", false);
					if (strStldocInfoType_DocFilePath != null && (strStldocInfoType_DocFilePath=strStldocInfoType_DocFilePath.trim()).length() != 0)
					{
						try
						{
							int docnum = argt.getTable("process_data", 1).getInt("document_num", 1);
							PluginLog.debug ("Before saving document info value");
							StlDoc.saveInfoValue(docnum, strStldocInfoType_DocFilePath, output.documentExportPath);
							PluginLog.debug ("After saving document info value");
						}
						catch (Throwable t)
						{
							PluginLog.warn("Failed to save the exported document's path to '"+strStldocInfoType_DocFilePath+"' - "+t.getMessage());
						}
					}
				}
			}
		}

	}

	/*
	 * Overriding classes set a "Send-Output-As-Mail-Attachment" flag thru this.
	 * In case of returning 'true', overriding the method 'sendMail()' is required.
	 */
	boolean isSendMailRequested()
	{
		return false;
	}
	/*
	 * Overriding classes implement mailing functionality thru this.
	 * set the Send-Output-As-Mail-Attachment flag thru this.
	 * See also method 'isSendMailRequested()'
	 */
	void sendMail(DocOutput_Base output) throws OException
	{
	}

	protected boolean existsFile(String fileName) throws OException
	{
		File file = new File(fileName);
		return file.exists() && file.canWrite() && file.canRead();
	}
	protected void waitForFile(String fileName) throws OException
	{
		int loopCount = 0, loopMax = _constRepo_OutputMaxWaitSeconds; long sleepSeconds = 1;

		if (loopMax <= 0)
		{
			PluginLog.warn(String.format("Invalid setting for '%s':%d - using %d", 
					CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS, 
					loopMax, CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS_D));
			loopMax = CONSTREPO_INTVAR_OUTPUT_MAXWAITSECONDS_D;
		}

		PluginLog.info("Waiting maximum "+loopMax+" seconds for file creation");
		while (!existsFile(fileName) && ++loopCount <= loopMax)
			try
			{
				PluginLog.debug("DocsOutput - Waiting for file creation ("+loopCount+"/"+loopMax+")");
				Thread.sleep(sleepSeconds*1000);
			}
			catch (Throwable t) {}
		PluginLog.info("Waited "+loopCount+" seconds for file creation");

		if (loopCount>=loopMax)
			throw new OException("Aborted - file not created in time - "+fileName);
	}

	private boolean isCancellationDoc() throws OException
	{
		if (_constRepo_StldocStatusCancelled == null || _constRepo_StldocStatusCancelled.trim().length() == 0)
		{
			PluginLog.debug("'" + CONSTREPO_STRVAR_STLDOC_STATUS_CANCELLED + "' is not configured or empty - feature skipped");
			return false;
		}

		if (_constRepo_CancellationFilesuffix == null || _constRepo_CancellationFilesuffix.trim().length() == 0)
		{
			PluginLog.debug("'" + CONSTREPO_STRVAR_CANCELLATION_FILESUFFIX + "' is not configured or empty - feature skipped");
			return false;
		}

		_constRepo_StldocStatusCancelled = _constRepo_StldocStatusCancelled.trim();
		int iCancelStatusId = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, _constRepo_StldocStatusCancelled);
		if (iCancelStatusId < 0)
		{
			PluginLog.debug("'" + CONSTREPO_STRVAR_STLDOC_STATUS_CANCELLED + "' \"" + _constRepo_StldocStatusCancelled + "\" cannot be found - feature skipped");
			return false;
		}

		int iNextStatusId = argt.getTable(PROCESS_DATA, 1).getInt("next_doc_status", 1);
		int iDocumentNum = argt.getTable(PROCESS_DATA, 1).getInt("document_num", 1);
		if (iNextStatusId != iCancelStatusId)
		{
			PluginLog.debug("Document# " + iDocumentNum + " is not a Cancellation - feature skipped");
			return false;
		}

		PluginLog.debug("Document# " + iDocumentNum + " is a Cancellation - applying '"+_constRepo_CancellationFilesuffix+"'");
		return true;
	}
}
