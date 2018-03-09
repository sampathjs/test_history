package com.olf.jm.metalTransfer.confirmations.dataSource;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;



/**
 * The Class ReportBuilderDataSourceBase. Base class for report builder data sources
 */
public abstract class ReportBuilderDataSourceBase extends AbstractGenericScript {
	
	/** The Constant METADATA_MODE_STRING used then the plugin is running in metadata collection mode. */
	static final String METADATA_MODE_STRING = "Meta Data Collection Mode";
	
	/** The Constant EXECUTION_MODE_STRING used then the plugin is running in data collection mode. */
	static final String EXECUTION_MODE_STRING = "Execution Mode";
	
	/** The Constant METADATA_MODE. */
	static final int METADATA_MODE = 0;
	
	/** The Constant EXECUTION_MODE. */
	static final int EXECUTION_MODE = 1;

	/** The plugin mode. */
	private int pluginMode;

	/** The execution mode. */
	private String executionMode;
	
	/** The context. */
	private Context context;
	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/**
	 * Builds the return table.
	 *
	 * @param returnt the returnt
	 */
	abstract void buildReturnTable(Table returnt);

	/**
	 * Sets data into the return table.
	 *
	 * @param returnt the new return table
	 */
	abstract void setReturnTable(Table returnt);

	/**
	 * Gets the constants repository context label.
	 *
	 * @return the context label
	 */
	abstract String getConstRepContext();

	/**
	 * Gets the constants repository sub context label.
	 *
	 * @return the sub context label
	 */
	abstract String getConstRepSubContext();
	
	/** The data source arguments passed to the plugin. */
	private ConstTable datasourceArguments;
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public final Table execute(final Context currentContext, final ConstTable argt) {
		try {
			init();
		} catch (OException e) {
			String errorMessage = "Error initilising the logger. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		context = currentContext;

		datasourceArguments = argt;

		// Initialize execution mode variables
		setExecutionModes(datasourceArguments);

		PluginLog.info("Starting Data Source Plugin - " + this.getClass()
				+ " in " + executionMode + ".\n");

		// returnt needs to be filled in with the data for the data source
		// or just the columns that are available for the data source
		// depending on the mode.
		Table returnt = context.getTableFactory().createTable();
		
		if (pluginMode == METADATA_MODE) {
			// This mode just tells Report Builder what columns are available
			createMetaDataTable(returnt);
		} else {
			// This mode provides the actual data for the Data Source.
			// This table structure needs to match what is created in the
			// METADATA_MODE
			

			createMetaDataTable(returnt);
			populateDataTable(returnt);
		}
		PluginLog.info("Finished Data Source Plugin - " + this.getClass()
				+ " in " + executionMode + ".\n");

		return returnt;
	}
	
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws OException the o exception
	 */
	protected final void init() throws OException {
		constRep = new ConstRepository(getConstRepContext(), getConstRepSubContext());

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new OException("Error initialising logging. "
					+ e.getMessage());
		}
	}
	
	/**
	 * Sets the execution modes.
	 *
	 * @param argt the new execution modes
	 */
	// Sets loggingLevel and pluginMode for easier flow control
	private void setExecutionModes(final ConstTable argt) {
		pluginMode = argt.getInt("ModeFlag", 0);
		if (pluginMode == METADATA_MODE) {
			executionMode = METADATA_MODE_STRING;
		} else {
			executionMode = EXECUTION_MODE_STRING;
		}
	}	

	/**
	 * Populate data table.
	 * 
	 * If the same method is not used to create the table structure for the Meta
	 * Data collection is not used to create the table for the populate method
	 * then you NEED to make sure they are the same.
	 *
	 * @param returnt the returnt
	 */
	private void populateDataTable(final Table returnt) {
		PluginLog.info("Start populating table for " + executionMode + ".\n");

		setReturnTable(returnt);

		PluginLog
				.info("Finished populating table for " + executionMode + ".\n");
		return;
	}

	/**
	 * Creates the meta data table.
	 * 
	 * This method creates the structure of the table used for the data source.
	 * This and the populate need to have the same structure. 
	 *
	 * @param returnt the returnt
	 */
	private void createMetaDataTable(final Table returnt) {
		PluginLog.info("Creating table for " + executionMode + ".\n");

		buildReturnTable(returnt);

		PluginLog.info("Finished creating table for " + executionMode + ".\n");
		return;
	}
	
	/**
	 * Gets the query result table name.
	 *
	 * @return the query result table name
	 */
	protected final String getQueryResultTableName()  {
		return datasourceArguments.getString("QueryResultTable", 0);
	}
	
	/**
	 * Gets the query result id.
	 *
	 * @return the query result id
	 */
	protected final int getQueryResultID()  {
		return datasourceArguments.getInt("QueryResultID", 0);
	}
	
	/**
	 * Gets the query results.
	 *
	 * @return the query results
	 */
	protected final Table getQueryResults() {
		String sql = "SELECT * from " + getQueryResultTableName() + " WHERE unique_id = " + getQueryResultID();
		
        IOFactory iof = context.getIOFactory();
        
        PluginLog.debug("About to run SQL. \n" + sql);
        
        
        Table queryResultTable = null;
        try {
        	queryResultTable = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            PluginLog.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        
        return queryResultTable;
 	}
	
	/**
	 * Gets the script context.
	 *
	 * @return the script context
	 */
	protected final Context getScriptContext() {
		return context;
	}
}
