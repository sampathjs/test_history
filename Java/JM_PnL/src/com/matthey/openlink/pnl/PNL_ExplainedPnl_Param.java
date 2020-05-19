package com.matthey.openlink.pnl;

/**
 * 
 * Description:
 * Param Script For PNL Comparison. Takes input as path of 2 files to be compared.
 * Revision History:
 * 07.05.20  GuptaN02  initial version
 *  
 */

import com.matthey.utilities.ExceptionUtil;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class PNL_ExplainedPnl_Param implements IScript{
	
	private String datasourceColumns="extract_id:integer;extract_date:string;extract_time:integer;bunit:string;metal_ccy:integer;open_date:string;open_volume:double;"
			+ "open_price:double;open_value:double;deal_date:string;deal_num:integer;deal_leg:integer;deal_pdc:integer;deal_reset_id:integer;buy_sell:string;"
			+ "delivery_volume:double;delivery_price:double;delivery_value:double;deal_profit:double;accum_profit:double;close_date:string;close_volume:double;"
			+ "close_price:double;close_value:double";
	private String pkColumnNames=null;
	private String tolerance=null;
	private String columnsToCompare=null;
	private String outputFilePath=null;
	String newline = System.getProperty("line.separator");
	

	@Override
	public void execute(IContainerContext context) throws OException {
		Table tAsk = Util.NULL_TABLE;
		int retVal=-1;
		try{
			init();
			Table argt = context.getArgumentsTable();
			if (Table.isTableValid(argt) != 1) {
				throw new OException("Invalid argt received from context");
			}
			tAsk = createAskPopup();
			retVal = Ask.viewTable(tAsk, "Pnl Explain ", "Please enter the required details for Pnl Comparison");
			if (retVal == 0) {
				throw new OException("User clicked cancel while entering details");
			}
			constructArgt(argt,tAsk);
		}
		catch(Exception e)
		{
			PluginLog.error("Issue took place while executing param script");
			ExceptionUtil.logException(e, 0);
			throw new OException("Issue took place while executing param script"+e.getMessage());
		}

	}
	
	private void init() throws OException {
		{
			Table taskInfo=Util.NULL_TABLE;
			try {
				ConstRepository constRepo = new ConstRepository("PNL", "ExplainPnl");
				String logLevel=constRepo.getStringValue("logLevel", "info");
				taskInfo=Ref.getInfo();
				String taskName=taskInfo.getString("task_name", 1);
				PluginLog.init(logLevel, SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Error_Logs\\",taskName+".log");
				PluginLog.info("Start :" + getClass().getName());
				PluginLog.info("Reading data from const repository");

				pkColumnNames=constRepo.getStringValue("pkColumnNames","deal_num;deal_leg;deal_pdc;deal_reset_id");
				columnsToCompare=constRepo.getStringValue("columnsToCompare","deal_profit:String;delivery_volume:String;delivery_price:String");
				tolerance=constRepo.getStringValue("tolerance","0.00");
				outputFilePath=constRepo.getStringValue("outputCSVFile","\\\\gbromeolfs01d\\endur_dev\\Dirs\\SUPPORT\\Outdir\\reports\\Daily Pnl Reporting\\Result_TradingPnLHistory.csv");

				PluginLog.info("Data read from const repository successfully");
				PluginLog.info("Parameters values are:\n datasourceColumns: "+datasourceColumns+newline+"pkColumnNames: "+pkColumnNames+newline
						+ "columnsToCompare:"+columnsToCompare+newline+ "tolerance: "+tolerance);
			} catch (Exception e) {
				PluginLog.error("Error while executing init method. "+e.getMessage());
				ExceptionUtil.logException(e, 0);
				throw new OException("Error while executing init method. "+e.getMessage());
			}
			finally{
				if(taskInfo!=null)
					taskInfo.destroy();
				
			}
		}



	}

	/**
	 * Creating argument table from const repo and user input
	 * @param argt
	 * @param tAsk
	 * @throws OException
	 */
	private void constructArgt(Table argt,Table tAsk) throws OException {
		try{
			PluginLog.info("Creating argt table as per user input and value from const repository");
			argt.addCol("old_csv_File", COL_TYPE_ENUM.COL_STRING);
			argt.addCol("new_csv_File", COL_TYPE_ENUM.COL_STRING);
			argt.addCol("datasource_columns", COL_TYPE_ENUM.COL_STRING);
			argt.addCol("primary_key", COL_TYPE_ENUM.COL_STRING);
			argt.addCol("columnsToCompare", COL_TYPE_ENUM.COL_STRING);
			argt.addCol("tolerance_threshold", COL_TYPE_ENUM.COL_STRING);
			argt.addCol("outputFilePath", COL_TYPE_ENUM.COL_STRING);
			argt.addRow();

			String oldCsvFile = getReturnValueFromAsk(tAsk, 1);
			String newCsvFile = getReturnValueFromAsk(tAsk, 2);

			argt.setString("old_csv_File", 1, oldCsvFile);
			argt.setString("new_csv_File", 1, newCsvFile);
			argt.setString("datasource_columns", 1, datasourceColumns);
			argt.setString("primary_key", 1, pkColumnNames);
			argt.setString("tolerance_threshold", 1, tolerance);
			argt.setString("columnsToCompare", 1, columnsToCompare);
			argt.setString("outputFilePath", 1, outputFilePath);
			PluginLog.info("Successfully created argt table as per user input and value from const repository");
		}
		catch(Exception e )
		{
			PluginLog.error("Error took place while creating and populating argt table");
			ExceptionUtil.logException(e, 0);
			throw new OException("Error took place while creating and populating argt table"+e.getMessage());
		}

	}

	private String getReturnValueFromAsk(Table tAsk, int row) throws OException {
		return tAsk.getTable("return_value", row).getString("return_value", 1);
	}

	/**
	 * Creats GUI for user input
	 * @return
	 * @throws OException
	 */
	private Table createAskPopup() throws OException {
		Table tAsk=Util.NULL_TABLE;
		try{
			PluginLog.info("Creating GUI Table for receiving user input: ");
			tAsk = Table.tableNew();
			Ask.setTextEdit(tAsk, "Old PnL CSV File Path", "", ASK_TEXT_DATA_TYPES.ASK_STRING, "", 0);
			Ask.setTextEdit(tAsk, "New Pnl CSV File Path", "", ASK_TEXT_DATA_TYPES.ASK_STRING, "", 0);
			PluginLog.info("Completed GUI Table for receviing user input: ");
			return tAsk;
		}
		catch(Exception e)
		{
			PluginLog.error("Error took place while creating GUI for user input");
			ExceptionUtil.logException(e, 0);
			throw new OException("Error took place while creating GUI for user input"+e.getMessage());
		}

	}

}
