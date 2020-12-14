package standard.include;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.olf.apm.Constants;
import com.olf.apm.definition.ColumnDefinition;
import com.olf.apm.definition.ConfigurationDefinition;
import com.olf.apm.definition.FilterDefinition;
import com.olf.apm.definition.PackageDefinition;
import com.olf.apm.exception.FactoryException;
import com.olf.apm.exception.InvalidOlapMetadataException;
import com.olf.apm.exception.OlapMetadataVersionMismatch;
import com.olf.apm.logging.Logger;
import com.olf.apm.olap.ApmOlapProvider;
import com.olf.apm.olap.IApmDomain;
import com.olf.apm.olap.IApmOlapProvider;
import com.olf.apm.olap.IApmSchema;
import com.olf.apm.directory.ApmDirectoryProvider;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/**
 * Class responsible for managing the creation and population of client OLAP metadata for APM.<br/>
 * On construction, the metadata definitions are read from the database, filtered by the list of
 * package names provided. This list of package names is meant to be the packages
 * configured in the APM Service.
 */
public class ApmOlapManager {
	/** The OLAP provider. */
	private final IApmOlapProvider olapProvider;

	/** APM OLAP domain. */
	private IApmDomain apmDomain;

	/**
	 * Creates a new instance of the OLAP manager.
	 * 
	 * @param databaseFacade
	 *            The database facade.
	 * @param packageNames
	 *            The name of the packages set in the APM Service configuration.
	 * 
	 * @throws FactoryException
	 *             If the creation of an instance of APM Lookup provider or OLAP Metadata provider
	 *             fails due to failure to create one of its dependencies.
	 */
	public ApmOlapManager() throws FactoryException {
		this.olapProvider = new ApmOlapProvider();
	}

	/**
	 * Creates the OLAP metadata for the packages covered by this OLAP manager.<br/>
	 * The metadata is not saved.
	 * 
	 * @param configDefinition
	 * @param packageDefinitions
	 * @param filterDefinitions
	 * @param columnDefinitions
	 * 
	 * @throws OException
	 *             If creation of the OLAP metadata fails because the lookup lists' data have not
	 *             been created yet.
	 */
	public void createMetadata(ConfigurationDefinition configDefinition,
	                           Collection<PackageDefinition> packageDefinitions,
	                           Collection<FilterDefinition> filterDefinitions,
	                           Collection<ColumnDefinition> columnDefinitions) throws OException {
		Logger.instance().info("Creating OLAP metadata cubes, dimensions and measures...");

		try {
			apmDomain = olapProvider.createOlapMetadata(configDefinition, packageDefinitions, filterDefinitions, columnDefinitions);
		} catch (Exception exception) {
			Logger.instance().error(exception, "Failed to create the APM OLAP metadata.");

			throw new OException("Failed to create the APM OLAP metadata.");
		}

		Logger.instance().info("Finished creating OLAP metadata.");
	}

	/**
	 * Saves the OLAP metadata to the data store (ADS).<br/>
	 * The lookup lists data must be saved first; saving the OLAP metadata will fail if the lookup
	 * lists don't exist in the data store.
	 * 
	 * @throws FactoryException
	 *             If creation of an instance of the lookup data store fails.
	 * 
	 * @throws InvalidOlapMetadataException
	 *             If the OLAP metadata definition is invalid.
	 * 
	 * @throws OlapMetadataVersionMismatch
	 *             If the version of the OLAP metadata to save is out-dated relative to that in the
	 *             data store.
	 * @throws IOException
	 */
	public void save() throws OlapMetadataVersionMismatch, InvalidOlapMetadataException {
		Logger.instance().info("Saving APM OLAP domain...");

		apmDomain.save();

		Logger.instance().info("Finished saving APM OLAP domain.");
	}

	/**
	 * Exports the OLAP metadata as a Mondrian schema XML.
	 * 
	 * @param filename
	 *            Full name of the file to write the XML schema to (includes the full path to the
	 *            file and the file's full name).
	 * 
	 * @throws IOException
	 *             If the export fails.
	 * 
	 * @throws OException
	 *             If the file name is incorrect: {@code null} or empty string.
	 */
	public void exportAsMondrian(String filename) throws IOException, OException {
		if (filename == null) {
			throw new OException("The name of the file for the Mondrian schema export cannot be null");
		}

		if (filename.isEmpty()) {
			throw new OException("The name of the file for the Mondrian schema export cannot be empty string");
		}

		Logger.instance().info("Exporting APM OLAP as Mondrian schema XML...");

		IApmSchema apmMondrianSchema = apmDomain.getSchema(Constants.APM_MONDRIAN_SCHEMA_NAME);

		if (apmMondrianSchema != null) {
			Map<String, String> errors = apmMondrianSchema.exportAsMondrian(filename);

			for (Entry<String, String> entry : errors.entrySet()) {
				String cubeName = entry.getKey();
				String message = entry.getValue();

				Logger.instance().warning("On exporting the cube %1$s the following message was returned: %2$s", cubeName, message);
			}
		} else {
			Logger.instance().warning("These is no APM Mondrian schema to export.");
		}

		Logger.instance().info("Finished exporting APM OLAP as Mondrian schema XML.");
	}

   public static void createDirectory(Table tAPMArgumentTable, String cacheName, String packageName, String entityGroupName, String entityType, String datasetTypeName, String scenarioName) throws OException {
	   try {
		   Logger.instance().info("Creating APM Directory...");
		   
		   ApmDirectoryProvider directory = new ApmDirectoryProvider();
		   
		   String serviceName = tAPMArgumentTable.getString("service_name", 1);
		   
		   directory.createDirectory(serviceName, cacheName, packageName, entityGroupName, entityType, datasetTypeName, scenarioName);
		   
		   Logger.instance().info("APM Directory Created.");
	   } catch (FactoryException exception) {
		   Logger.instance().warning(exception, "An exception occurred while creating the APM Directory!");
		   exception.printStackTrace();
	   }
	   
   }
   
   public static void pruneDirectory(Table tAPMArgumentTable) throws OException {
       try {
           Logger.instance().info("Pruning APM Directories...");
           
           ApmDirectoryProvider directory = new ApmDirectoryProvider();
           
           String serviceName = tAPMArgumentTable.getString("service_name", 1);
           
           directory.removeServiceFromDirectoryEntities(serviceName);
           
           Logger.instance().info("APM Directories Pruned.");
       } catch (FactoryException exception) {
           Logger.instance().warning(exception, "An exception occurred while pruning the APM Directories!");
           exception.printStackTrace();
       }
   }
   
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
