package com.jm.reportbuilder.Eventus;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.jm.ftp.FTPEventus;
import com.olf.jm.logging.Logging;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
public class EventusReportOutput implements IScript {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository repository = null ;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public final String CONTEXT = "Reports";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public final String SUBCONTEXT = "Eventus";

	public EventusReportOutput() throws OException {
		super();
	}

	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException {

		Table dataTable = Util.NULL_TABLE;
		Table paramTable = Util.NULL_TABLE;
		try {

			repository = new ConstRepository(CONTEXT, SUBCONTEXT);
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
			Logging.info("Started Report Output Script: " + this.getClass().getSimpleName());
			Table argt = context.getArgumentsTable();
			dataTable = argt.getTable("output_data", 1);
			paramTable = argt.getTable("output_parameters", 1);

			String outputFolder = paramTable.getString("parameter_value",
					paramTable.findString("parameter_name", "OUT_DIR", SEARCH_ENUM.FIRST_IN_GROUP));
			String fileName = paramTable.getString("parameter_value",
					paramTable.findString("parameter_name", "FILE_NAME", SEARCH_ENUM.FIRST_IN_GROUP));
			String fileExt = paramTable.getString("parameter_value",
					paramTable.findString("parameter_name", "OUT_EXT", SEARCH_ENUM.FIRST_IN_GROUP));
			String fullPath = outputFolder + "\\" + fileName + "." + fileExt;
			String reportDate = paramTable.getString("parameter_value",
					paramTable.findString("parameter_name", "Report_Date", SEARCH_ENUM.FIRST_IN_GROUP));

			Logging.info("Generating CSV file with file name:" + fileName);
			generatingOutputCsv(dataTable, paramTable, fullPath);
			Logging.info("Uploading data to SFTP ");
			ftpFile(fullPath, reportDate);
			
		} catch (OException e) {
			Logging.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());

		} catch (Exception e) {
			String errMsg = "Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		} finally {
			Logging.debug("Ended Report Output Script: " + this.getClass().getSimpleName());
			Logging.close();
		}

	}

	private void ftpFile(String strFullPath, String reportDate) throws Exception {

		try {
			FTPEventus ftpEventus = new FTPEventus(repository, reportDate);
			ftpEventus.put(strFullPath);
		} catch (Exception e) {
			Logging.info("FTP failed " + e.getMessage());
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Generating the csv file
	 * 
	 * @param dataTable
	 * @param fullPath
	 * @param header
	 * @param footer
	 * @throws OException
	 */
	private void generatingOutputCsv(Table dataTable, Table paramTable, String fullPath) throws OException {

		try {
			String csvTable = dataTable.exportCSVString();
			Str.printToFile(fullPath, csvTable, 1);
			dataTable.printTableDumpToFile(fullPath);
		} catch (OException e) {
			Logging.error("Couldn't generate the csv " + e.getMessage());
			throw new OException(e.getMessage());
		}
	}
}
