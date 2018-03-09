package com.matthey.openlink.config;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/*
 * History:
 * 2016-10-03	V1.0	scurran	-	Initial Version
 */

/**
 * Enum containing constants, especially ConstantsRepository variables.
 * @author scurran
 * @version 1.0
 */

public enum ConfigurationItemDocumentChange implements IConstRepItem {
	LOG_LEVEL ("logLevel", "info"),
	LOG_DIR ("logDir", ""),
	LOG_FILE ("logFile", "");
	
	
	public static final String CONST_REP_CONTEXT="JDE";
	public static final String CONST_REP_SUBCONTEXT="DocumentChange";
	
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
	
	private ConfigurationItemDocumentChange (final String value) {
		this.value = value;
		this.constRepVarName = null;
		this.defaultValue = null;
	}
	
	private ConfigurationItemDocumentChange (final String constRepVarName, final String defaultValue) {
		this.constRepVarName = constRepVarName;
		this.defaultValue = defaultValue;
		this.value = null;
	}	
	@Override
	public String getValue () {
		if (value == null && constRepVarName != null) {
			try {
				value = constRep.getStringValue(constRepVarName, defaultValue).replaceAll(IConstRepItem.PR, "%");
			} catch (ConstantTypeException | ConstantNameException | OException e) {
				throw new RuntimeException ("Error retrieving value from Constants Repository", e);
			} 
		}
		return value;
	}

	@Override
	public String getContext() {
		return CONST_REP_CONTEXT;
	}

	@Override
	public String getSubContext() {
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
}
