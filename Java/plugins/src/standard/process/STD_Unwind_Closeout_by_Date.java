/*$Header: /cvs/master/olf/plugins/standard/process/STD_Unwind_Closeout_by_Date.java,v 1.11 2012/05/16 15:30:36 dzhu Exp $*/

/*
File Name:                      STD_Unwind_Closeout_by_Date.java

Report Name:                    None

Output File Name:               None

History Revision:               Mar 05, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
								Oct 06, 2010 - (adelacruz) Replaced Util.exitFail with throwing an OException
                                Feb 08, 1999

Main Script:                    This is the MAIN script
Parameter Script:               None
Display Script:                 None

Script category: 		N/A

Report Description:             This script will unwind (reverse) all deals that
                                were matched off today.
                                This is a Process Script.

Assumption:                     None

Instruction:

Use EOD Results?                False

EOD Results that are used:      None

When can the script be run?

Columns:                        None

 */

/************** General Comments ***********************************************
 *
 * 1. To CLOSEOUT deals, use one of these three scripts:
 *    STD_Adhoc_FIFO.java - using STD_Adhoc_Parameter.java to select deals
 *    STD_Selected_FIFO.java - using saved query to select deals
 *    STD_FIFO_by_Pfolio.java - using STD_Portfolio_Param.java to select deals
 *
 * 2. To have a REPORT of what's been closed-out, use STD_FIFO_Match_Report.java.
 *
 * 3. To UNWIND in case of error on the same day of match-off/closeout, use
 *    STD_Unwind_Closeout_by_Date.java
 *
 ********************************************************************************/

package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

import standard.include.JVS_INC_Standard;
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class STD_Unwind_Closeout_by_Date implements IScript {

	private JVS_INC_Standard mINCStandard;
	private String err_log_file;

	public STD_Unwind_Closeout_by_Date(){
		mINCStandard = new JVS_INC_Standard();

	 }

	public void execute(IContainerContext context) throws OException
	{
		String sFileNameError = "STD_Unwind_Closeout_by_Date";
		err_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileNameError);

		Table argt = context.getArgumentsTable();

		mINCStandard.Print(err_log_file, "START","\nStarting STD Unwind Closeout by Date script.");
		int today=0,retval=0;
		today = OCalendar.today();

		retval = Closeout.tableUnwindCloseoutByDate(today);

		if (retval != 1)
			throw new OException( "Error value returned from Closeout.tableUnwindCloseoutByDate(today)");

		mINCStandard.Print(err_log_file, "END","\nEnd of STD Unwind Closeout by Date script.");
	}

}
