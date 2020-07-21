/*$Header: /cvs/master/olf/plugins/standard/script_aux/STD_UserDataWorksheetRetrieve.java,v 1.8.60.1 2014/09/25 14:25:03 chrish Exp $*/

/*
 * Revision History: Mar 06, 2012 DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
 */

package standard.script_aux;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;
@ScriptAttributes(allowNativeExceptions=true)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_USER_DATA_WORKSHEET)
public class STD_UserDataWorksheetRetrieve implements IScript {

	private JVS_INC_Standard mINCStandard;
	private String err_log_file;

	public STD_UserDataWorksheetRetrieve(){
		mINCStandard = new JVS_INC_Standard();

	 }

	public void execute(IContainerContext context) throws OException
	{
		String sFileNameError = "STD_UserDataWorksheetRetrieve";
		err_log_file = Util.errorInitScriptErrorLog(sFileNameError);

		 Table argt = context.getArgumentsTable();

		 int retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ;
		 int wizard_created =  1 ;
		 String user_data_worksheet_definition_name = "";
		 String user_table_name = "";
		 String default_user_table_name = "user_sample_data_worksheet";
		 String table_name = "";
		 Table data_table = Util.NULL_TABLE;
		 Table properties_table = Util.NULL_TABLE;
		 Table name_table = Util.NULL_TABLE;

		 // Get the user_data_worksheet_definition_name from the argt table.
		 user_data_worksheet_definition_name = Ref.userDATAWORKSHEETGetDefinitionName ();
		 if ( Str.len (user_data_worksheet_definition_name) == 0 )
		 {
			 mINCStandard.Print(err_log_file, "ERROR","\n The argt table does not contain the name of a User Data Worksheet Definition");
		     Util.exitFail();
		 }
		 // Get the user_table_name from the tables created by the Wizard.
		 user_table_name = Ref.userDATAWORKSHEETGetUserTableName (user_data_worksheet_definition_name) ;
		 if ( Str.len(user_table_name) == 0 )
		 {
		     // This User Data Worksheet Definition was not created using a Wizard.
		     wizard_created = 0 ;
		     // Ask the user to enter a Table name.
		     name_table = Table.tableNew ("name_table");
		     // Place the default_user_table_name  in the name_table
		     name_table.addCol( "name", COL_TYPE_ENUM.COL_STRING);
		     name_table.addRow();
		     name_table.setString( 1, 1, default_user_table_name);
		     retval = request_table_name (name_table);
		     if ( OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() == retval)
		     {
		        user_table_name = name_table.getString( 1, 1);
		     }
		     else
		     {
		        // Use the default USER table.
		        user_table_name = default_user_table_name;
		     }
		     name_table.destroy();
		 }
		 // Load the data_table from the data base
		 data_table = Table.tableNew (user_table_name);
		 retval = DBUserTable.load (data_table);
		 if ( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		 {
			 mINCStandard.Print(err_log_file, "ERROR","\n" + DBUserTable.dbRetrieveErrorInfo(retval,"DBUserTable.load() failed"));
		     data_table.destroy() ;
		     Util.exitFail();
		 }
		 // In order to save our user table by the corresponding STD_SampleDataWorksheetSave script,
		 // a Properties Table must be created.  This properties table must specify at least one column
		 // of the User table, user_table_name, to be a key column.

		 // Create the properties table - structure only.
		 properties_table = Ref.userDATAWORKSHEETInitProperties();

		 if ( 1 == wizard_created)
		 {
		     // If this User Data Worksheet Definition was created using a Wizard, then the read only
		     // columns and key columns have essentially been set in the Wizard and we merely need
		     // to retrieve the information.
		     // Fill in the properties_table, with the columns which are designated as key columns,
		     // and with the columns designated as read only columns.
		     Ref.userDATAWORKSHEETFillProperties (user_data_worksheet_definition_name, user_table_name, properties_table);
		 }
		 else
		 {
		     // This User Data Worksheet Definition was not created using a Wizard.
		     // Are we dealing with our standard sample user table?
		     if ( Str.iEqual (table_name, default_user_table_name) == 1 )
		     {
		        // Since the OpenLink sample USER table is being used,
		        // We fill the properties_table to illustrate some
		        // of the features of the User Data Worksheet functionality, such as
		        // making a column read-only and specifying a column as a key.

		        // Make the column "worksheet_id" of the table, user_sample_data_worksheet,
		        // a key.  The corresponding save script,
		        // STD_SampleDataWorksheetSave, will utilize the key to
		        // insert new entries.
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
		        // type COL_TYPE_ENUM.COL_INT must be set by calling the function
		        // Ref.userDATAWORKSHEETSetColKey()
		     }
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
		 int retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
		 int rtncode = 0;
		 int first_try = 1;
		 Table ask_table = Util.NULL_TABLE;
		 String default_user_table_name = table.getString( 1, 1);
		 String table_name;

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
		          retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ;
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
		       retval = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ;
		       break ;
		    }
		    // Clear rows when restarting while loop
		    ask_table.clearRows();
		 } // while

		 // Avoid memory leaks
		 ask_table.destroy();

		 return retval ;
	}

}
