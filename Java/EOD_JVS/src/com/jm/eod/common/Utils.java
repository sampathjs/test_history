
/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * Description: 
 * 		Utilities for metals balance report.
 * 
 * Project : Metals balance sheet
 * Customer : Johnson Matthey Plc. 
 * last modified date : 29/October/2015
 * 
 * @author:  Douglas Connolly /OpenLink International Ltd.
 * @modified by :
 * @version   1.0 // Initial Release
 */

package com.jm.eod.common;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

public class Utils
{
	public static List<Parameters> parameters = new ArrayList<>(0);
	
	static public void removeTable(Table data) throws OException 
	{
		if (Table.isTableValid(data) == 1){
			data.destroy();
		}
	}

	static public Table runSql(String cmd) throws OException
	{			 
		Table data = Table.tableNew();
		int retval = DBaseTable.execISql(data, cmd);
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException(DBUserTable.dbRetrieveErrorInfo(retval, "SQL Failed.")); 
		}
        return data;
	}

	static public void initLogging(ConstRepository cr, String dfltFname) throws OException
	{
		String logLevel = "Error"; 
		String logFile  = dfltFname + ".log"; 
		String logDir   = null;
		String useCache = "No";
 
        try
        {
        	logLevel = cr.getStringValue("logLevel", logLevel);
        	logFile  = cr.getStringValue("logFile", logFile);
        	logDir   = cr.getStringValue("logDir", logDir);
        	useCache = cr.getStringValue("useCache", useCache);            

        	Logging.init(Utils.class, cr.getContext(), cr.getSubcontext());
        }
        catch (Exception e)
        {
        	String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
        	throw new OException(msg);
        }
	}
	
    
    /**
     * Retrieve name of pre-defined query
     * @param args: script parameters
     */
    public static String getParam(Table args, String name) throws OException
    {
		if (args.getNumRows() < 1 || args.getColNum(name) < 1)
		{
			throw new OException(String.format("Missing parameter: %s", name));
		}
		String value = args.getString(name, 1);
		return value == null ? "" : value.trim();
    }
    
    public static void setDefaultParams(String name, String value) throws OException
    {
    	parameters.add(new Parameters(Const.REGION_COL_NAME, COL_TYPE_ENUM.COL_STRING, value));
		String qryRegion = value.length() > 0 ? value + "_" : "";
		parameters.add( new Parameters(Const.QUERY_COL_NAME, COL_TYPE_ENUM.COL_STRING, String.format(name, qryRegion)));
		parameters.add( new Parameters(Const.QUERY_DATE, COL_TYPE_ENUM.COL_STRING, OCalendar.formatJdForDbAccess(OCalendar.today()-1)));
		parameters.add( new Parameters(Const.QUERY_REPORT, COL_TYPE_ENUM.COL_STRING, "Missed_Resets.eod"));
    }
    
    public static void setParams(String name, String value) throws OException
    {
    	parameters.add(new Parameters(name, COL_TYPE_ENUM.COL_STRING, value));
    }
    
    public static void addParams(Table argt) throws OException {
    	
    	if (argt== null)
    		throw new OException("Parameter table invalid");
    	
    	int row = argt.getNumRows();
    	for (Parameters parameter : parameters) {
    		if (argt!= null && argt.getColNum(parameter.getName())<1) {
    			argt.addCol(parameter.getName(), parameter.getType());
    		} else {
    			if (argt.getColType(parameter.getName()) != parameter.getType().toInt())
    				throw new OException(String.format("Attempt to change parameter(%s) type: %s", parameter.getName(), parameter.getType().toString()));
    		}
		}
    	
    	if (row<1) {
    		argt.addRow();
    		row++;
    	}
    	for (Parameters parameter : parameters) {
    		argt.setString(parameter.getName(), row, parameter.getValue());
		}
    	
    }
    public static void setParams(Table params, String qryFmt, String regionCode) throws OException
    {
    	List<Parameters> parameters = new ArrayList<>(0);
    	parameters.add(new Parameters(Const.REGION_COL_NAME, COL_TYPE_ENUM.COL_STRING, regionCode));
    	
//    	params.addCol(Const.REGION_COL_NAME, COL_TYPE_ENUM.COL_STRING);
//    	params.addCol(Const.QUERY_COL_NAME, COL_TYPE_ENUM.COL_STRING);
//    	params.addRow();
//		
//		params.setString(Const.REGION_COL_NAME, 1, regionCode);
		String qryRegion = regionCode.length() > 0 ? regionCode + "_" : "";
		parameters.add( new Parameters(Const.QUERY_COL_NAME, COL_TYPE_ENUM.COL_STRING, String.format(qryFmt, qryRegion)));
//		params.setString(Const.QUERY_COL_NAME, 1, String.format(qryFmt, qryRegion));

		parameters.add( new Parameters(Const.QUERY_DATE, COL_TYPE_ENUM.COL_STRING, OCalendar.formatJdForDbAccess(OCalendar.today()-1)));
		parameters.add( new Parameters(Const.QUERY_REPORT, COL_TYPE_ENUM.COL_STRING, "Missed_Resets.eod"));
		
		addParams(params, parameters);
    }
    public static void addParams(Table argt, List<Parameters> parameters) throws OException {
    	
    	if (argt== null)
    		throw new OException("Parameter table invalid");
    	
    	int row = argt.getNumRows();
    	for (Parameters parameter : parameters) {
    		if (argt!= null && argt.getColNum(parameter.getName())<1) {
    			argt.addCol(parameter.getName(), parameter.getType());
    		} else {
    			if (argt.getColType(parameter.getName()) != parameter.getType().toInt())
    				throw new OException(String.format("Attempt to change parameter(%s) type: %s", parameter.getName(), parameter.getType().toString()));
    		}
		}
    	
    	if (row<1) {
    		argt.addRow();
    		row++;
    	}
    	for (Parameters parameter : parameters) {
    		argt.setString(parameter.getName(), row, parameter.getValue());
		}
    	
    }
    
}