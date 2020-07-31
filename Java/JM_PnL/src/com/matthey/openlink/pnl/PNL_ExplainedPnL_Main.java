package com.matthey.openlink.pnl;

/**
 * 
 * Description:
 * This script compares pnl data(input as csv) between 2 dates and gives output in excel in current directory.
 * Revision History:
 * 07.05.20  GuptaN02  initial version
 *  
 */

import com.matthey.utilities.CompareCSVFiles;
import com.matthey.utilities.ReportingUtils;
import com.matthey.utilities.ExceptionUtil;
import com.matthey.utilities.enums.CompareCSVResult;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;



public class PNL_ExplainedPnL_Main implements IScript  {

	String templateFilePath=null;
	String outputFileName=null;
	String targetFilePath=null;

	@Override
	public void execute(IContainerContext context) throws OException {
		try{
			init();
			Table tblArgt= context.getArgumentsTable();
			if(Table.isTableValid(tblArgt)!=1)
				throw new OException("Argument table not valid");
			CompareCSVFiles compare= new CompareCSVFiles(tblArgt);
			Table output=compare.compareCSV();
			if(output.getNumRows()<=1)
				throw new OException("Output table not valid");
			output=prepareOutput(output,compare);
			ReportingUtils.SaveToExcel(templateFilePath,outputFileName,targetFilePath,output);
		}
		catch(Exception e)
		{
			Logging.error("Error took place while working on pnl Explain..");
			ExceptionUtil.logException(e, 0);
			throw new OException("Error took place while working on pnl Explain  "+e.getMessage());
		} finally {
			Logging.close();
		}
	}

	private Table prepareOutput(Table output,CompareCSVFiles compare) throws OException
	{
		try{
			for(int outputRow=1;outputRow<=output.getNumRows();outputRow++)
			{
				String validationResult= output.getString("validation_result", outputRow);
				if(validationResult.equalsIgnoreCase(CompareCSVResult.NOT_MATCHING.getValue()) )
				{
					Boolean isMatchingPrice=isColumnMatching("delivery_price",compare,outputRow);
					Boolean isMatchingVolume=isColumnMatching("delivery_volume",compare,outputRow);
					String result= isMatchingPrice && isMatchingVolume ? ExplainPnlResult.NOT_MATCHING.getValue():ExplainPnlResult.AMENDED.getValue();
					compare.getValidationResultTable().setString("validation_result", outputRow, result);			
				}
			}
		}
		catch (Exception e)
		{
			String message = "Exception occurred while updating validation resutls for output." + e.getMessage();
			Logging.error(message);
			throw new OException(message);

		}
		return output;
	}


	/**
	 * @param column
	 * @param compare
	 * @param outputRow
	 * @return
	 * @throws OException
	 * It checks if deal is amended or impacted 
	 */
	private boolean isColumnMatching(String column, CompareCSVFiles compare,int outputRow) throws OException {
		Table validationTable=Util.NULL_TABLE;
		boolean isMatching=false;
		try {
			validationTable=compare.getValidationResultTable();
			isMatching=(compare.getStringValue(validationTable, column+"_old", outputRow)).equalsIgnoreCase(compare.getStringValue
					(validationTable, column+"_new", outputRow));
		} catch (OException e) {
			String message = "Exception occurred while comparing if deal is amendend or only impacted" + e.getMessage();
			Logging.error(message);
			throw new OException(message);
		}
		return isMatching;
		
	}

	protected void init() throws OException {
		Table taskInfo=Util.NULL_TABLE;
		try{
			ConstRepository constRepo = new ConstRepository("PNL", "ExplainPnl");
			String logLevel=constRepo.getStringValue("logLevel","info");
			taskInfo=Ref.getInfo();
			String taskName=taskInfo.getString("task_name", 1);
			Logging.init(this.getClass(), constRepo.getContext(), constRepo.getSubcontext());
			Logging.info("Start :" + getClass().getName());
			Logging.info("Fetching value from const repository:..");
			templateFilePath=constRepo.getStringValue("templateFilePath", "");
			outputFileName=constRepo.getStringValue("outputFileName");
			targetFilePath = Util.reportGetDirForToday();
			String message="";
			
			if(templateFilePath==null || templateFilePath.isEmpty())
				message+="Parameter not defined for datasourceColumns \n";
			
			if(outputFileName==null || outputFileName.isEmpty())
				message+="Parameter not defined for outputFileName \n";
			
			if(targetFilePath==null || targetFilePath.isEmpty())
				message+="Parameter not defined for targetFilePath \n";
			
			if(!message.isEmpty())
			{
				message+="Following parameters are not defined. ";
				Logging.error(message);
				throw new OException(message);
			}
			
			Logging.info("Fetched value from const repository, values are as below:\n  templateFilePath = "+templateFilePath+"\n outputFileName= "+outputFileName+"\n targetFilePath="+targetFilePath);
		}
		catch(Exception e)
		{
			Logging.error("Failed while initialising "+e.getMessage());
			ExceptionUtil.logException(e, 0);
			throw new OException("Failed while initialising "+e.getMessage());

		}
		finally{
			if(taskInfo!=null)
				taskInfo.destroy();
		}

	}
	
	/**
	 * @author GuptaN02
	 * This Enum is used to label results in comparing 2 csv Files
	 */
	public enum ExplainPnlResult {
		NOT_MATCHING("Impacted",0),
		AMENDED("Amended",4);

		private final String value;
		private final int code;


		public String getValue() {
			return value;
		}

		public int getCode() {
			return code;
		}


		ExplainPnlResult(String value, int code)
		{
			this.value=value;
			this.code=code;
		}
	}



}

