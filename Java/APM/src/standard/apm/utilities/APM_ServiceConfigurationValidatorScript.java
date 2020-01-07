package standard.apm.utilities;

/*
 * This Utility OpenJVS script looks at all APM Services and compiles a list of Unique Dataset Keys (Package, Portfolio, Scenario, and Dataset)
 * And displays these in a Table. The script also detects if any APM Service produces Dataset Keys that overlaps with any other APM Service.
 * Duplicates are displayed in a 2nd Table and also indicates the names of the 2 services that overlap.
 * Use the corresponding Task to execute this Utility script.
 */
import java.io.PrintWriter;
import java.io.StringWriter;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class APM_ServiceConfigurationValidatorScript implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
    	Table reportingTable = null;
    	Table allServices = null, serviceProperties = null, scenarios = null;
    	int numAPMServices = 0;
    	boolean haveErrors = false;

		try
    	{
			OConsole.oprint("\n");
			OConsole.message("\n ----- APM_Service Configuration Validator Script Start-----\n\n");

			reportingTable = createReportingTableStructure();

	    	allServices = Util.serviceGetStatusAll();
	    	int numServices = allServices.getNumRows();
	    		    		    	
			for (int row = 1; row <= numServices; row++)
	    	{
	    		if(!allServices.getString("service_group_text", row).equalsIgnoreCase("APM"))
	    			continue;
	    		
	    		if (serviceProperties != null) serviceProperties.destroy();
	    		if (scenarios != null) scenarios.destroy();
	    		
	    		String serviceName = allServices.getString("service_name", row);

	    		serviceProperties = Services.getServiceMethodProperties(serviceName, "ApmService");
	    		int dataset = Services.getPicklistId(serviceProperties, "dataset_type_id");
	    		String simName = Services.getPicklistString(serviceProperties, "simulation_name");
	    		Table packages = Services.getMselPicklistTable(serviceProperties, "package_name");
	    		Table pfolios  = Services.getMselPicklistTable(serviceProperties, "internal_portfolio");

	    		int numPackages = (Table.isTableValid(packages) == 1) ? packages.getNumRows() : 0; 
	    		int numPfolios = (Table.isTableValid(pfolios) == 1) ? pfolios.getNumRows() : 0; 	    		
	    		if (numPackages <= 0 || numPfolios <= 0) {
	    			populateWarningMsg(reportingTable, serviceName, "No Package and/or Portfolio configured");
	    			continue;
	    		}

	    		scenarios = getSimDefScenarios(reportingTable, serviceName, simName);
	    		int numScenarios = (Table.isTableValid(scenarios) == 1) ? scenarios.getNumRows() : 0; 
	    		if (numScenarios <= 0) {
	    			populateWarningMsg(reportingTable, serviceName, "Invalid or Corrupt Simulation Definition '" + simName + "'");
	    			continue;
	    		}

	    		OConsole.oprint("Determining APM Dataset keys for APM Service '" + serviceName + "'\n");
	    		determineDatasetKeysForApmService(reportingTable, serviceName, packages, numPackages, pfolios, numPfolios, scenarios, numScenarios, dataset);	    			    		
	    		numAPMServices++;
	    	}						
    	} catch (Exception e) {
    		haveErrors = true;
			populateErrorMsg(reportingTable, e.getMessage());
			exceptionStackTraceToOConsole(e);
			throw new OException(e);
    	} finally {
    		if (allServices != null) allServices.destroy();
    		if (serviceProperties != null) serviceProperties.destroy();    
    		if (scenarios != null) scenarios.destroy();    
    		
     		if (Table.isTableValid(reportingTable) == 1) {
    			finalizeReportingTable(reportingTable, numAPMServices, haveErrors);
    	    	reportingTable.viewTable();
    			reportingTable.destroy();
    		}
        	OConsole.message("\n----- APM Service Configuration Validator Script End -----\n\n");
    	}
    }

	private static Table createReportingTableStructure() throws OException  
    {
		Table reportingTable = null;
		try {
			reportingTable = Table.tableNew("APM Dataset Keys");
			reportingTable.addCol("unique_dataset_keys", COL_TYPE_ENUM.COL_TABLE, "Unique Datasets");
			reportingTable.addCol("duplicate_dataset_keys", COL_TYPE_ENUM.COL_TABLE, "Duplicate Datasets");
			reportingTable.addCol("message",  COL_TYPE_ENUM.COL_STRING, "Message");
			reportingTable.addCol("errors",  COL_TYPE_ENUM.COL_TABLE, "Warnings and Errors");
			reportingTable.addRow();

			Table uniqueDatasetKeys = Table.tableNew("Unique Datasets");
			uniqueDatasetKeys.addCol("package",   COL_TYPE_ENUM.COL_STRING, "Package");
			uniqueDatasetKeys.addCol("pfolio",    COL_TYPE_ENUM.COL_STRING, "Portfolio");
			uniqueDatasetKeys.addCol("scenario",  COL_TYPE_ENUM.COL_STRING, "Scenario");
			uniqueDatasetKeys.addCol("dataset",   COL_TYPE_ENUM.COL_INT,    "Dataset");
			uniqueDatasetKeys.setColFormatAsRef("dataset", SHM_USR_TABLES_ENUM.APM_DATASET_TYPE_TABLE);
			uniqueDatasetKeys.addCol("service",   COL_TYPE_ENUM.COL_STRING, "APM Service");
			
			Table duplicateDatasetKeys = Table.tableNew("Duplicate Datasets");	
			duplicateDatasetKeys.addCol("service1",  COL_TYPE_ENUM.COL_STRING, "APM Service1");
			duplicateDatasetKeys.addCol("service2",  COL_TYPE_ENUM.COL_STRING, "APM Service2");
			duplicateDatasetKeys.addCol("package",   COL_TYPE_ENUM.COL_STRING, "Package");
			duplicateDatasetKeys.addCol("pfolio",    COL_TYPE_ENUM.COL_STRING, "Portfolio");
			duplicateDatasetKeys.addCol("scenario",  COL_TYPE_ENUM.COL_STRING, "Scenario");
			duplicateDatasetKeys.addCol("dataset",   COL_TYPE_ENUM.COL_INT,    "Dataset");
			duplicateDatasetKeys.setColFormatAsRef("dataset", SHM_USR_TABLES_ENUM.APM_DATASET_TYPE_TABLE);
				    	
			reportingTable.setTable("unique_dataset_keys", 1, uniqueDatasetKeys);
			reportingTable.setTable("duplicate_dataset_keys", 1, duplicateDatasetKeys);
		} catch (Exception e) {
			throw new OException(e);
		}
		
		return reportingTable; 
	}
    
	private static Table getSimDefScenarios(Table reportingTable, String serviceName, String simName) throws OException 
    {
    	Table scenarios = null;
    	Table simulationDef = null;

    	try {
			if(simName.equalsIgnoreCase("None"))
			{
				scenarios = Table.tableNew("scenarios");
				scenarios.addCol("scenario_name", COL_TYPE_ENUM.COL_STRING);
				scenarios.addRow();
				scenarios.setString(1, 1, "Base");
			}
			else
			{
				simulationDef = Sim.loadSimulation(simName);
				if (Table.isTableValid(simulationDef) == 1 && simulationDef.getNumRows() > 0)
					scenarios = simulationDef.getTable("scenario_def", 1);	    				
			}
		} catch (Exception e) {
			throw new OException(e);
		} finally {
			if (simulationDef != null) {
				if (scenarios != null)
					simulationDef.setTable("scenario_def", 1, null); // Unhook scenario before destroying the parent table
				simulationDef.destroy();
			}
		}
	
    	return scenarios;
	}

	private static void determineDatasetKeysForApmService(Table reportingTable, String serviceName, Table packages,
			int numPackages, Table pfolios, int numPfolios, Table scenarios, int numScenarios, int dataset) throws OException 
	{
		for (int packageNum = 1; packageNum <= numPackages; packageNum++)
		{
			for(int pfolioNum = 1; pfolioNum <= numPfolios; pfolioNum++)
			{
				for(int scenNum = 1; scenNum <= numScenarios; scenNum++)
				{
					insertDataset(reportingTable, serviceName, packages.getString(1, packageNum), 
							pfolios.getString(1, pfolioNum), scenarios.getString("scenario_name", scenNum), dataset);
				}
			}
		}	
	}

	private static void insertDataset(Table reportingTable, String serviceName, String packageName, String pfolio, String scenario, int dataset) throws OException
    {  	
    	
    	int insertRow = -1;
    	boolean insert = true;
    	
    	try {
			Table uniqueDatasetKeys = reportingTable.getTable("unique_dataset_keys", 1);
			Table duplicateDatasetKeys = reportingTable.getTable("duplicate_dataset_keys", 1);

			int first = uniqueDatasetKeys.findString("package", packageName, SEARCH_ENUM.FIRST_IN_GROUP);
			if(first > 0)
			{
				int last  = uniqueDatasetKeys.findString("package", packageName, SEARCH_ENUM.LAST_IN_GROUP);
				
				first = uniqueDatasetKeys.findStringRange("pfolio", first, last, pfolio, SEARCH_ENUM.FIRST_IN_GROUP);
				if(first > 0)
				{
					last = uniqueDatasetKeys.findStringRange("pfolio", first, last, pfolio, SEARCH_ENUM.LAST_IN_GROUP);
					
					first = uniqueDatasetKeys.findStringRange("scenario", first, last, scenario, SEARCH_ENUM.FIRST_IN_GROUP);
					if(first > 0)
					{
						last = uniqueDatasetKeys.findStringRange("scenario", first, last, scenario, SEARCH_ENUM.LAST_IN_GROUP);
						
						first =  uniqueDatasetKeys.findIntRange("dataset", first, last, dataset, SEARCH_ENUM.FIRST_IN_GROUP);
						if(first > 0)
						{
							// found a duplicate
							OConsole.oprint("   Duplicate Dataset Key Found: Service1: " + uniqueDatasetKeys.getString("service", first) + ", Service2: " + serviceName + "\n");
							OConsole.oprint("         Package: " + packageName + ", Portfolio: " + pfolio + ", Scenario: " + scenario + ", Dataset: " + Ref.getName(SHM_USR_TABLES_ENUM.APM_DATASET_TYPE_TABLE, dataset) + "\n");
						    					
							int row = duplicateDatasetKeys.addRow();
							duplicateDatasetKeys.setString("service1",   row, uniqueDatasetKeys.getString("service", first));
							duplicateDatasetKeys.setString("service2",   row, serviceName);
							duplicateDatasetKeys.setString("package",    row, packageName);
							duplicateDatasetKeys.setString("pfolio",     row, pfolio);
							duplicateDatasetKeys.setString("scenario", row, scenario);
							duplicateDatasetKeys.setInt("dataset",  row, dataset);
							insert = false;
						}
					}
				}
			}
			
			if(insert)
			{
				insertRow = -first;
				uniqueDatasetKeys.insertRowBefore(insertRow);
				uniqueDatasetKeys.setString("service", insertRow, serviceName);
				uniqueDatasetKeys.setString("package", insertRow, packageName);
				uniqueDatasetKeys.setString("pfolio",  insertRow, pfolio);
				uniqueDatasetKeys.setString("scenario",insertRow, scenario);
				uniqueDatasetKeys.setInt("dataset",    insertRow, dataset);
			}
		} catch (Exception e) {
			throw new OException(e);
		}
    }

	private void finalizeReportingTable(Table reportingTable, int numAPMServices, boolean haveErrors) throws OException {
		String baseMsg = "Scanned " + numAPMServices + " APM Services.";
		String enhMsg;

		if (haveErrors)
		{
			enhMsg = baseMsg + " Errors occured - Check 'Warnings and Errors' table.";
		}
		else 
		{
			Table duplicateDatasetKeys = reportingTable.getTable("duplicate_dataset_keys", 1);
			int numDuplicates = duplicateDatasetKeys.getNumRows();
			if (numDuplicates > 0) {
				enhMsg = baseMsg + " Found " + numDuplicates + " duplicate Dataset Keys.";
				duplicateDatasetKeys.group("package, pfolio, scenario, dataset");	
			}
			else {
				enhMsg = baseMsg + " Services are configured without any duplicate Dataset Keys.";
			}
		}

		reportingTable.setString("message",  1, enhMsg);
		OConsole.oprint("\nSummary: " + enhMsg + "\n");
	}

	private static void populateWarningMsg(Table reportingTable, final String serviceName, final String warnMsg) throws OException {
		populateErrorTableAndOlisten(reportingTable, "WARNING: Skipping APM Service '" + serviceName + "': " + warnMsg);
	}

	private static void populateErrorMsg(Table reportingTable, String errorMsg) throws OException {
		populateErrorTableAndOlisten(reportingTable, "ERROR: " + errorMsg + " - Please check Olisten and Error Logs.");
	}

	private static void populateErrorTableAndOlisten(Table reportingTable, String msg) throws OException {
		if (Table.isTableValid(reportingTable) == 1) 
		{
			Table errorMsgTable = reportingTable.getTable(4, 1);
			if (Table.isTableValid(errorMsgTable) == 0) {
				errorMsgTable = createErrorTableStructure();
				reportingTable.setTable(4, 1, errorMsgTable);
			}
			errorMsgTable.setString("message", errorMsgTable.addRow(), msg);
		}
		OConsole.message("\n--- " + msg + "\n");
	}

	private static Table createErrorTableStructure() throws OException  
    {
		Table errorTable = Table.tableNew("Warnings and Errors");
		errorTable.addCol("message", COL_TYPE_ENUM.COL_STRING, "Message");
		return errorTable;
    }
	
	private static void exceptionStackTraceToOConsole(Exception ex) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		String sStackTrace = sw.toString(); // stack trace as a string
		OConsole.message(sStackTrace);
	}

}
