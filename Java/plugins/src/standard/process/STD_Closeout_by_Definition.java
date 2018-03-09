/*$Header: /cvs/master/olf/plugins/standard/process/STD_Closeout_by_Definition.java,v 1.5 2012/06/26 14:55:20 chrish Exp $*/

/*
File Name:                      STD_Closeout_by_Definition.java

Report Name:                    None

Output File Name:               None

Date of Last Revision:          Oct 08, 2010 - (adelacruz) Replaced Util.exitFail with throwing an OException
                                September 18, 2009

Main Script:                    This is the MAIN script
Parameter Script:               STD_SavedQueryParam.java or STD_Adhoc_Parameter.java                     
Display Script:                 None
Script category: 		N/A
Report Description:             This script will run the closeout definition. It is equivalent to running the closeout definition
                                from the Closeout Processing module from the Operations Manager.
                                This is a Process Script.  

Assumption:                     None

Instruction:

Use EOD Results?                False

EOD Results that are used:      None

When can the script be run?     

Columns:                        None

 */

package standard.process;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class STD_Closeout_by_Definition implements IScript
{ 
	private JVS_INC_Standard m_INCStandard;

	public STD_Closeout_by_Definition()
	{
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		/******************** USER CONFIGURABLE PARAMETERS *********************/

		String closeoutDefinition = "Closeout";   // Set to desired closeout definition

		/***********************************************************************/

		int queryId, retVal;
		String fileName = "STD_Closeout_by_Definition";
		String errorLogFile = Util.errorInitScriptErrorLog(fileName);
		String errorMessage = "";
		Table argt = context.getArgumentsTable();

		m_INCStandard.Print(errorLogFile, "START", "*** Start of " + fileName + " script ***");

		/* Check to see that this Script was run with a Param Script*/
		if(argt.getNumRows() <= 0 || argt.getColNum( "QueryId") < 0)
		{
			errorMessage = "This script requires a Param script, either STD_Adhoc_Parameter.java or STD_Saved_Query_Param.java";
			m_INCStandard.Print(errorLogFile, "ERROR", errorMessage);
			m_INCStandard.Print(errorLogFile, "END", "*** End of " + fileName + " script ***\n");
			throw new OException( errorMessage );
		}

		queryId = argt.getInt("QueryId", 1);

		retVal = Closeout.byDefinition(closeoutDefinition, queryId);
		Query.clear(queryId);
		if (retVal <= 0)
		{
			errorMessage = DBUserTable.dbRetrieveErrorInfo(retVal, "");
			m_INCStandard.Print(errorLogFile, "ERROR", errorMessage);
			m_INCStandard.Print(errorLogFile, "END", "*** End of " + fileName + " script ***\n");
			throw new OException( errorMessage );
		}
		else
		{
			m_INCStandard.Print(errorLogFile, "END", "*** End of " + fileName + " script ***");
		}
		return;
	}
}
