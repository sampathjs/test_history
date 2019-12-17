/*$Header: //ReferenceDataMigrationScript.java, v1 15 Oct 2015 $*/
/*
File Name:                       ReferenceMigrationScript.java

Report Name:                     None

Output File Name:                ReferenceMigrationScript.log in the folder which is defined in Constants Repository for "Migration"

Revision History:                Oct 15, 2015 - First version

Main Script:                     This script
Parameter Script:                None.          
Display Script:                  None.

Script Description:              This script allows to import of Contacts from Migration Manager user tables.
								 Tables used are USER_migr_p_acct_si and USER_migr_op_refdata_fields.
								 USER_migr_p_acct_si - this table contains the data coming from external system.
								 USER_migr_op_refdata_fields - contains mapping between the above table to Endur Field names for different import types
								 This script will read from the above tables and creates Accounts and Settle Instructions and links them to BU. In the process, it also 
								 updates Account Info  fields.

Assumptions:                    Constants Repository is configured as part of Migration Manager..

Instructions:                    None.     

Uses EOD Results?                NA

EOD Results that are used:       None

When can the script be run?      Anytime

Recommended Script Category? 	N/A
 */
 
package com.customer.pglebu;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class AccountsEnhancement implements IScript
{
	ConstRepository constRep;
	@Override
    public void execute(IContainerContext context) throws OException
    {
    	//Define the variables used in the script
    	String sqlAccount, sqlRefdata,Acctfield,retfieldAcc,EndurfieldAcc, sqlRefdatasettle, retfieldsettle, Endurfieldsettle,Settlefield,reqflag ;
    	
    	Table argt = context.getArgumentsTable(); 
        String select= argt.getString("Table", 1);
    	    	
    	sqlAccount="select * from " + select + " where row_type='2'";
	    Table tAcct= Table.tableNew();
	    Table tRefdata= Table.tableNew();
	    Table tRefdatasettle=Table.tableNew();
	    sqlRefdata="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND UPPER(DATA_CATEGORY)=('ACCOUNT_DATA') AND UPPER(DATA_OBJECT)=('Account')";
	    sqlRefdatasettle="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND UPPER(DATA_CATEGORY)=('ACCOUNT_DATA') AND UPPER(DATA_OBJECT)=('Settle Instr.')";
	    
	    //Constants Repository Statics
    	String MIGRATION = "Migration";
    	String MIGRATIONSUBCONTEXT 	= "";  
    	
    	 //Constants Repository init
		constRep = new ConstRepository(MIGRATION, MIGRATIONSUBCONTEXT);
		initPluginLog(); //Plug in Log init
		PluginLog.info("Started process for Reference Data Migration for Accounts and Settlement Instructions:");
    	
		//Get mapping information of Accounts from mapping table in USer Tables. This maps input table to Endur field names
    	try 
		{
	  		DBaseTable.execISql(tRefdata, sqlRefdata);
		}
		catch (OException Error)
		{
			PluginLog.error("The Script failed for the following reason: "	+ Error.getMessage());
			throw new OException(Error); 
		}
	    
	  	int rowCountRefdata=tRefdata.getNumRows();
		 
		//Get all the required data from USER table to be imported
	  	try 
		{
	  		DBaseTable.execISql(tAcct, sqlAccount);
		}
		catch (OException Error)
		{
			PluginLog.error("The Script failed for the following reason: "	+ Error.getMessage());
			throw new OException(Error);  
		}

	  	int rowCountAcct=tAcct.getNumRows();
	  	 
		//Get data from Account info types which is configured in Endur already. 		 
	  	Table tAccountInfo= Table.tableNew();
        String sqlAccountInfo="select * from account_info_type"; 
        
        try 
		{
        	DBaseTable.execISql(tAccountInfo, sqlAccountInfo);
		}
		catch (OException Error)
		{
			PluginLog.error("The Script failed for the following reason: "	+ Error.getMessage());
			throw new OException(Error); 
		}
		//Get mapping information for Settle Instruction from mapping table in USer Tables. This maps input table to Endur field names
        try 
		{
	  		DBaseTable.execISql(tRefdatasettle, sqlRefdatasettle);
		}
		catch (OException Error)
		{
			PluginLog.error("The Script failed for the following reason: "	+ Error.getMessage());
			throw new OException(Error);  
		}
	       
	  	int rowCountRefdatasettle=tRefdatasettle.getNumRows();
	  	
		//Accounts and Settle Instruction - Start looping through the data got from import table	  
	  	first:
	  	for(int iAcct=1; iAcct<=rowCountAcct;iAcct++)//
	  	{
	  		PluginLog.info("Processing Accounts and Settlement Instructions " + iAcct + "/" + rowCountAcct + " number of rows");
	  		
	  		int rowid= tAcct.getInt("row_id",iAcct); 
	  		String AcctName= tAcct.getString("account_name", iAcct);
	  		String queryName= "Select account_id from account where account_name= '"+ AcctName +"';";
	  		Table name= Table.tableNew();
	  		try 
			{
		  		DBaseTable.execISql(name, queryName);
			}
			catch (OException Error)
			{
				PluginLog.error("The Script failed for the following reason: "	+ Error.getMessage());
				throw new OException(Error);  
			}
	  		int Acctid= name.getInt(1, 1);
	  		Table acct = Ref.retrieveAccount(Acctid); //Retrieve Account Table
	  		String newpartyid= tAcct.getString("linked_bu_name", iAcct);

	 			acct.setInt("account_status", 1, 1);
	 			acct.getTable("account_group_member", 1).clearRows();
	 			acct.getTable("sel_currency", 1).clearRows();
	 			acct.getTable("account_info", 1).clearRows();
	 			
	 			for(int irefdata=1;irefdata<=rowCountRefdata; irefdata++)
		  		{
			  		
	  			Acctfield= tRefdata.getString("data_column", irefdata); //Account field Name
    	    	retfieldAcc= tAcct.getString(Acctfield, iAcct);			//Value for the field that need to be set	
    	    	EndurfieldAcc=tRefdata.getString("endur_field_name", irefdata); // Endur field where value need to be set
    	    	reqflag=tRefdata.getString("required", irefdata); //Check for required flag if field is mandatory to be set
    	    	    	    	
				//Loop through to check for Mandatory req flag
    	    	if((retfieldAcc== null||(retfieldAcc.trim().equalsIgnoreCase("")))&& reqflag.equalsIgnoreCase("Yes"))
				{
					String errormsg= "Account Endur_field_name: " + EndurfieldAcc + " could not be set";
					PluginLog.error(errormsg+ " \n Couldn't process row no. " + iAcct + " for row_id " + rowid);
					errorupdate(rowid, errormsg, select);
					continue first;	
				}
				
				//Proceed to setting the values
				else
				{
    	    	try{
	  			
    	    	if( EndurfieldAcc.equalsIgnoreCase("description") ||  EndurfieldAcc.equalsIgnoreCase("account_number")|| EndurfieldAcc.equalsIgnoreCase("account_name")|| EndurfieldAcc.equals("account_iban"))
    	    		
    	    	{
    	    		acct.setString(EndurfieldAcc, 1, retfieldAcc);
    	    		
    	    	}
    	    	else if (EndurfieldAcc.equalsIgnoreCase("holder_id")){
    	    		
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.PARTY_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, retfieldAcc) );
    	    	}
    	    	else if (EndurfieldAcc.equalsIgnoreCase("account_class")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE, retfieldAcc));
    	    	}
    	    	else if (EndurfieldAcc.equalsIgnoreCase("account_type")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE, retfieldAcc));
    	    	}
    	    	else if (EndurfieldAcc.equalsIgnoreCase("linked_account_id")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.PARTY_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, retfieldAcc));
    	    	}
    	    	else if (EndurfieldAcc.equalsIgnoreCase("on_bal_sheet_flag")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.NO_YES_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.NO_YES_TABLE, retfieldAcc));
    	    	}
    	    	else if(EndurfieldAcc.equalsIgnoreCase("currency_id")){
    	    		   		    	
    	  			String[] result = retfieldAcc.split(", "); // To split the string to update the individual values which are comma separated
    	  			
    			      for (int xlength=0; xlength<result.length; xlength++)
    			      {
    			    	  String splitString= result[xlength];
    			    	  int retEnum= checkenum(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, splitString, reqflag, EndurfieldAcc ,iAcct, rowid, select);
    						 if (retEnum==-1){
    		  					continue first;	
    		  				 }
    			    	  acct.getTable("sel_currency", 1).addRow();
    			    	  acct.getTable("sel_currency", 1).setInt(EndurfieldAcc, xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE,splitString));
    			    	 
    			      }
    	    	}
    	    	
    	    	else if (EndurfieldAcc.equalsIgnoreCase("base_currency")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, retfieldAcc));
    	    	}
    	    	
    	    	else if (EndurfieldAcc.equalsIgnoreCase("account_country")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
    	    		acct.setInt(EndurfieldAcc, 1, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retfieldAcc));
    	    	}
    	    	else if (EndurfieldAcc.equalsIgnoreCase("account_group")){
    	    		int retEnum= checkenum(SHM_USR_TABLES_ENUM.ACCOUNT_GROUP_TABLE, retfieldAcc, reqflag, EndurfieldAcc ,iAcct, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
					 acct.getTable("account_group_member", 1).addRow();
    	    		acct.getTable("account_group_member", 1).setInt("account_group_member_id", 1, Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_GROUP_TABLE, retfieldAcc));
    	    		acct.getTable("account_group_member", 1).setInt("account_group_id", 1, Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_GROUP_TABLE, retfieldAcc));
    	    	}
    	    	
    	    	else
				{
					tAccountInfo.sortCol("type_name");				
	        		int irowAcct= tAccountInfo.findString("type_name", EndurfieldAcc, SEARCH_ENUM.FIRST_IN_GROUP);
	        	
			        		int itypeIdActInfo= tAccountInfo.getInt("type_id",irowAcct);
												
							
								acct.getTable("account_info", 1).addRow();
								int iInforows = acct.getTable("account_info", 1).getNumRows();
								
								acct.getTable("account_info", 1).setInt("info_type_id", iInforows, itypeIdActInfo);
								acct.getTable("account_info", 1).setString("info_value", iInforows, retfieldAcc);
								    	    	
    	    	}
    	    	}
				
	  			catch (OException Error)
	  			{
	  				PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
	  				continue;
	  				
	  			}  		
    	    	
				}
	  		}

	    	    	int retval = Ref.updateAccount(acct);
	    	    	if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	    	    		OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval,	"\nAdd New Account has failed !"));
	  					}
	  				int newaccountid = acct.getInt("account_id", 1);	
	    	    	/*int retvalLink = Ref.linkAccountToParty(newaccountid, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,newpartyid));
	    			if (retvalLink != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	    				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retvalLink, "\nTest has failed !"));
	    	    	
	    			}*/
	    			Table setNameTable=Table.tableNew();
	    			String setName= "Select settle_id from settle_instructions where settle_name= '"+ AcctName +"';";
	    			try 
	    			{
	    		  		DBaseTable.execISql(setNameTable, setName);
	    			}
	    			catch (OException Error)
	    			{
	    				PluginLog.error("The Script failed for the following reason: "	+ Error.getMessage());
	    				throw new OException(Error);  
	    			}
	    	  		int settid= setNameTable.getInt(1, 1);
	    			Table settle = Ref.retrieveSettleInstruction(settid);//Retrieve Settle Instruction
	    			settle.setInt("settle_status", 1, 1);
	    			settle.setInt("account_id", 1, newaccountid);
	    			 
	    			settle.setInt("party_id", 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,newpartyid));
	    			settle.getTable("settle_currency", 1).clearRows();
	    			settle.getTable("stl_ins", 1).clearRows();
	    			
	    		      for(int irefdatasettle=1;irefdatasettle<=rowCountRefdatasettle; irefdatasettle++)
	  		  		{
	  			  		
	  	  			Settlefield= tRefdatasettle.getString("data_column", irefdatasettle);//Settle Instruction field Name
	      	    	retfieldsettle= tAcct.getString(Settlefield, iAcct);				//Value for the field that need to be set	
	      	    	Endurfieldsettle=tRefdatasettle.getString("endur_field_name", irefdatasettle); // Endur field where value need to be set
	      	    	reqflag=tRefdata.getString("required", irefdatasettle); //Check for required flag if field is mandatory to be set
					
					//Loop through to check for Mandatory req flag
	      	    	if((retfieldsettle== null||(retfieldsettle.trim().equalsIgnoreCase("")))&& reqflag.equalsIgnoreCase("Yes"))
					{
	      	    		String errormsg= "Settle Instruction Endur_field_name: " + Endurfieldsettle + " could not be set";
						PluginLog.error(errormsg+ " \n Couldn't process row no. " + iAcct + " for row_id " + rowid);
						errorupdate(rowid, errormsg, select);
						continue first;	
					}
					//Proceed to setting values
					else
					{	      	    	    	    	
	      	    	try{
	  	  			
	      	    	if(Endurfieldsettle.equalsIgnoreCase("settle_name"))
	      	    		
	      	    	{
	      	    		settle.setString(Endurfieldsettle, 1, retfieldsettle);
	      	    	}
	      	    	else if(Endurfieldsettle.equalsIgnoreCase("currency_id")){
	      	    		String[] resultstl = retfieldsettle.split(", ");//To split the string to update the individual values which are comma separated
	    	  			
	    			      for (int xlength=0; xlength<resultstl.length; xlength++)
	    			      {
	    			    	  String splitStringstl= resultstl[xlength];
	    			    	int retEnum= checkenum(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, splitStringstl, reqflag, Endurfieldsettle ,iAcct, rowid, select);
	    						 if (retEnum==-1){
	    		  					continue first;	
	    		  				 }
	    			    	  settle.getTable("settle_currency", 1).addRow();
	    			    	  settle.getTable("settle_currency", 1).setInt(Endurfieldsettle, xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE,splitStringstl));
	    			      }
	      	    	}
	      	    	else if(Endurfieldsettle.equalsIgnoreCase("ins_type")){
	      	    		
						//List of Instruments in the Settle Instruction field
	      	    		if (retfieldsettle.equalsIgnoreCase("All")){
	      	    			String newretfieldsettle = "CASH, PREC-EXCH-FUT, METAL-B-SWAP, METAL-SWAP, COMM-PHYS, COMM-STOR, FX, DEPO-ML, LOAN-ML"; 
	      	    			String[] resultins = newretfieldsettle.split(", ");
		    	  			
		    			      for (int xlength=0; xlength<resultins.length; xlength++)
		    			      {
		    			    	  String splitStringins= resultins[xlength];
		    			    	  int retEnum= checkenum(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, splitStringins, reqflag, Endurfieldsettle ,iAcct, rowid, select);
		    						 if (retEnum==-1){
		    		  					continue first;	
		    		  				 }
		    			    	  settle.getTable("stl_ins", 1).addRow();
		    			    	  settle.getTable("stl_ins", 1).setInt(Endurfieldsettle, xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, splitStringins));
		    			      }
	      	    		}
	      	    		else{

	      	    		String[] resultins = retfieldsettle.split(", ");
	    	  			
	    			      for (int xlength=0; xlength<resultins.length; xlength++)
	    			      {
	    			    	  String splitStringins= resultins[xlength];
	    			    	  int retEnum= checkenum(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, splitStringins, reqflag, Endurfieldsettle ,iAcct, rowid, select);
	    						 if (retEnum==-1){
	    		  					continue first;	
	    		  				 }
	    				      settle.getTable("stl_ins", 1).addRow();
	    			    	  settle.getTable("stl_ins", 1).setInt(Endurfieldsettle, xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, splitStringins));
	    			      }
	      	    	}
	      	    	}
	      	    	}

		  			catch (OException Error)
		  			{
		  				PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
		  				continue;
		  			}
	      	    	}
	  		  		}
	      	    	
	      	    	int retvalstl = Ref.updateSettleInstruction(settle);
	    	    	if (retvalstl != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	    	    		OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retvalstl,	"\nAdd New Account has failed !"));
	  					}
	  				int newsettleid = settle.getInt("settle_id", 1);
		  		    /*	int retvalLinkstl = Ref.linkSettleInstructionToParty(newsettleid, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,newpartyid));
		    			if (retvalLinkstl != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
		    				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retvalLinkstl, "\nTest has failed !"));
		    			}*/
		    			
		    			Table temp=Table.tableNew();
		    			temp= Ref.retrieveAccount(newaccountid);
		    			String acctName= temp.getString("account_name", 1);
		    			Table tempsettle=Table.tableNew();
		    			tempsettle= Ref.retrieveSettleInstruction(newsettleid);
		    			String settleName= tempsettle.getString("settle_name", 1);
		    			
		    			 try
		    			 	{
		                 	//update the status of the row in the USER table to update status appropriately
		                 	Table user_migr_p_acct_si;
		                 	user_migr_p_acct_si= Table.tableNew();
		                 	// Set the name attribute of the table. This should be the name of the database table
		                 	user_migr_p_acct_si.setTableName(select);

		                 	DBUserTable.load(user_migr_p_acct_si);
		                 	
		                 	user_migr_p_acct_si.sortCol("row_id");
		                 	int row= user_migr_p_acct_si.findInt("row_id", rowid, SEARCH_ENUM.LAST_IN_GROUP);
		                 	user_migr_p_acct_si.setString("new_acct_name", row, acctName);
		                 	user_migr_p_acct_si.setString("new_si_name", row, settleName);
		                 	user_migr_p_acct_si.setString("status_flag", row, "UPDATED");
		                 	user_migr_p_acct_si.setString("target_status_flag",row , "UPDATED");
		                 	user_migr_p_acct_si.setInt("row_type",row , 7);
		                 	user_migr_p_acct_si.setString("status_msg", row, "Successfully Updated");
		                
		                 	user_migr_p_acct_si.group("row_id");
		    			
		                 	int iRC=DBUserTable.update(user_migr_p_acct_si);
		    			
		    			if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
		    				PluginLog.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
		    			}
		    	
		    			 }	
		    			catch(OException Error)
		    			{
		    				PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
		    			}	
		    			 
		    				
	  	}
	  	
	  	 PluginLog.info("Completed processing Accounts and Settlement Instructions for " + rowCountAcct + " number of rows");
	  	Table user_migr_p_acct_si;
     	user_migr_p_acct_si= Table.tableNew();
     	user_migr_p_acct_si.setTableName(select);
     	DBUserTable.load(user_migr_p_acct_si);
     	user_migr_p_acct_si.viewTable();
     	
	  	}
	
	//Update error message and status to User table
	private void errorupdate(int rownum, String msg, String select) throws OException{

		 try
		 	{
      	//update the status of the row in the USER table to +update status appropriately
      	Table user_migr_p_acct_si;
      	user_migr_p_acct_si= Table.tableNew();
      	// Set the name attribute of the table. This should be the name of the database table
      	user_migr_p_acct_si.setTableName(select);
       	DBUserTable.load(user_migr_p_acct_si);
       	user_migr_p_acct_si.sortCol("row_id");
      	int row=user_migr_p_acct_si.findInt("row_id", rownum, SEARCH_ENUM.LAST_IN_GROUP);
      	
      	user_migr_p_acct_si.setString("status_msg", row, msg);
      	user_migr_p_acct_si.setInt("error_flag", row , 1);
		
      	int iRC=DBUserTable.update(user_migr_p_acct_si);

		if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			PluginLog.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
		}
				
		}	
		catch(OException Error)
		{
			PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			
		}
		
	}
	
	 //Method to check Enum values existing in the Enum Tables
	private int checkenum(SHM_USR_TABLES_ENUM temp, String retfieldAcc, String reqflag, String EndurfieldAcc, int iAcct, int rowid, String select) throws OException{
			SHM_USR_TABLES_ENUM enumTable= temp;
		int ret= Ref.getValue(enumTable,retfieldAcc);
		if (ret==-1 && reqflag.equalsIgnoreCase("Yes"))
		{
			String errormsg= " Account/SI Endur_field_name: " + EndurfieldAcc + " could not be set";
			PluginLog.error(errormsg+" \n Couldn't process row no. " + iAcct + " for row_id " + rowid);
			errorupdate(rowid, errormsg, select);
			return ret;	
		}
		else return ret=1;
		
	}
	  	
	  //Initiate plug in logging
  		private void initPluginLog() throws OException {

  		String logLevel = constRep.getStringValue("logLevel", "info");
  		String logFile = constRep.getStringValue("logFile", this.getClass()
  				.getSimpleName()
  				+ ".log");
  		String logDir = constRep.getStringValue("logDir", "");

  		try {

  			if (logDir.trim().equalsIgnoreCase("")) {
  				PluginLog.init(logLevel);
  			} else {
  				PluginLog.init(logLevel, logDir, logFile);
  			}
  		} 
  		catch (Exception e) {
  			String errMsg = this.getClass().getSimpleName()	+ ": Failed to initialize logging module.";
  			Util.exitFail(errMsg);
    }
    	
    }
}
