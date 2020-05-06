/*
 * Description:
 * 
 * Liquidity Report based on Metals balance sheet project.
 * Liquidity values for 5 precious metals are extracted from Metal Balance Sheet-US and UK report.
 * 
 * History:
 * 2019-12-27	V1.0	Jyotsna	- Initial version, Developed under SR 259994
 * 
 */
 
package com.jm.rbreports.BalanceSheet;

import java.util.HashSet;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class RegionalLiquidityReport extends BalanceLineAccountsUK{
private static final String SUBCONTEXT = "Regional Liquidity Report";
private static final String CR_VAR_NAME_BALANCE_ID = "Balance Line IDs";
private static final String RPT_BALANCE_LINE_NAME = "Liquidity";

	
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
				PluginLog.info ("Number of rows for balance desc - " + balanceDesc.getNumRows());
			}
			finally{
				Utils.removeTable(balanceDescRegion);
			}	
		}
		
		getAccountInfo(balances, balanceDesc,regionSet);
		transposeData(outData, balances);
		checkBalanceLines(outData, balanceDesc, false);
		applyFormulas(outData);
		filterColumns(outData);
		}
		
		finally{
		Utils.removeTable(balanceDesc);
		Utils.removeTable(balances);
		
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
				
			}
	
			int metalId = balances.getInt("currency_id", balIdx);
			String outCol = metalId + "_" + COLHEADER_RB_ACTUAL;
			outData.setDouble(outCol, outIdx, outData.getDouble(outCol, outIdx) + balances.getDouble("balance", balIdx));
		}
	}
	
	
	/*
	 * Method getBalanceDesc
	 * Retrieve balance line descriptions from USER_jm_balance_line_us.
	 *  @param region: UK/US
	 *  @throws OException 
	 */
	protected Table getBalanceDesc(String region) throws OException 
	{
		PluginLog.info("Running method getBalanceDesc(String " + region + ")");
		PluginLog.info("For " + region + " region Building sql to get Balance description"); 
		String sql = "select id balance_line_id,\n"
				   + "       ltrim(rtrim(balance_line)) balance_line,\n" 
				   + "       ltrim(rtrim(description)) balance_desc,\n"
				   + "       display_order,\n"
				   + "       formula,\n"
				   + "       display_in_drilldown,\n"
				   + "		'" + region + "' region \n"
				   + " from   " + getUserBalanceLineTable(region) + "\n"
				   + " where id IN("
				   + getBalanceLineIDs(region)
				   + ")";
				   
		PluginLog.info("For " + region + " region Balance Line Description SQL is:" + sql);
		PluginLog.info("Running sql");
		return runSql(sql);
	}
	/*
	 * Method getAccountInfo
	 * Retrieve account details for each balance line account
	 * @param  outData: balance sheet data
	 * @param  balanceDesc: description of each balance line
	 * @param regionSet: hashset for regions defined in const repo 
	 * @throws OException 
	 */
	
	protected void getAccountInfo(Table data, Table balanceDesc, HashSet<String> regionSet) throws OException
	{
		String sql ="";
		for(String region: regionSet) {
			if(sql == null || sql.trim().isEmpty()){
				sql = getAccountinfosql(region);
			}else{
				sql = sql + " Union\n " + getAccountinfosql(region);
			}
		}
		PluginLog.info("Account Info SQL " + sql);
		Table info = runSql(sql);
		info.select(balanceDesc,"*", "balance_line EQ $balance_line");
		data.select(info, "*", "account_id EQ $account_id");
		
		Utils.removeTable(info);
	}
	/*
	 * Method getAccountinfosql
	 * build account info SQL per region for each balance line account
	 * @param region: UK/US
	 * @throws OException 
	 */
	protected String getAccountinfosql(String region) throws OException{
		String sql = "SELECT distinct a.account_id,\n"
				   + "       ltrim(rtrim(a.account_name)) account_name,\n"
				   + "       ai.info_value balance_line\n"
		           + "FROM   account a\n"
		           + "       JOIN account_info ai "
		           + "ON (a.account_id = ai.account_id AND ai.info_type_id = (SELECT ait.type_id FROM account_info_type ait "
		           + "WHERE type_name = '" + getAccountInfoTypeName(region) + "'))"
		           ;
		return sql;
	}
	/*
	 * Method getUserBalanceLineTable
	 * Retrieve balance line user table per region 
	 * @param region: UK/US
	 * @throws OException 
	 */
	protected String getUserBalanceLineTable (String region) throws OException {
		ConstRepository cr = getConstRepo(); 
		String crVar = CR_VAR_NAME_BALANCE_LINE_TABLE + " " + region;
		String userTable = cr.getStringValue (crVar);
		PluginLog.info ("Balance Line Table retrieved from Constants Repository variable " + cr.getContext() + "\\" + cr.getSubcontext() + "\\" + crVar + " = " + userTable );
		return userTable;
	}
	/*
	 * Method getAccountInfoTypeName
	 * Retrieve account info name per region 
	 * @param region: UK/US
	 * @throws OException 
	 */
	private String getAccountInfoTypeName (String region) throws OException {
		ConstRepository cr = getConstRepo();
		String crVar = CR_VAR_NAME_ACCOUNT_INFO + " " + region;
		String accountInfoTypeName = cr.getStringValue (crVar);
		PluginLog.info ("Account Info Type Name retrieved from Constants Repository variable " + cr.getContext() + "\\" + cr.getSubcontext() + "\\" + crVar + " = " + accountInfoTypeName );
		return accountInfoTypeName;
	}
	
	
/*
 * Method filterColumns
 * To filter out non-dependent balance line items for Liquidity in returnT
 * @param outData: returnT
 */
private void filterColumns(Table outData)  throws OException{
	
	PluginLog.info("Filtering out non-liqiudity rows from return table..");
	for (int count = outData.getNumRows();count>=1 ;count--){
		String balanceDesc = outData.getString("balance_desc", count);
		if(balanceDesc.equalsIgnoreCase(RPT_BALANCE_LINE_NAME))
		{
			continue;
		}
		outData.delRow(count);
	}
}
/*
 * Method getBalanceLineIDs
 * Retrieve balance line IDs per region from const repo 
 * @param region: UK/US
 * @throws OException 
 */
private String getBalanceLineIDs (String region) throws OException {
	ConstRepository cr = getConstRepo();
	String crVar = CR_VAR_NAME_BALANCE_ID  + " " + region;
	String balanceIDs = cr.getStringValue (crVar);
	PluginLog.info ("For " + region + " region Balance Line IDs retrieved from Constants Repository variable " + cr.getContext() + "\\" + cr.getSubcontext() + "\\" + crVar + " = " + balanceIDs );
	return balanceIDs;

}

/*
 * Method getRegionList
 * Retrieve region list from const repo 
 * @param : empty HashSet
 * @throws OException 
 */
protected void getRegionList(HashSet<String> regionSet) throws OException {

ConstRepository cr = getConstRepo();
Table regions =  cr.getMultiStringValue("Region");

for (int rowcount=1;rowcount<=regions.getNumRows();rowcount++){
String getRegion = regions.getString("value", rowcount);
regionSet.add(getRegion);   
}
}


}
