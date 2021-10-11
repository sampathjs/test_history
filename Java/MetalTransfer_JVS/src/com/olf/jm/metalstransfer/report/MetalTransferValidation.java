package com.olf.jm.metalstransfer.report;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author             | Description                                                                  |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 18-Aug-2021 |               | Rohit Tomar        | Initial version.                      									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/**
 * @author TomarR01
 *
 */
public class MetalTransferValidation implements IScript {

	@Override
	public void execute(IContainerContext arg0) throws OException {

		Table argt = arg0.getArgumentsTable();
		Table tblData = null;	
		Table tblExceptionData = null;
		String filePath = null;

		Logging.init(this.getClass(), MetalTransferValidationHelper.REPORTS, MetalTransferValidationHelper.METAL_TRANSFER_VALIDATION);

		try {

			if (argt.getNumRows() > 0) {

				MetalTransferValidationHelper validationHelper = new MetalTransferValidationHelper();

				String party = argt.getString(MetalTransferValidationHelper.PARTY, 1);
				String date_type = argt.getString(MetalTransferValidationHelper.DATE, 1);
				String start_date = argt.getString(MetalTransferValidationHelper.START_DATE, 1);
				String end_date = argt.getString(MetalTransferValidationHelper.END_DATE, 1);
				String[] Dealstatus = argt.getString(MetalTransferValidationHelper.STATUS, 1).split(",");
				String mailFlag = argt.getString(MetalTransferValidationHelper.EMAIL_FLAG, 1);

				filePath = validationHelper.getFilePath(MetalTransferValidationHelper.VALIDATION_REPORT); 
				
				for (String status : Dealstatus) {
					
					status = status.trim();

					Logging.info("retriving the metal transfer data for party : " + party + ", Range : " + start_date
							+ " to " + end_date + ", and status : " + status);

					tblData = validationHelper.getTransferData(party, date_type, start_date, end_date, status);

					if (status.equalsIgnoreCase(MetalTransferValidationHelper.VALIDATED) || status.equalsIgnoreCase(MetalTransferValidationHelper.NEW)) {

						Logging.info("formating the table in required structure");
						int maxCol = validationHelper.formatTable(tblData);

						Logging.info("formating data");
						validationHelper.prepareData(tblData, maxCol, status);

					} else {

						tblData = validationHelper.preparaCancelledData(tblData);
					}

					Logging.info("saving the report");
					validationHelper.formatAndSaveReport(tblData,status, filePath);

				}
				if (!(filePath == null || filePath.isEmpty() || filePath.equals(" ")) && mailFlag.equalsIgnoreCase(MetalTransferValidationHelper.YES)) {
					validationHelper.sendReportOnMail(filePath, party);
				}

				Logging.info("checking the exception data");
				tblExceptionData = validationHelper.getExceptionData(party, date_type, start_date, end_date);

				filePath = validationHelper.getFilePath(MetalTransferValidationHelper.EXCEPTION_REPORT);
				
				if (tblExceptionData.getNumRows() > 0) {
					validationHelper.formatAndSaveReport(tblExceptionData,MetalTransferValidationHelper.EXCEPTION_REPORT,filePath);
				}

			} else {
				Logging.info("Not a valid argument table");
			}
		} catch (Exception e) {
			Logging.error("Error in Metal transfer validation report. Reason :- "+e.getMessage());
			throw new RuntimeException("Error in Metal transfer validation report. Reason :- "+e.getMessage());
		} finally {
			if(Table.isValidTable(tblData) && tblData!= null ){
				tblData.destroy();
			}
			if(Table.isValidTable(tblExceptionData) && tblExceptionData!= null ){
				tblExceptionData.destroy();
			}
		}

	}
}
