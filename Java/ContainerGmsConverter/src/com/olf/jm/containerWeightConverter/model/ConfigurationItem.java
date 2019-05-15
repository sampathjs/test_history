package com.olf.jm.containerWeightConverter.model;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/*
 * History:
 * 2017-10-18	V1.0	scurran	-	Initial Version
 */

public enum ConfigurationItem implements ConstRepItem {
	LOG_LEVEL ("logLevel","INFO"),	
	LOG_FILE ("logFile","ContainerWeightConverter.log"),	
	LOG_DIRECTORY ("logDir", "%AB_OUTDIR%\\error_logs"),
	HK_CONVERSION_FACTOR ("hkConversionFactor", "31.103468"),
	NUM_DECIMAL_PLACES ("gmsDecimalPlaces", "2");
	
	public static final String CONST_REP_CONTEXT="Warehouse";
	public static final String CONST_REP_SUBCONTEXT="ContainerWeightConverter";
	
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
				value = constRep.getStringValue(constRepVarName, defaultValue);
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
	
	public String getConstRepPath () {
		return getContext() + "\\" + getSubContext() + "\\" + getVarName();
	}
}
