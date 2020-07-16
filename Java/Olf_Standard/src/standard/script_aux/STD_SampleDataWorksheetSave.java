/*$Header: /cvs/master/olf/plugins/standard/script_aux/STD_SampleDataWorksheetSave.java,v 1.8.60.1 2014/09/25 14:25:02 chrish Exp $*/

/*
 * Revision History: Mar 06, 2012 DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
 */

package standard.script_aux;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;
@ScriptAttributes(allowNativeExceptions=true)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_USER_DATA_WORKSHEET)
public class STD_SampleDataWorksheetSave implements IScript {

	private JVS_INC_Standard mINCStandard;
	private String err_log_file;

	public STD_SampleDataWorksheetSave(){
		mINCStandard = new JVS_INC_Standard();

	 }

	public void execute(IContainerContext context) throws OException
	{
		String sFileNameError = "STD_SampleDataWorksheetSave";
		err_log_file = Util.errorInitScriptErrorLog(sFileNameError);

	   Table argt = context.getArgumentsTable();

	   int retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	   Table data_table = Util.NULL_TABLE;
	   Table properties_table = Util.NULL_TABLE;
	   int worksheet_id_col = 0;
	   int max_id = 0;
	   int numrows = 0;
	   int row = 0;

	   if ( 0 == argt.getNumRows() )
	   {
	      return;
	   }

	   // Get the data and the properties table from argt
	   data_table = Ref.userDATAWORKSHEETGetData ();
	   properties_table = Ref.userDATAWORKSHEETGetProperties ();
	   if ( Table.isTableValid(properties_table) == 0 )
	   {
	   	mINCStandard.Print(err_log_file, "ERROR","\nFailed to save, due to missing Properties Table.");
	      Util.exitFail() ;
	   }
	   // Get the "worksheet_id" column if it has one, from the data_table
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
	               data_table.setInt( worksheet_id_col, row, ++max_id);
	            }
	         }
	         // We now are in  position to save the data.  The following function
	         // not only deletes records from the database which have been marked
	         // for deletion, it also deletes them from the in-memory data_table.
	         // NOTE: Updating the in-memory table is necessary for the
	         // User Data Worksheet screen to reflect the changes.
	         retval = Ref.userDATAWORKSHEETSaveUserTable (data_table, properties_table);
	         if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
	         {
	         	mINCStandard.Print(err_log_file, "ERROR","\nFailed to save the default user table.");
	            Util.exitFail() ;
	         }
	         data_table.sortCol( worksheet_id_col);
	      }
	   }
	   else
	   {
	      // Getting here implies that the OpenLink Sample User Data Worksheet was not used
	      // and that a "worksheet_id_col" column was not specified for the user-selected table
	      //
	      // The following is a brief set of steps to follow in order to properly save the
	      // user data.
	      //
	      // 1-The data table contains three additional columns inserted by the core code,
	      //   "udw_insert", "udw_update", "udw_delete" where,
	      //   A value of 1 for "udw_insert" indicates that a new record was added.
	      //   A value of 1 for "udw_update" indicates that the record was updated.
	      //   A value of 1 for "udw_delete" indicates that the record should be deleted.
	      //   These values should be used to determine the appropriate action to take to save the data
	      //   based on the end-user desired changes
	      //
	      // 2-The final state of all saved data must be reflected in the data_table so that the saved changes
	      //   are accurately shown to the end-user on the screen.  That is, when this saved script completes,
	      //   the information in the data_table will be displayed to the end-user on the screen.  As such,
	      //   deleted records need to be removed from the data_table.
	      //
	      // 3-If the script wishes to remove the additionally-added columns (see #1 above), it can do so by
	      //   calling the function Ref.userDATAWORKSHEETRemoveStateColumns()

	   }
	}

}
