package com.jm.eod;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.ODateTime;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.embedded.application.ScriptCategory;


import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.Generic })
public class RestartServices_P extends AbstractGenericScript {

	
	@Override
	public Table execute(Context context, ConstTable table) {
		
	    Table data = context.getTableFactory().createTable();
		int intRet=1;
		try { 

			Logging.init(this.getClass(), "", "");

			Logging.info("START RestartServices");
					
            data.addColumn("run_restart", EnumColType.String);
            data.addColumn("err_msg", EnumColType.String);
            data.addRow();
            data.setString("run_restart", 0, "Y");
            
            String strErrMsg="";
            
            Person p = context.getUser();
            int intPersonId = p.getId();
            
            
			// Check EOD permission
            String strSQL = "select *   ";
            strSQL += "from ";
            strSQL += "personnel p inner join  users_to_groups ug on ug.user_number = p.id_number ";
            strSQL += "where p.id_number = "  + intPersonId + " and ug.group_number = 20005 ";
            Table tblResults = context.getIOFactory().runSQL(strSQL);
            
    		if (null==tblResults || tblResults.getRowCount()<1){
    			
    			strErrMsg +=  "User must have EOD group assigned to run.\n";

    			data.setString("run_restart", 0, "N");
    			data.setString("err_msg", 0, strErrMsg);
    			intRet =0;
    			
    		}
            
    		ODateTime dtServerDate = ODateTime.getServerCurrentDateTime();
    		dtServerDate.getTime();
    		
			// After 930 PM
    		int intNowSecsPastMidnight = ODateTime.getServerCurrentDateTime().getTime();
    		
    		ConstRepository constRep = new ConstRepository("EOD", "RestartServices");
    		int intCutOffHour = constRep.getIntValue("CutOffHour");
    		double dblCutOffSecsPastMidnight = 60*60*intCutOffHour;
    		
    		if(intNowSecsPastMidnight < dblCutOffSecsPastMidnight ){
    			strErrMsg +=  "This task can only be run after " + intCutOffHour+ ".\n";
    			
    			data.setString("run_restart", 0, "N");
    			data.setString("err_msg", 0, strErrMsg);
    			intRet =0;

    		}
    		
    		if(intRet == 1){

    			intRet = Ask.okCancel("This will restart services in Endur, Continue?");
    			
    	        if (intRet == 0)
    	        {
    	        	Logging.info("User pressed cancelled, exiting.");
    	        	strErrMsg +=  "User pressed cancelled, exiting. \n";
    	        	data.setString("run_restart", 0, "N");
    	        	
    	        }
    		}else{
    			Ask.ok(strErrMsg);
    		}
			
		} catch (Exception e) {
			Logging.info(e.toString());
		}
		
		Logging.info("END RestartServices");
		
		return data;
	}


}
