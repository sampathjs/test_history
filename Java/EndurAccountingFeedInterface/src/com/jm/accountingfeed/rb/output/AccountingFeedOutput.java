package com.jm.accountingfeed.rb.output;

import java.io.File;
import java.text.ParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.LedgerExtractionTableColumn;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.rb.datasource.ReportParameter;
import com.jm.accountingfeed.util.Constants;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Report builder output plugin responsible for:
 * 
 * 1. Extracting ledger data to XML files 2. Logging an audit item in the relevant boundary table (for stamping to pick up later)
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public abstract class AccountingFeedOutput implements IScript
{
	private int extractionId;
	private ODateTime startTimeODateTime;

	protected ReportParameter reportParameter;
	protected Table tblOutputData;
	protected Object xmlData;
	protected String errorDetails;

	public AccountingFeedOutput() throws AccountingFeedRuntimeException
	{
		try
		{
			startTimeODateTime = ODateTime.getServerCurrentDateTime();
		}
		catch (OException oException)
		{
			String message = "Exception while getting current time.\n" + oException.getMessage();
			PluginLog.error(message);
			Util.printStackTrace(oException);
			throw new AccountingFeedRuntimeException(message, oException);
		}
	}

	/**
	 * This method Generates the XML output for Accounting Feed Interfaces - 1. Inserts a Master record in user_jm_ledger_extraction table 2. Read the output data of report and convert it to Xml format 3. Write the Xml file on disk 4. Insert the Xml data into Interface specific Audit Boundary table
	 * 5. Update the Master record in user_jm_ledger_extraction table
	 */
	@Override
	public void execute(IContainerContext context) throws OException
	{
		Util.setupLog();

		Table tblArgt = context.getArgumentsTable();
		Table tblOutputParameters = tblArgt.getTable("output_parameters", 1);
		PluginLog.info("Started AccountingFeedOutput script");
		reportParameter = new ReportParameter(tblOutputParameters);

		tblOutputData = tblArgt.getTable("output_data", 1);

		insertNewLedgerExtractionRecord();
		
		PluginLog.info("Started caching XML data. Rows=" + tblOutputData.getNumRows());
		cacheXMLData();
		PluginLog.debug("Completed caching XML data");

		try
		{
			PluginLog.info("Started creating XML file");
			generateXMLOutputFile();
			PluginLog.debug("Completed creating XML file");
		}
		catch (OException oException)
		{
			String message = "Exception while generating Accouting Feed output: " + oException.getMessage();
			PluginLog.error(message);
			errorDetails = message;
		}
		catch (JAXBException e)
		{
			String error = "JAXBException while generating Accouting Feed output: " + e.getMessage();
			PluginLog.error(error);
			errorDetails = error;
		}

		PluginLog.info("Started extracting audit records.");
		extractAuditRecords();
		PluginLog.debug("Completed extracting audit records.");
		
		processComplete();
		
		PluginLog.info("Completed AccountingFeedOutput script");
	}

	/**
	 * Populate xmlData object.
	 * 
	 * @throws OException
	 */
	protected abstract void cacheXMLData() throws OException;

	/**
	 * Populate Interface specific boundary table.
	 * 
	 * @throws OException
	 */
	protected abstract void extractAuditRecords() throws OException;

	protected abstract Class<?> getRootClass();

	/**
	 * Generate the XML output file for the Interface Xml file dir and file name are Report specific and are defined as Report parameters
	 * 
	 * @throws OException
	 * @throws JAXBException
	 */
	protected void generateXMLOutputFile() throws OException, JAXBException
	{
		boolean isSuccessful;

		String targetDirectoryPathString = reportParameter.getStringValue(ReportBuilderParameter.TARGET_DIR.toString());
		String targetFileName = reportParameter.getStringValue(ReportBuilderParameter.TARGET_FILENAME.toString());

		PluginLog.info("Started generating XML output file " + targetFileName + " under directory " + targetDirectoryPathString);
		
		targetDirectoryPathString = targetDirectoryPathString.trim();

		File targetDirectory = new File(targetDirectoryPathString);
		File file = new File(targetDirectoryPathString + "/" + targetFileName);

		if (!targetDirectory.exists())
		{
			PluginLog.info("Directory doesn't exist: " + targetDirectory);
			isSuccessful = targetDirectory.mkdirs();

			if (isSuccessful)
			{
				PluginLog.debug("Directories created successfully. Path:" + targetDirectoryPathString);
			}
			else
			{
				String message = "Directories not created.Path: " + targetDirectoryPathString;
				PluginLog.error(message);
				throw new AccountingFeedRuntimeException(message);
			}
		}

		JAXBContext jaxbContext = JAXBContext.newInstance(getRootClass());
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(xmlData, file);
		PluginLog.info("Completed generating XML output file");
	}

	/**
	 * Insert a new row into user_jm_ledger_extraction.
	 * 
	 * @throws OException
	 */
	protected void insertNewLedgerExtractionRecord() throws OException
	{
		String ledgerExtractionTable = ExtractionTableName.LEDGER_EXTRACTION.toString();
		String extractionUser = Ref.getUserName();
		ODateTime tradingDate = com.olf.openjvs.Util.NULL_DATE_TIME;
		ODateTime businessDate = com.olf.openjvs.Util.NULL_DATE_TIME;

		Table tableToInsert = com.olf.openjvs.Util.NULL_TABLE;
		PluginLog.info("Started inserting row in " + ledgerExtractionTable);
		try
		{
			String dateAsString = OCalendar.formatDateInt(com.olf.openjvs.Util.getTradingDate());
			tradingDate = ODateTime.strToDateTime(dateAsString);

			dateAsString = OCalendar.formatDateInt(com.olf.openjvs.Util.getBusinessDate());
			businessDate = ODateTime.strToDateTime(dateAsString);

			tableToInsert = Table.tableNew(ledgerExtractionTable);
			int ret = DBUserTable.structure(tableToInsert);
			tableToInsert.delCol(LedgerExtractionTableColumn.EXTRACTION_ID.toString()); // Auto incrementing identiy - no need to fill in
			tableToInsert.delCol(LedgerExtractionTableColumn.ROW_CREATION.toString()); // Auto filled by database - no need to fill in
			tableToInsert.delCol(LedgerExtractionTableColumn.EXTRACTION_END_TIME.toString()); // Don't set this now, filled in at end of process
			tableToInsert.delCol(LedgerExtractionTableColumn.NUM_ROWS_EXTRACTED.toString()); // Don't set this now, filled in at end of process

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new AccountingFeedRuntimeException("Unable to load table: " + ledgerExtractionTable);
			}

			String ledgerType = reportParameter.getStringValue(ReportBuilderParameter.BOUNDARY_TABLE.toString());
			String region = reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString());

			tableToInsert.addRow();

			tableToInsert.setString(LedgerExtractionTableColumn.REGION.toString(), 1, region);
			tableToInsert.setDateTime(LedgerExtractionTableColumn.EXTRACTION_START_TIME.toString(), 1, startTimeODateTime);
			tableToInsert.setString(LedgerExtractionTableColumn.LEDGER_TYPE_NAME.toString(), 1, ledgerType);
			tableToInsert.setString(LedgerExtractionTableColumn.EXTRACTION_USER.toString(), 1, extractionUser);
			tableToInsert.setDateTime(LedgerExtractionTableColumn.ENDUR_TRADING_DATE.toString(), 1, tradingDate);
			tableToInsert.setDateTime(LedgerExtractionTableColumn.ENDUR_BUSINESS_DATE.toString(), 1, businessDate);

			PluginLog.debug("Inserting user_jm_ledger_extraction. ledgerType=" + ledgerType + ",region=" + region + ",User=" + extractionUser);
			int returnValue = DBUserTable.insert(tableToInsert);
			if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(returnValue, "DBUserTable.insert() failed"));
				throw new AccountingFeedRuntimeException(DBUserTable.dbRetrieveErrorInfo(returnValue, "DBUserTable.insert() failed"));
			}

			setExtractionId();
		}
		catch (OException oException)
		{
			String message = "Exception occurred while adding record in ledger extraction table.\n" + oException.getMessage();
			PluginLog.error(message);
			Util.printStackTrace(oException);
			throw oException;
		}
		finally
		{
			if (Table.isTableValid(tableToInsert) == 1)
			{
				tableToInsert.destroy();
			}
			if (ODateTime.isNull(businessDate) == 0)
			{
				businessDate.destroy();
			}
			if (ODateTime.isNull(tradingDate) == 0)
			{
				tradingDate.destroy();
			}
		}
		PluginLog.info("Completed inserting row in ledger extraction table");
	}

	/**
	 * Update the ledger extraction as complete and log end time etc.
	 * 
	 * @throws OException
	 */
	protected void processComplete() throws OException
	{
		Table argumentTable = Table.tableNew();
		Table resultTable = Table.tableNew("target_table");
		Table numberOfRowsExtractedTable = Table.tableNew();
		try
		{
			int numberOfRowsExtracted;

			String boundaryTableName;

			PluginLog.info("Started updating end time and number of row extracted details in ledger extraction table.");
			
			argumentTable.addCol("extraction_id", COL_TYPE_ENUM.COL_INT);
			argumentTable.addCol("new_extraction_end_time", COL_TYPE_ENUM.COL_DATE_TIME);
			argumentTable.addCol("new_num_rows_extracted", COL_TYPE_ENUM.COL_INT);

			argumentTable.addRow();

			argumentTable.setInt("extraction_id", 1, extractionId);
			argumentTable.setDateTime("new_extraction_end_time", 1, ODateTime.getServerCurrentDateTime());

			boundaryTableName = reportParameter.getStringValue(ReportBuilderParameter.BOUNDARY_TABLE.toString());
			DBaseTable.execISql(numberOfRowsExtractedTable, "SELECT COUNT(*) AS number_of_rows_extracted FROM " + boundaryTableName + " WHERE extraction_id=" + extractionId);
			numberOfRowsExtracted = numberOfRowsExtractedTable.getInt("number_of_rows_extracted", 1);

			PluginLog.debug(numberOfRowsExtracted + " rows extracted for table "+ boundaryTableName );
			argumentTable.setInt("new_num_rows_extracted", 1, numberOfRowsExtracted);

			int returnValue = DBase.runProcFillTable(Constants.STORED_PROC_UPDATE_LEDGER_EXTRACTION, argumentTable, resultTable);
			if (returnValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				String message = DBUserTable.dbRetrieveErrorInfo(returnValue, "DBase.runProcFillTable() failed");
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(returnValue, "DBase.runProcFillTable() failed"));
				throw new AccountingFeedRuntimeException(message);
			}
		}
		catch (OException oException)
		{
			String message = "Exception occurred while extracting records.\n" + oException.getMessage();
			PluginLog.error(message);
			Util.printStackTrace(oException);
			throw new AccountingFeedRuntimeException(message, oException);
		}
		finally
		{
			argumentTable.destroy();
			resultTable.destroy();
			numberOfRowsExtractedTable.destroy();
		}
		PluginLog.info("Completed updating end time and number of row extracted details in ledger extraction table.");
	}

	/**
	 * Get the latest extraction id that has been generated for a specific ledger.
	 * 
	 * @param ledgerExtractionTable
	 * @return
	 * @throws OException
	 */
	private void setExtractionId() throws OException
	{
		Table extracationIdTable = null;
		int dateFormatInt = 103;

		try
		{
			PluginLog.info("Started setting extraction id in AccountingFeedOutput for current extraction");
			
			String extractionIdQuery =
					"SELECT MAX(ledger." + LedgerExtractionTableColumn.EXTRACTION_ID.toString() + ") AS extraction_id_for_current_report  \n" +
					"FROM " + ExtractionTableName.LEDGER_EXTRACTION.toString() + " ledger \n" +
					"WHERE ledger.region='" + reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()) + "' \n" 
					+"AND CONVERT(datetime,ledger." + LedgerExtractionTableColumn.EXTRACTION_START_TIME + ","+dateFormatInt+") = CONVERT(datetime,'" + startTimeODateTime + "', "+dateFormatInt+")";

			PluginLog.debug("extractionIdQuery=" + extractionIdQuery);

			extracationIdTable = Table.tableNew();

			int ret = DBaseTable.execISql(extracationIdTable, extractionIdQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Exception while running query " + extractionIdQuery));
				throw new AccountingFeedRuntimeException(DBUserTable.dbRetrieveErrorInfo(ret, "Exception while running query " + extractionIdQuery));
			}

			this.extractionId = extracationIdTable.getInt("extraction_id_for_current_report", 1);
		}
		finally
		{
			if (extracationIdTable != null)
			{
				extracationIdTable.destroy();
			}
		}
		PluginLog.info("Completed setting extraction id " + this.extractionId + " in AccountingFeedOutput for current extraction");
	}

	/**
	 * XML date fields need to be formatted and output in this format: yyyymmdd
	 * 
	 * @param endurDateTime
	 * @return
	 * @throws OException
	 */
	protected String getJDEFormattedStringFromEndurDate(ODateTime endurDateTime) throws OException
	{
		int julianDate = endurDateTime.getDate();

		if (julianDate == 0 || julianDate == -1)
		{
			return "";
		}

		String strDate = OCalendar.formatDateInt(julianDate);

		java.util.Date javaDate;

		try
		{
			javaDate = Constants.endurDateFormat.parse(strDate);
		}
		catch (ParseException e)
		{
			String message = "Unable to convert date: " + strDate;
			PluginLog.error(message);
			Util.printStackTrace(e);
			throw new AccountingFeedRuntimeException(message, e);
		}

		return Constants.jdeDateFormat.format(javaDate);
	}

	/**
	 * @return the extractionId
	 */
	public int getExtractionId()
	{
		return this.extractionId;
	}
}
