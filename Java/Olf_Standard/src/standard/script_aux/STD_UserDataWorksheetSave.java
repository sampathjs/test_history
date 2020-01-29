/*$Header: /cvs/master/olf/plugins/standard/script_aux/STD_UserDataWorksheetSave.java,v 1.8.60.1 2014/09/25 14:25:03 chrish Exp $*/

/*
 * Revision History: Mar 06, 2012 DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
 */

package standard.script_aux;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;
@ScriptAttributes(allowNativeExceptions=true)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_USER_DATA_WORKSHEET)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class STD_UserDataWorksheetSave implements IScript {

	private JVS_INC_Standard mINCStandard;
	private String err_log_file;

	public STD_UserDataWorksheetSave(){
		mINCStandard = new JVS_INC_Standard();

	 }

	public void execute(IContainerContext context) throws OException
	{
		String sFileNameError = "STD_UserDataWorksheetSave";
		err_log_file = Util.errorInitScriptErrorLog(sFileNameError);

		 Table argt = context.getArgumentsTable();

		 int retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
		 Table data_table = Util.NULL_TABLE;
		 Table properties_table = Util.NULL_TABLE;
		 String default_user_table_name = "user_sample_data_worksheet";
		 String user_table_name = "";
		 int worksheet_id_col = 0;
		 int max_id = 0;
		 int numrows = 0;
		 int row = 0;

		 // Guard against this script being called blindly.
		 if (Table.isTableValid (argt) == 0 || (argt.getNumRows() <= 0) )
		 {
		    return;
		 }
		 // Get the data and the properties table from argt
		 data_table = Ref.userDATAWORKSHEETGetData ();
		 properties_table = Ref.userDATAWORKSHEETGetProperties ();
		 if (Table.isTableValid (properties_table) == 0)
		 {
			 mINCStandard.Print(err_log_file, "ERROR","\nFailed to save, due to missing Properties Table.");
		  Util.exitFail() ;
		 }
		 // An empty properties_table means that all columns are read only
		 // and should not be modified in any way, including deletion. Being
		 // empty also means that no keys have been set, and if no keys have been
		 // set, our function, Ref.userDATAWORKSHEETSaveUserTable() will fail.
		 numrows = properties_table.getNumRows();
		 if ( numrows <= 0 )
		 {
		  // We ensure that a table that is read only, (and should not by implication
		  // not have rows any rows deleted) cannot be saved.
			 mINCStandard.Print(err_log_file, "ERROR","\nFailed to save, because all columns were read only columns.");
		  Util.exitFail() ;
		 }
		 // We have a non-empty properties table.  Either it was essentially created
		 // by the Wizard, or it was created in a retrieve script.
		 // Get the name of the User table
		 user_table_name = data_table.getTableName();
		 if ( Str.iEqual (user_table_name, default_user_table_name) != 1 )
		 {
		  // The User table is not our default User table.  A properties table has
		  // been created for it, either by the Wizard, or directly in the retrieve
		  // script.  In either case we save it.  Recall that it will not be saved
		  // if it doesn't have at least one column designated as a key column.
		  retval = Ref.userDATAWORKSHEETSaveUserTable (data_table, properties_table);
		  if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
		  {
			  mINCStandard.Print(err_log_file, "ERROR","\nFailed to save the user table" + user_table_name);
		   Util.exitFail() ;
		  }
		 }
		 else
		 {
		  // Get the "worksheet_id" column.
		  worksheet_id_col = data_table.getColNum( "worksheet_id");
		  if ( worksheet_id_col > 0 )
		  {
		   if (data_table.getColType( worksheet_id_col) == COL_TYPE_ENUM.COL_INT.toInt())
		   {
		    // New records have an key of 0, so we must update them accordingly.
		    data_table.sortCol( worksheet_id_col);
		    numrows = data_table.getNumRows();
		    // After sorting, the last row has the largest id
		    max_id = data_table.getInt( worksheet_id_col, numrows);

		    // Loop through the data_table, and set the keys for all
		    // new records.
		    for ( row = 1; row <= numrows; row++ )
		    {
		        if ( 0 == data_table.getInt( worksheet_id_col, row) )
		       {
		         data_table.setInt( worksheet_id_col, row,  ++max_id);
		        }
		    }
		    // We now are in  position to save the data.  The following function
		    // not only deletes records from the database which have been marked
		    // for deletion, it also deletes them from the in-memory data_table.
		    // NOTE: Updating the in-memory table is necessary for the
		    // User Data Worksheet screen to reflect the changes.
		    retval = Ref.userDATAWORKSHEETSaveUserTable (data_table,  properties_table);
		    if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
		    {
		   	 mINCStandard.Print(err_log_file, "ERROR","\nFailed to save the default user table.");
		     Util.exitFail() ;
		    }
		    data_table.sortCol( worksheet_id_col);
		   }
		  }
		 }
	}

}
