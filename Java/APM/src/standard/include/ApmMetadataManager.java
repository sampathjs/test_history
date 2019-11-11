/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.include;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.apm.data.DbTable;
import com.olf.apm.data.DbTable.Row;
import com.olf.apm.data.IDatabaseFacade;
import com.olf.apm.definition.ColumnDefinition;
import com.olf.apm.definition.ConfigurationDefinition;
import com.olf.apm.definition.DefinitionsFactory;
import com.olf.apm.definition.FilterDefinition;
import com.olf.apm.definition.PackageDefinition;
import com.olf.apm.exception.DatabaseException;
import com.olf.apm.logging.Logger;

public class ApmMetadataManager {
	/** Name of the global package. Packages that aren't named explicitly are global. */
	public static final String GLOBAL_PACKAGE_NAME = "Global";

	/** Name of the stored procedure to get the APM version details: {@value} */
	private static final String APM_VERSION_DETAIL_SP_NAME = "USER_apm_retrieve_ver_details";

	/** The SQL WHAT to get the APM configuration information: {@value} */
	private static final String APM_CONFIGURATION_SQL_WHAT = "tfe_configuration.setting_name name, tfe_configuration.label label, tfe_configuration.description description, tfe_configuration.gui_control_type gui_control_type, tfe_configuration.value str_value, tfe_configuration.default_value default_on_off_str";
	/** The SQL FROM to get the APM configuration information: {@value} */
	private static final String APM_CONFIGURATION_SQL_FROM = "tfe_configuration";
	/** The SQL WHERE to get the APM configuration information: {@value} */
	private static final String APM_CONFIGURATION_SQL_WHERE = "tfe_configuration.type = 0";

	/** Name of the stored procedure to get the APM package defintions: {@value} */
	private static final String APM_PACKAGE_DEFINITIONS_SP_NAME = "USER_retrieve_apm_pkg_defs";

	/** The SQL WHAT to get the APM package settings: {@value} */
	private static final String APM_PACKAGE_SETTINGS_SQL_WHAT = "tfe_configuration.package_name parent_pkg_name, tfe_configuration.setting_name name, tfe_configuration.label label, tfe_configuration.description description, tfe_configuration.gui_control_type gui_control_type, tfe_configuration.value str_value,tfe_configuration.default_value default_on_off_str";
	/** The SQL FROM to get the APM package settings: {@value} */
	private static final String APM_PACKAGE_SETTINGS_SQL_FROM = "tfe_configuration, tfe_package_defs, tfe_version";
	/** The SQL WHERE to get the APM package settings: {@value} */
	private static final String APM_PACKAGE_SETTINGS_SQL_WHERE = "tfe_package_defs.db_upgrade = 'Completed.' and tfe_package_defs.apm_version = tfe_version.apm_version and tfe_version.db_upgrade = 'Completed.' and tfe_configuration.package_name = tfe_package_defs.package_name";

	/** Name of the stored procedure to get the APM filters metadata: {@value} */
	private static final String APM_FILTERS_METADATA_SP_NAME = "USER_retrieve_apm_filter_defs";
	/** Name of the stored procedure to get the APM filters settings: {@value} */
	private static final String APM_FILTERS_SETTINGS_SP_NAME = "USER_retrieve_apm_fltr_hrchy";

	/** Name of the stored procedure to get the APM columns metadata: {@value} */
	private static final String APM_COLUMNS_METADATA_SP_NAME = "USER_retrieve_apm_column_defs";

	/** The SQL WHAT to get the APM columns settings: {@value} */
	private static final String APM_COLUMNS_SETTINGS_SQL_WHAT = "tfe_pkg_folder_columns.*";
	/** The SQL FROM to get the APM columns settings: {@value} */
	private static final String APM_COLUMNS_SETTINGS_SQL_FROM = "tfe_package_defs, tfe_pkg_folder_columns, tfe_version, tfe_column_defs";
	/** The SQL WHERE to get the APM columns settings: {@value} */
	private static final String APM_COLUMNS_SETTINGS_SQL_WHERE = "tfe_package_defs.db_upgrade = 'Completed.' and tfe_pkg_folder_columns.package_name = tfe_package_defs.package_name and tfe_package_defs.apm_version = tfe_version.apm_version and tfe_version.db_upgrade = 'Completed.' and tfe_column_defs.column_name = tfe_pkg_folder_columns.column_name";

	private static final String APM_MONDRIAN_EXCLUSIONS_SQL_WHAT = "object_name, object_granularity";
	private static final String APM_MONDRIAN_EXCLUSIONS_SQL_FROM = "USER_APM_MondrianExclusions";
	private static final String APM_MONDRIAN_EXCLUSIONS_SQL_WHERE = "1=1";

	private static final String APM_DIMENSION_HIERARCHIES_SP_NAME = "USER_apm_get_dim_hierarchies";

	/** Database abstraction. */
	private final IDatabaseFacade db;
	/** Names of the packages enabled. */
	private final Set<String> packages;

	/** APM configuration definition. */
	private ConfigurationDefinition configDefinition;
	/** APM package definitions. */
	private Collection<PackageDefinition> packageDefinitions;
	/** APM filter definitions. */
	private Collection<FilterDefinition> filterDefinitions;
	/** APM column definitions. */
	private Collection<ColumnDefinition> columnDefinitions;

	/**
	 * The Mondrian exclusions, indexed by object type (i.e., {@code object_granularity} column in
	 * the {@code user_apm_mondrianexclusions} table).
	 */
	private Map<String, Set<String>> mondrianExclusionsPerObjectType;

	public ApmMetadataManager(IDatabaseFacade databaseFacade, Set<String> packageNames)
	        throws DatabaseException {
		this.db = databaseFacade;
		this.packages = packageNames;

		loadMetadataDefinitions(packages);
	}

	/**
	 * Loads the metadata definitions for the packages with the specified names.
	 * 
	 * @param packageNames
	 *            Name of the packages to load the metadata.
	 * 
	 * @throws DatabaseException
	 *             If the database access failed.
	 */
	private void loadMetadataDefinitions(Set<String> packageNames) throws DatabaseException {
		Logger.instance().info("Loading metadata definitions from DB");

		mondrianExclusionsPerObjectType = getMondrianExclusions(db);
		configDefinition = getConfigurationDefinition(db, packageNames);
		packageDefinitions = getPackageDefinitions(db, packageNames, mondrianExclusionsPerObjectType.get("Package"));
		filterDefinitions = getFilterDefinitions(db, packageNames, mondrianExclusionsPerObjectType.get("Filter"));
		columnDefinitions = getColumnDefinitions(db, packageNames, mondrianExclusionsPerObjectType.get("Column"));

		Logger.instance().info("Finished reading metadata definitions from DB");
	}

	public static Map<String, Set<String>> getMondrianExclusions(IDatabaseFacade databaseFacade) throws DatabaseException {
		Logger.instance().info("Loading Mondrian exclusions from DB using SQL 'select %1$s from %2$s where %3$s'.", APM_MONDRIAN_EXCLUSIONS_SQL_WHAT, APM_MONDRIAN_EXCLUSIONS_SQL_FROM, APM_MONDRIAN_EXCLUSIONS_SQL_WHERE);
		DbTable mondrianExclusions = databaseFacade.getTableFromSql(APM_MONDRIAN_EXCLUSIONS_SQL_WHAT, APM_MONDRIAN_EXCLUSIONS_SQL_FROM, APM_MONDRIAN_EXCLUSIONS_SQL_WHERE);
		Logger.instance().info("Loaded %d rows from the DB.", mondrianExclusions.numberOfRows());

		Map<String, Set<String>> mondrianExclusionsPerGranularity = new HashMap<>();

		for (Row row : mondrianExclusions) {
			String name = row.getString("object_name");
			String granularity = row.getString("object_granularity");

			Set<String> exclusions = mondrianExclusionsPerGranularity.get(granularity);
			if (exclusions == null) {
				exclusions = new HashSet<>();

				mondrianExclusionsPerGranularity.put(granularity, exclusions);
			}

			exclusions.add(name);
		}

		Logger.instance().info("Finished loading Mondrian exclusions: %s", mondrianExclusionsPerGranularity);

		return mondrianExclusionsPerGranularity;
	}

	public static ConfigurationDefinition getConfigurationDefinition(IDatabaseFacade databaseFacade,
	                                                                 final Set<String> packageNames) throws DatabaseException {
		Logger.instance().info("Generating APM configuration definitions.");

		Logger.instance().info("Loading APM version details from DB using store procedure '%1$s'", APM_VERSION_DETAIL_SP_NAME);
		DbTable versionDetails = databaseFacade.getTableFromStoredProc(APM_VERSION_DETAIL_SP_NAME);
		Logger.instance().info("Loaded %d rows from the DB.", versionDetails.numberOfRows());

		String configSettingsWhat = APM_CONFIGURATION_SQL_WHAT;
		String configSettingsFrom = APM_CONFIGURATION_SQL_FROM;
		String configSettingsWhere = APM_CONFIGURATION_SQL_WHERE;

		if (!packageNames.isEmpty()) {
			// update the query to only keep the packages we're interested in
			String sqlWherePackages = convertToSqlReadyString(packageNames);
			configSettingsWhere += " and (tfe_configuration.package_name in " + sqlWherePackages
			        + " or tfe_configuration.package_name = '' or tfe_configuration.package_name is null)";
		}

		Logger.instance().info("Loading APM configuration settings from DB using SQL 'select %1$s from %2$s where %3$s'.", configSettingsWhat, configSettingsFrom, configSettingsWhere);
		DbTable configSettings = databaseFacade.getTableFromSql(configSettingsWhat, configSettingsFrom, configSettingsWhere);
		Logger.instance().info("Loaded %d rows from the DB.", configSettings.numberOfRows());

		// schema level definitions
		ConfigurationDefinition configDefinition = DefinitionsFactory.createConfigurationSettings(versionDetails, configSettings);
		Logger.instance().info("Finished generating APM configuration definitions.");

		return configDefinition;
	}

	public static Collection<PackageDefinition> getPackageDefinitions(IDatabaseFacade databaseFacade,
	                                                                  final Set<String> packageNames,
	                                                                  Set<String> mondrianExclusions) throws DatabaseException {
		Logger.instance().info("Generating APM package definitions.");

		Logger.instance().info("Loading APM packages metadata from DB using stored procedure '%1$s'", APM_PACKAGE_DEFINITIONS_SP_NAME);
		DbTable packagesMetadata = databaseFacade.getTableFromStoredProc(APM_PACKAGE_DEFINITIONS_SP_NAME);
		Logger.instance().info("Loaded %d rows from the DB.", packagesMetadata.numberOfRows());
		

		String packageSettingsWhat = APM_PACKAGE_SETTINGS_SQL_WHAT;
		String packageSettingsFrom = APM_PACKAGE_SETTINGS_SQL_FROM;
		String packageSettingsWhere = APM_PACKAGE_SETTINGS_SQL_WHERE;

		if (!packageNames.isEmpty()) {
			// select only the rows for the packages we're interested in
			packagesMetadata = packagesMetadata.select(new DbTable.Selector() {
				@Override
				public boolean isValid(Row row) {
					String packageName = row.getString("package_name");

					// this filters in the null or empty package names (for the Global package) and those we have set
					if (packageName == null || packageName.isEmpty() || packageNames.contains(packageName)) {
						return true;
					}

					return false;
				}
			});

			// update the query to only keep the packages we're interested in
			String sqlWherePackages = convertToSqlReadyString(packageNames);
			packageSettingsWhere += " and (tfe_package_defs.package_name in " + sqlWherePackages
			        + " or tfe_package_defs.package_name = '' or tfe_package_defs.package_name is null)";
		}

		Logger.instance().info("Loading APM package settings from DB using SQL 'select %1$s from %2$s where %3$s'.", packageSettingsWhat, packageSettingsFrom, packageSettingsWhere);
		DbTable packagesSettings = databaseFacade.getTableFromSql(packageSettingsWhat, packageSettingsFrom, packageSettingsWhere);
		Logger.instance().info("Loaded %d rows from the DB.", packagesSettings.numberOfRows());

		// package/cube level definitions
		Collection<PackageDefinition> packageDefinitions = DefinitionsFactory.createPackageDefinitions(packagesMetadata, packagesSettings, mondrianExclusions);
		Logger.instance().info("Finished generating APM package definitions.");

		return packageDefinitions;
	}

	public static Collection<FilterDefinition> getFilterDefinitions(IDatabaseFacade databaseFacade,
	                                                                final Set<String> packageNames,
	                                                                Set<String> mondrianExclusions) throws DatabaseException {
		Logger.instance().info("Generating APM filter definitions");

		Logger.instance().info("Loading APM filters settings from DB using store procedure '%1$s'.", APM_FILTERS_SETTINGS_SP_NAME);
		DbTable filtersSettings = databaseFacade.getTableFromStoredProc(APM_FILTERS_SETTINGS_SP_NAME);
		Logger.instance().info("Loaded %d rows from the DB.", filtersSettings.numberOfRows());

		if (!packageNames.isEmpty()) {
			// select only the rows for the packages we're interested in
			filtersSettings = filtersSettings.select(new DbTable.Selector() {
				@Override
				public boolean isValid(Row row) {
					String packageName = row.getString("package_name");

					// this filters in the null or empty package names (for the Global package) and those we have set
					if (packageName == null || packageName.isEmpty() || packageNames.contains(packageName)) {
						return true;
					}

					return false;
				}
			});
		}

		final Set<String> filterNamesForSelectedPackages = new HashSet<>();

		for (Row row : filtersSettings) {
			String filterName = row.getString("filter_name");
			filterNamesForSelectedPackages.add(filterName);
		}

		Logger.instance().info("Loading APM filters metadata from DB using store procedure '%1$s'.", APM_FILTERS_METADATA_SP_NAME);
		DbTable filtersMetadata = databaseFacade.getTableFromStoredProc(APM_FILTERS_METADATA_SP_NAME);
		Logger.instance().info("Loaded %d rows from the DB.", filtersMetadata.numberOfRows());

		// get only the filters that match the names of those in the service's packages
		filtersMetadata = filtersMetadata.select(new DbTable.Selector() {
			@Override
			public boolean isValid(Row row) {
				String filterName = row.getString("filter_name");

				if (filterNamesForSelectedPackages.contains(filterName)) {
					return true;
				}

				return false;
			}
		});

		// column/measure level definitions
		Collection<FilterDefinition> filterDefinitions = DefinitionsFactory.createFilterDefinitions(filtersMetadata, filtersSettings, mondrianExclusions);
		Logger.instance().info("Finished generating APM filter definitions.");

		return filterDefinitions;
	}

	public static Collection<ColumnDefinition> getColumnDefinitions(IDatabaseFacade databaseFacade,
	                                                                final Set<String> packageNames,
	                                                                Set<String> mondrianExclusions) throws DatabaseException {
		Logger.instance().info("Generating APM column definitions.");

		String columnsSettingsWhat = APM_COLUMNS_SETTINGS_SQL_WHAT;
		String columnsSettingsFrom = APM_COLUMNS_SETTINGS_SQL_FROM;
		String columnsSettingsWhere = APM_COLUMNS_SETTINGS_SQL_WHERE;

		if (!packageNames.isEmpty()) {
			// update the query to only keep the packages we're interested in
			String sqlWherePackages = convertToSqlReadyString(packageNames);
			columnsSettingsWhere += " and (tfe_package_defs.package_name in " + sqlWherePackages
			        + " or tfe_package_defs.package_name = '' or tfe_package_defs.package_name is null)";
		}

		Logger.instance().info("Loading APM column settings from DB using SQL 'select %1$s from %2$s where %3$s'", columnsSettingsWhat, columnsSettingsFrom, columnsSettingsWhere);
		DbTable columnsSettings = databaseFacade.getTableFromSql(columnsSettingsWhat, columnsSettingsFrom, columnsSettingsWhere);
		Logger.instance().info("Loaded %d rows from the DB.", columnsSettings.numberOfRows());

		final Set<String> columnNames = new HashSet<>();
		for (Row row : columnsSettings) {
			String columnName = row.getString("column_name");
			columnNames.add(columnName);
		}

		Logger.instance().info("Loading APM column metadata from DB using stored procedure '%1$s'.", APM_COLUMNS_METADATA_SP_NAME);
		DbTable columnsMetadata = databaseFacade.getTableFromStoredProc(APM_COLUMNS_METADATA_SP_NAME);
		Logger.instance().info("Loaded %d rows from the DB.", columnsMetadata.numberOfRows());
		
		columnsMetadata = columnsMetadata.select(new DbTable.Selector() {
			@Override
			public boolean isValid(Row row) {
				String columnName = row.getString("column_name");

				if (columnNames.contains(columnName)) {
					return true;
				}

				return false;
			}
		});
		
		Logger.instance().info("Filtered to the columns that are enabled; remaining %d rows.", columnsMetadata.numberOfRows());

		// filter/dimension level definitions
		Collection<ColumnDefinition> columnDefinitions = DefinitionsFactory.createColumnDefinitions(columnsMetadata, columnsSettings, mondrianExclusions);
		Logger.instance().info("Finished generating APM column definitions.");

		return columnDefinitions;
	}

	private static String convertToSqlReadyString(Set<String> packageNames) {
		packageNames.add(GLOBAL_PACKAGE_NAME);

		StringBuilder allPackages = new StringBuilder();
		allPackages.append("(");

		boolean first = true;
		for (String packageName : packageNames) {
			if (first) {
				first = false;
			} else {
				allPackages.append(",");
			}

			allPackages.append("'");
			allPackages.append(packageName);
			allPackages.append("'");
		}

		allPackages.append(")");

		return allPackages.toString();
	}

	/**
	 * Returns the configuration definition for the current APM installation.
	 * 
	 * @return Configuration definition.
	 */
	public ConfigurationDefinition getConfigurationDefinition() {
		return configDefinition;
	}

	/**
	 * Returns the package definitions for the packages set in the respective APM service.
	 * 
	 * @return Package definitions.
	 */
	public Collection<PackageDefinition> getPackageDefinitions() {
		return packageDefinitions;
	}

	/**
	 * Returns the enabled filter definitions for the packages set in the respective APM service.
	 * 
	 * @return Filter definitions.
	 */
	public Collection<FilterDefinition> getFilterDefinitions() {
		return filterDefinitions;
	}

	/**
	 * Returns the enabled column definitions for the packages set in the respective APM service.
	 * 
	 * @return Column definitions.
	 */
	public Collection<ColumnDefinition> getColumnDefinitions() {
		return columnDefinitions;
	}
}
