package com.matthey.utilities;

/**
 * 
 * Description:
 * This script compares 2 csv.
 * Revision History:
 * 07.05.20  GuptaN02  initial version
 *  
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.matthey.utilities.enums.CompareCSVResult;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;


public class CompareCSVFiles 
{
	String expectedDatasourcePath=null;;
	String actualDatasourcePath=null;
	String datasourceColumns=null;
	String pkColumnNames=null;
	String outputFilePath=null;
	String tolerance=null;
	String columnsToCompare=null;

	Table validationResultTable = com.olf.openjvs.Util.NULL_TABLE;
	List<String> pkColumnNameList;
	List<String> skipColumnNameList;
	List<Column> dsSchema;
	List<Column> columnsToCompareList;
	double toleranceThreshold;
	
	 public CompareCSVFiles(Table tblArgt) throws OException
	 {
		 try{
			 if (tblArgt.getNumRows() == 0)
				 throw new OException("Missing input in the argument table. Expected columns: expected_csv_datasource_path, actual_csv_datasource_path, primary_key_column_names, skip_column_names, output_file_path");

			 Logging.info("Fetching values from Argt table");
			 this.expectedDatasourcePath = tblArgt.getString("old_csv_File", 1);
			 this.actualDatasourcePath = tblArgt.getString("new_csv_File", 1);
			 this.datasourceColumns = tblArgt.getString("datasource_columns", 1);
			 this.pkColumnNames = tblArgt.getString("primary_key", 1);
			 this.outputFilePath = tblArgt.getString("outputFilePath", 1);
			 this.tolerance = tblArgt.getString("tolerance_threshold", 1);
			 this.columnsToCompare=tblArgt.getString("columnsToCompare", 1);
			 this.toleranceThreshold = Double.parseDouble(tolerance);
			 
			String message= validateParameters();
			if(!message.isEmpty())
			{
				String errorMessage="Following issue took  place in intializing parameters::";
				errorMessage+=message;
				Logging.error(errorMessage);
				throw new OException(errorMessage);
			}
			
			 Logging.info("Parameters are as follow:\n expectedDatasourcePath=" + expectedDatasourcePath + "\n actualDatasourcePath=" + actualDatasourcePath + 
					 "\n pkColumnNames=" + pkColumnNames + "\n outputFilePath=" + outputFilePath + "\n columnsToCompare=" +columnsToCompare);
			 

			 Logging.info("Parameters fetched successfully from argt table");
		 }
		 catch(Exception e)
		 {
			Logging.error("Error occured while fetching parameters, with following exception" + e.getMessage());
			ExceptionUtil.logException(e, 0);
			throw new OException("Error occured while fetching parameters, with following exception" + e.getMessage());
		 }

	 }
	
	 /**
	 * @return
	 * @throws OException
	 * Validate Parameters
	 */
	private String validateParameters() throws OException {
		 String errorMessage="";
		 try{
			 File expectedFile = new File(expectedDatasourcePath);
			 File actualFile = new File(actualDatasourcePath);
			 File outputFile= new File(outputFilePath);
			 
			 if(!expectedFile.exists())
				 errorMessage+="File does not exist at expectedFilePath \n"+expectedDatasourcePath;
			 if(!actualFile.exists())
				 errorMessage+="File does not exist at actualFilePath \n"+actualDatasourcePath;
			 if(!outputFile.exists())
				 errorMessage+="File does not exist at outputFilePath \n"+outputFilePath;
			 
			 if(expectedDatasourcePath.isEmpty() || expectedDatasourcePath==null)
				 errorMessage+="Parameter not defined for expectedFilePath \n";
			 if(actualDatasourcePath.isEmpty() || actualDatasourcePath==null)
				 errorMessage+="Parameter not defined for actualFilePath \n";
			 if(outputFilePath.isEmpty() || outputFilePath==null)
				 errorMessage+="Parameter not defined for outputFilePath \n";
			 if(datasourceColumns.isEmpty() || datasourceColumns==null)
				 errorMessage+="Parameter not defined for datasourceColumns \n";
			 if(pkColumnNames.isEmpty() || pkColumnNames==null)
				 errorMessage+="Parameter not defined for pkColumnNames \n";
			 if(columnsToCompare.isEmpty() || columnsToCompare==null)
				 errorMessage+="Parameter not defined for columnsToCompare \n";
			 if(tolerance.isEmpty() || tolerance==null)
				 errorMessage+="Parameter not defined for tolerance \n";
			
			 
		 }
		 catch(Exception e)
		 {
			 Logging.error("Error occured while validating parameters" + e.getMessage());
			 ExceptionUtil.logException(e, 0);
			 throw new OException("Error occured while validating parameters" + e.getMessage());

		 }
		 return errorMessage;

	 }


		 public Table getValidationResultTable() {
			 return validationResultTable;
		 }

		 public List<String> getPkColumnNameList() {
			 return pkColumnNameList;
		 }


		 /**
		  * Prepare Structure for Validation Table
		 * @param columnsToCompare
		 * @throws OException
		 */
		public void setValidationResultTable(String columnsToCompare) throws OException {
			 try{
				 Logging.info("Preparing Structure for Validation Result Table");
				 this.columnsToCompareList = new ArrayList<>();
				 this.validationResultTable = Table.tableNew();
				 for (int primaryKeyNumber = 0; primaryKeyNumber < getPkColumnNameList().size(); primaryKeyNumber++)
				 {
					 validationResultTable.addCol(getPkColumnNameList().get(primaryKeyNumber), COL_TYPE_ENUM.COL_STRING);
				 }
				 List<String> dsColumnDetailList = Arrays.asList(columnsToCompare.split(";"));
				 prepareDynamicTable(dsColumnDetailList,TableCateogry.VALIDATION);
				 for(Column dsColumn: getColumnComapreSchema())
				 {
					 validationResultTable.addCol(dsColumn.getName(), dsColumn.getType());
				 }
				 validationResultTable.addCol("validation_result", COL_TYPE_ENUM.COL_STRING);
				 Logging.info("Prepared Structure for Validation Result Table");
			 }
			 catch(Exception e)
			 {
				 Logging.error("Error occured while creating validation table, with following exception" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error occured while creating validation table with following exception" + e.getMessage());
			 }


		 }

		 /**
		  * Save a list from semicolon separated string of Primary Key columns
		 * @param pkColumnNames
		 * @throws OException
		 */
		public void setPkColumnNameList(String pkColumnNames) throws OException {
			 try{
				 Logging.info("Setting primary key column value to class variable");
				 this.pkColumnNameList = Arrays.asList(pkColumnNames.split(";"));
			 }
			 catch(Exception e)
			 {
				 Logging.error("Error took place while Setting primary key column value to class variable" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error took place while Setting primary key column value to class variable" + e.getMessage());
			 }
		 }


		 public List<Column> getDsSchema() {
			 return dsSchema;
		 }

		 public List<Column> getColumnComapreSchema()
		 {
			 return columnsToCompareList;
		 }

		 /**
		  * Creates dataset schema from the data source column
		  * @param datasourceColumns
		  * @throws OException
		  */
		 public void setDsSchema() throws OException {
			 try{
				 Logging.info("Preparing schema from the datasource columns");
				 this.dsSchema = new ArrayList<>();
				 List<String> dsColumnDetailList = Arrays.asList(datasourceColumns.split(";"));
				 prepareDynamicTable(dsColumnDetailList,TableCateogry.DATASOURCE);
				 Logging.info("Preparing schema from the datasource columns");
			 }
			 catch(Exception e)
			 {
				 Logging.error("Failed while preparing dataset schema"+e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Failed while preparing dataset schema"+e.getMessage());
			 }
		 }

		 /**
		  * Prepare a list of column objects used for creating dynamic table structure for DataSource and Validation Table
		  *
		  * @param dsColumnDetailList the ds column detail list
		  * @param type the type
		  * @throws OException 
		  */
		 public void prepareDynamicTable(List<String> dsColumnDetailList,TableCateogry type) throws OException
		 {
			 try{
				 for(int index=0; index < dsColumnDetailList.size(); index++)
				 {
					 String dsColumnDetailString = dsColumnDetailList.get(index);
					 List<String> dsColumnDetail = Arrays.asList(dsColumnDetailString.split(":"));
					 if(dsColumnDetail.size() != 2)
					 {
							 throw new OException("Invalid column definition in the input variable datasource_columns:'" 
									 + dsColumnDetailString + "'. Valid format is 'column-name:data-type'.");	
					 }

					 if(type==TableCateogry.DATASOURCE)
					 {
						 Column column = prepareColumn(dsColumnDetail,"");

						 this.dsSchema.add(column);}
					 else{
						 Column columnOld = prepareColumn(dsColumnDetail,"_old");
						 Column columnNew = prepareColumn(dsColumnDetail,"_new");
						 this.columnsToCompareList.add(columnOld);
						 this.columnsToCompareList.add(columnNew);

					 }
				 }
			 }
			 catch(Exception e)
			 {
				 Logging.error("Failed while preparing dataset schema"+e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Failed while preparing dataset schema"+e.getMessage());
			 }

		 }

		 /**
		  * Prepare column object used by prepareDynamicTable
		 * @param dsColumnDetail
		 * @param suffix
		 * @return
		 * @throws OException
		 */
		private Column prepareColumn(List<String> dsColumnDetail, String suffix) throws OException
		 {
			 Column column=null;
			 try{
				 column = new Column();
				 column.setName(dsColumnDetail.get(0)+suffix);

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
			 }
			 catch(Exception e)
			 {
				 Logging.error("Failed while setting column name and column type"+e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Failed while setting column name and column type"+e.getMessage());

			 }
			 return column;
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

		 public Table  compareCSV() throws OException
		 {
			 try {
				 Logging.info("Started executing script "+this.getClass().getName());
				 setDsSchema();
				 Table expectedDataSource = createTable(expectedDatasourcePath);
				 Table actualDataSource = createTable(actualDatasourcePath);

				 setPkColumnNameList(pkColumnNames);
				 setValidationResultTable(columnsToCompare);

				 compareCSVs(expectedDataSource, actualDataSource);
				 getValidationResultTable().printTableDumpToFile(outputFilePath);
				 return validationResultTable;
			 }
			 catch (Exception e)
			 {
				 String message = "Exception occurred while processing output." + e.getMessage();
				 Logging.error(message);
				 throw new OException(message);
			 }
		 }



		 /**
		  * This method creates a JVS Table from the input csv file.
		  * @param dspath: path of the csv file having first row as column header.
		  * @return
		  * @throws OException
		  */
		 private Table createTable(String dspath) throws OException
		 {
			 Table csvData=Util.NULL_TABLE;

			 try{
				 Logging.info("Preparing Table for file at: "+dspath);
				 csvData = Table.tableNew();
				 for(Column dsColumn: getDsSchema())
				 {
					 csvData.addCol(dsColumn.getName(), dsColumn.getType());
				 }
				 if(Table.isTableValid(csvData)!=1)
					 throw new OException("Could not create table structure");
				 Logging.info("Table prepared successfully");

				 Logging.info("Reading csv from:"+dspath);
				 csvData.inputFromCSVFile(dspath);
				 csvData.delRow(1);
				 if(csvData.getNumRows()<1)
					 throw new OException("No data found in csv files at "+dspath);
				 Logging.info("Successfully read csv:");


				 return csvData;	
			 }
			 catch(Exception e)
			 {
				 Logging.error("Failed while preparing dataset schema for file at "+dspath+" with error: " +e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Failed while preparing dataset schema for file at "+dspath+" with error: " +e.getMessage());
			 }
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
				 int numRowsactualDS;
				 int remaingDataInActualTableRowNumber;

				 numRowsExpectedDS = expectedDSTable.getNumRows();
				 numRowsactualDS = actualDSTable.getNumRows();
				 Logging.info("Number of rows in Expected Table: "+numRowsExpectedDS+" and number of rows in actual table: "+numRowsactualDS);

				 for (expectedTableRowNum = 1; expectedTableRowNum <= numRowsExpectedDS; expectedTableRowNum++)
				 {
					 actualTableRowNum = getActualTableRowNumberToCompare(expectedDSTable, expectedTableRowNum, actualDSTable);

					 if ( actualTableRowNum == -1 )
					 {
						 addValidationResultRow(expectedDSTable, expectedTableRowNum,CompareCSVResult.ORPHAN_IN_ACTUAL_TABLE.getValue() ,actualDSTable,actualTableRowNum);
					 }
					 else
					 {
						 Logging.debug("Found matching row for expected row=" + expectedTableRowNum + " in actual data: Row # " + actualTableRowNum + " out of " + actualDSTable.getNumRows());
						 CompareCSVResult validationResult=compareRows(expectedDSTable, expectedTableRowNum, actualDSTable, actualTableRowNum);
						 addValidationResultRow(expectedDSTable, expectedTableRowNum, validationResult.getValue(),actualDSTable,actualTableRowNum);
						 actualDSTable.delRow(actualTableRowNum);
					 }
				 }

				 Logging.info("Num of records not present in Expected table:"+ actualDSTable.getNumRows());			

				 for (remaingDataInActualTableRowNumber = 1; remaingDataInActualTableRowNumber <= actualDSTable.getNumRows(); remaingDataInActualTableRowNumber++)
				 {
					 addValidationResultRow(expectedDSTable, -1,CompareCSVResult.ORPHAN_IN_EXPECTED_TABLE.getValue(),actualDSTable,remaingDataInActualTableRowNumber);
				 }
			 }
			 catch (Exception e)
			 {
				 Logging.error("Exception in comparing csv files" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Exception in comparing csv files" + e.getMessage());
			 }
			 finally
			 {
				 if(expectedDSTable!=null)
					 expectedDSTable.destroy();

				 if (actualDSTable!=null)
					 actualDSTable.destroy();
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
		 private int getActualTableRowNumberToCompare(Table expectedResultTable, int expectedTableRowNumber, Table actualResultTable) throws OException
		 {
			 CompareCSVResult isMatching;
			 int actualTableRowNumber;
			 int primaryKeyNumber;
			 int returnValue=-1;
			 String key;
			 try{
				 /* Get a actualTableRowNumber from actualResultTable to compare with expectedResultTable */
				 Logging.debug("Looking for row number: "+expectedTableRowNumber+" from expected table in actual table");
				 int pkSize=pkColumnNameList.size();
				 for (actualTableRowNumber = 1; actualTableRowNumber <= actualResultTable.getNumRows(); actualTableRowNumber++)
				 {
					 for(  primaryKeyNumber = 0;primaryKeyNumber<pkSize; primaryKeyNumber++ )
					 {
						 key = pkColumnNameList.get(primaryKeyNumber);
						 isMatching=isValueMatching(expectedResultTable, key, expectedTableRowNumber, actualResultTable, key, actualTableRowNumber);
						 if(isMatching!=CompareCSVResult.MATCHING)
						 {
							 break;
						 }
					 }
					 if (primaryKeyNumber == pkSize )
					 {
						 break;
					 }
				 }
				 if (actualTableRowNumber <= actualResultTable.getNumRows())
				 {
					 Logging.debug("Found primary key from expected table at row number "+expectedTableRowNumber+" in actual table at row number: "+actualTableRowNumber);
					 returnValue = actualTableRowNumber;
				 }
				 else {
					 Logging.debug("Could not found primary key from expected table at row number "+expectedTableRowNumber+" in actual table");
					 returnValue = -1;
				 }

			 }
			 catch(Exception e)
			 {
				 Logging.error("Error found while comparing  primary key in actual table, with following exception" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error found while comparing  primary key in actual table, with following exception" + e.getMessage());

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
		 private CompareCSVResult compareRows(Table expectedResultTable, int expectedTableRowNumber, Table actualResultTable, int actualTableRowNumber) throws OException
		 {
			 CompareCSVResult rowMatchResult = null;
			 try{
				 rowMatchResult = CompareCSVResult.MATCHING;

				 HashSet<String> distinctColumnsToCompare= getdistinctColumns();

				 for (String columnName:distinctColumnsToCompare)
				 {
					 CompareCSVResult columnMatchResult = isValueMatching(expectedResultTable, columnName, expectedTableRowNumber, actualResultTable, columnName, actualTableRowNumber);
					 if(CompareCSVResult.NOT_MATCHING.equals(columnMatchResult))
					 {
						 rowMatchResult=CompareCSVResult.NOT_MATCHING;
					 }
					 else if (!CompareCSVResult.NOT_MATCHING.equals(rowMatchResult) && CompareCSVResult.MATCHING_WITH_TOLERANCE.equals(columnMatchResult))
					 {
						 rowMatchResult=CompareCSVResult.MATCHING_WITH_TOLERANCE;
					 }
					 else if (CompareCSVResult.MATCHING.equals(rowMatchResult))
					 {
						 rowMatchResult=columnMatchResult;
					 }
				 }
			 }
			 catch(Exception e)
			 {
				 Logging.error("Error found while comparing rows for the primary key existing in both tables, with following exception" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error found while comparing rows for the primary key existing in both tables, with following exception" + e.getMessage());

			 }
			 return rowMatchResult;
		 }

		 protected HashSet<String> getdistinctColumns() throws OException {
			 HashSet<String> comaprisonColumnSet= new HashSet<String>();
			 try{
				 String ColumnName="";
				 for(int i=0;i<columnsToCompareList.size();i++)
				 {
					 String columnNameInit= columnsToCompareList.get(i).name;
					 if(columnNameInit.contains("_old"))
						 ColumnName=columnNameInit.replace("_old", "");
					 else
						 ColumnName=	columnNameInit.replace("_new", "");
					 comaprisonColumnSet.add(ColumnName);
				 }
			 }
			 catch(Exception e)
			 {
				 Logging.error("Error found while getting distinct columns from columnstoCompare, with following exception" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error found while getting distinct columns from columnstoCompare, with following exception" + e.getMessage());
			 }

			 return comaprisonColumnSet;
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
		 protected void addValidationResultRow(Table sourceTable, int sourceRowNum, String validationResult, Table targetTable,int targetRowNum) throws OException
		 {
			 try{
				 String columnnName="";
				 int validationColumnNumber;
				 String key;
				 int validationTableRowNumber = getValidationResultTable().addRow();
				 Logging.debug("Added row#" + validationTableRowNumber + " in status " + validationResult);

				 for (validationColumnNumber = 1; validationColumnNumber <= getValidationResultTable().getNumCols(); validationColumnNumber++)
				 {
					 if(validationColumnNumber<=getPkColumnNameList().size())
					 {
						 key = getPkColumnNameList().get(validationColumnNumber-1);
						 String stringValue=(validationResult.equalsIgnoreCase(CompareCSVResult.ORPHAN_IN_EXPECTED_TABLE.getValue()))? getStringValue(targetTable, key, targetRowNum):getStringValue(sourceTable, key, sourceRowNum);	 
						 Logging.debug("Updating value " + stringValue + " in validation result table for " + key + " column ");
						 getValidationResultTable().setString(key, validationTableRowNumber, stringValue);	
					 }
					 else{
						 columnnName=getValidationResultTable().getColName(validationColumnNumber);

						 if(columnnName.contains("_old") && !validationResult.contains(CompareCSVResult.ORPHAN_IN_EXPECTED_TABLE.getValue()))
						 {
							 getValidationResultTable().setString(columnnName, validationTableRowNumber,  getStringValue(sourceTable, columnnName.replace("_old", ""), sourceRowNum));
						 }
						 if(columnnName.contains("_new") && !validationResult.contains(CompareCSVResult.ORPHAN_IN_ACTUAL_TABLE.getValue()))
						 {
							 getValidationResultTable().setString(columnnName, validationTableRowNumber,  getStringValue(targetTable, columnnName.replace("_new", ""), targetRowNum));
						 }
					 }
				 }

				 getValidationResultTable().setString("validation_result", validationTableRowNumber, validationResult);	
			 }
			 catch(Exception e)
			 {
				 String message = "Exception occurred while populating validation output." + e.getMessage();
				 Logging.error(message);
				 throw new OException(message);

			 }

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
		 private CompareCSVResult isValueMatching(Table expectedTable, String expectedTableColumnName, int expectedTableRowNumber, Table actualTable, String actualTableColumnName, int actualTableRowNumber) throws OException
		 {
			 CompareCSVResult isMatching = CompareCSVResult.NOT_MATCHING;
			 try{
				 int columnType = expectedTable.getColType(expectedTableColumnName);

				 /* Check for the column type to decide which getter() needed to be used */
				 if (columnType == COL_TYPE_ENUM.COL_INT.toInt())
				 {
					 int expectedIntValue = expectedTable.getInt(expectedTableColumnName, expectedTableRowNumber);
					 int actualIntValue = actualTable.getInt(actualTableColumnName, actualTableRowNumber);
					 Logging.debug("Looking for value: "+expectedIntValue);
					 if (expectedIntValue == actualIntValue)
					 {
						 isMatching = CompareCSVResult.MATCHING;
						 Logging.debug("Found value: "+actualIntValue+" at row number "+actualTableRowNumber+" in actual table");
					 }
				 }
				 else if (columnType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
				 {
					 double expectedDoubleValue = expectedTable.getDouble(expectedTableColumnName, expectedTableRowNumber);
					 double actualDoubleValue = actualTable.getDouble(actualTableColumnName, actualTableRowNumber);
					 Logging.debug("Looking for value: "+expectedDoubleValue);
					 if (Double.compare(expectedDoubleValue, actualDoubleValue) == 0)
					 {
						 isMatching = CompareCSVResult.MATCHING;
						 Logging.debug("Found value: "+actualDoubleValue+" at row number "+actualTableRowNumber+" in actual table");
					 } else if(Math.abs(expectedDoubleValue - actualDoubleValue) < this.toleranceThreshold)
					 {
						 isMatching = CompareCSVResult.MATCHING_WITH_TOLERANCE;
						 Logging.debug("Found value: "+actualDoubleValue+" at row number "+actualTableRowNumber+" in actual table");
					 }
					 Logging.debug("Comparing doubles: expectedTableRowNumber="+expectedTableRowNumber+",expectedTableColumnName="+expectedTableColumnName+", expectedDoubleValue="+expectedDoubleValue+",actualDoubleValue="+actualDoubleValue+",isMatching="+isMatching);
				 }
				 else if (columnType == COL_TYPE_ENUM.COL_STRING.toInt())
				 {
					 String expectedStringValue = expectedTable.getString(expectedTableColumnName, expectedTableRowNumber);
					 String actualStringValue = actualTable.getString(actualTableColumnName, actualTableRowNumber);
					 Logging.debug("Looking for value: "+expectedStringValue);
					 if (expectedStringValue.equals(actualStringValue))
					 {
						 isMatching = CompareCSVResult.MATCHING;
						 Logging.debug("Found value: "+actualStringValue+" at row number "+actualTableRowNumber+" in actual table");
					 }
				 }
			 }
			 catch(Exception e)
			 {
				 Logging.error("Error found while comparing value, with following exception" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error found  while comparing value, with following exception" + e.getMessage());
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
		 public String getStringValue(Table dataTable, String columnName, int rowNumber) throws OException
		 {
			 String stringValue = "";
			 try{

				 Logging.debug("Fetching value for "+columnName+" from table.");
				 int columnType = dataTable.getColType(columnName);


				 /* Check for the column type to decide which getter() needed to be used */
				 if (columnType == COL_TYPE_ENUM.COL_INT.toInt())
				 {
					 int intValue = dataTable.getInt(columnName, rowNumber);
					 stringValue = String.valueOf(intValue);
					 if(!"".equalsIgnoreCase(stringValue))
						 Logging.debug("Fetched value for "+columnName+" from table successfully. Value is "+stringValue);
					 else
						 Logging.debug("Could not fetch value for "+columnName);


				 }
				 else if (columnType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
				 {
					 double doubleValue = dataTable.getDouble(columnName, rowNumber);
					 stringValue = String.valueOf(doubleValue);
					 if(!"".equalsIgnoreCase(stringValue))
						 Logging.debug("Fetched value for "+columnName+" from table successfully. Value is "+stringValue);
					 else
						 Logging.debug("Could not fetch value for "+columnName);
				 }
				 else if (columnType == COL_TYPE_ENUM.COL_STRING.toInt())
				 {
					 stringValue = dataTable.getString(columnName, rowNumber);
					 if(!"".equalsIgnoreCase(stringValue))
						 Logging.debug("Fetched value for "+columnName+" from table successfully. Value is "+stringValue);
					 else
						 Logging.debug("Could not fetch value for "+columnName);
				 }


			 }
			 catch(Exception e)
			 {
				 Logging.error("Error found in fetching value, with following exception" + e.getMessage());
				 ExceptionUtil.logException(e, 0);
				 throw new OException("Error found in fetching value, with following exception" + e.getMessage());

			 }
			 return stringValue;
		 }
		 

		 /**
		 * This enum is used to differentiate table while creating its structure dynamically.
		 *
		 */
		public enum TableCateogry {
			 DATASOURCE("Data Source"),
			 VALIDATION("Validation");

			 private String value="";

			 public String getValue() {
				 return value;
			 }

			 private TableCateogry(String value)
			 {
				 this.value=value;
			 }

}
}
