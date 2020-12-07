/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;
import java.util.Date;
import java.util.Calendar;

import standard.apm.ads.ADSException;
import standard.apm.ads.ADSInterface;
import standard.apm.ads.Factory;
import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_TYPE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.DEBUG_LEVEL_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

// All code in this file used to exist in GatherServiceStatusJVS.

/**
 *************************************************************************** 
 * 
 * Copyright 2008 Open Link Financial, Inc. *
 * 
 * ALL RIGHTS RESERVED *
 * 
 * ***************************************************************************
 * 
 * @author kdonepud
 * 
 *         Description: 
 *         This script will be run on APM start or when a service config changes.
 * 
 *         //Dataset Key for APM Map.put("dataset_type_id", 1); Map.put("scenario_id", 2); Map.put("entity_group_id", 5); Map.put("package", "GAS");
 * 
 */
public class ADS_GatherServiceStatus {

	///
	//private IGridCache dataSourceCache = null;
	private Object dataSourceCache = null;
	
	///
	//private IGridCache datasetMessagesCache = null;
	private Object datasetMessagesCache = null;
	
	///
	private static final String DATA_SOURCE_DETAILS_CACHE_NAME = "APM_Data_Source_Details";
	
	///
	private static final String DATASET_MESSAGES_CACHE_NAME = "APM_Dataset_Messages";
	
	///
	private static final String DATASET_STATUS_CACHE_NAME = "APM_Dataset_Status";
	
	///
	//private static final int WAIT_INDEFINITELY = -1;
	
	///
	private static final double DOUBLE_ERROR = 1.0E+32;
	
	///
	private static final double DOUBLE_BLANK = 1.0E-32;

	private ADSInterface s_ads = null;
	
	/// constructor.
	public ADS_GatherServiceStatus() throws OException
	{
		try {
			s_ads = Factory.getADSImplementation();
		} catch (ADSException e) {
			print("ADS_GatherServiceStatus static constructor, ADSException " + stackTraceToString(e));
		} catch (InstantiationException e) {
			print("ADS_GatherServiceStatus static constructor, InstantiationException " + stackTraceToString(e));			
		} catch (IllegalAccessException e) {
			print("ADS_GatherServiceStatus static constructor, IllegalAccessException " + stackTraceToString(e));			
		} catch (ClassNotFoundException e) {
			print("ADS_GatherServiceStatus static constructor, ClassNotFoundException " + stackTraceToString(e));			
		}
	}
	
	/// Delegates to the specific implementation which is loaded by the factory	 
	public void execute() throws OException {

	   print("Starting to gather Service Status Info ...");

	   Table serviceStatus = null;
	   
	   if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
		   print("before Gathering Service Status");
	   
	   serviceStatus = gatherServiceStatus();

	   if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
		   print("after Gathering Service Status");

	   if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
		   print("before updating the caches");
	   
	   updateCaches(serviceStatus);
	   
	   if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
		   print("after updating the caches");

	   serviceStatus.destroy();

	   print("Finished gathering Service Status Info ...");
   }
   
   ///
   private static void print(String message) {
   	
   	try {
	   OConsole.oprint(OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":[ADS_GatherServiceStatus] => " + message + "\n");
   	}
   	catch (Exception ex)
   	{
   		// what can we do with this ?
   	}
   }
   
   ///
   private Table gatherServiceStatus() throws OException {
	   
	   Table serviceStatus = Util.serviceGetStatusAll();

	   for (int serviceRow = serviceStatus.getNumRows(); serviceRow > 0; serviceRow--) {
		   String serviceGroupText = serviceStatus.getString("service_group_text", serviceRow);

		   if (serviceGroupText.equals("APM") || serviceGroupText.equals("APM Nomination")) {
			   Table serviceProperties;
			   String serviceName = serviceStatus.getString("service_name", serviceRow);
			   Table engineTable = serviceStatus.getTable("engine_table", serviceRow);

			   engineTable.addCol("method_properties", COL_TYPE_ENUM.COL_TABLE);
			   engineTable.addCol("dataset_details", COL_TYPE_ENUM.COL_TABLE);

			   for (int engineRow = 1; engineRow <= engineTable.getNumRows(); engineRow++) {
				   String engineName = engineTable.getString("engine_name", engineRow);

				   // The service method is appended with _<number> ... so remove it to derive the method name for the Engine
				   // String[] engineMethodName;
				   // engineMethodName = engineName.split("_", 2);
				   // serviceProperties = findServiceMethodProperties(serviceName, engineMethodName);
				   // serviceProperties = Services.getServiceMethodProperties(serviceName, engineMethodName[0]);
				   serviceProperties = GetOverlaysForMethod(serviceName, engineName);
					
				   // if (serviceProperties != null)
				   // {
				   // Table flatMethodProperties = flattenMethodProperties(serviceProperties);
				   // engineTable.setTable("method_properties", engineRow, flatMethodProperties);
				   // }
				   if (serviceProperties == null) {
					   print("Unable to retrieve APM service properties.");
				   } else if (engineName.contains("ApmService") || engineName.contains("ApmNomService")) {
					   try {
						   Table datasetDetails = deriveAPMDatasetDetails(serviceProperties);
						   engineTable.setTable("dataset_details", engineRow, datasetDetails);
					   } catch (OException e) {
						   print("APMService configured incorrectly." + e.getMessage());
					   }
				   }
			   }
		   } else {
				serviceStatus.delRow(serviceRow);
		   }
	   }
	   return serviceStatus;
   }   
   
   ///
   protected Table GetOverlaysForMethod(String serviceName, String sMethod) throws OException {

	   String sWhat;
	   String sFrom;
	   String sWhere;
	   int iRetVal;
	   Table overlays;
	   //Table userTableProps = null; // SDP

	   overlays = Table.tableNew("method_overlays");

	   // get ApmService methods - this only returns the ones that have been set
	   sWhat = "c.prop_id, c.prop_owner_id, c.prop_value, c.extra_value_id, p.prop_name, p.prop_type, p.prop_subtype";
	   sFrom = "grid_property_overlays c, job_cfg a, job_cfg b, grid_default_properties p";
	   sWhere = "a.name = '" + sMethod + "' and a.job_id = b.sub_id and b.job_id = c.overlay_owner_id and p.prop_id = c.prop_id";

	   iRetVal = DBaseTable.loadFromDbWithSQL(overlays, sWhat, sFrom, sWhere);
	   if (iRetVal < 1) {
		   print("GetOverlaysForMethod() Error: DB Call failed");			
		   overlays.destroy();
		   return null;
	   }

	   // now decide whether we need to drop through to the user table stuff
	   sWhat = "prop_value";
	   sFrom = "grid_default_properties";
	   sWhere =  "prop_group_id = 13 and prop_name = '" + "dataset_type_id'";
	   Table datasetPresent = Table.tableNew("dataset_type");
	   iRetVal = DBaseTable.loadFromDbWithSQL(datasetPresent, sWhat, sFrom, sWhere);
	   if (iRetVal < 1) {
		   print("GetOverlaysForMethod() Error: DB Call failed");			
		   overlays.destroy();
		   return null;
	   }

	   datasetPresent.destroy();

	   return overlays;
   }
   
   // Returns a table with the Dataset Details for the APM service
   private Table deriveAPMDatasetDetails(Table serviceProperties) throws OException {
	   Table datasetDetails = Table.tableNew("Dataset Details");

	   // dataset type
	   datasetDetails.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
	   datasetDetails.addCol("dataset_type_name", COL_TYPE_ENUM.COL_STRING);
	   datasetDetails.addRow();

	   Table datasetTable = GetMselTable(serviceProperties, "dataset_type_id");
	   int datasetTypeId = 0;
	   String datasetType = "Default";
	   if (datasetTable != null) {
		   datasetTypeId = datasetTable.getInt("id", 1);
		   datasetType = datasetTable.getString("value", 1);
		   datasetTable.destroy();
	   }

	   datasetDetails.setInt("dataset_type_id", 1, datasetTypeId);
	   datasetDetails.setString("dataset_type_name", 1, datasetType);

	   // simulation
	   String simulationName = GetStringProp(serviceProperties, "simulation_name");

	   Table simDef = Sim.loadSimulation(simulationName);
	   if (simDef == null)
		   simDef = Sim.createBaseSimulation();

	   Table scenarioDef = simDef.getTable("scenario_def", 1);
	   scenarioDef.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
	   scenarioDef.setColValInt("dataset_type_id", datasetTypeId);

	   // get APM scenario id's
	   Table scenarios = APM_GetScenarioList(simDef);
	   if (scenarios != null) {
		   scenarios.destroy(); // don't care about returned list here, passed sim def was filled in
	   }
	   datasetDetails.select(scenarioDef, "apm_scenario_id(scenario_id), scenario_name", "dataset_type_id EQ $dataset_type_id");

	   // entity groups
	   String serviceType = "DEAL";
	   Table entityGroupTable = GetMselTable(serviceProperties, "internal_portfolio"); // deal based services
	   if (entityGroupTable == null) {
		   entityGroupTable = GetMselTable(serviceProperties, "pipeline_id"); // nom based services
		      
		   if (entityGroupTable == null) {
		      throw new OException("Portfolio/Service provider is null. Unrecognised service type.");
		   }
		   
		   serviceType = "NOMINATION";
		   
	      // if the table is empty (means ALL) then we have to get the contents of the "pipeline_id" lookup list and apply all of them as dataset keys
	      if (entityGroupTable.getNumRows() == 1 && entityGroupTable.getInt("id", 1) == 0 ) {
	    	
	    	Table tLookupListId = Table.tableNew();
			if (DBaseTable.loadFromDbWithSQL(tLookupListId, "prop_subtype", "grid_default_properties", "prop_name = 'pipeline_id'") < 1) {
			      throw new OException("Could not get lookup list for Portfolio/Service provider.");
			}
			
			int lookupListId = tLookupListId.getInt(1, 1);
			Table pipelineIds = Ref.loadRefList(SHM_USR_TABLES_ENUM.fromInt(lookupListId));
			entityGroupTable.clearRows();
			entityGroupTable.select(pipelineIds, "id, name(value)", "id GT 0");
			pipelineIds.destroy();	    	  
	      }
		   
	   }
	   entityGroupTable.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
	   entityGroupTable.setColValInt("dataset_type_id", datasetTypeId);

	   EntityType serviceEntityType = EntityType.getByValue(serviceType);
	   String entityTypeGroupName = serviceEntityType.getEntityGroupLabel(); 
	   String entityType = serviceEntityType.getEntityType(); 

	   entityGroupTable.addCol("entity_type", COL_TYPE_ENUM.COL_STRING);
	   entityGroupTable.addCol("entity_type_group_name", COL_TYPE_ENUM.COL_STRING);
	   entityGroupTable.setColValString("entity_type", entityType);
	   entityGroupTable.setColValString("entity_type_group_name", entityTypeGroupName);
	   
	   datasetDetails.select(entityGroupTable, "id(entity_group_id), value(entity_group_name), entity_type, entity_type_group_name", "dataset_type_id EQ $dataset_type_id");

	   // packages
	   Table packageTable = GetMselTable(serviceProperties, "package_name");
	   if (packageTable == null) {
		   throw new OException("Package is null. May be package is not selected, please configure APMSerivce correctly. ");
	   }
	   packageTable.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
	   packageTable.setColValInt("dataset_type_id", datasetTypeId);
	   datasetDetails.select(packageTable, "id(package_id), value(package_name)", "dataset_type_id EQ $dataset_type_id");

	   // incremental on/off
	   datasetDetails.addCol("incremental_processing_on", COL_TYPE_ENUM.COL_INT);
	   datasetDetails.addCol("$incremental_processing_on$", COL_TYPE_ENUM.COL_STRING);

	   // set as defalt of on first
	   datasetDetails.setColValInt("incremental_processing_on", 1);
	   datasetDetails.setColValString("$incremental_processing_on$", "Yes");

	   String incrementalOn = GetStringProp(serviceProperties, "incremental_on");
	   // if there is an incremental setting...
	   if (incrementalOn.length() > 0) {
		   // if set to NO then set as no
		   if (incrementalOn.compareTo("No") == 0) {
			   datasetDetails.setColValInt("incremental_processing_on", 0);
			   datasetDetails.setColValString("$incremental_processing_on$", "No");
		   }
	   }

	   // datasetDetails.viewTable();
	   return datasetDetails;
   }
   
   ///
   protected Table GetMselTable(Table methodConfig, String valueName) throws OException {

		int propRow;
		int extraValueId;
		int propType;
		String sWhat;
		String sFrom;
		String sWhere;
		String firstValue;
		Table tValues;
		Table tMselValues;
		Table tLookupList;

		// find the property in the config
		methodConfig.sortCol("prop_name");
		propRow = methodConfig.findString("prop_name", valueName, SEARCH_ENUM.FIRST_IN_GROUP);
		if (propRow > 0) {

			// query extra values
			extraValueId = methodConfig.getInt("extra_value_id", propRow);
			propType = methodConfig.getInt("prop_subtype", propRow);
			firstValue = methodConfig.getString("prop_value", propRow);
			sWhat = "extra_value_id, extra_prop_value";
			sFrom = "grid_msel_extra_values";
			sWhere = "extra_value_id = " + extraValueId;

			tValues = Table.tableNew("msel_values");
			if (DBaseTable.loadFromDbWithSQL(tValues, sWhat, sFrom, sWhere) < 1) {
				print("Failed to get extra service method msel values");
				return null;
			}
			tValues.makeTableUnique();

			// create msel return table
			tMselValues = Table.tableNew("msel_values");
			tMselValues.addCol("id", COL_TYPE_ENUM.COL_INT);
			tMselValues.addCol("value", COL_TYPE_ENUM.COL_STRING);

			// !VET! - Code beneath added to support the 'old' style APM config. This is required for VET.
			// !VET! - For package_name in OLD cuts we might be getting multiple entries here separated by bar
			// !VET! - Begin ...
			if ( firstValue.contains("|") )
			{
				String[] aNames = firstValue.split("\\|");

				tMselValues.addNumRows(aNames.length);
				for (int row = 1; row <= aNames.length; row++) {
					tMselValues.setString("value", row, aNames[row-1]);
				}
			}
			else
			{
				tMselValues.addRow();
				tMselValues.setString("value", 1, firstValue);
			}
			// !VET! - ... End

			// copy msel values
			tMselValues.select(tValues, "extra_value_id(id), extra_prop_value(value)", "extra_value_id GT 0");

			// attempt to get the lookup list
			tLookupList = GetLookupList(valueName, propType);
			if (tLookupList == null)
				return null;

			// fill in lookup id's
			// !VET! - Begin ... Avoid using the OpenJVS fill() method ... 
			tLookupList.sortCol("value");
			String value;
			int row, id;
			for (int m = 1; m <= tMselValues.getNumRows(); m++)
			{
				value =  tMselValues.getString("value", m);
				row = tLookupList.findString("value", value, SEARCH_ENUM.FIRST_IN_GROUP);
				
				if (row > 0)
				{								
					id =  tLookupList.getInt("id", row);
					tMselValues.setInt("id", m, id);
				}
			}
			// !VET! - ... End

			tLookupList.destroy();
			return tMselValues;
		}
		return null;
	}

   ///
   protected String GetStringProp(Table methodConfig, String valueName) throws OException {
		int propRow = 0;
		String value = "";

		// find the property in the config
		methodConfig.sortCol("prop_name");
		propRow = methodConfig.findString("prop_name", valueName, SEARCH_ENUM.FIRST_IN_GROUP);
		if (propRow > 0) {
			value = methodConfig.getString("prop_value", propRow);
		}
		return value;
	}
   
   ///
   protected Table GetLookupList(String name, int id) throws OException {

		Table lookup;

		// check for apm exceptions - package
		if (name.compareTo("package_name") == 0) {
			lookup = Table.tableNew("apm_package_names");
			lookup.addCol("apm_package_id", COL_TYPE_ENUM.COL_INT);
			lookup.addCol("apm_package_name", COL_TYPE_ENUM.COL_STRING);
			DBaseTable.loadFromDb(lookup, "apm_package_names");
			lookup.setColName(1, "id");
			lookup.setColName(2, "value");
			return lookup;
		}

		// check for apm exceptions - dataset type
		if (name.compareTo("dataset_type_id") == 0) {
			lookup = Table.tableNew("apm_dataset_type");
			lookup.addCol("apm_dataset_id", COL_TYPE_ENUM.COL_INT);
			lookup.addCol("apm_dataset_name", COL_TYPE_ENUM.COL_STRING);
			DBaseTable.loadFromDb(lookup, "apm_dataset_type");
			lookup.setColName(1, "id");
			lookup.setColName(2, "value");
			return lookup;
		}

		// check standard reflists
		// horrible to convert int to enum in java so hardwired switch for pfolio, only one needed right now...
		if (id == 33) {
			lookup = Ref.loadRefList(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			if (lookup != null) {
				lookup.setColName(1, "id");
				lookup.setColName(2, "value");
				return lookup;
			}
		}

		// horrible to convert int to enum in java so hardwired switch for pfolio, only one needed right now...
		if (id == 81) {
			lookup = Ref.loadRefList(SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
			if (lookup != null) {
				lookup.setColName(1, "id");
				lookup.setColName(2, "value");
				return lookup;
			}
		}

		return null;
	}
   

	/**
	 * 
	 * APM_GetScenarioListAPM_GetScenarioList
	 * 
	 * Enrich an endur sim table with APM scenario id's
	 * 
	 * @param tSimDefCache
	 * @return
	 * @throws OException
	 */
	private Table APM_GetScenarioList(Table tSimDefCache) throws OException {
		Table tScenarios;
		Table tScenarioList = Table.tableNew("scenario_list");
		Table tArgs;
		Table tScenarioListResult;
		int iScenario;
		int iScenarioId = 0;
		String sScenarioName;
		int iRetVal = 1;

		tScenarios = tSimDefCache.getTable("scenario_def", 1);

		if (tScenarios.getColNum("apm_scenario_id") < 1) {
			tScenarios.addCol("apm_scenario_id", COL_TYPE_ENUM.COL_INT);
		}

		// ---------------- create and populate list of scenarios with ids for
		// processing by this batch/entity update process ---------
		tScenarioList.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		tScenarioList.addCol("scenario_name", COL_TYPE_ENUM.COL_STRING);
		tScenarioList.addNumRows(tScenarios.getNumRows());

		// build table for stored proc
		tArgs = Table.tableNew("params");
		tArgs.addCol("scenario_name", COL_TYPE_ENUM.COL_STRING);
		tArgs.addRow();

		// if a scenario for the simulation is not saved in the user table add
		// it and assign an ID
		for (iScenario = 1; iScenario <= tScenarios.getNumRows(); iScenario++) {
			sScenarioName = tScenarios.getString("scenario_name", iScenario);
			// Create the function parameters and run the the stored proc
			tArgs.setString(1, 1, sScenarioName);

			// call proc to get scenario id from apm_scenario_list, will
			// insert into table and assign id
			// if not found
			iRetVal = APM_DBASE_RunProc("USER_apm_update_scenariolist", tArgs);
			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				print("Unable to execute USER_apm_update_scenariolist");
			} else {
				tScenarioListResult = Table.tableNew("scenario_list_results");
				iRetVal = DBase.createTableOfQueryResults(tScenarioListResult);
				if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					print("Unable to retrieve results of USER_apm_update_scenariolist");
				} else {
					if (tScenarioListResult.getNumRows() > 0) {
						iScenarioId = tScenarioListResult.getInt("scenario_id", 1);
					} else {
						print("No rows returned from USER_apm_update_scenariolist");
						iRetVal = 0;
					}
				}
				tScenarioListResult.destroy();
			}

			if (iRetVal != 0) {
				// save the scenario id and name
				tScenarios.setInt("apm_scenario_id", iScenario, iScenarioId);
				tScenarioList.setInt("scenario_id", iScenario, iScenarioId);
				tScenarioList.setString("scenario_name", iScenario, sScenarioName);
			} else {
				break;
			}
		}

		if (iRetVal != 0) {
			// save the scenario list in argt
			tScenarioList.sortCol("scenario_name");
		}

		tArgs.destroy();
		return tScenarioList;
	}
	
	///
    private int APM_DBASE_RunProc(String sp_name, Table arg_table) throws OException {
        final int nAttempts = APM_Utils.MAX_NUMBER_OF_DB_RETRIES;

        int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

        int numberOfRetriesThusFar = 0;
        do {
        	// for error reporting further down
        	String message = null;
        	
            try {
                iRetVal = DBase.runProc(sp_name, arg_table);
            } catch (OException exception) {
                iRetVal = exception.getOlfReturnCode().toInt();
                
                message = exception.getMessage();
            } finally {
                if (iRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
                    numberOfRetriesThusFar++;
                    
                    if(message == null) {
                        message = String.format("Query execution retry %1$d of %2$d. Check the logs for possible deadlocks.", numberOfRetriesThusFar, APM_Utils.MAX_NUMBER_OF_DB_RETRIES);
                    } else {
                        message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, APM_Utils.MAX_NUMBER_OF_DB_RETRIES, message);
                    }

                    OConsole.oprint(message);

                    Debug.sleep(numberOfRetriesThusFar * 1000);
                } else {
                    // it's not a retryable error, so leave
                    break;
                }
            }
        } while (numberOfRetriesThusFar < nAttempts);

        if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
            print(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of " + sp_name + " failed"));

        return iRetVal;
    }
	
	///
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateCaches(Table serviceStatus) throws OException {
		try {
			//ADSInterface ads = Factory.getADSImplementation();
			
			if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
			   print("UpdateCaches: preFindGridCache: DataSourceDetails");
			// Use common interface
			//dataSourceCache = CacheManager.findGridCache(DATA_SOURCE_DETAILS_CACHE_NAME);
			dataSourceCache = s_ads.findGridCache(DATA_SOURCE_DETAILS_CACHE_NAME);
			
			if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
				print("UpdateCaches: postFindGridCache: DataSourceDetails. Cache Manager loaded and initialised");

			if (dataSourceCache == null) {
				//dataSourceCache = CacheManager.createGridCache(DATA_SOURCE_DETAILS_CACHE_NAME);
				dataSourceCache = s_ads.createGridCache(DATA_SOURCE_DETAILS_CACHE_NAME);
			}
			//datasetMessagesCache = CacheManager.findGridCache(DATASET_MESSAGES_CACHE_NAME);
			datasetMessagesCache = s_ads.findGridCache(DATASET_MESSAGES_CACHE_NAME);
			
			if (datasetMessagesCache == null) {
				//datasetMessagesCache = CacheManager.createGridCache(DATASET_MESSAGES_CACHE_NAME);
				datasetMessagesCache = s_ads.createGridCache(DATASET_MESSAGES_CACHE_NAME);
			}

			Set setOfNewServiceIds = new HashSet();
			// Iterate through each service
			for (int serviceRow = 1; serviceRow <= serviceStatus.getNumRows(); serviceRow++) {
				Map newServiceKey = new HashMap();
				Map newServiceInfo = new HashMap();

				int serviceId = serviceStatus.getInt("service_id", serviceRow);
				setOfNewServiceIds.add(serviceId);
				String serviceName = serviceStatus.getString("service_name", serviceRow);
				newServiceKey.put("service_id", serviceId);

				// need this info to be in the values of Dataset status cache, so that end user doesnt have to parse XML string
				String gridRunsite = serviceStatus.getString("grid_run_site_text", serviceRow);
				String wflowStatus = serviceStatus.getString("wflow_status_text", serviceRow);
				String serviceStatusText = "Offline"; 
				if ( serviceStatus.getString("service_status_text", serviceRow).equals("Running") )
					serviceStatusText = "Online";

				for (int serviceCol = 1; serviceCol <= serviceStatus.getNumCols(); serviceCol++) {
					String colName = serviceStatus.getColName(serviceCol);
					Object value = findCellValue(serviceStatus, serviceRow, serviceCol);
					if (value != null) {
						newServiceInfo.put(colName, value);
					}
				}

				Table engineTable = serviceStatus.getTable("engine_table", serviceRow);
				Table xmlEngineTable = Table.tableNew("XML Engine Table");
				xmlEngineTable.addCol("Engines", COL_TYPE_ENUM.COL_TABLE);
				xmlEngineTable.addRow();

				// tableToXMLString() works with Table of one row, so creating a new table with just one row
				xmlEngineTable.setTable(1, 1, engineTable.copyTable());
				// Engine table of a service is stored as a XML string in the cache.
				String xmlString = xmlEngineTable.tableToXMLString();
				newServiceInfo.put("engineTableXml", xmlString);

				try {
					//if (dataSourceCache.lock(newServiceKey, WAIT_INDEFINITELY)) {
					//if (s_ads.lockGridCache(dataSourceCache, newServiceKey, WAIT_INDEFINITELY)) {
						//Map currentServiceInfo = (Map) dataSourceCache.get(newServiceKey);
						Map currentServiceInfo = (Map) s_ads.getFromGridCache(dataSourceCache, newServiceKey);
						Map statusInfo = new HashMap();
						statusInfo.put("service_id", serviceId);
						statusInfo.put("$service_name$", serviceName);
						statusInfo.put("grid_run_site_name", gridRunsite);
						statusInfo.put("wflow_status", wflowStatus);
						statusInfo.put("$service_status_name$", serviceStatusText);

						/*
						 * if currentServiceInfo is null, it means its new service, SOO Insert new serivce in to Datasource cache and insert associated Datasets in Status cache
						 */
						if (currentServiceInfo == null) {
							
							//dataSourceCache.put(newServiceKey, newServiceInfo);
							s_ads.insertIntoGridCache( dataSourceCache, newServiceKey, newServiceInfo );

							if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_MEDIUM.toInt())
							{
								print("Inserting new service: " + newServiceKey.toString());
								print(newServiceInfo.toString());					
							}
							
							// get all the dataset keys and keys details(ie entity group name, scenario name, etc) for this new service
							Map<Map, Map> allDsKeys = getAllDatasetsKeysInEngineTable(serviceId, engineTable);

							insertDatasetsInToStatusCache(allDsKeys, statusInfo);

						} else if (!currentServiceInfo.equals(newServiceInfo)) {

							if (datasetsChanged(currentServiceInfo, newServiceInfo)) {
								// get the Dataset Keys from the Dataset Details table
								Map<Map, Map> allCurrentDatasetKeys = getAllDatasetKeys(serviceId, currentServiceInfo);
								Map<Map, Map> allNewDatasetKeys = getAllDatasetKeys(serviceId, newServiceInfo);

								// get the dataset that are added
								Map<Map, Map> addedDatasets = getAddedDatasets(allCurrentDatasetKeys, allNewDatasetKeys);

								// insert added datasets for this service
								insertDatasetsInToStatusCache(addedDatasets, statusInfo);

								Set<Map> deletedDatasets = getDeletedDatasets(allCurrentDatasetKeys, allNewDatasetKeys);
								Iterator iter = deletedDatasets.iterator();
								// /*******Test***/
								// KKD
								// IGridCache cache = CacheManager.findGridCache("DATASET_STATUS");

								// Iterator it = cache.getAll().keySet().iterator();
								// while(it.hasNext()) {
								// OConsole.oprint(" " + (Map)it.next());
								// }
								// remove deleted datasets from the Dataset status cache
								while (iter.hasNext()) {
									Map dsKey = (Map) iter.next();

									// KKD
									// Map value = (Map) cache.get(dsKey);
									//Map value = DatasetStatusManager.get(dsKey);
									Map value  = s_ads.getFromDatasetStatusManager(dsKey);
									
									if (value != null) {
										try {
											//DatasetStatusManager.removeDataset(dsKey, serviceName);
											s_ads.removeFromDatasetStatusManager(dsKey, serviceName);
										} catch (Exception npe) {
											// temporary fix until value extractors sorted
											print("Exception removing dataset." + dsKey.toString() + "" + npe);
											print("Exception removing dataset." + dsKey.toString() + "" + npe.getStackTrace());
											print("Exception removing dataset." + dsKey.toString() + "" + npe.getStackTrace().toString());
										}
									} else {
										print(" DSKey trying to delete " + dsKey + " but not found in Status cache");
									}

								}
							}
							// something changed so update Datasource cache.
							//dataSourceCache.put(newServiceKey, newServiceInfo);
							s_ads.insertIntoGridCache(dataSourceCache, newServiceKey, newServiceInfo);
						}
					//}

				} catch (Exception e) {
					print(" General error ");
					throw new OException(e);
				} finally {
					//dataSourceCache.unlock(newServiceKey);
					//s_ads.unlockGridCache(dataSourceCache, newServiceKey);
				}
			}

			findAndRemoveDeletedServices(setOfNewServiceIds);

			Object statusCache = s_ads.findGridCache(DATASET_STATUS_CACHE_NAME);
			if ( statusCache == null )
				print("Status cache not found !!");
			else
				print("Status cache size " + s_ads.getGridCacheSize(statusCache));

		} 
		catch (ADSException e) {
			print ("UpdateCaches: Error ADSException " + stackTraceToString(e));
			throw new OException("ADSException" + e);
		}
	}	
	
	// Returns the cell value
	private Object findCellValue(Table table, int row, int column) throws OException {
		if (table == null) {
			return null;
		}
		int colType = table.getColType(column);
		// table.viewTable();
		// -- INT
		if (colType == COL_TYPE_ENUM.COL_INT.toInt()) {
			int colFormat = table.getColFormat(column);
			if (colFormat == COL_FORMAT_TYPE_ENUM.FMT_NONE.toInt()) {
				long result = ((Number) (table.getInt(column, row))).longValue();
				return result;
			} else if (colFormat == COL_FORMAT_TYPE_ENUM.FMT_REF.toInt()) {
				return (table.formatCellData(column, row));
			} else if (colFormat == COL_FORMAT_TYPE_ENUM.FMT_IVL_LIST.toInt()) {
				return table.formatCellData(column, row);
			} else if (colFormat == COL_FORMAT_TYPE_ENUM.FMT_DATE.toInt()) {
				return convertToDate(table.getInt(column, row));
			}
			return ((Number) (table.getInt(column, row))).longValue();
			// -- DOUBLE
		} else if (colType == COL_TYPE_ENUM.COL_DOUBLE.toInt()) {
			Double result = table.getDouble(column, row);
			if (result == DOUBLE_BLANK || result == DOUBLE_ERROR) {
				return null;
			} else {
				return result;
			}
		}
		// -- STRING
		else if (colType == COL_TYPE_ENUM.COL_STRING.toInt()) {
			return table.getString(column, row);
		}
		// -- DATE
		else if (colType == COL_TYPE_ENUM.COL_DATE_TIME.toInt()) {
			return convertToDate(table.getDateTime(column, row));
			// -- UNKNOWN
		} else {
			// OConsole.oprint("Col Type = " + colType );
			return null;
			// throw new Exception("Column type encountered that cannot currently be handled");
		}
	}
	
	///
	protected Date convertToDate(ODateTime oDateTime) throws OException {

		long julianDate = new Integer(oDateTime.getDate()).longValue();
		long julianTime = new Integer(oDateTime.getTime()).longValue();
		// It seems that coherence POF (over extends) expects a Gregorian date (between 1582 and 2199), and will throw an exception
		// if the date lies outside these limits. To 'solve' this, returning a null when the Julian date is zero, since this
		// presumably represents a null value.
		if (julianDate <= 0) {
			return null;
		}
		// Also refactored the entire method to reuse the julian date methods within DateUtils
		//return DateUtils.convertJulianToDate(julianDate, julianTime);
		
		Date date = null;
		
		try {
			date = s_ads.convertJulianToDate(julianDate, julianTime);
		} catch (ADSException e) {
			throw new OException(e);
		}
		
		return ( date );
	}
	
	///
	protected Date convertToDate(int julianDate) throws OException {
		Calendar theCalendar = Calendar.getInstance();
		theCalendar.set(OCalendar.getYear(julianDate), OCalendar.getMonth(julianDate) - 1, OCalendar.getDay(julianDate), 0, 0, 0);
		theCalendar.set(Calendar.MILLISECOND, 0);
		return (theCalendar.getTime());
	}
	
	///
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Map, Map> getAllDatasetsKeysInEngineTable(int service, Table engineTable) throws OException {
		Table datasetDetails = Table.tableNew();
		Map keyValueMap = new HashMap();
		for (int engineRow = 1; engineRow <= engineTable.getNumRows(); engineRow++) {
			datasetDetails = engineTable.getTable("dataset_details", engineRow);
			if (datasetDetails != null) {
				for (int dsRow = 1; dsRow <= datasetDetails.getNumRows(); dsRow++) {
					Map dsKey = new HashMap();
					Map dsKeyDetails = new HashMap();
					
					dsKey.put("dataset_type_id", datasetDetails.getInt("dataset_type_id", dsRow));
					dsKey.put("scenario_id", datasetDetails.getInt("scenario_id", dsRow));
					dsKey.put("entity_group_id", datasetDetails.getInt("entity_group_id", dsRow));
					dsKey.put("package", datasetDetails.getString("package_name", dsRow));
					dsKey.put("service_id", service);
					
					// just dsKey details in string for so that end user doesn't have to do a look up for the ids
					dsKeyDetails.put("service_id_ads", service);
					dsKeyDetails.put("dataset_type_id_ads", datasetDetails.getInt("dataset_type_id", dsRow));
					dsKeyDetails.put("entity_group_id_ads", datasetDetails.getInt("entity_group_id", dsRow));	
					dsKeyDetails.put("entity_type", datasetDetails.getString("entity_type", dsRow));
					dsKeyDetails.put("entity_type_group_name", datasetDetails.getString("entity_type_group_name", dsRow));
					dsKeyDetails.put("$dataset_type_name$", datasetDetails.getString("dataset_type_name", dsRow));
					dsKeyDetails.put("$scenario_name$", datasetDetails.getString("scenario_name", dsRow));
					dsKeyDetails.put("$entity_group_name$", datasetDetails.getString("entity_group_name", dsRow));
					dsKeyDetails.put("incremental_processing_on", datasetDetails.getInt("incremental_processing_on", dsRow));
					dsKeyDetails.put("$incremental_processing_on$", datasetDetails.getString("$incremental_processing_on$", dsRow));
						
					keyValueMap.put(dsKey, dsKeyDetails);
				}
			}
		}
		return keyValueMap;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void insertDatasetsInToStatusCache(Map allDatasets, Map statusInfo) throws OException {
		try {
			Iterator iter = allDatasets.entrySet().iterator();
			while (iter.hasNext()) {
				Entry entry = (Entry) iter.next();
				Map dsKey = (Map) entry.getKey();
				Map dsKeyDetails = (Map) entry.getValue();
				dsKeyDetails.putAll(statusInfo);

				// KKD
				// Object o = getStatusCache().get(dsKey);
				//Object o = DatasetStatusManager.get(dsKey);
				Object o = s_ads.getFromDatasetStatusManager(dsKey);
				
				// if dataset not in cache it's brand new so add defaults for
				// batch_status and running_batch_status we can't always add these or we'll
				// make datasets stale or mess up running batches if this runs after service info has change or during a running batch
				if (o == null) {
					dsKeyDetails.put("batch_status", "Not Run");
					dsKeyDetails.put("running_batch_status", "Not Running");
					//DatasetStatusManager.insertDataset(dsKey, dsKeyDetails);
					s_ads.insertIntoDatasetStatusManager(dsKey, dsKeyDetails);
					
					if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_MEDIUM.toInt())
					{
						print("Inserting new Dataset Key: " + dsKey.toString());
						print(dsKeyDetails.toString());					
					}
				} else {
					//DatasetStatusManager.updateDatasetStatusInfo(dsKey, dsKeyDetails);
					s_ads.updateDatasetStatusManager(dsKey, dsKeyDetails);
					
					if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_MEDIUM.toInt())
					{
						print("Updating existing Dataset Key: " + dsKey.toString());
						print(dsKeyDetails.toString());					
					}
				}

			}
		} 
//		catch (InconsistentCacheTypeException e) {
//			print("insertDatasetsInToStatusCache: Error  InconsistentCacheTypeException " + stackTraceToString(e));
//		throw new OException("InconsistentCacheTypeException" + e);
//
//		} catch (CacheException e) {
//			print("insertDatasetsInToStatusCache: Error  CacheException " + stackTraceToString(e));
//			throw new OException("CacheException" + e);
//		}
		catch (ADSException e)
		{
			print("insertDatasetsInToStatusCache: Error ADSException " + stackTraceToString(e));
			throw new OException("ADSException" + e);			
		}		
	}

	///
	private static String stackTraceToString(Throwable e) {
		String retValue = null;
		StringWriter sw = null;
		PrintWriter pw = null;
		try {
			sw = new StringWriter();
			pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			retValue = sw.toString();
		} finally {
			try {
				if (pw != null)
					pw.close();
				if (sw != null)
					sw.close();
			} catch (IOException ignore) {
			}
		}
		return retValue;
	}
	
	///
	@SuppressWarnings("rawtypes")
	private boolean datasetsChanged(Map currentServiceInfo, Map newServiceInfo) throws OException {
		String currentEngineTableXml = (String) currentServiceInfo.get("engineTableXml");
		String newEngineTableXml = (String) newServiceInfo.get("engineTableXml");

		// find out if we need to update DatasetStatus cache
		if (!currentEngineTableXml.equals(newEngineTableXml)) {
			Table currentEngineTable = Table.xmlStringToTable(currentEngineTableXml);
			Table newEngineTable = Table.xmlStringToTable(newEngineTableXml);

			// get Dataset Details Table for current Engine
			Table currentDatasetsTable = getDatasetsTableForEngine(currentEngineTable);

			// get Dataset Details Table for new Engine
			Table newDatasetsTable = getDatasetsTableForEngine(newEngineTable);

			/*
			 * if APMService is not configured properly, i,e if enttiy group or package is not selected, DatasetsTable will be null, so if both currentDatasetTable and newDatasetTable
			 * are null indicates nothing changes so return false;
			 */
			if (currentDatasetsTable != null && newDatasetsTable != null) {
				String xmlCurrentDatasetsTable = currentDatasetsTable.tableToXMLString();
				String xmlNewDatasetsTable = newDatasetsTable.tableToXMLString();

				// if Datasets have changed then findout which are deleted and which are added.
				if (!xmlCurrentDatasetsTable.equals(xmlNewDatasetsTable)) {
					return true;
				}
			} else if (currentDatasetsTable == null && newDatasetsTable == null) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}
	
	///
	private Table getDatasetsTableForEngine(Table engineTable) throws OException {
		Table datasetsTable = null;// Table.tableNew("DatasetsTable");
		search: for (int engineTableRow = 1; engineTableRow <= engineTable.getNumRows(); engineTableRow++) {
			Table engines = engineTable.getTable("Engines", engineTableRow);

			for (int enginesRow = 1; enginesRow <= engines.getNumRows(); enginesRow++) {
				Table engine_table = engines.getTable("engine_table", enginesRow);

				for (int engine_tableRow = 1; engine_tableRow <= engine_table.getNumRows(); engine_tableRow++) {
					Table datasetDetailsTableWrapper = engine_table.getTable("dataset_details", engine_tableRow);

					if (datasetDetailsTableWrapper == null) {
						continue;
					}
					for (int datasetsTable1Row = 1; datasetsTable1Row <= datasetDetailsTableWrapper.getNumRows(); datasetsTable1Row++) {
						datasetsTable = datasetDetailsTableWrapper.getTable("Dataset_Details", datasetsTable1Row);
						if (datasetsTable != null) {
							break search;
						}
					}
				}
			}
		}
		return datasetsTable;
	}
	
	///
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Map, Map> getAllDatasetKeys(int service, Map serviceInfo) throws OException {
		String engineTableXml = (String) serviceInfo.get("engineTableXml");

		Map keyValueMap = new HashMap();
		
		Table engineTable = Table.xmlStringToTable(engineTableXml);

		// get Dataset Details Table for current Engine
		Table datasetDetailsTable = getDatasetsTableForEngine(engineTable);

		// If APM serivce is not configured properly then datasetTable will be null
		if (datasetDetailsTable != null) {
			for (int dsRow = 1; dsRow <= datasetDetailsTable.getNumRows(); dsRow++) {
				Map dsKey = new HashMap();
				Map dsKeyDetails = new HashMap();
				String datasetType = datasetDetailsTable.getString("dataset_type_name", dsRow);

				dsKey.put("dataset_type_id", java.lang.Integer.parseInt(datasetDetailsTable.getString("dataset_type_id", dsRow)));
				dsKey.put("scenario_id", java.lang.Integer.parseInt(datasetDetailsTable.getString("scenario_id", dsRow)));
				dsKey.put("entity_group_id", java.lang.Integer.parseInt(datasetDetailsTable.getString("entity_group_id", dsRow)));
				dsKey.put("package", datasetDetailsTable.getString("package_name", dsRow));
				dsKey.put("service_id", service);
				
				// just dsKey details in string for so that end user doesn't have to do a look up for the ids
				dsKeyDetails.put("service_id_ads", service);
				dsKeyDetails.put("dataset_type_id_ads", java.lang.Integer.parseInt(datasetDetailsTable.getString("dataset_type_id", dsRow)));
				dsKeyDetails.put("entity_group_id_ads", java.lang.Integer.parseInt(datasetDetailsTable.getString("entity_group_id", dsRow)));				
				dsKeyDetails.put("entity_type", datasetDetailsTable.getString("entity_type", dsRow));
				dsKeyDetails.put("entity_type_group_name", datasetDetailsTable.getString("entity_type_group_name", dsRow));
				dsKeyDetails.put("$dataset_type_name$", datasetType);
				dsKeyDetails.put("$scenario_name$", datasetDetailsTable.getString("scenario_name", dsRow));
				dsKeyDetails.put("$entity_group_name$", datasetDetailsTable.getString("entity_group_name", dsRow));

				if ( datasetDetailsTable.getColNum("incremental_processing_on") > 0 && datasetDetailsTable.getColType("incremental_processing_on") == COL_TYPE_ENUM.COL_STRING.toInt() )
				{
					dsKeyDetails.put("incremental_processing_on", java.lang.Integer.parseInt(datasetDetailsTable.getString("incremental_processing_on", dsRow)));
					dsKeyDetails.put("$incremental_processing_on$", datasetDetailsTable.getString("_incremental_processing_on_", dsRow));            	               	   
				}
				else
				{                              
					dsKeyDetails.put("incremental_processing_on", datasetDetailsTable.getInt("incremental_processing_on", dsRow));
					dsKeyDetails.put("$incremental_processing_on$", datasetDetailsTable.getString("$incremental_processing_on$", dsRow));
				}
					
				keyValueMap.put(dsKey, dsKeyDetails);
			}
		}
			
		return keyValueMap;
	}
	
	///
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Map, Map> getAddedDatasets(Map<Map, Map> currentDatasets, Map<Map, Map> newDatasets) {
		Map<Map, Map> addedDatasets = new HashMap(newDatasets);
		Iterator iter = currentDatasets.keySet().iterator();
		while (iter.hasNext()) {
			Map key = (Map) iter.next();
			addedDatasets.remove(key);
		}
		return addedDatasets;
	}
	
	///
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Set<Map> getDeletedDatasets(Map<Map, Map> currentDatasets, Map<Map, Map> newDatasets) {
		Map deletedDatasets = new HashMap(currentDatasets);
		Iterator iter = newDatasets.keySet().iterator();
		while (iter.hasNext()) {
			Map key = (Map) iter.next();
			deletedDatasets.remove(key);
		}

		return deletedDatasets.keySet();
	}
	
	///
	@SuppressWarnings("rawtypes")
	private void findAndRemoveDeletedServices(Set setOfNewServiceIds) throws OException {
		try 
		{			
			Set diffServices = s_ads.diffGridCache( dataSourceCache, "service_id", setOfNewServiceIds );
			
			if (diffServices != null && diffServices.size() > 0 )
			{
				s_ads.removeFromDatasetStatusManager( "service_id", diffServices );
				s_ads.removeFromGridCache(dataSourceCache, "service_id", diffServices );	
				
				if ( Debug.getDEBUG() >= DEBUG_LEVEL_ENUM.DEBUG_MEDIUM.toInt())
				{
					print("Removed services: " + diffServices.toString());
				}
				
			}
		} catch ( Exception e ) {
			
		}
			
	}
}
