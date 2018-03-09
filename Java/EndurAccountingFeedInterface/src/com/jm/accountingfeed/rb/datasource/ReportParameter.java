package com.jm.accountingfeed.rb.datasource;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

/**
 * Handler for report builder parameters passed in via the GUI
 */
public class ReportParameter
{
	/* This will be loaded in the constructor */
	private Table parameterTable = Util.NULL_TABLE;
	
	/**
	 * Constructor 
	 * 
	 * @param tblArgt - argt of a report builder datasource plugin
	 * @throws OException
	 */
	public ReportParameter(Table argParameterTable) throws OException
	{   	        	        	
		parameterTable = argParameterTable;
	}
	
	/**
	 * Find the param row  
	 * 
	 * @param customParam
	 * @return
	 * @throws OException
	 */
	private int getRow(String customParam) throws OException
	{
		int findRow = -1;
		
		findRow = parameterTable.unsortedFindString(1, customParam, SEARCH_CASE_ENUM.CASE_INSENSITIVE);        
		
		return findRow;
	}
		
	public String getStringValue(String customParamName) throws OException
	{
		String ret = "";
		
		int findRow = getRow(customParamName);
		if (findRow > 0)
		{
			ret = parameterTable.getString(2, findRow);
		}
		
		return ret;
	}
}