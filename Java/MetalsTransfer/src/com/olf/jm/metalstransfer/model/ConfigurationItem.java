package com.olf.jm.metalstransfer.model;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/*
 * History:
 * 2016-02-23	V1.0	jwaechter	-	Initial Version
 */

/**
 * Enum containing constants, especially ConstantsRepository variables.
 * @author jwaechter
 * @version 1.0
 */
public enum ConfigurationItem implements ConstRepItem {
	LOG_LEVEL ("logLevel", "INFO"),
	LOG_FILE ("logFile", "MetalTransfers.log"),
	LOG_DIRECTORY ("logDir", "%AB_OUTDIR%"),
	ALLOWED_USERS ("allowedUsers", "fa_ol_user1, fa_ol_user2");
	;

	public static final String CONST_REP_CONTEXT="FrontOffice";
	public static final String CONST_REP_SUBCONTEXT="MetalTransfers";
	
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
				value = constRep.getStringValue(constRepVarName, defaultValue).trim();
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

}
