package standard.include;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.olf.apm.data.IDatabaseFacade;
import com.olf.apm.definition.FilterDefinition;
import com.olf.apm.exception.FactoryException;
import com.olf.apm.logging.Logger;
import com.olf.apm.lookup.ApmLookupProvider;
import com.olf.apm.lookup.IApmLookupList;
import com.olf.apm.lookup.IApmLookupProvider;

/**
 * Class responsible for managing the creation, population and persisting of the APM lookup lists.
 */
public class ApmLookupManager {
	/** Name of the class implementing the IDataStore interface for the Lookup API: {@value} */
	private static final String LOOKUP_DATA_STORE_IMPLEMENTATION_CLASS_NAME = "com.olf.lookupdata.impl.ADSDataStore";

	/** Database abstraction. */
	private final IDatabaseFacade db;
	/** Lookup provider. */
	private final IApmLookupProvider lookupProvider;
	/** Created lookup lists, indexed by name. */
	private final Map<String, IApmLookupList> lookupListsPerDimensionName;

	/**
	 * Creates a new instance of lookup manager.
	 * 
	 * @param databaseFacade
	 *            The database facade.
	 * 
	 * @throws FactoryException
	 *             If the creation of an instance of APM Lookup provider fails due to failure to
	 *             create one of its dependencies.
	 */
	public ApmLookupManager(IDatabaseFacade databaseFacade) throws FactoryException {
		this.db = databaseFacade;
		this.lookupProvider = new ApmLookupProvider(LOOKUP_DATA_STORE_IMPLEMENTATION_CLASS_NAME);

		this.lookupListsPerDimensionName = new HashMap<>();
	}

	/**
	 * Creates the lookup lists for the provided filter definitions. The lookup data is also loaded
	 * from the respective source and saved into the data store.
	 * 
	 * @param filterDefinitions
	 *            Filter definition for which to create lookup lists.
	 */
	public void createLookupLists(Collection<FilterDefinition> filterDefinitions) {
		Logger.instance().info("Creating lookup lists...");

		for (FilterDefinition filter : filterDefinitions) {
			try {
				IApmLookupList lookupList = lookupProvider.createLookupList(filter, db);

				if (lookupList != null) {
					String filterName = filter.getName();
					lookupListsPerDimensionName.put(filterName, lookupList);
				}
			} catch (Exception exception) {
				// if anything fails, log and carry on to the next
				Logger.instance().error(exception, "Failed to create lookup list for filter %1$s.", filter.getName());
			}
		}

		Logger.instance().info("Finished creating lookup lists.");
	}

	/**
	 * Returns the lookup lists loaded by calling the method {@link #createLookupLists(Collection)},
	 * indexed by the name of the APM filter they belong.
	 * 
	 * @return Lookup lists per filter name.
	 */
	public Map<String, IApmLookupList> getLookupListsByName() {
		return Collections.unmodifiableMap(lookupListsPerDimensionName);
	}

	/**
	 * Returns all the lookup lists loaded calling the method {@link #createLookupLists(Collection)}
	 * 
	 * @return Lookup lists.
	 */
	public Collection<IApmLookupList> getLookupLists() {
		return Collections.unmodifiableCollection(lookupListsPerDimensionName.values());
	}

	/**
	 * Saves the lookup lists to the data store (ADS).
	 */
	public void save() {
		Logger.instance().info("Saving lookup lists to ADS...");

		lookupProvider.saveLookupLists(lookupListsPerDimensionName.values());

		Logger.instance().info("Finished saving lookup lists to ADS...");
	}
}
