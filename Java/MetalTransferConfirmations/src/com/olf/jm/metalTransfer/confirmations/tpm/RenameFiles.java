package com.olf.jm.metalTransfer.confirmations.tpm;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.olf.embedded.application.Context;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openjvs.OException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


/**
 * The Class RenameFiles. TPM plugin to rename the output of a report builder definition. 
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class RenameFiles extends AbstractProcessStep {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT_NAME. */
	private static final String CONTEXT_NAME = "MetalTransferConfirmation";
	
	/** The Constant CONTEXT_SUB_NAME. */
	private static final String CONTEXT_SUB_NAME = "TPM";
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.tpm.ProcessStep#
	 * execute(com.olf.embedded.application.Context, 
	 * 		   com.olf.openrisk.tpm.Process, 
	 *         com.olf.openrisk.tpm.Token, 
	 *         com.olf.openrisk.staticdata.Person, 
	 *         boolean, 
	 *         com.olf.openrisk.tpm.Variables)
	 */
	@Override
	public final Table execute(final Context context, final Process process, final Token token,
			final Person submitter, final boolean transferItemLocks, final Variables variables) {
		try {
			init();
		} catch (OException e) {
			Logging.error("Error during initilisation. " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		
		moveFile(variables);
		Logging.close();
		return null;
	}
	
	/**
	 * Move the output file defined in the report builder parameters. Change the extension 
	 * from txt to xml.
	 *
	 * @param variables the variables
	 */
	private void moveFile(final Variables variables) {
		ConstTable reportParameters = getReportParameters(variables);
		
		if(reportParameters == null) {
			String errorMessage = "Unable to read filename skiping move";
			Logging.info(errorMessage);			
			return;
		}
		
		String fileName = getReportOutputName(reportParameters);
		
		String originalFileName = fileName.replace("xml", "txt");
		
		try {
			Files.move(new File(originalFileName).toPath(), new File(fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			String errorMessage = "Error renaming file " + originalFileName + " to " + fileName + ". " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}
	
	/**
	 * Gets the report parameters from the TPM variables.
	 *
	 * @param variables the variables
	 * @return the report parameters
	 */
	private ConstTable getReportParameters(final Variables variables) {
		Variable reportParameters = variables.getVariable("ReportParameters");
		
		if (reportParameters == null) {
			String errorMessage = "Error reading the report parameters for the TPM varaibles";
			Logging.info(errorMessage);
			return null;
		}
		
		ConstTable reportParametersTable = reportParameters.getValueAsTable();
		
		if (reportParametersTable == null || reportParametersTable.getRowCount() == 0) {
			String errorMessage = "Error reading the report parameters table for the TPM varaibles";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		return reportParametersTable;
	}
	
	/**
	 * Gets the report output name from the report builder parameters.
	 *
	 * @param reportParameters the report parameters
	 * @return the report output name
	 */
	private String getReportOutputName(final ConstTable reportParameters) {
		int row = reportParameters.find(reportParameters.getColumnId("parameter_name"), "OUTPUT_FILENAME", 0);
		
		if (row < 0) {
			String errorMessage = "Error reading output file name from the report parameters.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		String fileName = reportParameters.getString("parameter_value", row);
		
		if (fileName == null || fileName.length() == 0) {
			String errorMessage = "Error reading output file name from the report parameters. File name is blank or missing";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);				
		}
		
		return fileName;
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws OException the o exception
	 */
	private  void init() throws OException {
		constRep = new ConstRepository(CONTEXT_NAME, CONTEXT_SUB_NAME);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT_NAME, CONTEXT_SUB_NAME);


		} catch (Exception e) {
			throw new OException("Error initialising logging. "
					+ e.getMessage());
		}
	}

}
