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
 * This is the main script for metal transfer validation report
 * It read the below data from @param script and uses the helper class to retrieve and format the report data
 * 
 * PARTY - Report to be triggered for which region (multiselect)
 * DATE - The report to be triggered for Trade date or settle date (single select)
 * START_DATE - the start range of the date (Trade date or settle date)
 * END_DATE - the end range of the date (Trade date or settle date)
 * STATUS - The status of strategy deals (multiselect)
 * EMAIL_FLAG - Flag to send the report over mail or not
 * 
 * Parameters are configured in USER_CONS_REPOSITORY with context - 'Reports' and sub-context - 'MetalTransferValidation'
 * 
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

		Logging.init(this.getClass(), MetalTransferValidationHelper.CONTEXT, MetalTransferValidationHelper.SUB_CONTEXT);

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
			
			Logging.close();
		}

	}
}
