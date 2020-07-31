/*$Header: //ReferenceDataMigrationScript.java, v1 15 Oct 2015 $*/
/*
File Name:                       Migr_RefData_Parties.java

Report Name:                     None

Output File Name:                Migr_RefData_Parties.log in the folder which is defined in Constants Repository for "Migration"

Revision History:                Oct 15, 2015 - First version

Main Script:                     This script
Parameter Script:                None.          
Display Script:                  None.

Script Description:              This script allows to import Party Group, Legal Entity and Business Units from Migration Manager user tables.
								 Tables used are USER_migr_p_pg_le_bu and USER_migr_op_refdata_fields.
								 USER_migr_p_pg_le_bu - this table contains the data coming from external system.
								 USER_migr_op_refdata_fields - contains mapping between the above table to Endur Field names for different import types
								 This script will read from the above tables and creates Party Group, LE and BU and links them. In the process, it also 
								 updates Party Info, Int_Ext1, Int_Ext2, Int_Ext3 and links BU and LE.

Assumptions:                     Party Info mapping is already completed in Admin Manager and Party Info.
								 Constants Repository is configured as part of Migration Manager..

Instructions:                    None.     

Uses EOD Results?                NA

EOD Results that are used:       None

When can the script be run?      Anytime

Recommended Script Category? 	N/A
 */


package com.customer.migr_refdata_updates;



import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import java.util.*;

public class Migr_RefData_Parties_Updates implements IScript
{
	ConstRepository constRep;
	@Override
    public void execute(IContainerContext context) throws OException
    {

    	//Define all the variables used in the script
            int iPGLoop,jLE,jBU, rowCountpg, rowCountfieldsBU, rowPartyInfoLE, rowPG ;
            int retval;
            int iPG;
    		int irowLE, itypeIdLE, imultiSelect;
    		int irowBU, itypeIdBU, imultiSelectBU, iLERow=0, iBURow=0;
    		int ret;
            
            String sqlUserpg, sqlUserfields, fieldName, retfieldvalue,sqlUserfieldsBU, buParty="", leParty="", pgParty="";
            String sqlLEPartyInfo,  fieldNameBU, retfieldvalueBU, PartyInfoLEfieldName,retfieldvalueLEPartyInfo, sfieldName, sInfofieldName,sInfofieldNameBU;
            String retfieldvaluepg="";
            String retfieldvalueExtInt="";
            String BUName = "";
            String LEName = "";
            String sImportType = "";
            String sqlPartInfo;
            String sEndurLEName = "";
            String sEndurBUName = "";
            String pg="PG+LE+BU";
            String pgbu="PG+BU";
            String lebu="LE+BU";
            String le="LE";
            String bu="BU";
            String reqflagpg, reqflagle,reqflagbu;
            String del;

            Table argt = context.getArgumentsTable(); 
            String select= argt.getString("Table", 1);
           
            
          //Constants Repository Statics
        	String MIGRATION = "Migration";
        	String MIGRATIONSUBCONTEXT 	= "";   
        	
        	
            Table tPartyGroup=Table.tableNew();
            Table tUserTblFieldsPG= Table.tableNew();
            Table tUserTblFieldsBU=Table.tableNew();
            Table tLEPartyInfo= Table.tableNew();
            Table tbldelivery=Table.tableNew();
            Table intable, table;
            
            
           //Constants Repository init
			constRep = new ConstRepository(MIGRATION, MIGRATIONSUBCONTEXT);
			initLogging(); //Plug in Log init
			Logging.info("Started process for Reference Data Migration for Party Group, Legal Entity & Business Unit:");
            
        
			//Get all the required data from USER table to be imported
            sqlUserpg="select * from " + select + " where row_type='2'";
                   
        	try 
    		{
        		DBaseTable.execISql(tPartyGroup, sqlUserpg);
        		tPartyGroup.sortCol("row_id");
        		
    		}
    		catch (OException Error)
    		{
    			Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    			throw new OException(Error); 
    		}
    		           	
            rowCountpg=tPartyGroup.getNumRows();
           
          //Get mapping information from mapping table in USer Tables. This maps input table to Endur field names
            //This loop is for Party Group related fields
    		   sqlUserfields="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND DATA_OBJECT=('Party Group')";
    		   
    		  	try 
    			{
    		  		DBaseTable.execISql(tUserTblFieldsPG, sqlUserfields);
    			}
    			catch (OException Error)
    			{
    				Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    				throw new OException(Error); 
    			}
    		  	rowPG=tUserTblFieldsPG.getNumRows();
    			
    	     //Get mapping information from mapping table in USer Tables. This maps input table to Endur field names
               //This loop is for Business Unit related fields	
    			sqlUserfieldsBU="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND DATA_OBJECT=('BU') AND UPPER(data_category)=('PARTY_DATA')";
    			
    			try 
    			{
    				DBaseTable.execISql(tUserTblFieldsBU, sqlUserfieldsBU);	
    			}
    			catch (OException Error)
    			{
    				Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    				throw new OException(Error); 
    			}
    			
    			//Get mapping information from mapping table in USer Tables. This maps input table to Endur field names
                //This loop is for Legal Entity related fields
    			sqlLEPartyInfo="Select * from USER_migr_op_refdata_fields where UPPER(Active)=('YES') AND DATA_OBJECT=('LE') AND UPPER(data_category)=('PARTY_DATA')";		
    			try 
    			{
    				DBaseTable.execISql(tLEPartyInfo, sqlLEPartyInfo);	
    			}
    			catch (OException Error)
    			{
    				Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    				throw new OException(Error); 
    			}
    			    			
    			//Get data from party info types which is configured in Endur already. 
    			 Table PartyInfoDetail= Table.tableNew();
    		     sqlPartInfo="select * from party_info_types"; 
    		        
    		        try 
    				{
    		        	DBaseTable.execISql(PartyInfoDetail, sqlPartInfo);
    				}
    				catch (OException Error)
    				{
    					Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    					throw new OException(Error); 
    				}
    		       del="select * from delivery_code"; 
    		        
    		        try 
    				{
    		        	DBaseTable.execISql(tbldelivery, del);
    				}
    				catch (OException Error)
    				{
    					Logging.error("The Script failed for the following reason: "	+ Error.getMessage());
    					throw new OException(Error); 
    				}
    		      
    	              
    		//Party Group - Start looping through the Party Group data got from import table
    		 first:
    		for (iPGLoop = 1; iPGLoop <=rowCountpg ; iPGLoop++) {
    			
    			Logging.info("Processing Party Group " + iPGLoop + "/" + rowCountpg + " number of rows");
    			sImportType = tPartyGroup.getString("import_type", iPGLoop);
    			int rowid= tPartyGroup.getInt("row_id",iPGLoop);  
    			
    			try
    			{
    			for(iPG=1;iPG<=rowPG;iPG++)
    			{
    			fieldName= tUserTblFieldsPG.getString("data_column", iPG); //PG Field Name
    			retfieldvalue= tPartyGroup.getString(fieldName, iPGLoop);	 //Value for the field that need to be set			
				sfieldName=tUserTblFieldsPG.getString("endur_field_name", iPG); // Endur field where value need to be set
				reqflagpg=tUserTblFieldsPG.getString("required", iPG); //Check for required flag if field is mandatory to be set
				if((retfieldvalue== null|| retfieldvalue.trim().equalsIgnoreCase(""))&& reqflagpg.equalsIgnoreCase("Yes"))
				{
					String errormsg= "Party Group Endur_field_name: " + sfieldName + " could not be set";
					Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
					errorupdate(rowid, errormsg, select);
					continue first;
				}
				else
				{
				if (sfieldName.equalsIgnoreCase("short_name"))
				{
					retfieldvaluepg=retfieldvalue;
				}
					
				else if (sfieldName.equalsIgnoreCase("int_ext"))
				{
					retfieldvalueExtInt= retfieldvalue;
				}
				}
				
    			}
    			String retrievebu= tPartyGroup.getString("business_unit", iPGLoop );
    			String retrievele=tPartyGroup.getString("legal_entity", iPGLoop );
    				//Set intable for processing within the plug in to store the required data
                intable = Table.tableNew("New Parties");
                intable.addCol("party_name", COL_TYPE_ENUM.COL_STRING);
                intable.addCol("party_class", COL_TYPE_ENUM.COL_INT);
                
                //Indicate if LE or BU or LE+BU or PG+LE+BU are to be created
                if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu)){
                	intable.addNumRows(2);
                	intable.setInt("party_class", 1, 0);      //new legal entity
                	intable.setInt("party_class", 2, 1);      //new business unit
                	intable.setString("party_name", 1, retrievele);
                	intable.setString("party_name", 2, retrievebu);
                }
                else if (sImportType.equalsIgnoreCase(le)){
                	intable.addNumRows(1);
                 	intable.setInt("party_class", 1, 0);      //new legal unit
                 	intable.setString("party_name", 1, retrievele);
                }
                else if (sImportType.equalsIgnoreCase(bu)|| sImportType.equalsIgnoreCase(pgbu)){
                	intable.addNumRows(1);
                 	intable.setInt("party_class", 1, 1);      //new business unit
                 	intable.setString("party_name", 1, retrievebu);
                }

                table = Table.tableNew("Party table used later for import");
                
                //***********************************--------------------
                
              //Set group names for BU and LE
                if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu)){
                	table.setString("group_name", 1, retfieldvaluepg); //new legal entity
            		table.setString("group_name", 2, retfieldvaluepg);//new business unit   
                }
                else if (sImportType.equalsIgnoreCase(le) || (sImportType.equalsIgnoreCase(bu))|| sImportType.equalsIgnoreCase(pgbu)){
                	table.setString("group_name", 1, retfieldvaluepg); //assign party group name
                }
                
                
    			
                retval = RefBase.exportParties(intable, table); // this will create table with certain structure for you to import later
                if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
                	Logging.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo(retval, "\nREF_ExportParties() has failed !"));
                }
        		
        		//----------------------------------------------------------------------------------------------------------------------
        		//------------------------------------------------------------------------------------------------------------------
        		//Start setting field values for Legal Entity Row
                
                if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu)|| sImportType.equalsIgnoreCase(le)){
                	iLERow = 1;
                
    			
		                table.setInt("int_ext", iLERow, Ref.getValue(SHM_USR_TABLES_ENUM.INTERNAL_EXTERNAL_TABLE, retfieldvalueExtInt)); 
		    			table.setInt("party_status", iLERow, 1);						
		    			rowPartyInfoLE= tLEPartyInfo.getNumRows();
		    			table.getTable("party_info", iLERow).clearRows();
		    			
		    			//Loop through all the LE fields got from mapping table
		    			for(jLE=1; jLE<= rowPartyInfoLE; jLE++){
		    				
		    				PartyInfoLEfieldName= tLEPartyInfo.getString("data_column", jLE); //LE Field Name
		    				retfieldvalueLEPartyInfo= tPartyGroup.getString(PartyInfoLEfieldName, iPGLoop);				
		    				sInfofieldName=tLEPartyInfo.getString("endur_field_name", jLE);
		    				reqflagle=tLEPartyInfo.getString("required", jLE);
		    				
		    				
		    				if((retfieldvalueLEPartyInfo== null|| retfieldvalueLEPartyInfo.trim().equalsIgnoreCase(""))&& reqflagle.equalsIgnoreCase("Yes"))
		    				{
		    					String errormsg= "Legal Entity Endur_field_name: " + sInfofieldName + " could not be set";
		    					Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
		    					errorupdate(rowid, errormsg, select);
		    					continue first;
		    				}
		    				else
		    				{
		    				table.getTable("party_info", 2).addRow();
		    								
		    				if (sInfofieldName.equalsIgnoreCase("short_name") || sInfofieldName.equalsIgnoreCase("long_name"))
		    				{
		    					if (sInfofieldName.equalsIgnoreCase("short_name"))
		    					{
		    						LEName = retfieldvalueLEPartyInfo;
		    					}	
		    					table.setString(sInfofieldName, iLERow, retfieldvalueLEPartyInfo);
		    				}
		    					
		    				else if (sInfofieldName.equalsIgnoreCase("int_ref1")|| sInfofieldName.equalsIgnoreCase("int_ref2") || sInfofieldName.equalsIgnoreCase("int_ref3"))
		    				{
		    					table.getTable("legal_entity",iLERow).addRow();
		    					table.getTable("legal_entity",iLERow).setString(sInfofieldName, 1, retfieldvalueLEPartyInfo);
		    				}
		    				
		    				else
		    				{
		    					//party info table details
		    					PartyInfoDetail.sortCol("type_name");				
		    	        		irowLE= PartyInfoDetail.findString("type_name", sInfofieldName, SEARCH_ENUM.LAST_IN_GROUP);
		    	        		
		    	        		if (irowLE >=0) 
		    	        				{
		    			        		itypeIdLE= PartyInfoDetail.getInt("type_id",irowLE);
		    							imultiSelect=PartyInfoDetail.getInt("multi_select_flag", irowLE );
		    												
		    							if (imultiSelect==0) //if not multi select, then directly assign
		    							{
		    								table.getTable("party_info", iLERow).addRow();
		    								int iLEPartyInfoRows = table.getTable("party_info", 1).getNumRows();
		    								
		    								table.getTable("party_info", iLERow).setInt("type_id", iLEPartyInfoRows, itypeIdLE);
		    								table.getTable("party_info", iLERow).setString("value", iLEPartyInfoRows, retfieldvalueLEPartyInfo);
		    								
		    							}
		    							else // loop through all the values from csv and create multiple rows
		    							{
		    								String[] result = retfieldvalueLEPartyInfo.split(",");
		    							      for (int xlength=0; xlength<result.length; xlength++)
		    							      {
		    							    	  String splitStringLE= result[xlength];
		    							    	  ret= Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, splitStringLE);
			    			    					if (ret==-1 && reqflagle.equalsIgnoreCase("Yes"))
			    			    					{
			    			    						String errormsg= "Legal Entity Endur_field_name: " + sInfofieldName + " could not be set";
			    				    					Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
			    				    					errorupdate(rowid, errormsg, select);
			    				    					continue first;	
			    			    					}
		    							    	  table.getTable("party_info", iLERow).addRow();
		    							    	  table.getTable("party_info", iLERow).setInt("type_id", xlength+1, itypeIdLE);
		    							    	  table.getTable("party_info", iLERow).setInt("value", xlength+1,Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, splitStringLE));
		    							
		    							      }
		    							} 
		    					}
		    				
		    				}
		    			}
		    			}
                }
    			//------------------------------------------------------------------------------------------------------------------
    			//------------------------------------------------------------------------------------------------------------------
    			//start setting field values for Business Unit
                
                
                if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu)|| sImportType.equalsIgnoreCase(bu)|| sImportType.equalsIgnoreCase(pgbu)){
                	
                	if (sImportType.equalsIgnoreCase(bu)|| sImportType.equalsIgnoreCase(pgbu)){
                		iBURow = 1;
                	}
                	else if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu)) {
                		iBURow = 2;
                	}
                
		    			table.setInt("int_ext", iBURow, Ref.getValue(SHM_USR_TABLES_ENUM.INTERNAL_EXTERNAL_TABLE, retfieldvalueExtInt)); 												
		    			table.setInt("party_status", iBURow, 1);
		    			table.getTable("party_function",iBURow).clearRows();
	    				table.getTable("party_delivery_code",iBURow).clearRows();
	    				table.getTable("party_info",iBURow).clearRows();
		    								
		    			rowCountfieldsBU= tUserTblFieldsBU.getNumRows();
		    			
		    			//Loop through all BU related mapping fields
		    			for(jBU=1; jBU<= rowCountfieldsBU; jBU++){
		    				
		    				fieldNameBU= tUserTblFieldsBU.getString("data_column", jBU);
		    				retfieldvalueBU= tPartyGroup.getString(fieldNameBU, iPGLoop);				
		    				sInfofieldNameBU=tUserTblFieldsBU.getString("endur_field_name", jBU);
		    				reqflagbu=tUserTblFieldsBU.getString("required", jBU);
		    				
		    				if((retfieldvalueBU == null||(retfieldvalueBU.trim().equalsIgnoreCase("")))&& reqflagbu.equalsIgnoreCase("Yes"))
		    				{
		    					String errormsg= "Business Unit Endur_field_name: " + sInfofieldNameBU + " could not be set";
		    					Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
		    					errorupdate(rowid, errormsg, select);
		    					continue first;	
		    				}
		    				else
		    				{
		    				table.getTable("party_info", iBURow).addRow();
		    				
		    				if (sInfofieldNameBU.equalsIgnoreCase("short_name")|| sInfofieldNameBU.equalsIgnoreCase("long_name")) 
		    				{
		    					if (sInfofieldNameBU.equalsIgnoreCase("short_name"))
		    					{
		    					  BUName = retfieldvalueBU;
		    					}
		    					table.setString(sInfofieldNameBU, iBURow, retfieldvalueBU);
		    				}
		    				else if(sInfofieldNameBU.equalsIgnoreCase("int_ref1")|| sInfofieldNameBU.equalsIgnoreCase("int_ref2")|| sInfofieldNameBU.equalsIgnoreCase("int_ref2"))
		    				{
		    					table.getTable("business_unit",iBURow).addRow();
		    					table.getTable("business_unit",iBURow).setString(sInfofieldNameBU, 1, retfieldvalueBU);
		    				}
		    		
		    				else if (sInfofieldNameBU.equalsIgnoreCase("function_type"))
		    				{
		    					String[] resultFunction = retfieldvalueBU.split(", ");
							      for (int xlength=0; xlength<resultFunction.length; xlength++)
							      {
							    String splitString= resultFunction[xlength];
		    					table.getTable("party_function",iBURow).addRow();
		    					ret= Ref.getValue(SHM_USR_TABLES_ENUM.FUNCTION_TYPE_TABLE, splitString);
		    					if (ret==-1 && reqflagbu.equalsIgnoreCase("Yes"))
		    					{
		    						String errormsg= "Business Unit Endur_field_name: " + sInfofieldNameBU + " could not be set";
			    					Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
			    					errorupdate(rowid, errormsg, select);
			    					continue first;	
		    					}
		    					table.getTable("party_function",iBURow).setInt(sInfofieldNameBU, xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.FUNCTION_TYPE_TABLE, splitString));
		    					
		    					 }
							  }
		    				else if (sInfofieldNameBU.equalsIgnoreCase("CHIPS")|| sInfofieldNameBU.equalsIgnoreCase("CHAPS")|| sInfofieldNameBU.equalsIgnoreCase("SWIFT")|| sInfofieldNameBU.equalsIgnoreCase("ABA")){
		    					if(retfieldvalueBU== null||retfieldvalueBU.trim().equalsIgnoreCase("")){
		    						
		    					}
		    					else{
		    					table.getTable("party_delivery_code",iBURow).insertRowBefore(1);
		    					int retdel= Ref.getValue(SHM_USR_TABLES_ENUM.DELIVERY_CODE_TABLE,sInfofieldNameBU);
		    					if (retdel==-1 && reqflagbu.equalsIgnoreCase("Yes"))
		    					{
		    						String errormsg= " Business Unit Endur_field_name: " + sInfofieldNameBU + " could not be set";
		    						Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
		    						errorupdate(rowid, errormsg, select);
		    					}
		    					table.getTable("party_delivery_code",iBURow).setInt("delivery_type", 1,retdel);
		    					table.getTable("party_delivery_code",iBURow).setString("code", 1, retfieldvalueBU);
		    					}
		    				}
		    				
		    				else
		    				{
		    					//party info setting
		    				PartyInfoDetail.sortCol("type_name");
		            		irowBU= PartyInfoDetail.findString("type_name", sInfofieldNameBU, SEARCH_ENUM.LAST_IN_GROUP);
		    		        		
		            		if (irowBU >=0){
		            				itypeIdBU= PartyInfoDetail.getInt("type_id",irowBU);
		    						imultiSelectBU=PartyInfoDetail.getInt("multi_select_flag", irowBU );
		    	
		    						if (imultiSelectBU==0) //if not multi select, then directly assign
		    						{
		     							table.getTable("party_info", iBURow).addRow();
		    							//int iBUPartyInfoRows = table.getTable("party_info", 2).getNumRows();
		    							
		    							table.getTable("party_info", iBURow).setInt("type_id", jBU, itypeIdBU);
		    							table.getTable("party_info", iBURow).setString("value", jBU, retfieldvalueBU);
		    						}
		    						else // else loop from csv and create multiple rows for party info
		    						{
		    							String[] result = retfieldvalueBU.split(",");
		    						      for (int xlength=0; xlength<result.length; xlength++)
		    						      {
		    						    	  String splitStringBU= result[xlength];
		    						    	  ret= Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, splitStringBU);
		    			    					if (ret==-1 && reqflagbu.equalsIgnoreCase("Yes"))
		    			    					{
		    			    						String errormsg= "Business Unit Endur_field_name: " + sInfofieldNameBU + " could not be set";
		    				    					Logging.error(errormsg+" \n Couldn't process row no. " + iPGLoop + " for row_id " + rowid);
		    				    					errorupdate(rowid, errormsg, select);
		    				    					continue first;	
		    			    					}
		    						    	  table.getTable("party_info", iBURow).addRow();
		    						    	  table.getTable("party_info", iBURow).setInt("type_id", xlength+1, itypeIdBU);
		    						    	  table.getTable("party_info", iBURow).setInt("value", xlength+1, Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, splitStringBU));
		    						    	 
		    						      }
		    						}
		            		}
		    			}
		    			}
		    			}
                }
    				//------------------------------------------------------------------------------------------------------------------
    				//------------------------------------------------------------------------------------------------------------------
                              
                if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu)){
        		  	table.setString("def_bunit_name", 1, BUName); //link the LE to BU
                    table.setInt("def_bunit_flag", 1, 1);

                    table.setString("def_legal_name", 2, LEName); //link the BU to LE
                    table.setInt("def_legal_flag", 2, 1);                	
                }
                else if (sImportType.equalsIgnoreCase(bu)){
                	sEndurLEName = tPartyGroup.getString("legal_entity", iPGLoop);	
                	if (sEndurLEName.trim().equalsIgnoreCase("")) {
                		// Do Nothing. No need to link
                	}	
                	else {
                		table.setString("def_legal_name", iBURow, sEndurLEName); //link the BU to LE
                		table.setInt("def_legal_flag", iBURow, 1);
                	}
                }
                else if (sImportType.equalsIgnoreCase(le)){
                	sEndurBUName = tPartyGroup.getString("business_unit", iPGLoop);	
                	if (sEndurBUName.trim().equalsIgnoreCase("")) {
                		// Do Nothing. No need to link
                	}	
                	else {
                		table.setString("def_legal_name", iLERow, sEndurBUName); //link the LE to BU
                		table.setInt("def_legal_flag", iLERow, 1);                	
                	}
                }
               
                try{
                RefBase.importPartyTable(table, 1); 
                }
                catch(OException Error){
                	String errormsg=  Error.getLocalizedMessage();
       				Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage() + " for row_id " + rowid);
       				errorupdate(rowid, errormsg, select);
       				continue first;
                }
              
                int buId= table.getInt("party_id", iBURow);
                int leId= table.getInt("party_id", iLERow);
                int pgId=table.getInt("group_id", iBURow);
               
                String sqlpg="Select short_name from party_group where group_id="+ pgId;
                
                Table temppg= Table.tableNew();
                try 
	    		{
	    	  		DBaseTable.execISql(temppg, sqlpg);
	    		}
	    		catch (OException Error)
	    		{
	    		throw new OException(Error); 
	    		}
	         	pgParty= temppg.getString(1, 1);
                
	         	
                Table tempbu= Table.tableNew();
                tempbu =  Ref.retrieveParty(buId);
                buParty=tempbu.getString("short_name", 1);
                
                if (leId!=0){
                Table temple= Table.tableNew();
                temple =  Ref.retrieveParty(leId);
                leParty=temple.getString("short_name", 1);
                }
                
                table.destroy();      
    			
                try
   			 	{
                	//update the status of the row in the USER table to +update status appropriately
                	Table user_migr;
                	user_migr= Table.tableNew();
                	// Set the name attribute of the table. This should be the name of the database table
                	user_migr.setTableName(select);
                 	DBUserTable.load(user_migr);
                	user_migr.sortCol("row_id");
                	int row=user_migr.findInt("row_id", rowid, SEARCH_ENUM.LAST_IN_GROUP);
                	
                	user_migr.setString("new_pg_name", row, pgParty);
            		user_migr.setInt("new_pg_id", row, pgId);
                	if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu) || sImportType.equalsIgnoreCase(le)){
                		user_migr.setString("new_le_name", row, leParty);
                		user_migr.setInt("new_le_id", row, leId);
                	}
                	if (sImportType.equalsIgnoreCase(pg) || sImportType.equalsIgnoreCase(lebu) || sImportType.equalsIgnoreCase(bu)|| sImportType.equalsIgnoreCase(pgbu)){
                		user_migr.setString("new_bu_name", row, buParty);
                		user_migr.setInt("new_bu_id", row, buId);
                	}
                	user_migr.setString("status_flag", row, "UPDATED");
                	user_migr.setString("target_status_flag", row , "UPDATED");
                	user_migr.setInt("row_type", row , 7);
                	user_migr.setString("status_msg", row, "Successfully Updated");
                	user_migr.group("row_id");
   			
                	int iRC=DBUserTable.update(user_migr);
   			
   			
   			if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
   				String errormsg= DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed");
   				Logging.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
   				errorupdate(rowid, errormsg, select);
				continue first;
   			}
   			
   			
   			}	
   			catch(OException Error)
   			{
   				String errormsg=  Error.getLocalizedMessage();
   				Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage() + " for row_id " + rowid);
   				errorupdate(rowid, errormsg, select);
   				continue first;
   			}
    				
    			}	
                catch (OException Error)
    			{
    				String errormsg=  Error.getLocalizedMessage();
    				Logging.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage() + " for row_id " + rowid);
    				errorupdate(rowid, errormsg, select);
    				continue first;
    			}	 
    				
        }
    		        Logging.info("Completed processing Party Groups for " + rowCountpg + " number of rows");	
    		        Logging.close();
    		tPartyGroup.destroy();
    		tUserTblFieldsPG.destroy();	
    		tLEPartyInfo.destroy();
    		tUserTblFieldsBU.destroy();
    		
    		Table user_migr;
    		user_migr= Table.tableNew();
        	// Set the name attribute of the table. This should be the name of the database table
    		user_migr.setTableName(select);
         	DBUserTable.load(user_migr);
         	user_migr.viewTable();
    	}
	
		//Update error message and status to User table
		private void errorupdate(int rownum, String msg, String select) throws OException{

			 try
			 	{
             	//update the status of the row in the USER table to +update status appropriately
             	Table user_migr;
             	user_migr= Table.tableNew();
             	// Set the name attribute of the table. This should be the name of the database table
             	user_migr.setTableName(select);
              	DBUserTable.load(user_migr);
             	user_migr.sortCol("row_id");
             	int row=user_migr.findInt("row_id", rownum, SEARCH_ENUM.LAST_IN_GROUP);
             	
             	user_migr.setString("status_msg", row, msg);
             	user_migr.setInt("error_flag", row , 1);
			
             	int iRC=DBUserTable.update(user_migr);

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
  		private void initLogging() throws OException {

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


