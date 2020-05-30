/*$Header: //ReferenceDataMigrationScript.java, v1 15 Oct 2015 $*/
/*
File Name:                       Migr_RefData_Contacts.java

Report Name:                     None

Output File Name:                Migr_RefData_Contacts.log in the folder which is defined in Constants Repository for "Migration"

Revision History:                Oct 15, 2015 - First version

Main Script:                     This script
Parameter Script:                None.          
Display Script:                  None.

Script Description:              This script allows to import of Contacts from Migration Manager user tables.
								 Tables used are USER_migr_p_contacts and USER_migr_op_refdata_fields.
								 USER_migr_p_contacts - this table contains the data coming from external system.
								 USER_migr_op_refdata_fields - contains mapping between the above table to Endur Field names for different import types
								 This script will read from the above tables and creates Personnel and links them to LE and BU. In the process, it also 
								 updates Functional Groups.

Assumptions:                    Constants Repository is configured as part of Migration Manager..

Instructions:                    None.     

Uses EOD Results?                NA

EOD Results that are used:       None

When can the script be run?      Anytime

Recommended Script Category? 	N/A
 */
 
 package com.customer.migr_refdata;


import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class Migr_RefData_Contacts implements IScript
{
	ConstRepository constRep;
    public void execute(IContainerContext context) throws OException
    {
    	
    	Table argt = context.getArgumentsTable(); 
        String select= argt.getString("Table", 1);
        
		//Define the variables used in the script
    	int irow, itypeId;
    	String sPersonnel, sPersonneldata, sPersonnelfieldLE, retPersonnelfieldLE, EndurPersonnelfieldLE,reqflag,sqlInfo ;
	    sPersonnel="select * from " + select + " where row_type='2'";
	    Table tPersonnel= Table.tableNew();
	    Table tPersonnelData= Table.tableNew();
	    Table PInfoDetail= Table.tableNew();
	    sqlInfo="select * from personnel_info_types"; 
	    sPersonneldata="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND UPPER(DATA_CATEGORY)=('CONTACT_DATA')";
	    String newpartyBU="";
		String newpartyLE="";
	    
	    //Constants Repository Statics
    	String MIGRATION = "Migration";
    	String MIGRATIONSUBCONTEXT 	= ""; 
    	
    	//Constants Repository init
		constRep = new ConstRepository(MIGRATION, MIGRATIONSUBCONTEXT);
		initPluginLog(); //Plug in Log init
		Logging.info("Started process for Reference Data Migration for Personnel:");
		
		//Get mapping information from mapping table in USer Tables. This maps input table to Endur field names	
	  	try 
		{
	  		DBaseTable.execISql(tPersonnelData, sPersonneldata);
		}
		catch (OException Error)
		{
			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
			throw new OException(Error); 
		}
	  	
	  	int rowCountPersonnelData=tPersonnelData.getNumRows();
	    
		//Get all the required data from USER table to be imported
		try 
		{
			DBaseTable.execISql(tPersonnel, sPersonnel);
			tPersonnel.sortCol("row_id");
		}
		catch (OException Error)
		{
			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
			throw new OException(Error); 
		}
		
		//Get data from personnel info types which is configured in Endur already.         
	        try 
			{
	        	DBaseTable.execISql(PInfoDetail, sqlInfo);
			}
			catch (OException Error)
			{
				Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
				throw new OException(Error); 
			}
	        
	    int rowCountPersonnel=tPersonnel.getNumRows();
	    
	   
		//Contacts - Start looping through the Contacts data got from import table	   
	    first:
	    for(int iPersonnel=1; iPersonnel<=rowCountPersonnel; iPersonnel++){
	    	
	    	Logging.info("Processing Personnel " + iPersonnel + "/" + rowCountPersonnel + " number of rows");
		
	    	int rowid= tPersonnel.getInt("row_id",iPersonnel); 
	    	Table personal;

			personal = Table.tableNew("Personnel");

	    personal=Ref.retrievePersonnel(0);//Retrieve Personnel Table
	      
	    personal.setInt("status", 1, 1);
	   	   	    
	    	for(int irefdata=1;irefdata<=rowCountPersonnelData; irefdata++){
	    		
	    		sPersonnelfieldLE= tPersonnelData.getString("data_column", irefdata); //Contact field Name
    	    	retPersonnelfieldLE= tPersonnel.getString(sPersonnelfieldLE, iPersonnel);	//Value for the field that need to be set			
    	    	EndurPersonnelfieldLE=tPersonnelData.getString("endur_field_name", irefdata); // Endur field where value need to be set
    	    	reqflag=tPersonnelData.getString("required", irefdata); //Check for required flag if field is mandatory to be set
				
				//Loop through to check for Mandatory req flag
    	    	if((retPersonnelfieldLE== null||(retPersonnelfieldLE.trim().equalsIgnoreCase("")))&& reqflag.equalsIgnoreCase("Yes"))
				{
    	    		String errormsg="Personnel Endur_field_name: " + EndurPersonnelfieldLE + " could not be set";
					Logging.error( errormsg+" \n Couldn't process row no. " + iPersonnel + " for row_id " + rowid);
					errorupdate(rowid, errormsg, select);
					continue first;	
				}
				//Proceed to setting the values
				else
				{
					
					if(EndurPersonnelfieldLE.equalsIgnoreCase("personnel_type")){
						int retEnum= checkenum(SHM_USR_TABLES_ENUM.PERSONNEL_TYPE_TABLE, retPersonnelfieldLE, reqflag, EndurPersonnelfieldLE ,iPersonnel, rowid, select);
		   				 if (retEnum==-1){
		   					continue first;	
		   				 }
						personal.setInt(EndurPersonnelfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PERSONNEL_TYPE_TABLE, retPersonnelfieldLE));
						}
	    		else if(EndurPersonnelfieldLE.equalsIgnoreCase("mail_code")|| EndurPersonnelfieldLE.equalsIgnoreCase("fax") ||EndurPersonnelfieldLE.equalsIgnoreCase("city")
	    			|| EndurPersonnelfieldLE.equalsIgnoreCase("first_name") || EndurPersonnelfieldLE.equalsIgnoreCase("title")|| EndurPersonnelfieldLE.equalsIgnoreCase("last_name")
	    			|| EndurPersonnelfieldLE.equalsIgnoreCase("email")|| EndurPersonnelfieldLE.equalsIgnoreCase("addr1")|| EndurPersonnelfieldLE.equalsIgnoreCase("addr2")
	    			|| EndurPersonnelfieldLE.equalsIgnoreCase("short_alias_name")|| EndurPersonnelfieldLE.equalsIgnoreCase("phone")|| EndurPersonnelfieldLE.equalsIgnoreCase("name"))
	    		{
	    			personal.setString(EndurPersonnelfieldLE, 1, retPersonnelfieldLE);
	    		}
	    		else if (EndurPersonnelfieldLE.equalsIgnoreCase("country")){
	    			int retEnum= checkenum(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retPersonnelfieldLE, reqflag, EndurPersonnelfieldLE ,iPersonnel, rowid, select);
   				 if (retEnum==-1){
   					continue first;	
   				 }
   				
	    			personal.setInt(EndurPersonnelfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retPersonnelfieldLE));
	    		}
	    		else if (EndurPersonnelfieldLE.equalsIgnoreCase("state_id")){
	    			int retEnum= checkenum(SHM_USR_TABLES_ENUM.STATES_TABLE, retPersonnelfieldLE, reqflag, EndurPersonnelfieldLE ,iPersonnel, rowid, select);
	   				 if (retEnum==-1){
	   					continue first;	
	   				 }
	    			personal.setInt(EndurPersonnelfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retPersonnelfieldLE));
	    		}
	    		else if(EndurPersonnelfieldLE.equalsIgnoreCase("func_group_id")){
      	    			String[] resultf = retPersonnelfieldLE.split(",");// To split the string to update the individual values which are comma separated
	    	  			
	    			      for (int xlength=0; xlength<resultf.length; xlength++)
	    			      {
	    			    	  String splitStringf= resultf[xlength].trim();
	    			    	  int retEnum= checkenum(SHM_USR_TABLES_ENUM.FUNCTIONAL_GROUP_TABLE, splitStringf, reqflag, EndurPersonnelfieldLE ,iPersonnel, rowid, select);
	    		   				 if (retEnum==-1){
	    		   					continue first;	
	    		   				 }
	    			    	  personal.getTable("personnel_functional_group", 1).addRow();
	    			    	  personal.getTable("personnel_functional_group", 1).setInt(EndurPersonnelfieldLE, xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.FUNCTIONAL_GROUP_TABLE, splitStringf));
	    			      }
      	    		}
	    		else if(EndurPersonnelfieldLE.equalsIgnoreCase("JM Base Price Template"))
				{
					//party info table details
					PInfoDetail.sortCol("type_name");				
	        		irow= PInfoDetail.findString("type_name", EndurPersonnelfieldLE, SEARCH_ENUM.FIRST_IN_GROUP);
	        		
	        		if (irow >=0) 
	        				{
			        		itypeId= PInfoDetail.getInt("type_id",irow);
							
			        		personal.getTable("personnel_info", 1).addRow();
														
								personal.getTable("personnel_info", 1).setInt("type_id", 1, itypeId);
								personal.getTable("personnel_info", 1).setString("info_value", 1, retPersonnelfieldLE);
								
							}
	    		
	    	}
	    		else if(EndurPersonnelfieldLE.equalsIgnoreCase("linked_le_name")){
					int ret= Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,retPersonnelfieldLE);
					if (ret==0 && reqflag.equalsIgnoreCase("Yes"))
					{
						String errormsg= "Legal Entity: " + retPersonnelfieldLE + " could not be set";
						Logging.error(errormsg+" \n Couldn't process row no. " + iPersonnel + " for row_id " + rowid);
						errorupdate(rowid, errormsg, select);
						continue first;
					}
					newpartyLE= retPersonnelfieldLE;
					}	
					else if(EndurPersonnelfieldLE.equalsIgnoreCase("linked_bu_name")){
					int ret= Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,retPersonnelfieldLE);
					if (ret==0 && reqflag.equalsIgnoreCase("Yes"))
					{
						String errormsg= "Business Unit: " + retPersonnelfieldLE + " could not be set";
						Logging.error(errormsg+" \n Couldn't process row no. " + iPersonnel + " for row_id " + rowid);
						errorupdate(rowid, errormsg, select);
						continue first;
					}
					newpartyBU= retPersonnelfieldLE;
					}
				}
	    	
	    personal.addCol("save_name_address_flag", COL_TYPE_ENUM.COL_INT); 
	    personal.addCol("save_personnel_functional_group_flag", COL_TYPE_ENUM.COL_INT);    
	    personal.addCol("save_personnel_info_flag", COL_TYPE_ENUM.COL_INT);  
	    personal.setColValInt("save_name_address_flag", 1);
	    personal.setColValInt("save_personnel_functional_group_flag", 1);
	    personal.setColValInt("save_personnel_info_flag", 1);
	    	}
	    	
	    		try{
										
	    			int retval= Ref.savePersonnel(personal);	    		
    	    	if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
    	    		OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval,	"\nAdd New Account has failed !"));
  					}
  				int newpersonnelid = personal.getInt("id_number", 1);
  				
  				
  				int retvalLink = Ref.linkPartyToPersonnel( Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,newpartyLE), newpersonnelid);
    	    	
    			if (retvalLink != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
    			OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retvalLink, "\nTest has failed !"));
    			}
    			
    				int retvalLinkBU = Ref.linkPartyToPersonnel( Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,newpartyBU), newpersonnelid);
        	    	
        			if (retvalLinkBU != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
        				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retvalLinkBU, "\nTest has failed !"));
        			}
					
        			String sqlcontact="Select name from personnel where id_number="+ newpersonnelid;
	    		
			 	
	         	//update the status of the row in the USER table to update status appropriately
	         	Table user_migr_p_contacts;
	         	user_migr_p_contacts= Table.tableNew();
	         	// Set the name attribute of the table. This should be the name of the database table
	         	user_migr_p_contacts.setTableName(select);
	           	DBUserTable.load(user_migr_p_contacts);
	           		           	
	           	Table temp=Table.tableNew();
	         	
	           	try 
	    		{
	    	  		DBaseTable.execISql(temp, sqlcontact);
	    		}
	    		catch (OException Error)
	    		{
	    			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
	    			throw new OException(Error); 
	    		}
	         	String contact= temp.getString(1, 1);
	           	user_migr_p_contacts.sortCol("row_id");
	         	int row= user_migr_p_contacts.findInt("row_id", rowid, SEARCH_ENUM.LAST_IN_GROUP);
	         	user_migr_p_contacts.setString("new_contact", row, contact  );
	         	user_migr_p_contacts.setString("status_flag", row, "BOOKED");
	         	user_migr_p_contacts.setString("target_status_flag",row , "BOOKED");
	         	user_migr_p_contacts.setInt("row_type",row , 6);
	         	user_migr_p_contacts.setString("status_msg", row, "Successfully Booked");
	         	user_migr_p_contacts.group("row_id");
			
	         	int iRC=DBUserTable.update(user_migr_p_contacts);
			
			
			if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				Logging.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
			}
			 }	
	    		
			catch(OException Error)
			{
				Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			}
	    		
    }
	    Table user_migr_p_contacts;
     	user_migr_p_contacts= Table.tableNew();
     	user_migr_p_contacts.setTableName(select);
       	DBUserTable.load(user_migr_p_contacts);
       	user_migr_p_contacts.viewTable();
       	
       	Logging.info("Completed processing Personnel for " + rowCountPersonnel + " number of rows");
       	Logging.close();
    }
    
	//Update error message and status to User table
    private void errorupdate(int rownum, String msg, String select) throws OException{

		 try
		 	{
        	//update the status of the row in the USER table to +update status appropriately
        	Table user_migr_p_contacts;
        	user_migr_p_contacts= Table.tableNew();
        	// Set the name attribute of the table. This should be the name of the database table
        	user_migr_p_contacts.setTableName(select);
         	DBUserTable.load(user_migr_p_contacts);
         	user_migr_p_contacts.sortCol("row_id");
        	int row=user_migr_p_contacts.findInt("row_id", rownum, SEARCH_ENUM.LAST_IN_GROUP);
        	
        	user_migr_p_contacts.setString("status_msg", row, msg);
        	user_migr_p_contacts.setInt("error_flag", row , 1);
		
        	int iRC=DBUserTable.update(user_migr_p_contacts);

		if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			Logging.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
		}
				
		}	
		catch(OException Error)
		{
			Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			
		}
		
	}
    //Method to check Enum values existing in the Enum Tables
    private int checkenum(SHM_USR_TABLES_ENUM temp, String retfieldAcc, String reqflag, String EndurfieldAcc, int iPersonnel, int rowid, String select) throws OException{
		SHM_USR_TABLES_ENUM enumTable= temp;
	int ret= Ref.getValue(enumTable,retfieldAcc);
	if (ret==-1 && reqflag.equalsIgnoreCase("Yes"))
	{
		String errormsg= "Personnel Endur_field_name: " + EndurfieldAcc + " could not be set";
		Logging.error(errormsg+" \n Couldn't process row no. " + iPersonnel + " for row_id " + rowid);
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

  			Logging.init(this.getClass(), constRep.getContext(),constRep.getSubcontext());
  		} 
  		catch (Exception e) {
  			String errMsg = this.getClass().getSimpleName()	+ ": Failed to initialize logging module.";
  			Util.exitFail(errMsg);
    }

    	
    }
}
