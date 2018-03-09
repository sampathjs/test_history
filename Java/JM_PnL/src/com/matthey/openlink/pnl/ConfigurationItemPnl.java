package com.matthey.openlink.pnl;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/*
 * History:
 * 2015-09-01	V1.0	jwaechter	-	Initial Version
 * 2016-09-06	V1.1	jwaechter	-	Added logLevel, logDir and logFile 
 * 2017-11-16   V1.2    lma         -   Added minDealNum
 */

/**
 * Enum containing constants, especially ConstantsRepository variables.
 * @author jwaechter
 * @version 1.2
 */
public enum ConfigurationItemPnl implements ConstRepItem {
	LOOK_BACK ("lookBack", ""),
	LOG_LEVEL ("logLevel", "info"),
	LOG_DIR ("logDir", ""),
	LOG_FILE ("logFile", ""),
	MIN_DEAL_NUM ("minDealNum", "") 
	;

	public static final String CONST_REP_CONTEXT="MiddleOffice";
	public static final String CONST_REP_SUBCONTEXT="PnL";
	
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

	private ConfigurationItemPnl (final String value) {
		this.value = value;
		this.constRepVarName = null;
		this.defaultValue = null;
	}
	
	private ConfigurationItemPnl (final String constRepVarName, final String defaultValue) {
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

}
