package com.jm.util.weeklysalesreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.FileUtil;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openrisk.calendar.DateTime;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

@ScriptCategory({ EnumScriptCategory.Generic })
public class WeeklySalesReportOC extends AbstractGenericScript {

	private Context _context = null;

	private String defaultLogFile = null;
	private String targetFilePath = null;
	private String templateFilePath = null;
	private String outputFileName = null;
	private String emailRequired = null;
	private String mailService = null;
	private String toList = "";
	private String subject = null;
	private String body = null;
	private String toListExt = "";
	private String csvFileName = "";

	@Override
	public Table execute(Context context, ConstTable table) {

		this._context = context;

		Table output = null;

		try {

			init();

			output = createTable(output);

			readCSV(output);

			String file = saveExcel(output);

			if (emailRequired.equalsIgnoreCase("yes"))
				sendEmail(file);
			else
				Logging.info("Email_Required not set as Yes,No Email will be sent");

		} catch (Exception e) {
			Logging.error(e.getMessage());
			Util.exitFail();
		} finally {
			Logging.close();
		}

		return null;
	}

	public void sendEmail(String fileToAttach) throws Exception {

		if (subject == null || body == null || toList.isEmpty()) {
			Logging.error("Could not find manadatory parameter(Subject,body,toList) for Email Functionality");
			throw new Exception("Unable to find parameters for email functionality");
		}
		toList = toList + ";" + toListExt;

		boolean status = checkMailServiceStatus();
		if (!status)
			throw new Exception("Can't send email, service is offline");

		if ((body.indexOf("(d)") > 0)) {
			String prefix = body.substring(0, body.indexOf("(d)"));
			String suffix = body.substring(body.indexOf("(d)") + 3);
			body = prefix + "-" + OCalendar.formatDateInt(OCalendar.today()) + "-" + suffix;
		}
		String emailBody = "<html> <body> <font size=\"3\"> Hi \n\r <p>" + body
				+ "</p> \n\r Thanks </font> </body> </html>";

		Logging.info(" Sending Email");

		boolean retVal = sendEmail(toList, subject, emailBody, fileToAttach, mailService);

		if (!retVal) {
			Logging.error(" Error while sending email titled subject: " + subject + " to " + toList);
			throw new OException(" Error while sending email titled: " + subject + " to " + toList);
		} else
			Logging.info(" Mail Sent Successfully");
	}

	public boolean checkMailServiceStatus() throws OException {

		com.olf.openjvs.Table tbl = null;

		boolean status = false;

		try {
			tbl = Util.serviceGetStatus(mailService);
			if (com.olf.openjvs.Table.isTableValid(tbl) != 1) {
				Logging.error("Failed while checking email service status");
				throw new OException("Failed while checking email service status");
			}
			status = tbl.getString("service_status_text", 1).equalsIgnoreCase("Running") ? true : false;
		} finally {
			if (com.olf.openjvs.Table.isTableValid(tbl) == 1) {
				tbl.destroy();
			}
		}
		return status;
	}

	private void readCSV(Table output) throws OException {

		Logging.info("Reading CSV....");

		String csvPath = targetFilePath + "\\" + csvFileName.replaceFirst(".csv", "")
				+ OCalendar.formatJd(OCalendar.today(), com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601)
				+ csvFileName.substring(csvFileName.lastIndexOf("."));

		Logging.info("Reading CSV from: " + csvPath);

		output.importCsv(csvPath, true);

		if (output.getRowCount() == 0) {
			Logging.warn("No rows found in the output table. Please check file at: " + csvPath);
		}

		Logging.info("Csv has been read successfully");

	}

	private String saveExcel(Table pivot) throws OException, IOException, ParseException {

		String reportPartialName;
		int uniqueID = DBUserTable.getUniqueId();
		String currentDate = OCalendar.formatJd(OCalendar.today(),
				com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601);

		reportPartialName = targetFilePath + "\\" + outputFileName;

		String oldFileName = reportPartialName + templateFilePath.substring(templateFilePath.lastIndexOf("."));
		String newFileName = reportPartialName + "_" + currentDate + "_" + uniqueID
				+ templateFilePath.substring(templateFilePath.lastIndexOf("."));

		FileUtil.exportFileFromDB(templateFilePath, targetFilePath);
		Logging.info("Template has been copied to" + targetFilePath);

		Files.move(Paths.get(oldFileName), Paths.get(newFileName), StandardCopyOption.REPLACE_EXISTING);
		Logging.info("Output file has been renamed to" + newFileName);

		FileInputStream file = new FileInputStream(newFileName);

		XSSFWorkbook workbook = new XSSFWorkbook(file);

		XSSFSheet spreadsheet = workbook.getSheet("Data");

		XSSFRow excelRow;

		List<DateTime> list = new ArrayList<DateTime>();

		int rowid = 1;

		for (TableRow row : pivot.getRows()) {

			excelRow = spreadsheet.getRow(rowid++);

			Object[] objectArr = row.getValues();

			int cellid = 0;

			for (Object obj : objectArr) {

				XSSFCell cell = excelRow.createCell(cellid++);

				if (obj instanceof Double) {
					cell.setCellValue((Double) obj);
				}
				if (obj instanceof Integer) {
					cell.setCellValue((Integer) obj);
				}
				if (obj instanceof String) {
					cell.setCellValue((String) obj);
				}
				if (obj instanceof DateTime) {
					CellStyle cellStyle = workbook.createCellStyle();
					CreationHelper createHelper = workbook.getCreationHelper();
					cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-mmm-yyyy h:mm:ss AM/PM"));
					cell.setCellValue((Date) obj);
					cell.setCellStyle(cellStyle);
					if (cell.getColumnIndex() == 5) {
						list.add((DateTime) obj);
					}
				}
			}
		}

		XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);

		FileOutputStream out = new FileOutputStream(new File(newFileName));

		workbook.write(out);
		out.flush();
		out.close();
		workbook.close();

		Logging.info("Data has been saved to output file_" + newFileName);

		return newFileName;
	}

	private Table createTable(Table output) {

		output = _context.getTableFactory().createTable();

		Logging.info("Creating output table to hold data from csv");

		output.addColumn("Business_Unit", EnumColType.String);
		output.addColumn("Metal_Currency", EnumColType.String);
		output.addColumn("Opening_Date", EnumColType.DateTime);
		output.addColumn("Opening_Volume", EnumColType.Double);
		output.addColumn("Opening_Price", EnumColType.Double);
		output.addColumn("deal_date", EnumColType.DateTime);
		output.addColumn("Opening_Value", EnumColType.Double);
		output.addColumn("Buy_Sell", EnumColType.String);
		output.addColumn("Deal_Num", EnumColType.Int);
		output.addColumn("External_Business_Unit", EnumColType.String);
		output.addColumn("End_User", EnumColType.String);
		output.addColumn("Deal_Leg", EnumColType.Int);
		output.addColumn("Deal_Profile", EnumColType.Int);
		output.addColumn("Deal_Reset", EnumColType.Int);
		output.addColumn("Delivery_Volume", EnumColType.Double);
		output.addColumn("Delivery_Price", EnumColType.Double);
		output.addColumn("Delivery_Value", EnumColType.Double);
		output.addColumn("Closing_Date", EnumColType.String);
		output.addColumn("Closing_Volume", EnumColType.Double);
		output.addColumn("Closing_Price", EnumColType.Double);
		output.addColumn("Closing_Value", EnumColType.Double);
		output.addColumn("deal_profit", EnumColType.Double);
		output.addColumn("B2B", EnumColType.String);

		Logging.info("Table structure has been created successfully");

		return output;
	}

	private boolean sendEmail(String toList, String subject, String body, String fileToAttach, String mailServiceName)
			throws OException {

		EmailMessage mymessage = EmailMessage.create();
		boolean retVal = false;

		try {

			// Add subject and recipients
			mymessage.addSubject(subject);
			mymessage.addRecipients(toList);

			// Prepare email body
			StringBuilder emailBody = new StringBuilder();

			emailBody.append(body);

			mymessage.addBodyText(emailBody.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);

			// Add attachment
			if (fileToAttach != null && !fileToAttach.trim().isEmpty() && new File(fileToAttach).exists()) {

				Logging.info("Attaching file to the mail..");
				mymessage.addAttachments(fileToAttach, 0, null);
				retVal = true;

			}
			mymessage.send(mailServiceName);
		} catch (OException e) {
			throw new OException(e.getMessage());
		} finally {
			mymessage.dispose();
		}
		return retVal;
	}

	private void init() throws ConstantTypeException, ConstantNameException, OException {

		Logging.init(this.getClass(), "", "");
		Logging.info("Start :" + getClass().getName());
		ConstRepository constRepo = new ConstRepository("ReportBuilder", "WeeklySalesReport");
		Logging.info("Reading data from const repository");
		defaultLogFile = constRepo.getStringValue("DEFAULT_LOG_FILE");
		templateFilePath = constRepo.getStringValue("TEMPLATE_FILE_PATH");
		outputFileName = constRepo.getStringValue("OUTPUT_FILE_NAME");
		emailRequired = constRepo.getStringValue("EMAIL_REQUIRED");
		mailService = constRepo.getStringValue("MAIL_SERVICE");
		toList = convertUserNamesToEmailList(constRepo.getStringValue("TO_LIST"));
		subject = constRepo.getStringValue("SUBJECT") + "|" + OCalendar.formatDateInt(OCalendar.today());
		;
		body = constRepo.getStringValue("BODY");
		toListExt = constRepo.getStringValue("TO_LIST_EXT");
		targetFilePath = Util.reportGetDirForToday();
		csvFileName = constRepo.getStringValue("CSV_FILE_NAME");
		Logging.info("Data read from const repository successfully");
		Logging.info("Parameters values are: DEFAULT_LOG_FILE: " + defaultLogFile + ",\n TEMPLATE_FILE_PATH: "
				+ templateFilePath + ",\n OUTPUT_FILE_NAME" + outputFileName + ",\n EMAIL_REQUIRED:" + emailRequired
				+ ",\n MAIL_SERVICE: " + mailService + ",\n TO_LIST:" + toList + ",\n SUBJECT: " + subject
				+ ",\n BODY: " + body + ",\n TO_LIST_EXT: " + toListExt + "\n csvFileName: " + csvFileName);
	}

	public String convertUserNamesToEmailList(String listOfUsers) throws OException {

		listOfUsers = listOfUsers.replaceAll(",", ";");
		String personnelSplit[] = listOfUsers.split(";");
		int personnelSplitCount = personnelSplit.length;
		String SQLlistOfUsers = "";
		String SQLlistOfEmails = "";
		String retEmailValues = "";

		for (int iLoop = 0; iLoop < personnelSplitCount; iLoop++) {
			String thisUser = personnelSplit[iLoop].trim();
			if (thisUser.length() > 0) {
				if (thisUser.indexOf("@") > 0) {
					if (SQLlistOfEmails.length() > 0) {
						SQLlistOfEmails = SQLlistOfEmails + "," + "'" + thisUser + "'";
					} else {
						SQLlistOfEmails = "'" + thisUser + "'";
					}
				} else {
					if (SQLlistOfUsers.length() > 0) {
						SQLlistOfUsers = SQLlistOfUsers + "," + "'" + thisUser + "'";
					} else {
						SQLlistOfUsers = "'" + thisUser + "'";
					}
				}
			}
		}

		if (SQLlistOfUsers.length() > 0 || SQLlistOfEmails.length() > 0) {

			String sqlByUser = "SELECT * FROM personnel per \n" + " WHERE per.name IN (" + SQLlistOfUsers + ")\n"
					+ " AND per.status = 1";

			String sqlByEmail = "SELECT * FROM personnel per \n" + " WHERE per.email IN (" + SQLlistOfEmails + ")\n"
					+ " AND per.status = 1";

			String sqlUnion = "";
			if (SQLlistOfUsers.length() > 0) {
				sqlUnion = sqlByUser;
			}
			if (SQLlistOfEmails.length() > 0) {
				if (sqlUnion.length() > 0) {
					sqlUnion = sqlUnion + "UNION " + sqlByEmail;
				} else {
					sqlUnion = sqlByEmail;
				}

			}

			com.olf.openjvs.Table personnelTable = com.olf.openjvs.Table.tableNew();
			DBaseTable.execISql(personnelTable, sqlUnion);
			int personnelTableCount = personnelTable.getNumRows();
			for (int iLoop = 1; iLoop <= personnelTableCount; iLoop++) {
				String emailReturned = personnelTable.getString("email", iLoop);
				if (retEmailValues.length() > 0) {
					retEmailValues = retEmailValues + ";" + emailReturned;
				} else {
					retEmailValues = emailReturned;
				}
			}

			personnelTable.destroy();
		}

		if (retEmailValues.length() == 0) {
			Logging.error("Unrecognised email found : " + listOfUsers + " Going to use supports email");
			String sql = "SELECT * FROM personnel per \n" + " WHERE per.name ='Endur_Support'\n"
					+ " AND per.status = 1";
			com.olf.openjvs.Table personnelTable = com.olf.openjvs.Table.tableNew();
			DBaseTable.execISql(personnelTable, sql);
			int personnelTableCount = personnelTable.getNumRows();
			for (int iLoop = 1; iLoop <= personnelTableCount; iLoop++) {
				String emailReturned = personnelTable.getString("email", iLoop);
				if (retEmailValues.length() > 0) {
					retEmailValues = retEmailValues + ";" + emailReturned;
				} else {
					retEmailValues = emailReturned;
				}
			}
			personnelTable.destroy();
		}

		return retEmailValues;
	}
}