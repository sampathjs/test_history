package com.matthey.testutil.mains;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.common.Util;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.jm.logging.Logging;

/**
 * This utility converts the CSV in the appropriate XML file
 * @author SharmV04
 *
 */
public class SapHttpRequestorHelper
{
	private String inputCsvPath;
	private Table inputCSV;

	/**
	 * @param argCsvPath
	 */
	public SapHttpRequestorHelper(String argCsvPath)
	{
		inputCsvPath = argCsvPath;
	}

	/**
	 * @return
	 * @throws OException
	 * @throws IOException
	 */
	public List<String> getXmlMessages() throws OException, IOException
	{
		List<String> xmlMessageList;
		int numberOfRowsInInputCSV;
		int numberOfColumnsInInputCSV;
		int currentRowInInputCSV;
		int currentColumnInInputCSV;
		String path;
		String valueInCell;
		String columnName;
		String csvInputFileFolderPath;
		String templateFileName;
		byte[] encoded;
		String fileDataInString;
		
		inputCSV = Table.tableNew();
		
		inputCSV.inputFromCSVFile(inputCsvPath);
		Util.updateTableWithColumnNames(inputCSV);
		Logging.debug("Input CSV:");
		Util.printTableOnLogTable(inputCSV);
		
		csvInputFileFolderPath = inputCsvPath.substring(0, inputCsvPath.lastIndexOf('\\'));
		
		numberOfRowsInInputCSV = inputCSV.getNumRows();
		numberOfColumnsInInputCSV = inputCSV.getNumCols();
		
		Logging.debug("Number of rows in input CSV file: " + numberOfRowsInInputCSV);
		xmlMessageList = new LinkedList<String>();
		
		for (currentRowInInputCSV = 1; currentRowInInputCSV <= numberOfRowsInInputCSV; currentRowInInputCSV++)
		{
			templateFileName = inputCSV.getString("template_path", currentRowInInputCSV);
			path = csvInputFileFolderPath + "\\" + templateFileName;
			Logging.debug("Template file path: " + path);

			encoded = Files.readAllBytes(Paths.get(path));
			fileDataInString = new String(encoded, StandardCharsets.UTF_8);
			Logging.debug("Template file:");
			Logging.debug(fileDataInString);
			for(currentColumnInInputCSV=1;currentColumnInInputCSV<numberOfColumnsInInputCSV;currentColumnInInputCSV++)
			{
				valueInCell = inputCSV.getString(currentColumnInInputCSV, currentRowInInputCSV);
				columnName = inputCSV.getColName(currentColumnInInputCSV);
				Logging.debug("Replace $" + columnName +" with " + valueInCell + " in template XML");
				fileDataInString = fileDataInString.replaceAll(Pattern.quote(">$"+columnName+"<"), ">"+valueInCell+"<");			
			}
			Logging.debug("Message created from row " + currentRowInInputCSV +" of input CSV:");
			Logging.debug(fileDataInString);
			xmlMessageList.add(fileDataInString);
		}
		return xmlMessageList;
	}
	
	/**
	 * @param table
	 * @param rowNumber
	 * @throws OException
	 */
	public void updateInfoFieldValue(Table table, int rowNumber) throws OException
	{
		if( table.getColNum(SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER) >= 1)
		{
			String coverageInstructionNumber = inputCSV.getString(SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER, rowNumber);
			Logging.debug(SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER+" is present in table. Updating value of " + SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER+" in row " + rowNumber +" with with " + coverageInstructionNumber);
			table.setString(SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER, rowNumber, coverageInstructionNumber);
		}
		else
		{
			String metalTransferRequestNumber = inputCSV.getString(SAPTestUtilitiesConstants.METAL_TRANSFER_REQUEST_NUMBER, rowNumber);
			Logging.debug(SAPTestUtilitiesConstants.METAL_TRANSFER_REQUEST_NUMBER+" is present in table. Updating value of " + SAPTestUtilitiesConstants.METAL_TRANSFER_REQUEST_NUMBER+" in row " + rowNumber +" with with " + metalTransferRequestNumber);
			table.setString(SAPTestUtilitiesConstants.METAL_TRANSFER_REQUEST_NUMBER, rowNumber, metalTransferRequestNumber);
		}
	}
	
	/**
	 * @throws OException
	 */
	public void cleanup() throws OException
	{
		if (inputCSV != null)
		{
			inputCSV.destroy();
		}
	}
}