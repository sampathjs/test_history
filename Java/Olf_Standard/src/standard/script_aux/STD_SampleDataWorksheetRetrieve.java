/*$Header: /cvs/master/olf/plugins/standard/script_aux/STD_SampleDataWorksheetRetrieve.java,v 1.8.60.1 2014/09/25 14:25:02 chrish Exp $*/

/*
 * Revision History: Mar 06, 2012 DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
 */

package standard.script_aux;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=true)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_USER_DATA_WORKSHEET)
public class STD_SampleDataWorksheetRetrieve implements IScript {


	private JVS_INC_Standard mINCStandard;
	private String err_log_file;

	public STD_SampleDataWorksheetRetrieve(){
		mINCStandard = new JVS_INC_Standard();

	 }

	public void execute(IContainerContext context) throws OException
	{
		String sFileNameError = "STD_SampleDataWorksheetRetrieve";
		err_log_file = Util.errorInitScriptErrorLog(sFileNameError);

		Table argt = context.getArgumentsTable();

	    int retval;
	    int row;
	    String default_user_table_name = "user_sample_data_worksheet";
	    String table_name;

	    Table data_table = Util.NULL_TABLE;
	    Table properties_table = Util.NULL_TABLE;
	    Table name_table = Table.tableNew ("name_table");

	    // Place the default_user_table_name  in the name_table
	    name_table.addCol( "name", COL_TYPE_ENUM.COL_STRING);
	    name_table.addRow();
	    name_table.setString( 1, 1, default_user_table_name);

	    // Ask the user to enter a Table name.
	    retval = request_table_name (name_table);
	    if ( OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() == retval )
	    {
	       table_name = name_table.getString( 1, 1);
	    }
	    else
	    {
	       // Use the default USER table.
	       table_name = default_user_table_name;

	    }
	    name_table.destroy();

	    // Load the data_table from the data base
	    data_table = Table.tableNew (table_name);
	    retval = DBUserTable.load (data_table);
	    if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
	    {
	   	 mINCStandard.Print(err_log_file, "ERROR","\n" + DBUserTable.dbRetrieveErrorInfo(retval,"DBUserTable.load() failed"));
	       data_table.destroy() ;
	       Util.exitFail();
	    }

	    if ( Str.iEqual (table_name, default_user_table_name) == 1 )
	    {
	      // Since the OpenLink sample USER table is being used,
	      // we create a Properties Table to illustrate some
	      // of the features of the User Data Worksheet functionality, such as
	      // making a column read-only and specifying a column as a unique key.
	      properties_table = Ref.userDATAWORKSHEETInitProperties ();

	      // Make the column "worksheet_id" of the table, user_sample_data_worksheet, a key and
	      // therefore make it read only.  The corresponding save script,
	      // STD_SampleDataWorksheetSave, will utilize the key to
	      // insert new entries.
	      retval = Ref.userDATAWORKSHEETSetColReadOnly ("worksheet_id", data_table, properties_table);
	      if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
	      {
	         Util.exitFail();
	      }
	      retval = Ref.userDATAWORKSHEETSetColKey ("worksheet_id", data_table, properties_table);
	      if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
	      {
	         Util.exitFail();
	      }
	      // We make the "read_only_value" column, read only.
	      retval = Ref.userDATAWORKSHEETSetColReadOnly ("read_only_value", data_table, properties_table);
	      if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
	      {
	         Util.exitFail();
	      }
	      // Sort the data_table by the designated key column
	      data_table.sortCol( "worksheet_id");
	    }
	    else
	    {
	       // In order to properly save a user-selected table by the corresponding
	       // STD_SampleDataWorksheetSave script, a Properties Table must be created
	       // by calling Ref.userDATAWORKSHEETInitProperties() and a key column of
	       // type COL_TYPE_ENUM.COL_INT must be set by calling the function Ref.userDATAWORKSHEETSetColKey()
	    }
	    // The following function sets the Data Table and the Properties Table into returnt
	    // NOTE: This must be the last call that the script makes prior to exiting.
	    retval = Ref.userDATAWORKSHEETSetDataAndProperties (data_table, properties_table);
	    if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
	    {
	       Util.exitFail();
	    }
	}

	int request_table_name (Table table) throws OException
	{
	    int      retval                  = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	    int      rtncode                 = 0;
	    int      first_try               = 1;
	    Table ask_table               = Util.NULL_TABLE;
	    String   default_user_table_name = table.getString( 1, 1);
	    String   table_name;

	    // Ask the user to enter the name of a table. Give the user a default
	    // of the USER table, user_sample_user_data_worksheet.
	    ask_table = Table.tableNew ("");
	    while ( true )
	    {
	        rtncode = Ask.setTextEdit (ask_table, "Table", default_user_table_name, ASK_TEXT_DATA_TYPES.ASK_STRING, "Please enter the name of a Table.", 1);
	        if ( 0 == rtncode )
	        {
	      	  mINCStandard.Print(err_log_file, "ERROR","\n" + DBUserTable.dbRetrieveErrorInfo (retval, "ASK_SetTextEdit() failed"));
	              ask_table.destroy();
	              return OLF_RETURN_CODE.OLF_RETURN_APP_FAILURE.toInt();
	        }
	        if(first_try != 0)
	        {
	           rtncode = Ask.viewTable (ask_table, "User Data Worksheet", "Enter the name of a Table");
	           first_try = 0 ;
	        }
	        else
	        {
	           rtncode = Ask.viewTable (ask_table, "User Data Worksheet", "The previous Table was invalid");
	        }
	        if ( 1 == rtncode )
	        {
	            // User hit OK, get the name of the table the user entered.
	            table_name = (ask_table.getTable( "return_value", 1)).getString("return_value", 1);

	            // Confirm that the table_name is a valid table
	            if(DBUserTable.userTableIsValid (table_name) != 0)
	            {
	               retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	               table.setString( 1, 1, table_name);
	               break;
	            }
	            else
	            {
	            	mINCStandard.Print(err_log_file, "ERROR","\n The table, " + table_name + ", does not exist.");
	            }

	        }
	        else
	        {
	            // User Hit CANCEL
	            // We will use the OpenLink Sample User Data Worksheet
	            retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	            break ;
	        }
	        ask_table.clearRows();

	    } // while

	    // Avoid memory leaks
	    ask_table.destroy();

	    return retval ;
	}

}
