package com.olf.recon.rb.datasource.jdewrapper;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.fnd.OCalendarBase;
import com.olf.recon.enums.JdeDealExtractFieldAr;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.JDEConnection;
import com.olf.jm.logging.Logging;

/**
 * Class specific for Extended Report. Shifts dates and call the JDE Stored proc with new dates.
 * Connection config is same as that for original recon reports. 
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JDEDealExtractARExtended  extends JDEDealExtractAR
{
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("id", COL_TYPE_ENUM.COL_INT);
		output.addCol("note", COL_TYPE_ENUM.COL_STRING);
		output.addCol("gl_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("gl_date_plugin", COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol("account", COL_TYPE_ENUM.COL_STRING);
		output.addCol("type", COL_TYPE_ENUM.COL_STRING);
		output.addCol("document_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("value_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("value_date_plugin", COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol("qty_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("amount", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("currency", COL_TYPE_ENUM.COL_STRING);
		output.addCol("batch_num", COL_TYPE_ENUM.COL_STRING);
		output.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("key", COL_TYPE_ENUM.COL_STRING);
		output.addCol("debit_credit", COL_TYPE_ENUM.COL_STRING);
		output.addCol("tax_amount", COL_TYPE_ENUM.COL_DOUBLE);
	}
	
	@Override
	protected Table generateOutput(Table output) throws OException
	{
		/*** Recalculate dates. Push Start date 1 day back and end day 1 day forward. ***/
		int inWindowStartDate = windowStartDate;
		int inWindowEndDate = windowEndDate;
		
		windowStartDate = OCalendarBase.getLgbd(windowStartDate); 
		windowEndDate = OCalendarBase.getNgbd(windowEndDate);  
		
		windowStartDateStr = OCalendar.formatJd(windowStartDate);
		windowEndDateStr = OCalendar.formatJd(windowEndDate);
		
		Logging.info("window_start_date: " + windowStartDateStr + ", window_end_date: " + windowEndDateStr);
		
		String serverName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_SERVER_NAME);
		String databaseName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_DATABASE_NAME);
		String userName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_USERNAME);
		String password = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_PASSWORD);
		String storedProcNameDeals = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_NAME_DEALS_AR);
		String timeout = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_QUERY_TIMEOUT);
		int timeoutInt = (timeout != null && !"".equalsIgnoreCase(timeout)) ? Integer.parseInt(timeout) : 0;
			
		JDEConnection jdeConnection = new JDEConnection(serverName, databaseName, userName, password);
		
		CallableStatement callableStatement = null;
		ResultSet resultSet = null;
		
		Table tblSuppData = null;
		
		try 
		{	
			String jdeStartDate = getDateEndurToJDEFormat(windowStartDateStr);
			String jdeEndDate = getDateEndurToJDEFormat(windowEndDateStr);
			/* Executes a stored proc in JDE */
			Logging.info("Executing stored proc: " + storedProcNameDeals);
			jdeConnection.connect();
			callableStatement = jdeConnection.prepareCall(storedProcNameDeals, ledgerMode, jdeStartDate, jdeEndDate);
			if (timeoutInt > 0) callableStatement.setQueryTimeout(timeoutInt);
			resultSet = callableStatement.executeQuery();
			Logging.info("Returned from stored proc: " + storedProcNameDeals);
									
			while (resultSet.next()) 
			{				
				String account = resultSet.getString(JdeDealExtractFieldAr.ACCOUNT.toString());
				BigDecimal amount = resultSet.getBigDecimal(JdeDealExtractFieldAr.AMOUNT.toString()).abs(); 
				BigDecimal amountNonAbs = resultSet.getBigDecimal(JdeDealExtractFieldAr.AMOUNT.toString()); 
				String batchNum = resultSet.getString(JdeDealExtractFieldAr.BATCH_NUM.toString()); 
				String currency = resultSet.getString(JdeDealExtractFieldAr.CURRENCY.toString());
				int documentNum = resultSet.getInt(JdeDealExtractFieldAr.DOC_NUM.toString());
				ODateTime glDatePlugin = jdeJulianDateToDateTime(resultSet.getInt(JdeDealExtractFieldAr.GL_DATE.toString()));
				int glDate = resultSet.getInt(JdeDealExtractFieldAr.GL_DATE.toString());
				int id = resultSet.getInt(JdeDealExtractFieldAr.ID.toString());
				String note = resultSet.getString(JdeDealExtractFieldAr.NOTE.toString());
				BigDecimal qtyToz = resultSet.getBigDecimal(JdeDealExtractFieldAr.QTY_TOZ.toString()).divide(new BigDecimal(10000)).abs();
				String type = resultSet.getString(JdeDealExtractFieldAr.TYPE.toString());
				ODateTime valueDatePlugin = jdeJulianDateToDateTime(resultSet.getInt(JdeDealExtractFieldAr.VALUE_DATE.toString()));
				int valueDate = resultSet.getInt(JdeDealExtractFieldAr.VALUE_DATE.toString());
				int tranNum = resultSet.getInt(JdeDealExtractFieldAr.TRAN_NUM.toString());
				String debitCredit = amountNonAbs.doubleValue() < 0?"Credit":"Debit";
				
				/*** Account has spaces in between. Remove them. ***/
				account = (account != null && !account.isEmpty() ? account.trim().replaceAll(" ", ""): "");

				String key = id + "-" 
						+ account + "-"
						+ debitCredit + "-"
						+ (amount != null?String.format("%.2f", amount):"");
				
				int newRow = output.addRow();
				output.setInt("id", newRow, id);
				output.setString("note", newRow, note);
				output.setDateTime("gl_date_plugin", newRow, glDatePlugin);
				output.setInt("gl_date", newRow, glDate);
				output.setString("account", newRow, account);
				output.setString("type", newRow, type);
				output.setInt("document_num", newRow, documentNum);
				output.setDateTime("value_date_plugin", newRow, valueDatePlugin);
				output.setInt("value_date", newRow, valueDate);
				output.setDouble("qty_toz", newRow, qtyToz.doubleValue());
				output.setDouble("amount", newRow, amount.doubleValue());
				output.setString("currency", newRow, currency);
				output.setString("batch_num", newRow, batchNum);
				output.setInt("tran_num", newRow, tranNum);
				output.setString("debit_credit", newRow, debitCredit);
				output.setString("key", newRow, key);
			}
			
		} 
		catch (Exception e) 
		{
			throw new ReconciliationRuntimeException("Error whilst parsing result set!", e);
		}
		finally
		{
			try 
			{
				if (callableStatement != null) callableStatement.close();
				if (resultSet != null) resultSet.close();
				
				jdeConnection.disconnect();

				if (tblSuppData != null)
				{
					tblSuppData.destroy();
				}
			} 
			catch (SQLException e) 
			{
				throw new ReconciliationRuntimeException(e.getMessage());
			}
		}
		Logging.info("Completed JDE Extract");
		Logging.info("Records returned: " + output.getNumRows());
		
		/*** Remove duplicates ***/
		output.setColValString("note", " ");
		output.makeTableUnique();
		
		/*** Reset days back ***/
		windowStartDate = inWindowStartDate;
		windowEndDate = inWindowEndDate;
		
		windowStartDateStr = OCalendar.formatJd(windowStartDate);
		windowEndDateStr = OCalendar.formatJd(windowEndDate);
		
		return output;
	}

	protected ODateTime jdeJulianDateToDateTime (int jdeJulianDate) throws OException {
		int jdEndur = OCalendar.convertYYYYMMDDToJd("" + (jdeJulianDate / 1000 + 1900) + "0101");
		jdEndur += jdeJulianDate % 1000-1;
		ODateTime dateTime = ODateTime.dtNew();
		dateTime.setTime(0);
		dateTime.setDate(jdEndur);
		return dateTime;
	}

}
