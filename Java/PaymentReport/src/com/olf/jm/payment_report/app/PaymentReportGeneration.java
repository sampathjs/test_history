package com.olf.jm.payment_report.app;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.jm.payment_report.model.DMSOutputType;
import com.olf.openjvs.DocGen;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.Document;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ColumnFormatterAsDateTime;
import com.olf.openrisk.table.ColumnFormatterAsDouble;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.consecutivenumber.model.ConsecutiveNumberException;
import com.openlink.util.consecutivenumber.persistence.ConsecutiveNumber;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2015-05-04	V1.0 	jwaechter	- Initial Version
 * 2015-05-11   V1.1    jwaechter   - Added new items country/ city / legal entity to XML
 *                                  - now using BO doc numbering functionality to generate ID
 *                                  - now using BO document type for payment report to retrieve
 *                                    doc consecutive number
 * 2015-06-16	V1.2	jwaechter	- refactored generation of "Fields" section in XML
 *                                  - added fields "TotAmountUSD", "TotAmountEUR",
 *                                    "TotAmountGBP" and "TotAmountRAN" to fields section
 * 2015-12-01	V1.3	jwaechter	- fixed SQL to be able to deal with multiple 
 * 									  party info fields
 * 								    - changed the party info field containing 
 *                                    the "CostNum"
 *                                  - changed currency name from RAN to ZAR
 *                                  
  * 2016-10-10	V1.4	scurran 	- add validation of document number and skip of 0 
*/

/**
 * This plugin contains the logic to create and send the Payment Report to the user processing netting statements.
 * This plugin is meant to be run as post process service for the service type BO Document Processing.
 * Note that this plugins uses both OC and OpenJVS so the project has to reference both libraries.
 * <br/> <br/>The following steps are performed:
 * <br/>
 * <ol>
 *   <li> 
 *        Check the argt for the "Action". Depending on Constants 
 *        Repository settings the operation services ends succeeds or continues with logic.
 *        The "Action" contains whether the user is previewing or processing documents. 
 *   </li>
 *   <li>
 *      Retrieve the netting statement related that are needed for the report via SQL.
 *   </li>
 *   <li>
 *     Generate and the XML according to the XML schema required by the DMS report. 
 *     The xml is present in memory only.
 *   </li>
 *   <li>
 *     Pass on the generated XML along with the generated file name under
 *     which the generated report is saved to the DMS module for report
 *     generation. 
 *     The report is saved in the directory specified on the constants repository. 
 *   </li>
 *   <li>
 *     Send the generated report as an attachment to the user who processed the document.
 *   </li>
 * </ol>
 * Assumptions: <br/> 
 * <ol>
 *   <li> The netting statements are processed are all set up for a single internal legal entity. </li>
 *   <li> For the internal legal entity there is an entry in USER_bo_doc_numbering </li>
 * <ol>
 * <br/> <br/>
 * Constants Repository Configuration: <br/>
 * <table border ="1">
 *   <tr>
 *     <th>
 *       Variable
 *     </th>
 *     <th>
 *       Semantics
 *     </th>
 *     <th>
 *       Default Value if not specified explicitly in Constants Repository
 *     </th>
 *   </tr>
 *   <tr>
 *     <td>
 *       logLevel
 *     </td>
 *     <td>
 *       The log level to be used for PluginLog. See PluginLog Manual for possible values
 *     </td>
 *     <td>
 *       info
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       logFile
 *     </td>
 *     <td>
 *       The name of the file the logs are written to. May not be a full path.
 *     </td>
 *     <td>
 *       (Name of Class).log -> PaymentReportGeneration.log
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       logDir
 *     </td>
 *     <td>
 *       The directory the log file is created within.
 *     </td>
 *     <td>
 *       Value of the environment variable AB_OUTDIR
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       reportOutput
 *     </td>
 *     <td>
 *       The output type of the DMS generated report. Allowed values: OLX, PDF, DOCX, HTML, TXT, CSV
 *       See DMS manual for details.
 *     </td>
 *     <td>
 *       PDF
 *     </td>
 *   </tr>
 *  <tr>
 *     <td>
 *       runOnPreview
 *     </td>
 *     <td>
 *       Indicates whether to generate a payment report if plugin is run in preview mode (Note that preview + output are not covered).
 *       Allowed values: TRUE or FALSE. Should not be set to true in a production system.
 *     </td>
 *     <td>
 *       FALSE
 *     </td>
 *   </tr>
 *  <tr>
 *     <td>
 *       outputDirectory
 *     </td>
 *     <td>
 *       The directory the Payment Reports are stored in. 
 *     </td>
 *     <td>
 *       Value of the environment variable AB_OUTDIR
 *     </td>
 *   </tr>
 *  <tr>
 *     <td>
 *       template
 *     </td>
 *     <td>
 *       The full path of the DMS template to be used to generate the payment report.
 *     </td>
 *     <td>
 *       Value of the constant "PaymentReportGeneration.TEMPLATE_IN_DB"
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       emailSubject
 *     </td>
 *     <td>
 *       Subject of the email that is send to the user processing the PaymentReport
 *     </td>
 *     <td>
 *       PaymentReport
 *     </td>
 *   </tr>
 *  <tr>
 *     <td>
 *       emailBody
 *     </td>
 *     <td>
 *       Additional text for the email body of the email that is send to the user processing the PaymentReport
 *     </td>
 *     <td>
 *       
 *     </td>
 *   </tr>
 *  <tr>
 *     <td>
 *       emailSender
 *     </td>
 *     <td>
 *       Sender of the email that is send to the user processing the PaymentReport
 *     </td>
 *     <td>
 *       The email address of the user processing the PaymentReport.
 *     </td>
 *   </tr>
 *  <tr>
 *     <td>
 *       emailService
 *     </td>
 *     <td>
 *       The name of the OLF EmailService as configured in the Services Manager to be used
 *     </td>
 *     <td>
 *       MailService
 *     </td>
 *   </tr>
 * </table>
 * 
 * @author jwaechter
 * @version 1.2
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcStldocProcess })
public class PaymentReportGeneration extends AbstractGenericOpsServiceListener {

	/**
	 * Default template to be used if nothing is specified in constants repository.
	 */
	private static final String TEMPLATE_IN_DB = "/User/DMS_Repository/Categories/NettingStatements/DocumentTypes/JM-Payment-Report/Templates/JM Payment Report.olt";

	/**
	 *  Name of the payment report document type that is used on the USER_bo_doc_numbering table 
	 */
	private static final String PAYMENT_REPORT_DOC_TYPE_NAME = "Payment Report";

	private static final String CONST_REPO_CONTEXT = "BackOffice";
	private static final String CONST_REPO_SUBCONTEXT = "PaymentReport";

	/**
	 * Name of the party info type "Ext Business Unit Code"
	 */
	private static final String PARTY_CODE_UK = "Party Code UK";

	/**
	 * Flag indicating whether the OPS should run in preview mode or not.
	 */
	private boolean runOnPreview;	

	/**
	 * Flag indicating whether the plug in is run in preview mode or not.
	 */
	private boolean isPreviewMode;

	/**
	 * The output directory the generated document is saved to.
	 */
	private String outputDirectory;

	/**
	 * The output type of the DMS report.
	 */
	private DMSOutputType outputType;

	/**
	 * Template to be used for DMS report generation.
	 */
	private String templateInDb;

	/**
	 * Subject of the email send to the user.
	 */
	private String emailSubject;

	/**
	 * Body text of the email send to the user.
	 */
	private String emailBody;

	/**
	 * The sender's email address
	 */
	private String emailSender;

	/**
	 * The name of OpenLinks email service to be use.
	 */
	private String emailService;

	/**
	 * The id of the BO document type {@link #PAYMENT_REPORT_DOC_TYPE_NAME}.
	 */
	private int paymentReportDocTypeId;

	/**
	 * The id of the legal entity of the current run of the 
	 */
	private int intLegalEntity;

	/**
	 * Table containing the raw data to be used in the DMS report.
	 */
	private Table data;

	@Override
	public void postProcess(final Session session, final EnumOpsServiceType type,
			final ConstTable table, final Table clientData) {
		try {
			process(session, table);
		} catch (Throwable t) { // ensure every exception is logged
			PluginLog.error(t.toString());
			throw t;
		}
	}

	private void process(final Session session, final ConstTable table) {
		if (init (session, table)) {
			Table xmlRawData = null;
			Table docNumData = null;
			Table fieldData = null;
			try {
			List<Document> allDocuments = new ArrayList<Document> ();
				for (int rowNum = data.getRowCount()-1; rowNum >= 0; rowNum--) {
					int docNum = data.getInt("document_num", rowNum);
					
					if(docNum == 0) {
						PluginLog.info("Skipping row 0 document number.");
						continue;
					}
					Document doc = session.getBackOfficeFactory().retrieveDocument(docNum);
					allDocuments.add(doc);
				}
				
				if(allDocuments.size() == 0) {
					PluginLog.info("No documents to process, returning.");
					return;
				}

				xmlRawData = getData (allDocuments, session);
				
				// Update the value in the column ExtSetCcy with the value from the Payment Currency 
				// (Invoice) set by the data load script.
				updatePaymentCurrency(xmlRawData, data);
				
				PluginLog.info(xmlRawData.asCsvString(true));
				intLegalEntity = xmlRawData.getInt("InternalLEntity", 0); // assumption: 1 LE for ALL processed netting statements
				docNumData = getDocNumData(paymentReportDocTypeId, intLegalEntity, session);

				fieldData = session.getTableFactory().createTable("Field Data for XML");
				createFieldData(session, fieldData, docNumData, xmlRawData);

				String xml = createXML (xmlRawData, fieldData, session);
				PluginLog.info("Generated XML that is send to DMS: \n" + xml);
				session.getDebug().printLine(xml, EnumDebugLevel.Medium);
				String outputFileName = generateOutputFilename (session);


				PrintWriter out;
				try {
					out = new PrintWriter(outputFileName + ".xml");
					out.println(xml);
					out.close();
				} catch (FileNotFoundException e1) {
					throw new RuntimeException ("Error writing source xml");
				}

				PluginLog.info ("Attempting to create output document " + outputFileName);
				try {
					DocGen.generateDocument(templateInDb, outputFileName, xml, null, outputType.getOutputTypeId(), 0, null, null, null, null);
					PluginLog.info ("Output document " + outputFileName + " created successfully");
					PluginLog.info ("Sending out report to user...");
					EmailMessage message = EmailMessage.create();
					message.addAttachments(outputFileName, 0, "");
					message.addSubject(emailSubject);
					message.addBodyText(emailBody, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
					message.addRecipients(session.getUser().getEmailAddress());
					message.sendAs(emailSender, emailService);
					PluginLog.info ("Report send to user via email");
				} catch (OException e) {
					throw new RuntimeException (e);
				}
			} finally {
				if (xmlRawData != null) {
					xmlRawData.dispose();
				}
				if (docNumData != null) {
					docNumData.dispose();
				}
				if (fieldData != null) {
					fieldData.dispose();
				}
			}
		}
	}

	/**
	 * Creates the table containing the data for the <Fields> ... </Fields> section of the XML.
	 * @param session
	 * @param fieldData
	 * @param docNumData
	 * @param xmlRawData
	 */
	private void createFieldData(final Session session, final Table fieldData, final Table docNumData, final Table xmlRawData) {
		Date date = session.getTradingDate();
		SimpleDateFormat sdf = new SimpleDateFormat ("dd-MMM-yyyy");
		String newDocSeqNum = getNextDocNumber(docNumData, isPreviewMode, session);

		fieldData.addColumn("ReportNumber", EnumColType.String);
		fieldData.addColumn("ServerDate", EnumColType.String);
		fieldData.addColumn("TotAmountUSD", EnumColType.Double);
		fieldData.addColumn("TotAmountEUR", EnumColType.Double);
		fieldData.addColumn("TotAmountGBP", EnumColType.Double);
		fieldData.addColumn("TotAmountRAN", EnumColType.Double);

		double totAmountUSD = calculateSum (xmlRawData, "USD");
		double totAmountEUR = calculateSum (xmlRawData, "EUR");
		double totAmountGBP = calculateSum (xmlRawData, "GBP");
		double totAmountRAN = calculateSum (xmlRawData, "ZAR");

		fieldData.addRow();	

		fieldData.setString ("ReportNumber", 0, newDocSeqNum);
		fieldData.setString ("ServerDate", 0, sdf.format(date));
		fieldData.setDouble ("TotAmountUSD", 0, totAmountUSD);
		fieldData.setDouble ("TotAmountEUR", 0, totAmountEUR);
		fieldData.setDouble ("TotAmountGBP", 0, totAmountGBP);
		fieldData.setDouble ("TotAmountRAN", 0, totAmountRAN);
	}

	/**
	 * Retrieves the sum of the netting amount (value of column "NettingAmoung") for a certain currency 
	 * (ccy matches the value of column "ExtSetCcy").
	 * @param xmlRawData
	 * @param ccy
	 * @return
	 */
	private double calculateSum(Table xmlRawData, String ccy) {
		double sum=0.0d;
		for (TableRow row : xmlRawData.getRows()) {
			if (row.getString("ExtSetCcy").equalsIgnoreCase(ccy)) {
				sum += row.getDouble("NettingAmount");
			}
		}
		return sum;
	}

	/**
	 * Generates the file name of the output file including the output directory. 
	 * @param session
	 * @return the generated file name
	 */
	private String generateOutputFilename(Session session) {
		StringBuilder ofn = new StringBuilder ();
		Date date = session.getServerTime();
		String user = session.getUser().getName();
		SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd_HHmmss");
		ofn.append("PaymentReport_");ofn.append(sdf.format(date));
		ofn.append("_").append(user).append(".").append(outputType.getFileExtension());

		Path path = FileSystems.getDefault().getPath (outputDirectory, ofn.toString());
		return path.toString();		
	}


	/**
	 * Creates the content of the XML that is to be passed to the DMS for document generation.
	 * @param data the table containing the data for each processed netting statement. Each row 
	 * is converted into an individual <TableNettingData> section.
	 * Each colum creates a new xml attribute with the attributes name being the
	 * column name.
	 * @param fieldData contains the data for the "Fields" section of the XML. Each row is going to be
	 * converted into an individual <Fields> section but assumption is there is only a single row.
	 * Each column is converted into an individual field in each <Fields> section. 
	 * @param session
	 * @return
	 */
	private String createXML(Table data, Table fieldData, Session session) {
		formatData(data);
		formatData(fieldData);

		StringBuilder xml = new StringBuilder ();
		xml.append("<XmlData>\n");
		xml.append("  <JM_PaymentReport>\n");
		for (TableRow row : fieldData.getRows()) {
			xml.append("  	<Fields>\n");
			for (TableColumn col : fieldData.getColumns()) {
				xml.append("         <").append(col.getName()).append(">");
				xml.append(row.getValue(col.getNumber()).toString());
				xml.append("</").append(col.getName()).append(">\n");
			}
			xml.append("  	</Fields>\n");			
		}
		for (TableRow row : data.getRows()) {
			xml.append("  	<TableNettingData>\n");
			for (TableColumn col : data.getColumns()) {
				xml.append("         <").append(col.getName()).append(">");
				String value = row.getValue(col.getNumber()).toString();
				value = value.replaceAll("&", "&amp;");
				xml.append(value);
				xml.append("</").append(col.getName()).append(">\n");
			}
			xml.append("  	</TableNettingData>\n");			
		}
		xml.append("  </JM_PaymentReport>\n");	
		xml.append("</XmlData>\n");	
		return xml.toString();
	}

	/**
	 * Ensures proper formatting of the XML output. Loops through all columns and applies
	 * the same formatter for each column of a column type matching the formatters ones.
	 * @param data
	 */
	private void formatData(Table data) {
		TableFormatter tft = data.getFormatter();
		ColumnFormatterAsDouble formDouble = tft.createColumnFormatterAsDouble(EnumFormatDouble.Notnl);
		ColumnFormatterAsDateTime formDateTime = tft.createColumnFormatterAsDateTime(EnumFormatDateTime.Date);
		int colIds[] = new int [data.getColumnCount()];
		EnumColType colTypes[] = new EnumColType [data.getColumnCount()];
		formDouble.setPrecision(2);

		for (TableColumn col : data.getColumns()) {
			switch (data.getColumnType(col.getNumber())) {
			case Double:
				tft.setColumnFormatter(col.getNumber(), formDouble);
				break;
			case Date:
			case DateTime:
				tft.setColumnFormatter(col.getNumber(), formDateTime);
				break;
			}
			colIds[col.getNumber()] = col.getNumber();
			colTypes[col.getNumber()] = EnumColType.String;
		}
		data.convertColumns(colIds, colTypes);
	}
	/**
	 * Initializes the plugin by retrieving the constants repository values
	 * and initializing PluginLog.
	 * @param session
	 * @param table
	 * @return
	 */
	private boolean init(final Session session, final ConstTable table) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, 
					CONST_REPO_SUBCONTEXT);
			logLevel = constRepo.getStringValue("logLevel", "info");
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			String reportOutput = constRepo.getStringValue("reportOutput", "PDF");
			PluginLog.init(logLevel, logDir, logFile);

			outputType = DMSOutputType.fromExtension(reportOutput);
			String runOnPreviewString =  constRepo.getStringValue("runOnPreview", "FALSE").trim();
			runOnPreview = Boolean.parseBoolean(runOnPreviewString);
			outputDirectory = constRepo.getStringValue("outputDirectory", abOutdir);
			templateInDb = constRepo.getStringValue("template", TEMPLATE_IN_DB);
			emailSubject = constRepo.getStringValue("emailSubject", "PaymentReport");
			emailBody = constRepo.getStringValue("emailBody", "");
			emailSender = constRepo.getStringValue("emailSender", session.getUser().getEmailAddress());
			emailService = constRepo.getStringValue("emailService", "MailService");
			checkOutputDirectory();			
			paymentReportDocTypeId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentType, PAYMENT_REPORT_DOC_TYPE_NAME);
		}  catch (OException e) {
			throw new RuntimeException(e);
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info(this.getClass().getName() + " started");		
		String action = table.getString("Action", 0);
		data = table.getTable("data", 0);
		if (action.equalsIgnoreCase("Preview Gen Data")) {
			isPreviewMode = true;
			if (runOnPreview) {
				return true;			
			}
		}
		if (action.equalsIgnoreCase("Process")) {
			isPreviewMode = false;
			return true;
		}
		return false;
	}


	private void checkOutputDirectory() {
		Path path = FileSystems.getDefault().getPath (outputDirectory);
		if (!java.nio.file.Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			throw new RuntimeException ("Output Directory does not exist");
		}
	}

	/**
	 * Executes an SQL to retrieve the data to be exported to DMS.
	 * The SQL uses aliases that match the xml tag names of the data to ensure
	 * the mapping between table and XML can be done in a generic way.
	 * @param documents 
	 * @param session
	 * @return
	 */
	private Table getData (final List<Document> documents, final Session session) {
		String docNumPart = generateDocNumPart ("sdh.document_num", documents);
		String sql = 
				"\nSELECT sdh.document_num AS SequenceNum"
						+   "\n  ,sdh.doc_total_amount AS NettingAmount"
						+   "\n  ,sdh.pymt_due_date AS PymtDate"
						+   "\n  ,ISNULL(extbu.long_name, 'Not Defined') AS ExtBULongName"
						+   "\n  ,ISNULL(ext_acc.account_number, 'Not Defined') AS CostAccount"
						+   "\n  ,ISNULL(ext_acc.account_iban, 'Not Defined') AS IBAN"
						+   "\n  ,ext_settle_cur_via_details.name AS ExtSetCcy"
						+   "\n  ,ISNULL(extcash.long_name, 'Not Defined') AS SetExtCashLongName"
						+   "\n  ,ISNULL(extcashadr.addr1, 'Not Defined') AS SetExtCashAddr1"
						+   "\n  ,extcashadr.addr2 AS SetExtCashAddr2"
						+   "\n  ,extcashadr.mail_code AS SetExtCashAddCode"
						+   "\n  ,ISNULL(extcashadr.city, 'Not Defined') AS SetExtCashCity"
						+   "\n  ,ISNULL(extcashadrcountry.name, 'Not Defined') AS SetExtCashCountry"
						+   "\n  ,ISNULL(extcashadr.bic_code, 'Not Defined') AS SetExtCashBic"
						+   "\n  ,pi.value AS CostNum"
						+   "\n  ,sdd.internal_lentity AS InternalLEntity"
						+   "\nFROM stldoc_header sdh"	
						+   "\n  INNER JOIN stldoc_details sdd ON sdd.document_num = sdh.document_num"
						+   "\n  AND sdd.event_num = "
						+   "\n    (SELECT MAX(sdd2.event_num) FROM stldoc_details sdd2 WHERE sdd2.document_num = sdd.document_num)"
						+   "\n  LEFT OUTER JOIN account ext_acc ON ext_acc.account_id = sdd.ext_account_id"
						+   "\n  LEFT OUTER JOIN party extbu ON extbu.party_id = sdd.external_bunit"
						+   "\n  LEFT OUTER JOIN party extcash ON extcash.party_id = ext_acc.holder_id"
						+   "\n  LEFT OUTER JOIN business_unit extcashadr ON extcashadr.party_id = extcash.party_id"
						+   "\n  LEFT OUTER JOIN currency ext_set_cur ON ext_set_cur.id_number = ext_acc.base_currency"
						+   "\n  LEFT OUTER JOIN currency ext_settle_cur_via_details ON ext_settle_cur_via_details.id_number = sdd.settle_ccy"
						+   "\n  LEFT OUTER JOIN party_info pi ON pi.party_id = extbu.party_id"
						+   "\n  AND pi.type_id = (SELECT pit.type_id FROM party_info_types pit WHERE "
						+   "\n    pit.type_name = '" + PARTY_CODE_UK + "')"
						+   "\n  LEFT OUTER JOIN country extcashadrcountry ON extcashadrcountry.id_number = extcashadr.country"
						+   "\nWHERE " + docNumPart
						;
		
		;
		Table sqlResult = session.getIOFactory().runSQL(sql);
		return sqlResult;
	}

	private String generateDocNumPart(String aliasAndCol, List<Document> documents) {
		StringBuilder sb = new StringBuilder (aliasAndCol).append(" IN (");
		boolean first = true;
		for (Document doc : documents) {
			if (first) {
				first = false;
				sb.append(doc.getId());
			} else {
				sb.append(",").append(doc.getId());
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Get the data row of the USER_bo_doc_numbering table that
	 * matches docTypeId and intLeId
	 * @param docTypeId
	 * @param intLeId
	 * @param session
	 * @return
	 */
	private Table getDocNumData (final int docTypeId, final int intLeId, final Session session) {
		String sql = 
				"\nSELECT * "
						+   "\nFROM USER_bo_doc_numbering"
						+   "\nWHERE doc_type_id = " + docTypeId + " AND our_le_id = " + intLeId;
		;
		Table sqlResult = session.getIOFactory().runSQL(sql);
		if (sqlResult.getRowCount() == 0) {
			throw new RuntimeException ("The legal entity #" + intLeId + " does not have an doc number sequence in "
					+ " USER_bo_doc_numbering");
		}
		return sqlResult;
	}

	/**
	 * Retrieves the email address for a provided personnel id.
	 * @param userId personnel id
	 * @param userName name of the user (used in exeception messages only)
	 * @return
	 * @throws RuntimeException in case the email address is invalid
	 */
	public static String getEmailFromUser (int userId, String userName, Session session) {
		String sql =
				"\nSELECT p.email "
						+	"\nFROM personnel p"
						+	"\nWHERE p.id_number = " + userId;
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			String email = sqlResult.getString("email", 1);
			if (!validateEmailAddress (email)) {
				String errorMessage = "Emailaddress " + email + " for user " + userName + " having ID #" + userId + " is not valid";
				throw new RuntimeException(errorMessage);
			}
			return email;			
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}

	/**
	 * Checks whether a provided String is a valid email address or not
	 * @param emailAddress
	 * @return
	 */
	private static boolean validateEmailAddress (String emailAddress) {
		String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(emailAddress);
		return matcher.matches();		
	}

	/**
	 * Retrieves the next doc number according to the logic of
	 * BO document numbering. Note that this method contains a copy of the
	 * BO document numbering logic and might need modifications concurrently
	 * with the BO doc numbering SC.
	 * @param tblDocNumberingData
	 * @param isPreview
	 * @return
	 */
	private String getNextDocNumber(Table tblDocNumberingData, boolean isPreview, Session session) 
	{
		if (isPreview)
			return peekNextDocNumber(tblDocNumberingData);

		long doc_num = -1L;
		ConsecutiveNumber cn;
		try
		{
			cn = new ConsecutiveNumber("OLI_DocNumbering");
		}
		catch (ConsecutiveNumberException e)
		{ throw new RuntimeException(e.getMessage()); }
		String item = ""+tblDocNumberingData.getInt("our_le_id", 0)
				+ "_"+tblDocNumberingData.getInt("doc_type_id", 0);

		String reset_number_to = tblDocNumberingData.getString("reset_number_to", 0).trim();
		if (reset_number_to.length() > 0)
		{
			try
			{
				doc_num = Long.parseLong(reset_number_to);
				cn.resetItem(item, doc_num);
			}
			catch (NumberFormatException e)
			{ throw new RuntimeException("NumberFormatException: " + reset_number_to); }
			catch (ConsecutiveNumberException e)
			{ throw new RuntimeException(e.getMessage()); }
			catch (OException ex) { throw new RuntimeException(ex); }
		}

		try
		{
			doc_num = cn.next(item);
		}
		catch (ConsecutiveNumberException e)
		{ throw new RuntimeException(e.getMessage()); } 
		catch (OException e) {	throw new RuntimeException(e);  }

		if (doc_num >= 0) {
			Table tblDocNumbering = tblDocNumberingData.cloneData();

			tblDocNumbering.setString("last_number", 0, ""+doc_num);
			tblDocNumbering.setString("reset_number_to", 0, "");

			if (!isPreview)
			{
				session.getIOFactory().getUserTable("USER_bo_doc_numbering").updateRows(tblDocNumbering, "doc_type_id, our_le_id");
			}
			tblDocNumbering.dispose();			
		}

		return doc_num >= 0 ? (""+doc_num) : null;
	}

	private String peekNextDocNumber(Table tblDocNumberingData)
	{
		long doc_num = 1L; // initial 'next' number
		String reset_number_to = tblDocNumberingData.getString("reset_number_to", 0).trim();
		String last_number = tblDocNumberingData.getString("last_number", 0).trim();

		if (reset_number_to.length() > 0)
		{
			try
			{
				doc_num = Long.parseLong(reset_number_to);
			}
			catch (NumberFormatException e)
			{ throw new RuntimeException("NumberFormatException: " + reset_number_to); }
		}
		else if (last_number.length() > 0)
		{
			try
			{
				doc_num = Long.parseLong(last_number) + 1;
			}
			catch (NumberFormatException e)
			{ throw new RuntimeException("NumberFormatException: " + last_number); }
		}

		return doc_num >= 0 ? (""+doc_num) : null;
	}
	
	/**
	 * Update payment currency column ExtSetCcy with the value of the dataload script 
	 * column payment_currency_invoice..
	 *
	 * @param rawXMLData the raw xml data
	 * @param eventData the event data
	 */
	private void updatePaymentCurrency(Table rawXMLData, Table eventData) {
		for(int i = 0; i < rawXMLData.getRowCount(); i++) {
			int docNumber = rawXMLData.getInt("SequenceNum", i);
			
			int row = eventData.find(0, docNumber, 0);
			
			if(row >= 0) {
				Table stlDoc = eventData.getTable("details", row);
				
				// All events have the same currency so ok to take value from first row
				String currency = stlDoc.getString("payment_currency_Invoice", 0);
				
				rawXMLData.setString("ExtSetCcy", i, currency);				
			} else {
				throw new RuntimeException("No event data found for document: " + docNumber); 
			}		
		}
	}
}
