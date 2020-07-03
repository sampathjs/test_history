package com.jm.shanghai.accounting.udsr.model.fixed;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/*
 * History:
 * 2018-11-14	V1.0	jwaechter	-	Initial Version
 * 2018-11-15	V1.1	jwaechter	-   Added table items
 * 2019-01-02	V1.2	jwaechter	-   Added tax/material number items
 * 2019-07-14	V1.3	jwaechter	- 	Added customer company mapping table items
 * 2019-07-15	V1.4	jwaechter	-   removed entries regarding mapping tables.
 */

/**
 * Enum containing constants, especially ConstantsRepository variables.
 * @author jwaechter
 * @version 1.4
 */
public enum ConfigurationItem implements ConstRepItem {
	LOG_LEVEL ("logLevel", "INFO"),
	LOG_FILE ("logFile", "ShanghaiAccounting.log"),
	LOG_DIRECTORY ("logDir", "%AB_OUTDIR%"),
	VIEW_RUNTIME_DATA_TABLE_BEFORE_MAPPING ("ViewRuntimeDataTableBeforeMapping", "false"),
	VIEW_RUNTIME_DATA_TABLE_AFTER_MAPPING ("ViewRuntimeDataTableAfterMapping", "false"),
	RETRIEVAL_CONFIG_TABLE_NAME ("RetrievalConfigTableName", "USER_jm_acc_retrieval_config"),
	GL_PREFIX ("General Ledger Prefix", "GL"),
	SL_PREFIX ("Sales Ledger Prefix", "SL"),
	IA_PREFIX ("Interest Accrual Prefix", "IA"),
	SHANGHAI_COMPANY_ID ("Shanghai Company ID", "CN10"), // AccountingDocument -> Header ->  Company ID in XML output file
	OUTPUT_FILE_IN_TABLE_VIEWER ("ShowOutputFileInTableViewer", "false");
	;

	public static final String CONST_REP_CONTEXT="Accounting";
	public static final String CONST_REP_SUBCONTEXT="Shanghai";
	
	private static final ConstRepository constRep;
	
	static {
		try {
			constRep = new ConstRepository(CONST_REP_CONTEXT, CONST_REP_SUBCONTEXT);
		} catch (OException e) {
			throw new RuntimeException ("Error initializing ConstantsRepository", e);
		}
	}
	
	private String value;
	private final String constRepVarName;
	private final String defaultValue;

	private ConfigurationItem (final String value) {
		this.value = value;
		this.constRepVarName = null;
		this.defaultValue = null;
	}
	
	private ConfigurationItem (final String constRepVarName, final String defaultValue) {
		this.constRepVarName = constRepVarName;
		this.defaultValue = defaultValue;
		this.value = null;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.interfaces.lims.model.ConstRepItem#getValue()
	 */
	@Override
	public String getValue () {
		if (value == null && constRepVarName != null) {
			try {
				value = constRep.getStringValue(constRepVarName, defaultValue).replaceAll(ConstRepItem.PR, "%");
			} catch (ConstantTypeException | ConstantNameException | OException e) {
				throw new RuntimeException ("Error retrieving value from Constants Repository", e);
			} 
		}
		return value;
	}

	@Override
	public String getContext() {
		// TODO Auto-generated method stub
		return CONST_REP_CONTEXT;
	}

	@Override
	public String getSubContext() {
		// TODO Auto-generated method stub
		return CONST_REP_SUBCONTEXT;
	}

	@Override
	public String getVarName() {
		return constRepVarName;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * Sets all values to null, triggering re- retrieval from DB for any call to {@link #getValue()}
	 * after that.
	 */
	public static void resetValues () {
		for (ConfigurationItem c : ConfigurationItem.values()) {
			c.value = null;
		}
	}
}
