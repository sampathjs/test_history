package com.custom;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.Workflow;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class StddbWorkflowList implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
					
		Table pTable = Workflow.getStatusAll();
		Table pJobs  = Util.NULL_TABLE;
		Table returnt = context.getReturnTable();
		int NumRows;
		int Idx;
		int NumJobRows;
		int Jdx;
		int TaskRow;  
		int JobRow;
		int ServiceType;
		  
		/* CREATE THE WORKFLOW TABLE */
		returnt.addCol("workflow_name",      COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("workflow_status",    COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("workflow_start",     COL_TYPE_ENUM.COL_DATE_TIME);
		returnt.addCol("workflow_end",       COL_TYPE_ENUM.COL_DATE_TIME);
		returnt.addCol("workflow_run_site",  COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("workflow_jobs",      COL_TYPE_ENUM.COL_INT);
		returnt.addCol("job_sequence",       COL_TYPE_ENUM.COL_INT);
		returnt.addCol("job_name",           COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("job_status",         COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("job_type",           COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("job_start",          COL_TYPE_ENUM.COL_DATE_TIME);
		returnt.addCol("job_end",            COL_TYPE_ENUM.COL_DATE_TIME);
		returnt.addCol("service_type",       COL_TYPE_ENUM.COL_INT);
		   
		NumRows = pTable.getNumRows();
		for(Idx = 1; Idx <= NumRows; Idx++)
		{
			/* ADD TASK ROW */
			TaskRow = returnt.addRow();
			returnt.setString("workflow_name",       TaskRow, pTable.getString("name",         Idx)); 
			returnt.setString("workflow_status",     TaskRow, pTable.getString("status",       Idx)); 
			returnt.setDateTime("workflow_start",      TaskRow, pTable.getDateTime("start_time",   Idx)); 
			returnt.setDateTime("workflow_end",        TaskRow, pTable.getDateTime("end_time",     Idx)); 
			returnt.setString("workflow_run_site",   TaskRow, pTable.getString("run_site",     Idx)); 
			returnt.setInt("workflow_jobs",       TaskRow, 0);
			ServiceType =  Str.strToInt( pTable.getString("service_type",Idx) );
			returnt.setInt("service_type",        TaskRow, ServiceType); 

		         
			pJobs = pTable.getTable("jobs", Idx);
			NumJobRows = pJobs.getNumRows();
			if(NumJobRows > 0)
			{

				returnt.setInt("workflow_jobs",       TaskRow, NumJobRows);

				for(Jdx = 1; Jdx <= NumJobRows; Jdx++)
				{ 
					JobRow = returnt.addRow();

					/* COPY THE TASK ROW DATA */
					returnt.setString("workflow_name",       JobRow, returnt.getString("workflow_name", TaskRow)); 
					returnt.setString("workflow_status",     JobRow, returnt.getString("workflow_status", TaskRow)); 
		            returnt.setDateTime("workflow_start",      JobRow, returnt.getDateTime("workflow_start",TaskRow)); 
		            returnt.setDateTime("workflow_end",        JobRow, returnt.getDateTime("workflow_end",        TaskRow)); 
		            returnt.setString("workflow_run_site",   JobRow, returnt.getString("workflow_run_site",   TaskRow)); 
		            returnt.setInt("workflow_jobs",       JobRow, NumJobRows);
		            returnt.setInt("service_type",        JobRow, ServiceType); 


		            /* COPY THE JOB ROW DATA */
		            returnt.setInt("job_sequence",        JobRow, pJobs.getInt("sequence",     Jdx));
		            returnt.setString("job_name",            JobRow, pJobs.getString("name",         Jdx));
		            returnt.setString("job_status",          JobRow, pJobs.getString("status",       Jdx));
		            returnt.setString("job_type",            JobRow, pJobs.getString("module_type",  Jdx));
		            returnt.setDateTime("job_start",           JobRow, pJobs.getDateTime("start_time",   Jdx)); 
		            returnt.setDateTime("job_end",             JobRow, pJobs.getDateTime("end_time",     Jdx)); 

		         }
		      }
		   }
		    
		// TABLE_ViewTable(returnt);

		pTable.destroy();
	}

}

	
