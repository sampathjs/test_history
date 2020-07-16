/*$Header: //ReferenceDataMigrationScript.java, v1 15 Oct 2015 $*/
/*
File Name:                       Migr_RefData_Addresses.java

Report Name:                     None

Output File Name:                Migr_RefData_Addresses.log in the folder which is defined in Constants Repository for "Migration"

Revision History:                Oct 15, 2015 - First version

Main Script:                     This script
Parameter Script:                None.          
Display Script:                  None.

Script Description:              This script allows to import of Address from Migration Manager user tables.
								 Tables used are USER_migr_p_addr and USER_migr_op_refdata_fields.
								 USER_migr_p_addr - this table contains the data coming from external system.
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
 
 package com.customer.migr_refdata_updates;


import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class Migr_RefData_Addresses_Updates implements IScript
{
	ConstRepository constRep;
	@Override
    public void execute(IContainerContext context) throws OException
    {
		
		Table argt = context.getArgumentsTable(); 
        String select= argt.getString("Table", 1);
        
    	//Define the variables used in the script
    	    int ret;
    	    String sqladdress, sqlAddressdata, sqlAddressdataBU, AddressfieldLE, retfieldAddressLE, AddressEndurfieldLE, AddressfieldBU, retfieldAddressBU, AddressEndurfieldBU, reqflag ;
    	    String lebu="LE+BU";
    	    String bu="BU";
    	    sqladdress="select * from " + select + " where row_type='2'";
    	    Table tAddress= Table.tableNew();
    	    Table tAddressLE= Table.tableNew();
    	    sqlAddressdata="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND UPPER(DATA_CATEGORY)=('ADDRESS_DATA') AND UPPER(DATA_OBJECT)=('LE')";
    	    
    	    Table tAddressBU= Table.tableNew();
    	    sqlAddressdataBU="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND UPPER(DATA_CATEGORY)=('ADDRESS_DATA') AND UPPER(DATA_OBJECT)=('BU')";
    	    
    	  //Constants Repository Statics
        	String MIGRATION = "Migration";
        	String MIGRATIONSUBCONTEXT 	= ""; 
        	
        	//Constants Repository init
			constRep = new ConstRepository(MIGRATION, MIGRATIONSUBCONTEXT);
			initPluginLog(); //Plug in Log init
			Logging.info("Started process for Reference Data Migration for Address:");
    		 
			//Get mapping information from mapping table in USer Tables. This maps input table to Endur field names	
    	  	try 
    		{
    	  		DBaseTable.execISql(tAddressBU, sqlAddressdataBU);
    		}
    		catch (OException Error)
    		{
    			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    			throw new OException(Error);  
    		}
    	    int rowCountAddressBU=tAddressBU.getNumRows();
    		
			//Get mapping information from mapping table in USer Tables. This maps input table to Endur field names					
    	  	try 
    		{
    	  		DBaseTable.execISql(tAddressLE, sqlAddressdata);
    		}
    		catch (OException Error)
    		{
    			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    			throw new OException(Error);  
    		}
    	  	int rowCountAddressLE=tAddressLE.getNumRows();
    	    
			//Get all the required data from USER table to be imported
    		try 
    		{
    			DBaseTable.execISql(tAddress, sqladdress);
    			tAddress.sortCol("row_id");
    		}
    		catch (OException Error)
    		{
    			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    			throw new OException(Error);  
    		}
    		int rowCountAddress=tAddress.getNumRows();
			
			//Create Legal ENtity and Business Unit Table
    	    Table newLegalEntity= Table.tableNew();
    	    Table newBunit=Table.tableNew();
    	    
			//Address - Start looping through the Address data got from import table	
    	    first:
    	    for(int iaddress=1; iaddress<=rowCountAddress; iaddress++){
    	    	
    	    	 Logging.info("Processing Address " + iaddress + "/" + rowCountAddress + " number of rows");
    	    	 
    	    	int rowid= tAddress.getInt("row_id",iaddress); 
    	    	String idle=tAddress.getString("new_le_address_id", iaddress);
    	    	int leid=Integer.parseInt(idle);
    	    	String idbu=tAddress.getString("new_bu_address_id", iaddress);
    	    	int buid=Integer.parseInt(idbu);
    	    	String simporttype= tAddress.getString("import_type",iaddress); //Check for Import Type, if LE+BU or only BU
    	    	String scheck=	tAddress.getString("address_type",iaddress); //Check for Address type to Insert rows if other than Main Address
    	    	String sparty=tAddress.getString("linked_le_name", iaddress);
    	    	if(simporttype.equalsIgnoreCase(lebu)){
    	    	newLegalEntity= Ref.retrieveParty(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,sparty));
    	    	
    	    	newLegalEntity.getTable("party_address", 1).sortCol("party_address_id");
    	    	int lrow= newLegalEntity.getTable("party_address", 1).findInt("party_address_id", leid, SEARCH_ENUM.FIRST_IN_GROUP);
    	    					
    	    	for(int jaddress=1;jaddress<=rowCountAddressLE;jaddress++ )
    	    	{
    	    	AddressfieldLE= tAddressLE.getString("data_column", jaddress);//Address field Name
    	    	retfieldAddressLE= tAddress.getString(AddressfieldLE, iaddress);//Value for the field that need to be set
    	    	AddressEndurfieldLE=tAddressLE.getString("endur_field_name", jaddress); // Endur field where value need to be set
    	    	reqflag=tAddressLE.getString("required", jaddress); //Check for required flag if field is mandatory to be set
				
				//Loop through to check for Mandatory req flag
    	    	if((retfieldAddressLE== null||(retfieldAddressLE.trim().equalsIgnoreCase("")))&& reqflag.equalsIgnoreCase("Yes"))
    			{
    				String errormsg= "Party Address for LE Endur_field_name: " + AddressEndurfieldLE + " could not be set";
    				Logging.error(errormsg + " \n Couldn't process row no. " + iaddress + " for row_id " + rowid);
    				errorupdate(rowid, errormsg, select);
    				continue first;	
    			}
				
				//Proceed to setting the values
				else
				{
					if(scheck.equalsIgnoreCase("Main")){
						newLegalEntity.getTable("party_address", 1).setInt("default_flag", lrow, Ref.getValue(SHM_USR_TABLES_ENUM.NO_YES_TABLE,"YES"));
						if (AddressEndurfieldLE.equalsIgnoreCase("addr1")|| AddressEndurfieldLE.equalsIgnoreCase("addr2") || AddressEndurfieldLE.equalsIgnoreCase("city")|| AddressEndurfieldLE.equalsIgnoreCase("mail_code")|| AddressEndurfieldLE.equalsIgnoreCase("fax")|| AddressEndurfieldLE.equalsIgnoreCase("phone")) 
				    			{
				    				newLegalEntity.getTable("party_address", 1).setString(AddressEndurfieldLE, lrow, retfieldAddressLE);//To update Party_Address Table inside Party Table
				    				newLegalEntity.getTable("legal_entity",1).setString(AddressEndurfieldLE, 1, retfieldAddressLE);// To update Legal_Entity table inside Party table
				    			}
						else if(AddressEndurfieldLE.equalsIgnoreCase("addr_reference_name")|| AddressEndurfieldLE.equalsIgnoreCase("irs_terminal_num")){
							newLegalEntity.getTable("party_address", 1).setString(AddressEndurfieldLE, lrow, retfieldAddressLE);
						}
				    		else if(AddressEndurfieldLE.equalsIgnoreCase("country"))
				    				{
				    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
				    				 if (retEnum==-1){
				    					continue first;	
				    				 }
				    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retfieldAddressLE));
				    				newLegalEntity.getTable("legal_entity",1).setInt(AddressEndurfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retfieldAddressLE));
				    					}
				    			else if (AddressEndurfieldLE.equalsIgnoreCase("county_id"))
				    					{
				    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
				    				 if (retEnum==-1){
				    					continue first;	
				    				 }
				    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE,lrow, Ref.getValue(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE,retfieldAddressLE));
				    					}
				    			/*else if (AddressEndurfieldLE.equalsIgnoreCase("linked_le_name"))
				    					{
				    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,retfieldAddressLE));
				    					}*/
				    			else if (AddressEndurfieldLE.equalsIgnoreCase("address_type"))
				    					{
				    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
					    				 if (retEnum==-1){
						    					continue first;	
						    				 }
				    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE,retfieldAddressLE));
				    				}
				    			
				    			else if (AddressEndurfieldLE.equalsIgnoreCase("state_id"))
				    					{
				    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.STATES_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
				    				 if (retEnum==-1){
					    					continue first;	
					    				 }
				    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retfieldAddressLE));
				    				newLegalEntity.getTable("legal_entity",1).setInt(AddressEndurfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retfieldAddressLE));
				    					}
				    			else if (AddressEndurfieldLE.equalsIgnoreCase("contact_id"))
				    					{
				    				newLegalEntity.getTable("party_address", 1).setString(AddressEndurfieldLE, lrow, retfieldAddressLE.toString());
				    					}
				    			else if (AddressEndurfieldLE.equalsIgnoreCase("description"))
		    					{
		    				newLegalEntity.getTable("party_address", 1).setString(AddressEndurfieldLE, 1, retfieldAddressLE.toString());
		    					}
					}
					else{
    			if (AddressEndurfieldLE.equalsIgnoreCase("addr1")|| AddressEndurfieldLE.equalsIgnoreCase("addr2") || AddressEndurfieldLE.equalsIgnoreCase("addr_reference_name")|| AddressEndurfieldLE.equalsIgnoreCase("irs_terminal_num") 
    			 || AddressEndurfieldLE.equalsIgnoreCase("city")|| AddressEndurfieldLE.equalsIgnoreCase("mail_code")|| AddressEndurfieldLE.equalsIgnoreCase("fax")|| AddressEndurfieldLE.equalsIgnoreCase("phone")|| AddressEndurfieldLE.equalsIgnoreCase("description")) 
    			{
    				newLegalEntity.getTable("party_address", 1).setString(AddressEndurfieldLE, lrow, retfieldAddressLE);
    			}
    			else if(AddressEndurfieldLE.equalsIgnoreCase("country"))
    					{
    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
   				 if (retEnum==-1){
	    					continue first;	
	    				 }
    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retfieldAddressLE));
    					}
    			else if (AddressEndurfieldLE.equalsIgnoreCase("county_id"))
    					{
    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
      				 if (retEnum==-1){
   	    					continue first;	
   	    				 }
    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE,retfieldAddressLE));
    					}
    			/*else if (AddressEndurfieldLE.equalsIgnoreCase("linked_le_name"))
    					{
    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,retfieldAddressLE));
    					}*/
    			else if (AddressEndurfieldLE.equalsIgnoreCase("address_type"))
    					{
    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
     				 if (retEnum==-1){
  	    					continue first;	
  	    				 }
    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE,retfieldAddressLE));
    				   }
    			
    			else if (AddressEndurfieldLE.equalsIgnoreCase("state_id"))
    					{
    				int retEnum= checkenum(SHM_USR_TABLES_ENUM.STATES_TABLE, retfieldAddressLE, reqflag, AddressEndurfieldLE ,iaddress, rowid, select);
    				 if (retEnum==-1){
 	    					continue first;	
 	    				 }
    				newLegalEntity.getTable("party_address", 1).setInt(AddressEndurfieldLE, lrow, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retfieldAddressLE));
    					}
    			else if (AddressEndurfieldLE.equalsIgnoreCase("contact_id"))
    					{
    				newLegalEntity.getTable("party_address", 1).setString(AddressEndurfieldLE, lrow, retfieldAddressLE.toString());
    					}
				}
    	    	}
    	    	}
    	    	int retval=Ref.updateParty(newLegalEntity);
    	    	
    	    	if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
    				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "\nAdd Address to Legal Entity has failed !"));
    				
    	    	}
    	    	}
    	    	  
    	    String spartyBU=tAddress.getString("linked_bu_name", iaddress);
    	    if(simporttype.equalsIgnoreCase(lebu) || simporttype.equalsIgnoreCase(bu))
			{
    	    newBunit= Ref.retrieveParty(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,spartyBU));
    	    newBunit.getTable("party_address", 1).sortCol("party_address_id");
    	    int brow= newBunit.getTable("party_address", 1).findInt("party_address_id", buid, SEARCH_ENUM.LAST_IN_GROUP);
	    	for(int jaddress=1;jaddress<=rowCountAddressBU;jaddress++ ){
	    		
	    	
	    	AddressfieldBU= tAddressBU.getString("data_column", jaddress);//Address field Name for BU
	    	retfieldAddressBU= tAddress.getString(AddressfieldBU, iaddress);//Value for the field that need to be set for BU			 	
	    	AddressEndurfieldBU=tAddressBU.getString("endur_field_name", jaddress); // Endur field where value need to be set in BU in Party
	    	reqflag=tAddressBU.getString("required", jaddress);
	    	if((retfieldAddressBU== null||(retfieldAddressBU.trim().equalsIgnoreCase("")))&& reqflag.equalsIgnoreCase("Yes"))
			{
	    		String errormsg= "Party Address for BU Endur_field_name: " + AddressEndurfieldBU + " could not be set";
				Logging.error(errormsg + " \n Couldn't process row no. " + iaddress + " for row_id " + rowid);
				errorupdate(rowid, errormsg, select);
				continue first;	
			}
			else
			{	    	
			if(scheck.equalsIgnoreCase("Main")){
				newBunit.getTable("party_address", 1).setInt("default_flag", brow, Ref.getValue(SHM_USR_TABLES_ENUM.NO_YES_TABLE,"YES"));
			
			if (AddressEndurfieldBU.equalsIgnoreCase("addr1")|| AddressEndurfieldBU.equalsIgnoreCase("addr2") || AddressEndurfieldBU.equalsIgnoreCase("city")|| AddressEndurfieldBU.equalsIgnoreCase("mail_code")|| AddressEndurfieldBU.equalsIgnoreCase("fax")|| AddressEndurfieldBU.equalsIgnoreCase("phone")) 
			{
				newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, brow, retfieldAddressBU);
				newBunit.getTable("business_unit",1).setString(AddressEndurfieldBU, 1, retfieldAddressBU);
			}
			else if(AddressEndurfieldBU.equalsIgnoreCase("addr_reference_name")|| AddressEndurfieldBU.equalsIgnoreCase("irs_terminal_num") )
			{
				newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, brow, retfieldAddressBU);
			}
			else if(AddressEndurfieldBU.equalsIgnoreCase("country"))
					{
				int retEnum= checkenum(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
				 if (retEnum==-1){
    					continue first;	
    				 }
				newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retfieldAddressBU));
				newBunit.getTable("business_unit", 1).setInt(AddressEndurfieldBU, 1, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retfieldAddressBU));
					}
			else if (AddressEndurfieldBU.equalsIgnoreCase("county_id"))
					{
				
					int retEnum= checkenum(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
					 if (retEnum==-1){
	    					continue first;	
	    				 }
				newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE,retfieldAddressBU));
					}
			/*else if (AddressEndurfieldBU.equalsIgnoreCase("linked_le_name"))
					{
				newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,retfieldAddressBU));
					}*/
			else if (AddressEndurfieldBU.equalsIgnoreCase("address_type"))
					{
				int retEnum= checkenum(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
				 if (retEnum==-1){
   					continue first;	
   				 }
				newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE,retfieldAddressBU));
					}
			
			else if (AddressEndurfieldBU.equalsIgnoreCase("state_id"))
					{
				int retEnum= checkenum(SHM_USR_TABLES_ENUM.STATES_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
				 if (retEnum==-1){
  					continue first;	
  				 }
				newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retfieldAddressBU));
				newBunit.getTable("business_unit", 1).setInt(AddressEndurfieldBU, 1, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retfieldAddressBU));
					}
			else if (AddressEndurfieldBU.equalsIgnoreCase("contact_id"))
					{
				newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, brow, retfieldAddressBU.toString());
					}
			else if (AddressEndurfieldBU.equalsIgnoreCase("description"))
			{
		newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, 1, retfieldAddressBU.toString());
			}
			}
			
			else{
				if (AddressEndurfieldBU.equalsIgnoreCase("addr1")|| AddressEndurfieldBU.equalsIgnoreCase("addr2") || AddressEndurfieldBU.equalsIgnoreCase("city")|| AddressEndurfieldBU.equalsIgnoreCase("mail_code")|| AddressEndurfieldBU.equalsIgnoreCase("fax")|| AddressEndurfieldBU.equalsIgnoreCase("phone")|| AddressEndurfieldBU.equalsIgnoreCase("description")) 
				{
					newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, brow, retfieldAddressBU);
					
				}
				else if(AddressEndurfieldBU.equalsIgnoreCase("addr_reference_name")|| AddressEndurfieldBU.equalsIgnoreCase("irs_terminal_num") )
				{
					newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, brow, retfieldAddressBU);
				}
				else if(AddressEndurfieldBU.equalsIgnoreCase("country"))
						{
					int retEnum= checkenum(SHM_USR_TABLES_ENUM.COUNTRY_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
					newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.COUNTRY_TABLE,retfieldAddressBU));
					
						}
				else if (AddressEndurfieldBU.equalsIgnoreCase("county_id"))
						{
					int retEnum= checkenum(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
					newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.PS_COUNTY_TABLE,retfieldAddressBU));
						}
				/*else if (AddressEndurfieldBU.equalsIgnoreCase("linked_le_name"))
						{
					newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, 1, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE,retfieldAddressBU));
						}*/
				else if (AddressEndurfieldBU.equalsIgnoreCase("address_type"))
						{
					int retEnum= checkenum(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
					newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_ADDRESS_TYPE_TABLE,retfieldAddressBU));
						}
				
				else if (AddressEndurfieldBU.equalsIgnoreCase("state_id"))
						{
					int retEnum= checkenum(SHM_USR_TABLES_ENUM.STATES_TABLE, retfieldAddressBU, reqflag, AddressEndurfieldBU ,iaddress, rowid, select);
					 if (retEnum==-1){
	  					continue first;	
	  				 }
					newBunit.getTable("party_address", 1).setInt(AddressEndurfieldBU, brow, Ref.getValue(SHM_USR_TABLES_ENUM.STATES_TABLE,retfieldAddressBU));
						}
				else if (AddressEndurfieldBU.equalsIgnoreCase("contact_id"))
						{
					newBunit.getTable("party_address", 1).setString(AddressEndurfieldBU, brow, retfieldAddressBU.toString());
						}
				}
			}
			
	    	}
	    	
			int retvalBU=Ref.updateParty(newBunit);
	    	 
	    	if (retvalBU != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retvalBU, "\nAdd Address to Business Unit has failed !"));
	    	}
			}
	    	
			try
		 	{
         	//update the status of the row in the USER table to update status appropriately
         	Table user_migr_p_addr;
         	user_migr_p_addr= Table.tableNew();
         	// Set the name attribute of the table. This should be the name of the database table
         	user_migr_p_addr.setTableName(select);
           	DBUserTable.load(user_migr_p_addr);
     
         	user_migr_p_addr.sortCol("row_id");
         	int row= user_migr_p_addr.findInt("row_id", rowid, SEARCH_ENUM.LAST_IN_GROUP);
         	if(simporttype.equalsIgnoreCase(lebu)){
         	user_migr_p_addr.setString("new_le_address_id", row, idle);
         	}
         	
         	user_migr_p_addr.setString("new_bu_address_id", row, idbu);
         	user_migr_p_addr.setString("status_flag", row, "UPDATED");
         	user_migr_p_addr.setString("target_status_flag",row , "UPDATED");
         	user_migr_p_addr.setInt("row_type",row , 7);	
         	user_migr_p_addr.setString("status_msg", row, "Successfully UPDATED");
         	user_migr_p_addr.group("row_id");
		
         	int iRC=DBUserTable.update(user_migr_p_addr);

		if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			Logging.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
			}		
		 	}	
			catch(OException Error)
			{
			Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			}
			newLegalEntity.destroy();
			newBunit.destroy();
	    	}
    	    
    	    Table user_migr_p_addr;
         	user_migr_p_addr= Table.tableNew();
         	user_migr_p_addr.setTableName(select);
           	DBUserTable.load(user_migr_p_addr);
         	user_migr_p_addr.viewTable();
         	Logging.info("Completed processing Address for " + rowCountAddress + " number of rows");
         	Logging.close();
    	    
    }
	//Method to check Enum values existing in the Enum Tables
	private int checkenum(SHM_USR_TABLES_ENUM temp, String retfield, String reqflag, String AddressEndurfieldBU, int iaddress, int rowid, String select) throws OException
	{
		SHM_USR_TABLES_ENUM enumTable= temp;
		int ret= Ref.getValue(enumTable,retfield);
		if (ret==-1 && reqflag.equalsIgnoreCase("Yes"))
		{
			String errormsg= " Address Endur_field_name: " + AddressEndurfieldBU + " could not be set";
			Logging.error(errormsg+" \n Couldn't process row no. " + iaddress + " for row_id " + rowid);
			errorupdate(rowid, errormsg, select);
			return ret;	
		}
		else return ret=1;
		
	}
	
	//Update error message and status to User table
	private void errorupdate(int rownum, String msg, String select) throws OException{

		 try
		 	{
       	//update the status of the row in the USER table to +update status appropriately
       	Table user_migr_p_addr;
       	user_migr_p_addr= Table.tableNew();
       	// Set the name attribute of the table. This should be the name of the database table
       	user_migr_p_addr.setTableName(select);
        	DBUserTable.load(user_migr_p_addr);
        	user_migr_p_addr.sortCol("row_id");
       	int row=user_migr_p_addr.findInt("row_id", rownum, SEARCH_ENUM.FIRST_IN_GROUP);
       	
       	user_migr_p_addr.setString("status_msg", row, msg);
       	user_migr_p_addr.setInt("error_flag", row , 1);
		
       	int iRC=DBUserTable.update(user_migr_p_addr);

		if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			Logging.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
		}
				
		}	
		catch(OException Error)
		{
			Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			
		}
		
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

