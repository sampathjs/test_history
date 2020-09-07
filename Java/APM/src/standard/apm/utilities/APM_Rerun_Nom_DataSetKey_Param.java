/*==========================================================================================================================================
File Name:                      APM_Rerun_Nom_DataSetKey_Param.java

Main Script:                    APM_Rerun_DataSetKey.java
Parameter Script:               This is a parameter script

Revision History:
               
Script Description:             This parameter script prompts the user for the list of online APM nom services and 
								allows them to choose to only rerun a subset of the service providers for that service.
                             
Instructions:                   Create an OpenJvs task with this script as the parameter script and APM_Rerun_DataSetKey.java as the main script.

Parameters:                     argt table
Format of argt table:           col name               col type         	description
                                ========               ========         	===========
                                service_name        	 String             List of the service to rerun 
                                simulation_name          String             APM Service simulation name
                                package_name 			 String             Package or list of packages in APM Service
                                dataset_type_name        String             APM service dataset type name
                     			entity_type              String             For this script this will always be DEAL
                                entity_group_id          int                The portfolio ID
                    
============================================================================================================================================*/

package standard.apm.utilities;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.SERVICE_MGR_ENGINES_ENUM;
import com.olf.openjvs.fnd.ServicesBase;


public class APM_Rerun_Nom_DataSetKey_Param implements IScript{
	
	public void execute(IContainerContext context) throws OException {
		
		Table askServiceWindow = null;
		Table askPipelineWindow = null;
		Table pipelineList = null;
		
		try{
			Table argt = context.getArgumentsTable();
			String service = "";
			
			//Create the list of currently online APM services and prompt for the user to select one
			askServiceWindow = createServiceAskWindow();
	        int retVal = Ask.viewTable(askServiceWindow, "Service Selection", "Select Service To Rerun: ");
	        
	        // If retVal = 0 the user did not select a service
	        if(retVal == 1)
	        {
	        	service = askServiceWindow.getTable("return_value", 1).getString(1, 1);
	        }
	        else
	        {
	        	throw new OException("Invalid Service Selected");
	        }
	        
	        //Now gather the other necessary information about this service
	        Table serviceProperties = ServicesBase.getServiceMethodProperties(service, "ApmNomService");

	        if(serviceProperties != null)
	        {
	        	int pFieldLabelColNum = serviceProperties.getColNum("pfield_label");
	        	int pFieldValueColNum = serviceProperties.getColNum("pfield_value");
	        	
	        	/* Retrieve all of the pipelines for that service and prompt 
	        	 * the user to select one or several of them to rerun */
	        	for(int i = 1; i <= serviceProperties.getNumRows(); i++)
	        	{
	        		String label = serviceProperties.getString(pFieldLabelColNum, i);
	        		if(label.equals("Service Provider"))
	        		{
	        			String pipelines = serviceProperties.getString(pFieldValueColNum, i);
	        			askPipelineWindow = createPipelineAskWindow(pipelines);
	        			break;
	        		}
	        	}
	        	
		        retVal = Ask.viewTable(askPipelineWindow, "Service Providers Selection", "Select Service Providers To Rerun: ");
		        
		        //If retVal = 0 or there are no rows in the return table, no pipelines were selected
		        if(retVal == 1 && askPipelineWindow.getTable("return_value", 1).getNumRows() > 0)
		        {
		        	pipelineList = askPipelineWindow.getTable("return_value", 1);
		        }
		        else
		        {
		        	throw new OException("Invalid Service Providers Selected");
		        }
		        
		        /* Add the argt columns needed by APM_Rerun_DataSetKey */
				argt.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
				argt.addCol("simulation_name", COL_TYPE_ENUM.COL_STRING);
				argt.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
				argt.addCol("dataset_type_name", COL_TYPE_ENUM.COL_STRING);
				argt.addCol("entity_type", COL_TYPE_ENUM.COL_STRING); //This is always "NOMINATION"
				argt.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
				
				/* All other required argt fields will be the same for every
				 * row except for the pipeline. Retrieve them first */
				String simulationName = "";
				String packageName = "";
				String datasetTypeName = "";
				
	        	for(int i = 1; i <= serviceProperties.getNumRows(); i++)
	        	{
	        		String label = serviceProperties.getString(pFieldLabelColNum, i);
	        		if(label.equals("Simulation Name"))
	        		{
	        			simulationName = serviceProperties.getString(pFieldValueColNum, i);
	        		}
	        		else if(label.equals("APM Package"))
	        		{
	        			packageName = serviceProperties.getString(pFieldValueColNum, i);
	        		}
	        		else if(label.equals("Dataset Type"))
	        		{
	        			datasetTypeName = serviceProperties.getString(pFieldValueColNum, i);
	        		}
	        	}
	        	
	        	/* Now we know all of our fields, so add rows to argt for every pipeline we are rerunning */
	        	String allPipelines = "";
		        for(int i = 1; i <= pipelineList.getNumRows(); i++)
		        {
		        	String selectedPipeline = pipelineList.getString(1, i);
		        	allPipelines += selectedPipeline + ", ";
		        	populateArgTable(argt, selectedPipeline, service, simulationName, packageName, datasetTypeName);
		        }
		        Ask.ok("Selected service provider(s) " + allPipelines + "will be rerun for service " + service + ".");
	        }
	        else
	        {
	        	throw new OException("No properties could be retrieved for the given service. Exiting.");
	        }
		}
		catch(OException oe)
		{
			OConsole.print(oe.toString());
			Ask.ok("No valid service providers were selected to run. Exiting.");
		}
		finally{
			if(askServiceWindow != null)
			{
				askServiceWindow.destroy();
			}
			if(askPipelineWindow != null)
			{
				askPipelineWindow.destroy();
			}
			if(pipelineList != null)
			{
				pipelineList.destroy();
			}		
		}
			
	}
	
	public Table createServiceAskWindow() throws OException{
		
		Table serviceTable = null;
		Table serviceNameTable = null; //trimmed down table to just one column of service names
		Table askWindow = null;

		serviceTable = Table.tableNew();
		askWindow = Table.tableNew();
		serviceNameTable = Table.tableNew();
		
		serviceTable = ServicesBase.getAppServicesConfiguredForGroupType(SERVICE_MGR_ENGINES_ENUM.APM_NOM_SERVICE_ID);
		serviceNameTable.addCol("Service Name", COL_TYPE_ENUM.COL_STRING);
		
		/* We now have the list of all current APM Services in Services Manager. First
		 * let's eliminate the rows of offline services because only ones that
		 * are currently online should appear as options in the picklist. */
		int serviceIdColNum = serviceTable.getColNum("service_id");
		int serviceNameColNum = serviceTable.getColNum("service_name");
		
		for(int i = serviceTable.getNumRows(); i > 0; i--)
		{
			int serviceId = serviceTable.getInt(serviceIdColNum, i);
			String serviceName = serviceTable.getString(serviceNameColNum, i);
			if(ServicesBase.serviceIsServiceRunning(serviceId) == 1)	
			{
				int nextIndex = serviceNameTable.addRow();
				serviceNameTable.setString("Service Name", nextIndex, serviceName);
			}
		}
		
        Ask.setAvsTable(askWindow, serviceNameTable, "APM Nom Services", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, Util.NULL_TABLE, "Select Online Service", 0);

		if(serviceTable != null)
		{
			serviceTable.destroy();
		}		
		if(serviceNameTable != null)
		{
			serviceNameTable.destroy();
		}	
				
		return askWindow;
		
	}
	
	public Table createPipelineAskWindow(String pipelines) throws OException{
		
		Table pipelineTable = null;
		Table askWindow = null;

		pipelineTable = Table.tableNew();
		askWindow = Table.tableNew();
		
		pipelineTable.addCol("Service Providers", COL_TYPE_ENUM.COL_STRING);
		
		/* The pipeline string has all of the pipelines separated by commas. 
		   create an array of the pipelines to populate the table with */
		String[] pipelinesArray = pipelines.split(","); 		
		for(int i = 0; i < pipelinesArray.length; i++)
		{
			pipelineTable.addRow();
			pipelineTable.setString(1, i+1, pipelinesArray[i]);
		}
		
        Ask.setAvsTable(askWindow, pipelineTable, "Service Providers", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, Util.NULL_TABLE, "Select Portfolio to Rerun", 0);
        
		if(pipelineTable != null)
		{
			pipelineTable.destroy();
		}	

		return askWindow;
	}
	
	public Table populateArgTable(Table argt, String pipeline, String serviceName, 
							      String simulationName, String packageName, String dataSetType) throws OException{
		
		Table pipelineIdQuery = Table.tableNew();
		String[] packageSeparator = packageName.split(",");
		
		try 
		{
			/* For the pipeline that was passed in, find the associated id */
			String queryString = "select pipeline_id from gas_phys_pipelines where pipeline_name = '" + pipeline + "'";
			int retval = DBase.runSql(queryString);
			DBase.createTableOfQueryResults(pipelineIdQuery);
			int id = pipelineIdQuery.getInt(1, 1);
	
			/* If multiple packages are part of this batch, each package is its own row
			 * of the argt table. The packageName string is in the format of "package1,package2,package3".
			 * Split the string on the comma and add an argt row for every individual package found */
			for (int i = 0; i < packageSeparator.length; i++)
			{
				int nextIndex = argt.addRow();
				
				argt.setString("service_name", nextIndex, serviceName);
				argt.setString("simulation_name", nextIndex, simulationName);
				argt.setString("package_name", nextIndex, packageSeparator[i]);
				argt.setString("dataset_type_name", nextIndex, dataSetType);
				argt.setString("entity_type", nextIndex, "NOMINATION"); //This script is only for nominations
				argt.setInt("entity_group_id", nextIndex, id);
				
			}
			
			//argt.viewTable();
			return argt;

		} 
		catch (OException oe) 
		{
			OConsole.print(oe.toString());
		} 
		finally
		{
			if(pipelineIdQuery != null)
			{
				pipelineIdQuery.destroy();
			}
		}
		
		return null;
		
	}
	
	
}
