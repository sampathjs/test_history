/*$Header: //BatchMigrationScript.java, v1 13 Nov 2015 $*/
/*
File Name:                       Migr_Batch.java

Report Name:                     None

Output File Name:                Migr_Batch.log in the folder which is defined in Constants Repository for "Migration" or located in location outdir/reports/

Revision History:                Nov 13, 2015 - First version
								 Mar 15, 2016 - commented in code line to set Batch Brand:
					             batch1.setValue(EnumNominationFieldId.CommodityBrand, tbldata.getString("CommodityBrand", idata));
Main Script:                     This script
Parameter Script:                None.          
Display Script:                  None.

Script Description:              This script allows to import of Batches from user table.
								 Tables used are USER_migr_batches and USER_migr_op_batchdata_fields.
								 USER_migr_batches - this table contains the data coming from external system.
								 USER_migr_op_batchdata_fields - contains mapping between the above table to Endur Field names for different import types
								 This script will read from the above tables and creates Batches and links them to COMM-STOR.

Assumptions:                    Constants Repository is configured as part of Migration Manager..

Instructions:                    None.     

Uses EOD Results?                NA

EOD Results that are used:       None

When can the script be run?      Anytime

Recommended Script Category? 	N/A
 */

/*
 * History:
 * xxxx-xx-xx	V1.0	    		-	Initial Version
 * 2018-02-02	V1.1	sma  		-	Add info fields "Bar Sizes" , "Gms Weight Entered" to delivery tickets
 */
package com.customer.migr_batch;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.market.EnumIdxGroup;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.EnumVolume;
import com.olf.openrisk.scheduling.EnumVolumeQuantityFieldId;
import com.olf.openrisk.scheduling.IndexSubGroup;
import com.olf.openrisk.scheduling.VolumeQuantity;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.ReferenceObject;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.DeliveryTickets;
import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.PlannedMeasure;
import com.olf.openrisk.trading.PlannedMeasures;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.Generic })
public class Migr_Batch extends AbstractGenericScript {
	ConstRepository constRep;
	@Override
	public Table execute(Context context, ConstTable table) {
		// TODO Auto-generated method stub
		context.getDebug().printLine("Starting OC BatchDemoScript_1");
		
		//Variable declaration
		int idata, idatrownum, imap, imaprownum, irowid, imigrrownum, imapmeasure, imapmeasurerownum, idatmeasurerownum, controwid, ret;
		String sDatafield, sEndurfield, svalue, smapquery, errormsg;
		StringBuffer sb=new StringBuffer("");
		StringBuffer sbmeasure=new StringBuffer("");
		String squery, squerymeasure, simporttype;
		String batmeacont="batch+measures+container";
		String batmea="batch+measures";
		String cont="container";
		int controw=0, loopret=0;
	    
		//Constants Repository Statics
    	String MIGRATION = "Migration";
    	String MIGRATIONSUBCONTEXT 	= ""; 
    	
    	//Constants Repository init
		try {
			constRep = new ConstRepository(MIGRATION, MIGRATIONSUBCONTEXT);
			initPluginLog(); //Plug in Log init
			PluginLog.info("Started process for Batch Migration :");
				}
		catch (OException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		// Get a TableFactory
		TableFactory tf = context.getTableFactory();
		// Use the factory to create the tables
		Table tbldata = tf.createTable();
		Table tblmigr= tf.createTable();
		Table tblmap= tf.createTable();
		Table tblmapmeasure=tf.createTable();
		Table tbldatameasure=tf.createTable();
		Table tbltype=tf.createTable();
		Table tbltemp=tf.createTable();
		Table tblabtran=tf.createTable();
		
		IOFactory io = context.getIOFactory();
		StaticDataFactory sdf = context.getStaticDataFactory();
		//Get the data from USER_migr_batches
		String SqlString = "select * from USER_migr_batches where row_type='2'";
		try
		{
			tblmigr=io.runSQL(SqlString);
		}
		catch (Exception Error) 
		{
			PluginLog.error("\n Check sqlCommand: " + SqlString+ "\n Unable to load User_migr_batches table");
			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		}
		imigrrownum= tblmigr.getRowCount();
		tblmigr.sort("row_id");
		
		//Get the mapping table and create new data table with mapped columns for Batch and container fields
		String SqlMap = "select * from USER_migr_op_batchdata_fields where data_object='Batch' OR data_object='Container' OR data_object='Warehouse'";
		try
		{
			tblmap=io.runSQL(SqlMap);
		}
		catch (Exception Error) 
		{
			PluginLog.error("\n Check sqlCommand: " + SqlMap +"\n Unable to load Batch and container mapped data table USER_migr_op_batchdata_fields");
			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		}
		imaprownum= tblmap.getRowCount();
		
		for(imap=0; imap<imaprownum;imap++){
			sDatafield=tblmap.getString("data_column", imap);
        	sEndurfield=tblmap.getString("endur_field_name", imap);
        	if(imap==0){
        	squery="Select row_id, " + sDatafield + " as " + sEndurfield;
        	 sb.append(squery);
        	}
        	else{
        		squery=", " + sDatafield + " as " + sEndurfield;
           	 	sb.append(squery);
        	}
        	
      	}
		String smap= sb.toString() + " from USER_migr_batches where row_type='2';";
				
		try
		{
			tbldata=io.runSQL(smap);
		}
		catch (Exception Error) 
		{
			PluginLog.error("\n Check sqlCommand: " + smap +"\n Unable to load Batch and container mapped data onto the table USER_migr_batches");
			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		}
		idatrownum= tbldata.getRowCount();
		tbldata.sort("row_id");
		
		//Get the mapping table and create new data table with mapped columns for Measures fields
		String SqlMapMeasure = "select * from USER_migr_op_batchdata_fields where data_object='Measures'";
		try
		{
			tblmapmeasure=io.runSQL(SqlMapMeasure);
		}
		catch (Exception Error) 
		{
			PluginLog.error("\n Check sqlCommand: " + SqlMapMeasure +"\n Unable to load Measures mapped data table USER_migr_op_batchdata_fields");
			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		}
		imapmeasurerownum= tblmapmeasure.getRowCount();
		
		
		for(imapmeasure=0; imapmeasure<imapmeasurerownum;imapmeasure++){
			sDatafield=tblmapmeasure.getString("data_column", imapmeasure);
        	sEndurfield=tblmapmeasure.getString("endur_field_name", imapmeasure);
        	if(imapmeasure==0){
        	squerymeasure="Select row_id, " + sDatafield + " as '" + sEndurfield + "'";
        	 sbmeasure.append(squerymeasure);
        	}
        	else{
        		squerymeasure=", " + sDatafield + " as '" + sEndurfield + "'";
           	 	sbmeasure.append(squerymeasure);
        	}
        	
      	}
		String smapmeasure= sbmeasure.toString() + " from USER_migr_batches where row_type='2';";
		
		try
		{
			tbldatameasure=io.runSQL(smapmeasure);
		}
		catch (Exception Error) 
		{
			PluginLog.error("\n Check sqlCommand: " + smapmeasure+"\n Unable to Measures mapped data onto the table USER_migr_batches");
			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		}
		idatmeasurerownum= tbldatameasure.getColumnCount();
		tbldatameasure.sort("row_id");
		
		//measurement_type table for Measure lookup value
		String stype=  "Select * from measurement_type;";
		try
		{
			tbltype=io.runSQL(stype);
		}
		catch (Exception Error) 
		{
			PluginLog.error("\n Check sqlCommand: " + stype +"\n Measurement Types table was unable to load ");
			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		}
		
	String session = "starting";
		first:
		for(idata=0; idata<imigrrownum; idata++){
		 
			int id=idata+1;
			PluginLog.info("Processing Batches " + id + "/" + imigrrownum + " number of rows");
			irowid=tbldata.getInt("row_id", idata);
			simporttype=tblmigr.getString("import_type", idata);
			
			//If only Container then go back to looping as COntainers are taken care in Batches+Measures+Containers
			if(simporttype.equalsIgnoreCase(cont)){
				continue first;
			}
			
	       String contnum=tbldata.getString("NumberContainers", idata);
	       String batnum=tbldata.getString("CommodityBatchNumber", idata);
	       	      	       
	       try{
	       int errorret= errorcheck(tblmap,  imaprownum, tbldata, idata, irowid, simporttype, context);
	       if (errorret==-1){
	    	   continue first;
	       }
	       }
	       catch (OException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	       String type= tbldata.getString("CategoryId", idata);
	       int mtype= tbltype.find(2, type, 0);
	       String name= tbltype.getString("name", mtype);	
			
	        session = "Creating Batch 1";

            Batch batch1;
            batch1 = context.getSchedulingFactory().createBatch();
            
            batch1.setValue(EnumNominationFieldId.MovementDate, tbldata.getString("MovementDate", idata));
            batch1.setValue(EnumNominationFieldId.CategoryId, tbldata.getString("CategoryId", idata));
            batch1.setValue(EnumNominationFieldId.MeasureGroupId, tbldata.getString("MeasureGroupId", idata));
            batch1.setValue(EnumNominationFieldId.CommodityBatchNumber, tbldata.getString("CommodityBatchNumber", idata)); 
            batch1.setValue(EnumNominationFieldId.CommodityForm, tbldata.getString("CommodityForm", idata));
            batch1.setValue(EnumNominationFieldId.CommodityBrand, tbldata.getString("CommodityBrand", idata));
            batch1.setValue(EnumNominationFieldId.CountryOfOrigin, tbldata.getString("CountryOfOrigin", idata));
            batch1.setValue(EnumNominationFieldId.BestAvailableVolumeGrossQuantity, tbldata.getString("GrossQuantity", idata));
            batch1.setValue(EnumNominationFieldId.BestAvailableVolumeNetQuantity, tbldata.getString("NetQuantity", idata));
            batch1.setValue(EnumNominationFieldId.Unit, tbldata.getString("Unit", idata));
            
            
            
            if(simporttype.equalsIgnoreCase(batmeacont)){
            batch1.setValue(EnumNominationFieldId.NumberContainers, tbldata.getString("NumberContainers", idata));
            batch1.setValue(EnumNominationFieldId.ContainerType, tbldata.getString("ContainerType", idata));
            }
            
            
            session = "Modifying existing Batch 1 measure";
            PlannedMeasures plannedMeasures1;
            plannedMeasures1 =  batch1.getBatchPlannedMeasures();
            PlannedMeasure plannedMeasure1;
                       
                 
            session = "Adding measures to Batch 1"; 
            
            loop:
                for(int i=1; i<idatmeasurerownum; i++){
               try{  
                String field= tbldatameasure.getColumnName(i);
                ret=tbltype.find(1, field, 0);
                String val= tbldatameasure.getString(i, idata);
                if (val==null || "".equals(val.trim())|| ret==-1 || field.equalsIgnoreCase(name)){
                	continue loop;
                }
                  	java.util.Iterator<PlannedMeasure> it = plannedMeasures1.iterator();
                    while (it.hasNext())
                    {
                        plannedMeasure1 = it.next();
                        String s;
                        if (( s=plannedMeasure1.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType)).matches(field))
                        {
                        	//plannedMeasure1.setValue(EnumPlannedMeasureFieldId.Unit, "ppm");
                            plannedMeasure1.setValue(EnumPlannedMeasureFieldId.LowerValue, val);
                            continue loop;
                        }
         
                }
 
                         plannedMeasure1 = plannedMeasures1.addItem();
                         plannedMeasure1.setValue(EnumPlannedMeasureFieldId.Unit, "ppm");
                         plannedMeasure1.setValue(EnumPlannedMeasureFieldId.MeasurementType, field);
                         plannedMeasure1.setValue(EnumPlannedMeasureFieldId.LowerValue, val);
                              
                }
           
           catch (Exception Error) 
   		{
        	   PluginLog.error(tbldatameasure.getColumnName(i));
        	   PluginLog.error("\n Error: " + Error.getLocalizedMessage());
   			continue loop;
   		}
            
            
            }
            if (simporttype.equalsIgnoreCase(batmeacont)){
            String sel= sb.toString() + " from USER_migr_batches where row_type='2' AND sadbtn='"+ batnum +"'";
            try
       		{
       			tbltemp=io.runSQL(sel);
       		}
       		catch (Exception Error) 
       		{
       			PluginLog.error("\n Check sqlCommand: " + sel);
       			PluginLog.error("\n Error: " + Error.getLocalizedMessage());
       		}
            controw= tbltemp.getRowCount();
           
            
           // Get DeliveryTicket collection
           session = "Adding containers to Batch 1";
           DeliveryTickets deliveryTickets;
           deliveryTickets =  batch1.getBatchContainers();
           
           // Add container 1 to the collection
           DeliveryTicket deliveryTicket;
           
           for (int j=0; j<controw; j++){
           deliveryTicket = deliveryTickets.addItem();
           deliveryTicket.setValue(EnumDeliveryTicketFieldId.Id, tbltemp.getString("Id", j));
           deliveryTicket.setValue(EnumDeliveryTicketFieldId.Volume, tbltemp.getString("Volume", j));
           deliveryTicket.setValue(EnumDeliveryTicketFieldId.GrossVolume, tbltemp.getString("GrossVolume", j));
           deliveryTicket.setValue(EnumDeliveryTicketFieldId.SealNumber, tbltemp.getString("SealNumber", j));
           deliveryTicket.setValue(EnumDeliveryTicketFieldId.ContainerType, tbltemp.getString("ContainerType", j));
           deliveryTicket.getField("Bar Sizes").setValue(tbltemp.getString("BarSizes", j)); //add info field "Bar Sizes" to delivery ticket
           deliveryTicket.getField("Gms Weight Entered").setValue(tbltemp.getString("GmsWeightEntered", j)); //add info field "Gms Weight Entered" to delivery ticket

           controwid= tbltemp.getInt("row_id", j); 
           errormsg="Succeded as part of Batches+Measures+Containers";
           try{
               //update the status of the row in the USER table to update status appropriately
                 com.olf.openjvs.Table user_migr_batches;
              	user_migr_batches= com.olf.openjvs.Table.tableNew();
              	// Set the name attribute of the table. This should be the name of the database table
              	user_migr_batches.setTableName("USER_migr_batches");
               DBUserTable.load(user_migr_batches);
               user_migr_batches.sortCol("row_id");
              	int row= user_migr_batches.findInt("row_id", controwid, SEARCH_ENUM.LAST_IN_GROUP);
              	user_migr_batches.setString("status_flag", row, "BOOKED");
               	user_migr_batches.setString("target_status_flag",row , "BOOKED");
               	user_migr_batches.setInt("row_type",row , 6);
               	user_migr_batches.setString("status_msg", row, errormsg);
               	user_migr_batches.group("row_id");
      		
               	int iRC=DBUserTable.update(user_migr_batches);
               	if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
               		PluginLog.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
          		}
            }
            catch(OException Error)
      		{
      			
      				PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
				
      			}		
            }           	
            }
 
            session = "Saving Batch 1";
            batch1.save();
            int batchId1 = -1;
            int nomId1 = -1;
            batchId1 = batch1.getBatchId();
            nomId1 = batch1.getId();
            String batchDescription1;
            batchDescription1 = batch1.toString();
            context.getDebug().printLine("Batch 1 created: " + batchDescription1);
           
            batch1.dispose();

            try{
                //update the status of the row in the USER table to update status appropriately
                  com.olf.openjvs.Table user_migr_batches;
               	user_migr_batches= com.olf.openjvs.Table.tableNew();
               	// Set the name attribute of the table. This should be the name of the database table
               	user_migr_batches.setTableName("USER_migr_batches");
                DBUserTable.load(user_migr_batches);
                user_migr_batches.sortCol("row_id");
               	int row= user_migr_batches.findInt("row_id", irowid, SEARCH_ENUM.LAST_IN_GROUP);
               	user_migr_batches.setInt("batch_id", row, batchId1  );
               	user_migr_batches.setInt("delivery_id", row, nomId1  );
               	user_migr_batches.setString("status_flag", row, "BOOKED");
               	user_migr_batches.setString("target_status_flag",row , "BOOKED");
               	user_migr_batches.setInt("row_type",row , 6);
               	user_migr_batches.setString("status_msg", row, "Successfully Booked");
               	user_migr_batches.group("row_id");
      		
               	int iRC=DBUserTable.update(user_migr_batches);
               	if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
               		PluginLog.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
          		}
            }
            catch(OException Error)
      		{
      			
      				PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
				
      		}
            
            batch1 = context.getSchedulingFactory().retrieveBatchByDeliveryId(nomId1);
            
            try {
				int link=commstorlink(tblabtran, batch1,tbldata, idata, context, io);
				if (link==-1){
					
					errormsg="Warning: Batch Saved succesfully. COMM-STOR could not be set to the mentioned Refernece";
		    		PluginLog.error( errormsg+" \n Couldn't process row no. " + idata + " for row_id " + irowid +" for COMM-STOR linkage");
					errorupdate(irowid, errormsg);
					continue first;
				}
			} catch (OException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            	
            PluginLog.info("Processed Batches " + id + "/" + imigrrownum + " number of rows");	
         
		}                       
		
	 try{
         //update the status of the row in the USER table to update status appropriately
           com.olf.openjvs.Table user_migr_batches;
        	user_migr_batches= com.olf.openjvs.Table.tableNew();
        	// Set the name attribute of the table. This should be the name of the database table
        	user_migr_batches.setTableName("USER_migr_batches");
        	DBUserTable.load(user_migr_batches);
        	user_migr_batches.sortCol("row_id");
        	user_migr_batches.viewTable();        	
        
     }
     catch(OException Error)
		{
			
				PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
		}
	 
	 PluginLog.info("Completed Processing Batches for " + imigrrownum + " rows");
		return null;
	
	}
	//Update error message and status to User table
    public void errorupdate(int rownum, String msg) throws OException{

		 try
		 	{
        	//update the status of the row in the USER table to +update status appropriately
        	com.olf.openjvs.Table user_migr_batches;
        	user_migr_batches= com.olf.openjvs.Table.tableNew();
        	// Set the name attribute of the table. This should be the name of the database table
        	user_migr_batches.setTableName("USER_migr_batches");
         	DBUserTable.load(user_migr_batches);
         	user_migr_batches.sortCol("row_id");
        	int row=user_migr_batches.findInt("row_id", rownum, SEARCH_ENUM.LAST_IN_GROUP);
        	
        	user_migr_batches.setString("status_msg", row, msg);
        	user_migr_batches.setInt("error_flag", row , 1);
		
        	int iRC=DBUserTable.update(user_migr_batches);

		if (iRC != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			PluginLog.error("The Script failed for the following reason: "	+ DBUserTable.dbRetrieveErrorInfo (iRC, "DBUserTable.saveUserTable () failed"));
		}
				
		}	
		catch(OException Error)
		{
			PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			
		}
		
	}
    
    //Error check method to validate the data provided in the User table
    public int errorcheck(Table tblmap, int imaprownum, Table tbldata, int idata, int irowid, String simporttype, Context context)throws OException{
    	int i; 
		int ret=0;
		
    	try{
    		for(i=0; i<imaprownum; i++)
    		{
    		String field= tblmap.getString("endur_field_name", i);
    		String reqflag= tblmap.getString("required", i);
    		String value= tbldata.getString(field, idata);
    		String dataobject=tblmap.getString("data_object", i);
    		
    		//To check for Null or empty fields
    		if(simporttype.equalsIgnoreCase("batch+measures+container")&&(value== null||(value.trim().equals("")))&& reqflag.equals("Yes"))
			{
	    		String errormsg="Endur_field_name: " + field + " could not be set";
	    		PluginLog.error( errormsg+" \n Couldn't process row no. " + idata + " for row_id " + irowid);//Need to replace Plugin.log.error
				errorupdate(irowid, errormsg);
				ret=-1;
					break;			
			}
    		
    		if(simporttype.equalsIgnoreCase("batch+measures")&&(value== null||(value.trim().equals("")))&& reqflag.equals("Yes")&& dataobject.equalsIgnoreCase("Batch"))
			{
	    		String errormsg="Endur_field_name: " + field + " could not be set";
	    		PluginLog.error( errormsg+" \n Couldn't process row no. " + idata + " for row_id " + irowid);//Need to replace Plugin.log.error
				errorupdate(irowid, errormsg);
				ret=-1;
					break;			
			}
    		//To check for values existing in the Enum table
    		if (field.equalsIgnoreCase("CategoryId")&& reqflag.equals("Yes")){
    			 ret= enumcheck(field , SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE, value, idata, irowid); 
    			 if (ret==-1){
       				 break;
    			 }
    		}
    		else if(field.equalsIgnoreCase("MeasureGroupId")&& reqflag.equals("Yes")){
   			 ret= enumcheck(field , SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE, value, idata, irowid);   
   			 if (ret==-1){
   				 break;
   			 }
    		}
    		else if(field.equalsIgnoreCase("ContainerType")&& reqflag.equals("Yes") && simporttype.equalsIgnoreCase("batch+measures+container")){
      			 ret= enumcheck(field , SHM_USR_TABLES_ENUM.COMMODITY_CONTAINER_TYPE_TABLE, value, idata, irowid);   
      			 if (ret==-1){
       				 break;
       			 }
       		}
    		else if(field.equalsIgnoreCase("CommodityForm")&& reqflag.equals("Yes")){
     			 ret= enumcheck(field , SHM_USR_TABLES_ENUM.COMMODITY_FORM_TABLE, value, idata, irowid);   
     			 if (ret==-1){
      				 break;
      			 }
      		}
    		else if(field.equalsIgnoreCase("CountryOfOrigin")&& reqflag.equals("Yes")){
     			 ret= enumcheck(field , SHM_USR_TABLES_ENUM.COUNTRY_TABLE, value, idata, irowid);   
     			 if (ret==-1){
      				 break;
      			 }
      		}
    	/*	else if(field.equalsIgnoreCase("CommodityBrand")&& reqflag.equals("Yes")){
     			 ret= enumcheck(field ,  SHM_USR_TABLES_ENUM.COMMODITY_BRAND_NAME_TABLE, value, idata, irowid);   
     			 if (ret==-1){
      				 break;
      			 }
      		} */
    		else if(field.equalsIgnoreCase("Unit")&& reqflag.equals("Yes")){
    			 ret= enumcheck(field , SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, value, idata, irowid);   
    			 if (ret==-1){
     				 break;
     			 }
     		}
   		}
    		
    	}
    	catch(OException Error)
		{
    		PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			
		}
		return ret;
    	
    }
    
    //Method to check whether the value exists in Enum table
    public int enumcheck(String field, SHM_USR_TABLES_ENUM y, String value, int idata, int irowid)throws OException
    {
    	
    	try{
    	int ret= Ref.getValue(y, value);
    	//OConsole.oprint(ret);
           if ("".equals(ret) || ret==-1){
           	String errormsg="Endur_field_name: " + field + " could not be set";
           	PluginLog.error( errormsg+" \n Couldn't process row no. " + idata + " for row_id " + irowid);//Need to replace Plugin.log.error
				errorupdate(irowid, errormsg);
				return ret;
				
           }
    	}
    	catch(OException Error)
		{
    		PluginLog.error("The Script failed for the following reason: "	+ Error.getLocalizedMessage());
			
		}
		 return 1;  
    }	
    
    //Method to link a batch to COMM-STOR deal
    public int commstorlink(Table tblabtran, Batch batch1,Table tbldata, int idata, Context context, IOFactory io)throws OException{
    	
    	  String sqlabtran= "Select * from ab_tran where ins_type=" + EnumInsType.CommStorage.getValue();
    	  try
  		{
  			 tblabtran=io.runSQL(sqlabtran);
  		}

  		catch (Exception Error) 
  		{
  			PluginLog.error("\n Check sqlCommand: " + sqlabtran);
  			PluginLog.error("\n Error: " + Error.getLocalizedMessage());

  		}
  		
   	  try{
          String reference= tbldata.getString("Reference", idata);
          tblabtran.sort("deal_tracking_num", false);
          int findref=tblabtran.find(11, reference, 0);
          
          if("".equals(findref) || findref==-1){
        	  
        	  PluginLog.error("Unable to link COMM STOR with Reference: " + reference);
        	  return -1;
          }
          else{
        	  
              int dealNumber= tblabtran.getInt("deal_tracking_num", findref);
          Transaction tranStorage = 
          		context.getTradingFactory().retrieveTransactionByDeal(dealNumber);

          batch1.assignBatchToStorage(tranStorage);
          batch1.save();
          }
   	  }
   	  catch(Exception Error) {
   		
		PluginLog.error("\n Error: " + Error.getLocalizedMessage());
		return -1;
   		  
   	  }
    	return 1;
    }

    //Initiate plug in logging
		private void initPluginLog() throws OException {
			
		try {
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", this.getClass()
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("errorLogDir", "");

		

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


