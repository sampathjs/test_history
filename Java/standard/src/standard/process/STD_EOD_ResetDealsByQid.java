/* $Header: /cvs/master/olf/plugins/standard/process/STD_EOD_ResetDealsByQid.java,v 1.14 2013/02/27 15:04:07 chrish Exp $*/

/*

File Name:              STD_EOD_ResetsDealsByQid.java
Output Filname:         [index_name].daily.rst
						STD_EOD_ResetsDealsByQid.txt (error log)


Main Script:            This is the main script
Parameter Script:       STD_Adhoc_Parameter.java, STD_SavedQueryParam.java
Output Script:          None
Page Viewer Script:     None

Script category: 		N/A

Date of Last Revision:	Mar 02, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
						Aug 10, 2011 - Added error log generation and passing it as table to workflow
						Nov 17, 2010 - Replaced Util.exitFail with throwing an OException
                        Apr 17, 2003 - Added header tag and revised header description.

Report/Script Description:
Resets all of the indexes for a day given a transaction query of deals.
It doesn't assume any deal statuses.

Assumptions:			None

Instructions:
Set logerrors to 1 to generate error log and pass it as a table to workflow
Set logerrors to 0 NOT to generate error log and NOT to pass it as a table to workflow

Use EOD Results?		False
EOD Results that are Used:	N/A

When Can the Script be Run?

Columns:			N/A

 */

package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class STD_EOD_ResetDealsByQid implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		/************************* USER CONFIGURABLE PARAMETERS **********************************************/
		//  int logerrors = 0;  //Set to 0 to not to generate error log
		    int logerrors = 1;	//Set to 1 to generate an error log and pass it as a table to workflow
		/*****************************************************************************************************/


		Table argt, reset_info, log_errors_table;
		int x,today,qid,errors_in_resets;
		String date_str,log_file,start_str,error_str,end_str, file_name;;

		argt = context.getArgumentsTable();
		if (Table.isTableValid(argt) == 0)
		   throw new OException("Invalid argument table.");

		if (argt.getColNum("QueryId") <= 0)
		   throw new OException("Argument table is missing a QueryId column.");

		qid = argt.getInt( "QueryId",1);
		if (qid <= 0)
		   throw new OException("Invalid query id in argument table.");

		today = OCalendar.today();
		date_str = OCalendar.formatDateInt(today);

		log_file = "Reset_Process.log";
		Util.errorInitLog(log_file);
		file_name = "STD_EOD_ResetDealsByQid";

		start_str = "INFO: => Starting Reset Processing for " + date_str + ". <=";
		Util.errorWriteString(log_file, start_str);

		reset_info = EndOfDay.resetDealsByQid(qid, today);
		if (Table.isTableValid(reset_info) == 0)
		{
		   error_str = "ERROR: Invalid table returned from the EndOfDay.resetDealsByQid method";
		   Util.errorWriteString(log_file, error_str);

		   end_str = "INFO: => Finished Processing Resets. <=";
		   Util.errorWriteString(log_file, end_str);

		   return;
		}

		/* Check the Reset Info Table for Failures */
		errors_in_resets = 0;


		for(x=1;x<=reset_info.getNumRows();x++)
		{
			if (reset_info.getInt( "success",x) == 0){
				errors_in_resets++;
			}
		}

		// if logerrors equals 1, output the log_error table into a <filename>.txt file
		log_errors_table = reset_info.cloneTable();
		if (logerrors == 1 && errors_in_resets > 0)
		{
			log_errors_table.addNumRows(errors_in_resets);
			int row = 1;
			for(x=1;x<=reset_info.getNumRows();x++)
			{
				if (reset_info.getInt( "success",x) == 0)
					reset_info.copyRow(x, log_errors_table, row++);
			}
			Report.reportStart(file_name + ".txt", "Missing Resets Error Log");
			Report.printTableToReport(log_errors_table, REPORT_ADD_ENUM.FIRST_PAGE);
			Report.reportEnd();
		}

		if (errors_in_resets > 0)
		{
			error_str = "ERROR: => There were " + errors_in_resets + " errors in " +
			" the fixings process. See the Reset Report for details.";
			Util.errorWriteString(log_file, error_str);
		}
		else
		{
			error_str = "INFO: => Reset process completed with no errors. <=";
			Util.errorWriteString(log_file, error_str);
		}

		end_str = "INFO: => Finished Processing Resets. <=";
		Util.errorWriteString(log_file, end_str);

		/* Clean Up and Check for Errors */
		reset_info.destroy();
		if (errors_in_resets > 0)
		{
			if (logerrors == 1)
				Util.exitFail(log_errors_table); //pass log_erros table to workflow
			else
				throw new OException( "Errors while reseting deals by Qid\n" );
		}
		log_errors_table.destroy();
		return;
	}

}
