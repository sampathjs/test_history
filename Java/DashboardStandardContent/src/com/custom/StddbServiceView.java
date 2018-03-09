package com.custom;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.fnd.ConnexUtilityBase;

public class StddbServiceView implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
		
		int debug = 0;
		Table pTable = Util.serviceGetStatusAll();
		Table pTable2 = ConnexUtilityBase.getStatusAll();
		Table returnt = context.getReturnTable();
		int NumRows;
		int Idx;
		int Jdx;
		int ServiceStatusId;
		int GroupStatusId;
		int GroupCount;
		String GroupName;
		String DispatcherName;
		
		
		pTable2.copyRowAddAll(pTable);
		pTable2.destroy();
		
		if(debug != 0)
		   {
		      pTable.viewTable();
		   }
		
		/* BUILD THE TABLE */
		returnt.addCol("service_id",             COL_TYPE_ENUM.COL_INT,    "Service ID");
		returnt.addCol("service_name",           COL_TYPE_ENUM.COL_STRING, "Service Name");
		returnt.addCol("service_status_text",    COL_TYPE_ENUM.COL_STRING, "Service Status Text");
		returnt.addCol("service_status_id",      COL_TYPE_ENUM.COL_INT,    "Service Status");
		returnt.addCol("service_online",         COL_TYPE_ENUM.COL_INT,    "Service Online");
		returnt.addCol("service_offline",        COL_TYPE_ENUM.COL_INT,    "Service Offline");
		returnt.addCol("service_type_text",      COL_TYPE_ENUM.COL_STRING, "Service Type");
		returnt.addCol("service_type",           COL_TYPE_ENUM.COL_INT,    "Service Type ID");
		returnt.addCol("group_name",             COL_TYPE_ENUM.COL_STRING, "Service Group Name");
		returnt.addCol("group_status_text",      COL_TYPE_ENUM.COL_STRING, "Service Group Status Text");
		returnt.addCol("group_status_id",        COL_TYPE_ENUM.COL_INT,    "Service Group Status");
		returnt.addCol("group_online",           COL_TYPE_ENUM.COL_INT,    "Service Group Online");
		returnt.addCol("group_offline",          COL_TYPE_ENUM.COL_INT,    "Service Group Offline");
		returnt.addCol("group_count",            COL_TYPE_ENUM.COL_INT,    "Service Group Count");
		returnt.addCol("dispatcher_name",        COL_TYPE_ENUM.COL_STRING, "Dispatcher Name");
		returnt.addCol("cluster_name",           COL_TYPE_ENUM.COL_STRING, "Cluster Name");
		returnt.addCol("run_site",               COL_TYPE_ENUM.COL_INT,    "Run Site ID");
		returnt.addCol("run_site_name",          COL_TYPE_ENUM.COL_STRING, "Run Site Name");
		
		returnt.select(pTable, "service_id, service_name, wflow_status_text(service_status_text), service_type_text, service_type, service_group_text(group_name), run_site_text(dispatcher_name), run_site, run_site_text(run_site_name)", "service_id GT 0");
		
		NumRows = returnt.getNumRows();
		
		   if(NumRows > 0)
		   {
		      for(Idx = 1; Idx <= NumRows; Idx++)
		      {
		         ServiceStatusId = 0; /* Empty */

		         GroupName      = returnt.getString("group_name",          Idx);      
		         GroupCount     = returnt.getInt("group_count",         Idx);      
		         DispatcherName = returnt.getString("dispatcher_name",     Idx);

		         /* Set the Status based on the Text */
		         if(Str.iEqual(returnt.getString("service_status_text", Idx), "offline") == 1)
		         {
		            ServiceStatusId = 2;
		            returnt.setInt("service_status_id", Idx, ServiceStatusId);
		            returnt.setInt("service_online",    Idx, 0);
		            returnt.setInt("service_offline",   Idx, ServiceStatusId);
		         }
		         else
		         {
		            ServiceStatusId = 1;
		            returnt.setInt("service_status_id", Idx, ServiceStatusId);
		            returnt.setInt("service_online",    Idx, ServiceStatusId);
		            returnt.setInt("service_offline",   Idx, 0);
		         }
		           
		         /* TIDY UP DISPATCHER NAME */
		         if((Str.isEmpty(DispatcherName) == 1) || 
		            (Str.iEqual(DispatcherName, "None") == 1))
		         {
		            returnt.setString("dispatcher_name", Idx, "None");
		         }

		         /* Set the status_id, grp_status_id, grp_count */     
		         for(Jdx = 1; Jdx <= NumRows; Jdx++)
		         {
		            GroupStatusId = returnt.getInt("group_status_id", Jdx);

		            /* SET ALL OF THE GROUP VALUES */
		            if(Str.iEqual(returnt.getString("group_name", Jdx), GroupName) == 1)
		            {
		               /* CHECK THE EXISTING GROUP STATUS */
		               switch(GroupStatusId)
		               {
		               case 0:
		                  returnt.setInt("group_status_id", Jdx,  ServiceStatusId);
		                  break;
		               case 1:
		                  if(ServiceStatusId != 1)
		                     returnt.setInt("group_status_id", Jdx,  3);
		                  break;

		               case 2:
		                  if(ServiceStatusId != 2)
		                     returnt.setInt("group_status_id", Jdx,  3);
		                  break;

		               case 3:
		                  /* NOTHING TO DO ALREADY IN MIXED MODE */
		                  break;

		               }

		               returnt.setInt("group_count", Jdx, GroupCount+1);
		            }
		         }
		      }

		      /* NOW TIDY UP AND POPULATE THE GROUP DATA */
		      for(Idx = 1; Idx <= NumRows; Idx++)
		      {
		          GroupStatusId = returnt.getInt("group_status_id", Idx);
		          switch(GroupStatusId)
		          {
		          case 1: /* All online */
		             returnt.setString("group_status_text", Idx, "All Services Online");
		             returnt.setInt("group_online",  Idx,  1);
		             returnt.setInt("group_offline", Idx,  0);
		             break;

		          case 2: /* All offline */
		             returnt.setString("group_status_text", Idx, "All Services Offline");
		             returnt.setInt("group_online", Idx,   0);
		             returnt.setInt("group_offline", Idx,  2);
		             break;

		          case 3: /* Mixture */
		             returnt.setString("group_status_text", Idx, "Some Services Online");
		             returnt.setInt("group_online", Idx,   3);
		             returnt.setInt("group_offline", Idx,  0);
		             break;
		          }
		       }
		   }




		
		
		
		


	}

}
