/*$Header: /cvs/master/olf/plugins/standard/process/STD_Historical_Price_Refixing_P.java,v 1.11 2012/06/26 14:55:21 chrish Exp $*/
/**************************************************************************************************
Script File Name:    STD_Historical_Price_Refixing_P.java

This version has an attached report.

Report Name:         N/A

Output File Name:    N/A

Revision History:    08/07/07 bsowers   Initial check-in
                     06/29/09 cmcleod   Report cleanup & conversion to OpenJVS
                     10/13/10 adelacruz - Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
                                        - Replaced Util.exitFail with throwing an OException
                                        - Removed debug/test code

Parameter Script:    This is the parameter script
Main Script:         STD_Historical_Price_Refixing.java
Page Viewer Script:  None
Output Script:       None
Include Script:      JVS_INC_Standard

Script category: 		N/A

Description:         This script is fixes historical prices that have been 
                     modified for deals that use indexes recieved in argt                  

Assumptions:         None

Config Requirements: 

EOD or OtF Results:  N/A

Sim Results used:    N/A

Execution:           Executed from a Trading Manager Task.
 ********************************************************************************/

package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class STD_Historical_Price_Refixing_P implements IScript {

	private JVS_INC_Standard mINCStandard;
	private String err_log_file;

	public STD_Historical_Price_Refixing_P(){
		mINCStandard = new JVS_INC_Standard();

	 }
	
	public void execute(IContainerContext context) throws OException
	{

		String sFileNameError = "STD_Historical_Price_Refixing_P";
		err_log_file = Util.errorInitScriptErrorLog(sFileNameError);
		
		Table argt = context.getArgumentsTable();

		Table index_list, ask_tbl, index_list_selected, temp;
		String what, from, where;
		int ret_val;

		argt.addCol( "index_list", COL_TYPE_ENUM.COL_TABLE);
		argt.addRow();

		/*create list of indexes for use in this report */
		index_list = Table.tableNew();
		what = " SELECT index_id, index_name, format ";
		from = " FROM idx_def ";
		where = " WHERE index_status = 2 and db_status = 1 ";

		try{
			DBaseTable.execISql( index_list, what + from + where );
		}
		catch( OException oex ){
			mINCStandard.Print(err_log_file, "ERROR","\nOException at execute(), unsuccessful database query, " + oex.getMessage() );
		}

		index_list.group( "index_name");

		ask_tbl = Table.tableNew();
		Ask.setAvsTable(ask_tbl, index_list, "Select Curves", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1);
		ret_val = Ask.viewTable(ask_tbl, "Curve Selection", "Select your choice from pick list below:");

		if(ret_val <= 0)
		{
			mINCStandard.Print(err_log_file, "END","\nUser pressed cancel. Aborting...");
			index_list.destroy(); 
			ask_tbl.destroy(); 
			throw new OException( "User Clicked 'Cancel'" );
		}

		temp = ask_tbl.getTable( "return_value", 1);
		index_list_selected = temp.copyTable();
		index_list_selected.setTableTitle( "Index List");
		index_list_selected.setTableName( "index_id_list");
		index_list_selected.setColName( 1, "index_id");
		index_list_selected.select( index_list, "index_name, format", "index_id EQ $index_id");
		index_list_selected.group( "index_name");
		argt.setTable( "index_list", 1, index_list_selected);

		ask_tbl.destroy();
		return;
	}


}

