package com.olf.jm.reportbuilder;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 07-Dec-2020 |               | FernaI01        | 																				   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */



import com.matthey.utilities.ExceptionUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class ReportBuilderPNLSnapshot_DS implements IScript {

	public static final String CONTEXT = "";
	public static final String SUBCONTEXT = "";
	
	protected Table tblParam;
	protected Table returnt;
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
				
				returnt.addCol("bunit", COL_TYPE_ENUM.COL_STRING);
				
				returnt.addCol("metal_ccy", COL_TYPE_ENUM.COL_STRING);
				
				returnt.addCol("open_date", COL_TYPE_ENUM.COL_STRING);
				
				returnt.addCol("open_volume", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("open_price", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("open_value", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("buy_sell", COL_TYPE_ENUM.COL_STRING);
				
				returnt.addCol("deal_date", COL_TYPE_ENUM.COL_STRING);
				
				returnt.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
				
				returnt.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
				
				returnt.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
				
				returnt.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
				
				returnt.addCol("delivery_volume", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("delivery_price", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("delivery_value", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("close_date", COL_TYPE_ENUM.COL_STRING);
				
				returnt.addCol("close_volume", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("close_price", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("close_value", COL_TYPE_ENUM.COL_DOUBLE);
				
				returnt.addCol("deal_profit", COL_TYPE_ENUM.COL_DOUBLE);

			}			
			else
			{
				tblParam = tblArgt.getTable("PluginParameters", 1);
				

				String strStartDateUI = getParamValue(tblArgt, "startDateUI", true);
				String strReportDateUI = getParamValue(tblArgt, "reportDateUI", true);
				
				ReportBuilder report = ReportBuilder.createNew("PNL Snapshot DS");
				
				String strTmp = OCalendar.formatDateInt(OCalendar.parseString(strStartDateUI), DATE_FORMAT.DATE_FORMAT_ISO8601_EXTENDED);
				report.setParameter("ALL", "startDate",strTmp);
				
				strTmp = OCalendar.formatDateInt(OCalendar.parseString(strReportDateUI), DATE_FORMAT.DATE_FORMAT_ISO8601_EXTENDED);
				report.setParameter("ALL", "reportDate",strTmp);
				
				com.olf.openjvs.Table reportOutput = com.olf.openjvs.Table.tableNew();
	            report.setOutputTable(reportOutput);
				report.runReport();		

				returnt.select(reportOutput,"*","deal_num GT 0");

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

		Logging.info("**********" + this.getClass().getName() + " end  **********");
	}

	
	/*
	 * Method  getParamValue
	 * Retrieve value for specified parameter.
	 * @param  inData: table of input parameters
	 * @param  paramName: name of parameter
	 * @param  optional: allow blank parameter value (true/false)
	 * @return parameter value
	 */
	private String getParamValue(Table inData, String paramName, boolean optional) throws OException 
	{
		String outValue = "";
		if(inData.getColNum("PluginParameters")< 1)
		{
			throw new OException("Report Builder Definition is corrupt or running outside report Builder Context"); 
		}
		
		Table params = inData.getTable("PluginParameters", 1); 
		int rowNo = params.unsortedFindString(1, paramName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if (rowNo < 1 && !optional)
		{
			throw new OException("Report Builder Definition is corrupt or running  "
					+ "outside report Builder Context. Missing parameter - " + paramName); 
		}

		String paramValue = params.getString("parameter_value", rowNo);				
		if (paramValue != null ) 
		{
				outValue = paramValue;
		}
		else if (!optional) 
		{
			throw new OException("Report Builder Definition is corrupt or running  "
						+ "outside report Builder Context. Missing or empty parameter - " + paramName + ".");
		}
		
		return outValue;
	}

	

	/**
	 * Setup log.
	 *
	 * @throws OException the o exception
	 */
	protected void setupLog() throws OException
	{

		try {

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new OException("Error initialising logging. " + e.getMessage());
		}
		
		
		
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	

}
