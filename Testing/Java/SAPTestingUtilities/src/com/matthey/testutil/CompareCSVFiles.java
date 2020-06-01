package com.matthey.testutil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.matthey.testutil.common.Util;
import com.matthey.testutil.enums.ResultTableColumns;
import com.matthey.testutil.enums.ValidationResults;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Compare CSV files and export result in CSV file
 * @author SharmV04
 */
public class CompareCSVFiles implements IScript
{

	Table validationResultTable = com.olf.openjvs.Util.NULL_TABLE;
	List<String> pkColumnNameList;
	List<String> skipColumnNameList;
	List<Column> dsSchema;
	double toleranceThreshold;
	
	/**
	 * @return {@link validationResultTable}
	 */
	public Table getValidationResultTable() {
		return validationResultTable;
	}

	/**
	 * @return {@link pkColumnNameList}
	 */
	public List<String> getPkColumnNameList() {
		return pkColumnNameList;
	}

	/**
	 * @return  {@link skipColumnNameList}
	 */
	public List<String> getSkipColumnNameList() {
		return skipColumnNameList;
	}

	/**
	 * @throws OException
	 */
	public void setValidationResultTable() throws OException {
		this.validationResultTable = Table.tableNew();
		for (int primaryKeyNumber = 0; primaryKeyNumber < getPkColumnNameList().size(); primaryKeyNumber++)
		{
			validationResultTable.addCol(getPkColumnNameList().get(primaryKeyNumber), COL_TYPE_ENUM.COL_STRING);
		}

		validationResultTable.addCol("validation_result", COL_TYPE_ENUM.COL_STRING);
		validationResultTable.addCol("column_name", COL_TYPE_ENUM.COL_STRING);
		validationResultTable.addCol("expected_value", COL_TYPE_ENUM.COL_STRING);
		validationResultTable.addCol("actual_value", COL_TYPE_ENUM.COL_STRING);

	}

	/**
	 * @param pkColumnNames Semicolon separated {@code pkColumnNames}
	 */
	public void setPkColumnNameList(String pkColumnNames) {
		this.pkColumnNameList = Arrays.asList(pkColumnNames.split(";"));
	}

	/**
	 * @param skipColumnNames Semicolon separated {@code skipColumnNames}
	 */
	public void setSkipColumnNameList(String skipColumnNames) {
		this.skipColumnNameList = Arrays.asList(skipColumnNames.split(";"));
	}

	/**
	 * @return {@link dsSchema}
	 */
	public List<Column> getDsSchema() {
		return dsSchema;
	}

	/**
	 * @param datasourceColumns
	 * @throws OException
	 */
	public void setDsSchema(String datasourceColumns) throws OException {
		this.dsSchema = new ArrayList<>();
		List<String> dsColumnDetailList = Arrays.asList(datasourceColumns.split(";"));
		for(int index=0; index < dsColumnDetailList.size(); index++)
		{
			String dsColumnDetailString = dsColumnDetailList.get(index);
			List<String> dsColumnDetail = Arrays.asList(dsColumnDetailString.split(":"));
			if(dsColumnDetail.size() != 2)
			{
				throw new OException("Invalid column definition in the input variable datasource_columns:'" 
							+ dsColumnDetailString + "'. Valid format is 'column-name:data-type'.");
			}

			Column column = new Column();
			column.setName(dsColumnDetail.get(0));
			
			COL_TYPE_ENUM colType = COL_TYPE_ENUM.COL_STRING;
			String dsColumnType = dsColumnDetail.get(1);
			if("integer".equalsIgnoreCase(dsColumnType))
			{
				colType = COL_TYPE_ENUM.COL_INT;
			} else if("double".equalsIgnoreCase(dsColumnType))
			{
				colType = COL_TYPE_ENUM.COL_DOUBLE;
			}
			column.setType(colType);
			
			this.dsSchema.add(column);
		}

	}

	class Column
	{
		String name;
		COL_TYPE_ENUM type;
		
		public String getName() 
		{
			return name;
		}
		public void setName(String name) 
		{
			this.name = name;
		}
		public COL_TYPE_ENUM getType() 
		{
			return type;
		}
		public void setType(COL_TYPE_ENUM type) 
		{
			this.type = type;
		}				
	}
	
	@Override
	public void execute(IContainerContext context) throws OException
	{
		Util.setupLog();
		Table tblArgt = context.getArgumentsTable();
		Table actualDataSource = com.olf.openjvs.Util.NULL_TABLE;
		String expectedDatasourcePath;
		String actualCsvDatasourcePath;
		String datasourceColumns;
		String pkColumnNames;
		String skipColumnNames;
		String outputDirectoryPath;
		String tolerance;
		String actualCSVFilePath;
		File actualCsvDatasourceFile;

		Logging.info("Started executing script "+this.getClass().getName());

		if (tblArgt.getNumRows() == 0)
		{
			throw new OException("Missing input in the argument table. Expected columns: expected_csv_datasource_path, actual_csv_datasource_path, primary_key_column_names, skip_column_names, output_file_path");
		}

		try
		{
			expectedDatasourcePath = tblArgt.getString("expected_csv_datasource_path", 1);
			actualCsvDatasourcePath = tblArgt.getString("actual_csv_datasource_path", 1);
			datasourceColumns = tblArgt.getString("datasource_columns", 1);
			pkColumnNames = tblArgt.getString("primary_key_column_names", 1);
			skipColumnNames = tblArgt.getString("skip_column_names", 1);
			tolerance = tblArgt.getString("tolerance_threshold", 1);

			Logging.info("expectedDatasourcePath=" + expectedDatasourcePath + ",actualDatasourcePath=" + actualCsvDatasourcePath + 
					",pkColumnNames=" + pkColumnNames + ",skipColumnNames=" + skipColumnNames);

			setDsSchema(datasourceColumns);
			Table expectedDataSource = createTable(expectedDatasourcePath);
			
			actualCsvDatasourceFile = new File(actualCsvDatasourcePath);
			if( actualCsvDatasourceFile.isFile() )
			{
				actualDataSource = createTable(actualCsvDatasourcePath);
			}
			else
			{
				actualCsvDatasourcePath = Util.getAbsolutePath(actualCsvDatasourcePath);
				actualCSVFilePath = Util.getLatestFilePath(actualCsvDatasourcePath);
				actualDataSource = createTable( actualCSVFilePath );
			}
			
			setPkColumnNameList(pkColumnNames);
			setSkipColumnNameList(skipColumnNames);
			
			setValidationResultTable();
			
			this.toleranceThreshold = Double.parseDouble(tolerance);

			compareCSVs(expectedDataSource, actualDataSource);
			outputDirectoryPath = Util.getOutputDirectoryPath("Deal Checkpoint Validator");
			String fileName = Util.getFileNameWithTimeStamp( "ComparisonResult","csv");
			String filePath = outputDirectoryPath + File.separator + fileName;
			Logging.debug("Generating CSV output at path:" +  filePath);
			getValidationResultTable().printTableDumpToFile(filePath);
		}
		catch (Throwable throwable)
		{
			Util.printStackTrace(throwable
					);
			String message = "Exception occurred while processing output." + throwable.getMessage();
			Logging.error(message);
			throw new OException(message);

		}
		finally
		{
			if (Table.isTableValid(this.validationResultTable) == 1)
			{
				this.validationResultTable.destroy();
			}
		}
		Logging.info("Completed executing script "+this.getClass().getName());
	}

	/**
	 * This method creates a JVS Table from the input csv file.
	 * @param dspath: path of the csv file having first row as column header.
	 * @return
	 * @throws OException
	 */
	private Table createTable(String dspath) throws OException
	{
        Table csvData = Table.tableNew();
        
        csvData.inputFromCSVFile(dspath);
        Util.updateTableWithColumnNames(csvData);

        return csvData;		
	}
	
	/**
	 * Compares two csv datasources.
	 * @param expectedDSTable
	 * @param actualDSTable
	 * @throws OException
	 */
	private void compareCSVs(Table expectedDSTable, Table actualDSTable) throws OException
	{
		try
		{
			Logging.info("Started analysing CSVs.");
			
			int expectedTableRowNum;
			int actualTableRowNum = -1;
			int numRowsExpectedDS;
			int remaingDataInActualTableRowNumber;
			
			numRowsExpectedDS = expectedDSTable.getNumRows();

			for (expectedTableRowNum = 1; expectedTableRowNum <= numRowsExpectedDS; expectedTableRowNum++)
			{
				actualTableRowNum = getActualTableRowNumberToCompare(expectedDSTable, expectedTableRowNum, actualDSTable, pkColumnNameList);
				
				if ( actualTableRowNum == -1 )
				{
					addValidationResultRow(expectedDSTable, expectedTableRowNum, ValidationResults.TEST_REF_NOT_AVAILABLE_IN_ACTUAL_TABLE.getValue());
				}
				else
				{
					Logging.debug("Found matching row for expected row=" + expectedTableRowNum + " in actual data: Row # " + actualTableRowNum + " out of " + actualDSTable.getNumRows());
					compareRows(expectedDSTable, expectedTableRowNum, actualDSTable, actualTableRowNum);
					actualDSTable.delRow(actualTableRowNum);
				}
			}

			Logging.info("Num of records not present in Expected table:"+ actualDSTable.getNumRows());			

			for (remaingDataInActualTableRowNumber = 1; remaingDataInActualTableRowNumber <= actualDSTable.getNumRows(); remaingDataInActualTableRowNumber++)
			{
				addValidationResultRow(actualDSTable, remaingDataInActualTableRowNumber, ValidationResults.TEST_REF_NOT_AVAILABLE_IN_EXPECTED_TABLE.getValue());
			}
		}
		catch (OException oe)
		{
			Logging.error("Exception in comparing csv files" + oe.getMessage());
			throw new SapTestUtilRuntimeException(oe.getMessage());
		}
		finally
		{
			if (Table.isTableValid(expectedDSTable) == 1)
			{
				expectedDSTable.destroy();
			}
			if (Table.isTableValid(actualDSTable) == 1)
			{
				actualDSTable.destroy();
			}
		}
		Logging.info("Completed analysing CSVs");
	}

	/**
	 * Returns the row number of @param actualResultTable where all the primary-keys values are matching with the @param expectedTableRowNumber of @param expectedResultTable.    
	 * @param expectedResultTable
	 * @param expectedTableRowNumber
	 * @param actualResultTable
	 * @param primaryKeyList
	 * @return
	 * @throws OException
	 */
	private int getActualTableRowNumberToCompare(Table expectedResultTable, int expectedTableRowNumber, Table actualResultTable, List<String> primaryKeyList) throws OException
	{
		boolean isMatching;
		
		int actualTableRowNumber;
		int primaryKeyNumber;
		int returnValue;
		String key;
		/* Get a actualTableRowNumber from actualResultTable to compare with expectedResultTable */
		for (actualTableRowNumber = 1; actualTableRowNumber <= actualResultTable.getNumRows(); actualTableRowNumber++)
		{
			for(  primaryKeyNumber = 0;primaryKeyNumber<primaryKeyList.size(); primaryKeyNumber++ )
			{
				key = primaryKeyList.get(primaryKeyNumber);
				isMatching = 1==isValueMatching(expectedResultTable, key, expectedTableRowNumber, actualResultTable, key, actualTableRowNumber);
				if(!isMatching)
				{
					break;
				}
			}
			if (primaryKeyNumber == primaryKeyList.size() )
			{
				break;
			}
		}
		if (actualTableRowNumber <= actualResultTable.getNumRows())
		{
			returnValue = actualTableRowNumber;
		}
		else {
			returnValue = -1;
		}
		return returnValue;
	}
	
	/**
	 * Compares a row in Expected table with corresponding row in Actual table.
	 * If all column values are matching, then on row is added in Validation result table in status 'Matching'.
	 * Otherwise, one row is added in Validation result table in status 'Not Matching' for each mismatching column value.
	 * @param expectedResultTable
	 * @param expectedTableRowNumber
	 * @param actualResultTable
	 * @param actualTableRowNumber
	 * @throws OException
	 */
	private void compareRows(Table expectedResultTable, int expectedTableRowNumber, Table actualResultTable, int actualTableRowNumber) throws OException
	{
		String columnName;
		boolean isMatching = true;

		for (int columnNumber = 1; columnNumber <= expectedResultTable.getNumCols(); columnNumber++)
		{
			columnName = expectedResultTable.getColName(columnNumber);
			if ( getSkipColumnNameList().contains(columnName) )
			{
				continue;
			}
			int matchResult = isValueMatching(expectedResultTable, columnName, expectedTableRowNumber, actualResultTable, columnName, actualTableRowNumber); 
			if (matchResult != 1)
			{
				isMatching = false;
				String validationResult = ValidationResults.NOT_MATCHING.getValue();
				if(matchResult == 2)
				{
					validationResult = "Matching with tolerance";
				}
				int validationRowNum = addValidationResultRow(expectedResultTable, expectedTableRowNumber, validationResult);
				String expectedValue = getStringValue(expectedResultTable, columnName, expectedTableRowNumber);
				String actualValue = getStringValue(actualResultTable, columnName, actualTableRowNumber);
				getValidationResultTable().setString(ResultTableColumns.COLUMN_NAME.getValue(), validationRowNum, columnName);
				getValidationResultTable().setString(ResultTableColumns.EXPECTED_VALUE.getValue(), validationRowNum, expectedValue );
				getValidationResultTable().setString(ResultTableColumns.ACTUAL_VALUE.getValue(), validationRowNum, actualValue );
			}

		}

		/* If there is no change between these two row validationResultTable will be updated with Matching result */
		if (isMatching)
		{
			addValidationResultRow(expectedResultTable, expectedTableRowNumber, ValidationResults.MATCHING.getValue());
		}
	}

	/**
	 * Adds a new row in ValidationResult table:
	 * (i) Copies the value of primary key columns from @param sourceTable
	 * (ii) validation_result of the new row is @param validationResult  
	 * @param sourceTable
	 * @param sourceRowNum
	 * @param validationResult
	 * @return
	 * @throws OException
	 */
	private int addValidationResultRow(Table sourceTable, int sourceRowNum, String validationResult) throws OException
	{
		int primaryKeyNumber;
		String key;
		int validationTableRowNumber = getValidationResultTable().addRow();
		Logging.debug("Added row#" + validationTableRowNumber + " in status " + validationResult);

		for (primaryKeyNumber = 0; primaryKeyNumber < getPkColumnNameList().size(); primaryKeyNumber++)
		{
			key = getPkColumnNameList().get(primaryKeyNumber);
			String stringValue = getStringValue(sourceTable, key, sourceRowNum);
			Logging.debug("Updating value " + stringValue + " in validation result table for " + key + " column ");
			getValidationResultTable().setString(key, validationTableRowNumber, stringValue);			
		}
		getValidationResultTable().setString("validation_result", validationTableRowNumber, validationResult);		
		return validationTableRowNumber;
	}
	
	/**
	 * Compares a cell value in Expected table with corresponding cell value in Actual table
	 * @param expectedTable
	 * @param expectedTableColumnName
	 * @param expectedTableRowNumber
	 * @param actualTable
	 * @param actualTableColumnName
	 * @param actualTableRowNumber
	 * @return 0 = not matching, 1 = matching, 2 = matching within tolerance
	 * @throws OException
	 */
	private int isValueMatching(Table expectedTable, String expectedTableColumnName, int expectedTableRowNumber, Table actualTable, String actualTableColumnName, int actualTableRowNumber) throws OException
	{
		int isMatching = 0;
		int columnType = expectedTable.getColType(expectedTableColumnName);
		
		/* Check for the column type to decide which getter() needed to be used */
		if (columnType == COL_TYPE_ENUM.COL_INT.toInt())
		{
			int expectedIntValue = expectedTable.getInt(expectedTableColumnName, expectedTableRowNumber);
			int actualIntValue = actualTable.getInt(actualTableColumnName, actualTableRowNumber);
			if (expectedIntValue == actualIntValue)
			{
				isMatching = 1;
			}
		}
		else if (columnType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
		{
			double expectedDoubleValue = expectedTable.getDouble(expectedTableColumnName, expectedTableRowNumber);
			double actualDoubleValue = actualTable.getDouble(actualTableColumnName, actualTableRowNumber);
			
			if (Double.compare(expectedDoubleValue, actualDoubleValue) == 0)
			{
				isMatching = 1;
			} else if(Math.abs(expectedDoubleValue - actualDoubleValue) < this.toleranceThreshold)
			{
				isMatching = 2;
			}
			Logging.debug("Comparing doubles: expectedTableRowNumber="+expectedTableRowNumber+",expectedTableColumnName="+expectedTableColumnName+", expectedDoubleValue="+expectedDoubleValue+",actualDoubleValue="+actualDoubleValue+",isMatching="+isMatching);
		}
		else if (columnType == COL_TYPE_ENUM.COL_STRING.toInt())
		{
			String expectedStringValue = expectedTable.getString(expectedTableColumnName, expectedTableRowNumber);
			String actualStringValue = actualTable.getString(actualTableColumnName, actualTableRowNumber);
			
			Logging.debug("Expected string: " + expectedStringValue);
			Logging.debug("Actual string: " + actualStringValue);
			
			if ((expectedStringValue == null && actualStringValue ==null) 
					|| (expectedStringValue != null & actualStringValue != null &&expectedStringValue.equals(actualStringValue)))
			{
				Logging.debug("Expected and actual strings match");
				isMatching = 1;
			}
		}
		return isMatching;
	}
	
	/**
	 * Return string value of the a cell in @param dataTable
	 * @param dataTable
	 * @param columnName
	 * @param rowNumber
	 * @return
	 * @throws OException
	 */
	private String getStringValue(Table dataTable, String columnName, int rowNumber) throws OException
	{
		int columnType = dataTable.getColType(columnName);
		String stringValue = "";
		
		/* Check for the column type to decide which getter() needed to be used */
		if (columnType == COL_TYPE_ENUM.COL_INT.toInt())
		{
			int intValue = dataTable.getInt(columnName, rowNumber);
			stringValue = String.valueOf(intValue);
		}
		else if (columnType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
		{
			double doubleValue = dataTable.getDouble(columnName, rowNumber);
			stringValue = String.valueOf(doubleValue);
		}
		else if (columnType == COL_TYPE_ENUM.COL_STRING.toInt())
		{
			stringValue = dataTable.getString(columnName, rowNumber);
		}
		
		return stringValue;
	}
}
