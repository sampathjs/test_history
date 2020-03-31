/*
 * Description:
 * 
 * Stock Split by Form Report is based on Metals balance sheet project.
 * prepareMap method would basically ue StockPosition class objects to create following data structure:
 * 
 * StockPosition class is used to create a data structure using returnT as input and output a map like below:
 * Key - balance Line name (reading from user_const_repo
 * Value - List of StockPosition objects
 * each object has two string values and one list (list of metals, Key -Metal Name, value - Metal value(double type))
 * ------------------------------------------------------------------------------------------------------------------------------
 * Key - BalanceLine|	Value – List of StockPosition objects
 * ------------------------------------------------------------------------------------------------------------------------------
	L140 Stock UK	|	L140 Stock UK					|	L140 Stock UK					|	L140 Stock UK
					|	Sponge							|	Grain							|	Ingot
					|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag
					|	1.2 2.7 3.4	0.0 7.5 6.5 6.2 0.0	|	7.5 6.5 6.2 0.0 1.2 2.7 3.4	0.0 |	6.5 6.2 0.0 1.2 2.7 3.4 2.1 0.0
	-------------------------------------------------------------------------------------------------------------------------------		
	L145 Stock US	|	L145 Stock US					|	L145 Stock US					|	L145 Stock US
					|	Sponge							|	Grain							|	Ingot
					|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag
					|	1.2 2.7 3.4	0.0 7.5 6.5 6.2 0.0	|	7.5 6.5 6.2 0.0 1.2 2.7 3.4	0.0 |	6.5 6.2 0.0 1.2 2.7 3.4 2.1 0.0
	-------------------------------------------------------------------------------------------------------------------------------					
 * And so on.....
 * 
 * History:
 * 2020-04-16	V1.0	Jyotsna	- Initial version, Developed under SR 323601
 * 
 */
package com.jm.rbreports.BalanceSheet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.jm.rbreports.BalanceSheet.StockPosition;
import com.matthey.utilities.enums.EndurAccountInfoField;

public class StockSplitByForm extends RegionalLiquidityReport{
	
	private static final String SUBCONTEXT = "Stock Split by Form";
	
//overriding getter for subcontext in user_const_repo
	@Override
	protected String getSubcontext() {
		return SUBCONTEXT;
	}
	
	/*
	 * Method createStandardReport
	 * Create report showing aggregated balances by balance line/metal group
	 * @param rptDate: reporting date outData: returnT
	 * @throws OException 
	 */
	@Override
	protected void createStandardReport(Table outData, int rptDate) throws OException 
	{

		PluginLog.info ("Initializing returnT (outData) table...\n");
		initialiseContainer(outData);
		Table balances = runReport(ACCOUNT_BALANCE_RPT_NAME, rptDate);
		Table balanceDesc = Util.NULL_TABLE;
		try{
			PluginLog.info ("Retrieving region list from const repo..\n");
			//get region list from const repo
			HashSet<String> regionSet = new HashSet<>();
			getRegionList(regionSet);
			PluginLog.info ("Run getBalanceDesc() for each region..\n" );
			//build balancedesc table for each region     
			for(String region: regionSet) {
				Table balanceDescRegion = getBalanceDesc(region);
				try{
					if(Table.isTableValid(balanceDesc) != 1) {
						balanceDesc = balanceDescRegion.cloneTable();
					}
					balanceDescRegion.copyRowAddAll(balanceDesc);
					PluginLog.info ("Number of balance lines for " + region + "region" + balanceDesc.getNumRows());
				}
				finally{
					Utils.removeTable(balanceDescRegion);
				}	
			}
			getAccountInfo(balances, balanceDesc,regionSet);
			getFormType(balances);
			transposeData(outData, balances);
			checkBalanceLines(outData, balanceDesc, false);
			applyFormulas(outData);
			formatColumns(outData);
			Map<String,List<StockPosition>> balancelineMap = new HashMap<String,List<StockPosition>>();
			balancelineMap = prepareMap(outData,balancelineMap);
			pivotData(outData,balancelineMap);
		}
		finally{
			Utils.removeTable(balanceDesc);
			Utils.removeTable(balances);
		}
	}
	/*
	 * Method initialiseContainer :Overriding this method as we need metal form type too in outData(returnT) table
	 * Creates report structure.
	 * @param outData : empty table
	 * @throws OException 
	 */
	@Override
	protected void initialiseContainer(Table outData) throws OException
	{
	super.initialiseContainer(outData);
	outData.addCol("form_type", COL_TYPE_ENUM.COL_STRING, "Form");
	
	//delete extra (budget and forecast) columns from returnT
	PluginLog.info("deleting budget and forecast columns from returnT..\n");
	
	for( int colCount = outData.getNumCols(); colCount>=1; --colCount ){
		String colName = outData.getColName(colCount);
		if(colName.contains("Budget") || colName.contains("Forecast")){
	outData.delCol(colCount);
		}
	
	}}
	/*
	 * Method getFormType
	 * To filter get metal form type for accounts
	 * @param: balances table - output of Account balance retrieval
	 */
	
	protected void getFormType(Table balances) throws OException{
		Table infoType = Table.tableNew();
		int qId = 0;

		try{
			ConstRepository cr = getConstRepo();

			String accInfoName = cr.getStringValue("Account Info Type Name");

			EndurAccountInfoField accInfoType = EndurAccountInfoField.fromString(accInfoName);

			qId = Query.tableQueryInsert(balances, "account_id");
			String sql = "SELECT ai.account_id, ai.info_value AS form_type "
						+ "FROM account_info ai " //correct format of sql
						+ "INNER JOIN query_result qr "
						+ "		ON ai.account_id = qr.query_result "
						+ "	WHERE qr.unique_id = " + qId
						+ " 	AND ai.info_type_id = "
						+ accInfoType.toInt();

			infoType = runSql(sql);
			balances.select(infoType, "form_type", "account_id EQ $account_id");
		}
		catch (Exception oe) {
			PluginLog.error("\n Error while running getFormType method....");
			throw new OException(oe); 
		}finally{
			infoType.destroy();
			Query.clear(qId);
		}
	}
	/*
	 * Method transposeData
	 * Transpose data for report layout.
	 * @param outData: report output
	 * @param balances : balance sheet data
	 * @throws OException 
	 */
	@Override
	protected void transposeData(Table outData, Table balances) throws OException
	{
		int numRows = balances.getNumRows();
		
		
		for (int balIdx = 1; balIdx <= numRows; balIdx++)
		{
			String balLine = balances.getString("balance_line", balIdx);
			int acctId = balances.getInt("account_id", balIdx);
			int outIdx = findBalanceAccount(outData, balLine, acctId);
			if (outIdx < 1) 
			{
				outData.addRow();
				outIdx = outData.getNumRows();
				outData.setInt("account_id", outIdx, balances.getInt("account_id", balIdx));
				outData.setString("account_name", outIdx, balances.getString("account_name", balIdx));
				outData.setInt("balance_line_id", outIdx, balances.getInt("balance_line_id", balIdx));
				outData.setString("balance_line", outIdx, balances.getString("balance_line", balIdx));
				outData.setString("balance_desc", outIdx, balances.getString("balance_desc", balIdx));
				outData.setInt("display_order", outIdx, balances.getInt("display_order", balIdx));
				outData.setString("display_in_drilldown", outIdx, balances.getString("display_in_drilldown", balIdx));
				outData.setString("formula", outIdx, balances.getString("formula", balIdx));
				outData.setString("form_type", outIdx, balances.getString("form_type", balIdx));//overridden this method to include form type
			}
	
			int metalId = balances.getInt("currency_id", balIdx);
			String outCol = metalId + "_" + COLHEADER_RB_ACTUAL;
			outData.setDouble(outCol, outIdx, outData.getDouble(outCol, outIdx) + balances.getDouble("balance", balIdx));
		}
	}
	
	/*
	 * Method formatColumns
	 * To filter out records having null value in balance_desc column in outData
	 * @param outData: returnT
	 */
		
		protected void formatColumns(Table outData)  throws OException{
			
		PluginLog.info("Filtering out balance line items with blank balance line from outData table..");
		for (int count = outData.getNumRows();count>=1 ;count--){
			String balanceDesc = outData.getString("balance_desc", count);
			if(balanceDesc.isEmpty())
			{
				outData.delRow(count);
			}
			
		}
		outData.sortCol("form_type");
		
		
		PluginLog.info("Removing Actual from column names...");
		//get metal column IDs and prepare a set
		HashSet<String> colIdSet = getColumnIDs(outData);
		
		
		for (String metalColId:colIdSet){
			String colTitle = outData.getColTitle(metalColId);
			outData.setColTitle(metalColId, colTitle.replace(" Actual", ""));
		}
		
		}
		/*
		 * Method getColumnIDs
		 * outData table name for metal columns contains 'Actual' string, extracting column IDs based on that
		 * @param outData: returnT
		 * @return HashSet<String> colIdSet
		 */
		private HashSet<String> getColumnIDs(Table outData) throws OException {
			HashSet<String> colSet = new HashSet<>();
			
			PluginLog.info("Iterating through all the columns to retrieve metal columns ID list ...\n");
			for (int count = 1;count<=outData.getNumCols();count++){
				String colName = outData.getColName(count);
				if(colName.contains("Actual")){
					colSet.add(colName);
				}
				
			}
			return colSet;
		}
		
		/*
		 * Method prepareMap
		 * To prepare map of data in outData table such that key is balance line and value is list of objects of StockPosition class.
		 * This Data structure can be used to pivot outData table data in any layout
		 * @param outData: returnT
		 * @return Map<String, List<StockPosition>>
		 */
		
		protected Map<String, List<StockPosition>> prepareMap(Table outData,Map<String, List<StockPosition>> balancelineMap)throws OException{

			PluginLog.info("Preparing data structure from outData table....");

			//Iterate for each row in outData table and create a map of balanceline names as key and list of StockPosition class objects

			int numRows = outData.getNumRows();

			PluginLog.info("Number of rows in outData ..." + numRows);

			List<StockPosition> stockPositionList;
			try{
				HashSet<String> colSet = getColumnIDs(outData);
				for(int row = 1; row<=numRows; row++){

					String balanceLine = outData.getString("balance_desc", row);
					String formType = outData.getString("form_type", row);

					//check if the list of objects 'stockPositionList' contains any value
					stockPositionList = balancelineMap.get(balanceLine);
					if(stockPositionList == null) {
						stockPositionList = new ArrayList<>();
						//Add the new balance line into the Map
						balancelineMap.put(balanceLine, stockPositionList);

					}

					StockPosition selectedStockPosition = null;

					//for each object in stockPositionList check if the form type already exists
					for(StockPosition stockPosition : stockPositionList) {
						if(stockPosition.getFormType().equalsIgnoreCase(formType)) {
							selectedStockPosition = stockPosition;
							break;
						}
					}

					//if the formType does not exist in the list of objects stockPositionList, add it to the list
					if(selectedStockPosition == null) {
						selectedStockPosition = new StockPosition(balanceLine,formType);
						stockPositionList.add(selectedStockPosition);
					}

					

					for(String colName:colSet){
						double position = outData.getDouble(colName, row);
						selectedStockPosition.addMetalPosition(colName, position);	
					}

				}
			}catch (OException oe) {
				PluginLog.info("Map NOT prepared successfully...\n");
				PluginLog.error(oe.getMessage());
				throw oe;

			}
			finally{
				PluginLog.info("Map prepared successfully...\n");
			}
			return balancelineMap;
		}

		private void pivotData(Table outData,Map<String, List<StockPosition>> balancelineMap) throws OException {

			Table pivotTable = Util.NULL_TABLE;
			pivotTable = Table.tableNew();

			PluginLog.info("Creating schema of pivotTable...\n");
			addColumns(pivotTable);

			Set<String> balanceLines = balancelineMap.keySet();

			PluginLog.info("Balance line list for stock position: " + balanceLines + "\n");
			List<StockPosition> stockPositionList;
			try{
				HashSet<Integer> colIdSet = new HashSet<>();

				PluginLog.info("Create a set of metal columns...\n");

				for (int count = 1;count<= pivotTable.getNumCols();count++){

					int colType = pivotTable.getColType(count);
					if (COL_TYPE_ENUM.COL_DOUBLE.toInt() == colType) {
						colIdSet.add(count);
					}

				}
				PluginLog.info("Metal column IDs in pivotData: " + colIdSet + "\n");
				PluginLog.info("Iterate on balancelines set..\n" );

				for(String balanceLine : balanceLines) {

					stockPositionList = balancelineMap.get(balanceLine);
					int aggregatedRow = pivotTable.addRow();

					PluginLog.info("set up aggreated row for Balance line: " + balanceLine + " in pivotTable..");

					pivotTable.setString("balance_desc", aggregatedRow, balanceLine);

					//creating a new map for calculating total aggregated values per metal per form

					HashMap<String,Double> aggregatedMetalPosition = new HashMap<String,Double>();

					
					for(StockPosition stockPosition : stockPositionList) {
						int row = pivotTable.addRow();

						//set form_type value in balance_desc column
						String form = "        " + stockPosition.getFormType();
						pivotTable.setString("balance_desc", row, form);

						//setting up metal columns values in pivotTable

						double position;
						String colName;

						//iterate for all columns
						for(int colId:colIdSet){
							colName = pivotTable.getColName(colId);
							position = stockPosition.getMetalPosition(colName);
							pivotTable.setDouble(colName, row, position);

							//perform aggregation
							double aggregatedPosition;
							if(aggregatedMetalPosition.containsKey(colName)){
								aggregatedPosition = aggregatedMetalPosition.get(colName) + position ;
								aggregatedMetalPosition.put(colName, aggregatedPosition);
							}else{
								aggregatedMetalPosition.put(colName, position);
							}				
						}

					}
					// AddaggregatedMetalPosition to aggregatedRow
					for(int colId:colIdSet){
						String colName = pivotTable.getColName(colId);
						pivotTable.setDouble(colName, aggregatedRow, aggregatedMetalPosition.get(colName));
					}
				}
				
				PluginLog.info("\npivotData table is created successfully...");
				PluginLog.info("\n setting outData(returnT) equal to pivotData table");

				//preparing returnT from pivoData table
				outData.clearDataRows();
				for (int count = outData.getNumCols();count>=0;count--){
					outData.delCol(count);
				}

				addColumns(outData);

				pivotTable.copyRowAddAll(outData);

			}catch (OException oe) {
				PluginLog.info("pivotData table NOT prepared successfully...\n");
				PluginLog.error("Error creating pivotData table" + oe.getMessage());

				throw oe;

			}
			finally{
				Utils.removeTable(pivotTable);
			}

		}

	/*
	 * Method addColumns
	 * to create table schema
	 * @param: table
	 * @throws OException 
	 */
	
	private void addColumns(Table pivotTable) throws OException {
		pivotTable.addCol("balance_desc", COL_TYPE_ENUM.COL_STRING, "Balance Line");
		pivotTable.addCol("56_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Platinum");
		pivotTable.addCol("55_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Palladium");
		pivotTable.addCol("58_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Rhodium");
		pivotTable.addCol("61_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Iridium");	
		pivotTable.addCol("63_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Ruthenium");
		pivotTable.addCol("62_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Osmium");
		pivotTable.addCol("54_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Gold");
		pivotTable.addCol("53_Actual", COL_TYPE_ENUM.COL_DOUBLE, "Silver");
	}
	
	
	}
