package com.olf.jm.reportbuilder;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 24-Feb-2020 |               | GuptaN02        | Initial version. This is the base abstract class giving structure to plugin     |
 * 														   data source.                                                                     |					   
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */



import com.matthey.utilities.ExceptionUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public abstract class ReportBuilderDatasource implements IScript {

	protected Table tblParam;
	protected Table returnt;
	String prefixBasedOnVersion=null;
	@Override
	public void execute(IContainerContext context) throws OException {
		try{
			returnt = context.getReturnTable();
			setupLog();
			Table tblArgt = context.getArgumentsTable();
			int intModeFlag = tblArgt.getInt("ModeFlag", 1);
			if (intModeFlag == 0)
			{	
				Logging.info("Report is running in Meta Data Mode");
				initialiseReturnt();
			}			
			else
			{
				tblParam = tblArgt.getTable("PluginParameters", 1);
				

				if(Table.isTableValid(tblParam)!=1)
				{
					Logging.error("Invalid Parameter table, exiting");
					throw new OException("Invalid Parameter table, exiting");
				}
				prefixBasedOnVersion=fetchPrefix();
				generateOutput();
			}
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while executing main method "+e.getMessage());
			throw new OException("Error took place while executing main method "+e.getMessage());
		}finally{
			Logging.close();
		}

	}

	/**
	 * Generate output.
	 * Main logic for report builder operation is written here. Implemented by the class extending this class
	 * @throws OException the o exception
	 */
	protected abstract void generateOutput() throws OException;

	/**
	 * Gets the parameter value from the report builder parameter.
	 *
	 * @param parameter the parameter
	 * @return the parameter value
	 * @throws OException the o exception
	 */
	protected String getParameterValue(String parameter) throws OException {
		int intRowNum=-1;
		String value="";
		try{
			intRowNum = tblParam.unsortedFindString(prefixBasedOnVersion + "_name", parameter, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			if(intRowNum > 0)
			value = tblParam.getString(prefixBasedOnVersion + "_value", intRowNum);
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Could not fetch data for parameter: "+parameter+". Error is: "+e.getMessage());
			throw new OException("Could not fetch data for parameter: "+parameter+". Error is : "+e.getMessage());
		}
		return value;
		
	}

	/**
	 * Initialise returnt.
	 * This is used to populate structure of returnt table for Meta Data.Implemented by the class extending this class
	 * @throws OException the o exception
	 */
	protected abstract void initialiseReturnt() throws OException;

	/**
	 * Setup log.
	 *
	 * @throws OException the o exception
	 */
	protected void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;
		ConstRepository constRepo = new ConstRepository("Reports", "");
		String logLevel = constRepo.getStringValue("logLevel","DEBUG");

		try
		{
			String logFile = this.getClass().getSimpleName()+".log";
			Logging.init(this.getClass(), "Reports", "");

		}

		catch (Exception e)
		{
			ExceptionUtil.logException(e, 0);
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	
	/**
	 * Fetch prefix as table structure is changed in v17.
	 *
	 * @param paramTable the param table
	 * @return the string
	 * @throws OException the o exception
	 */
	private String fetchPrefix() throws OException {

		/* v17 change - Structure of output parameters table has changed. */

		String prefixBasedOnVersion = tblParam.getColName(1).equalsIgnoreCase("expr_param_name") ? "expr_param"
				: "parameter";

		return prefixBasedOnVersion;
	}



}
