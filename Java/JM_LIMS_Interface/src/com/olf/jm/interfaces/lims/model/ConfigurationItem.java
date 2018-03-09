package com.olf.jm.interfaces.lims.model;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/*
 * History:
 * 2015-09-01	V1.0	jwaechter	-	Initial Version
 * 2016-04-12	V1.1	jwaechter	-   Added SKIPPED_MEASURES_JM_CASE, SKIPPED_MEASURES_NON_JM_CASE
 */

/**
 * Enum containing constants, especially ConstantsRepository variables.
 * @author jwaechter
 * @version 1.1
 */
public enum ConfigurationItem implements ConstRepItem {
	LOG_LEVEL ("logLevel", "INFO"),
	LOG_FILE ("logFile", "LIMInterface.log"),
	LOG_DIRECTORY ("logDir", "%AB_OUTDIR%"),
	MANUAL_MEASURES_JM_CASE("manualMeasuresJMCase", "LOI, LOR"),
	MANUAL_MEASURES_NON_JM_CASE("manualMeasuresNonJMCase", ""),
	RIGHT_PADDING("rightPadding", "10"),
	SAFE_SECURITY_GROUP ("securityGroupSafeUser", "Safe / Warehouse"),
	AS400_SAMPLE_SERVER_NAME_UK ("SampleSeverNameUK", "172.30.4.5"),
	AS400_RESULT_SERVER_NAME_UK ("ResultServerNameUK", "172.30.4.5"),
	AS400_SAMPLE_DB_NAME_UK ("SampleDBNameUK", "LIMR5LDTA"),
	AS400_RESULT_DB_NAME_UK ("ResultDBNameUK", "LIMR5LDTA"),
	AS400_SAMPLE_USER_UK ("SampleUserNameUK", "openlink"),
	AS400_RESULT_USER_UK ("ResultUserNameUK", "openlink"),
	AS400_SAMPLE_PASSWORD_UK ("SamplePasswordUK", "olremote"),
	AS400_RESULT_PASSWORD_UK ("ResultPasswordUK", "olremote"),	
	AS400_SAMPLE_QUERY_UK ("SampleQueryUK", "SELECT SAMPLE_NUMBER, PRODUCT, JM_BATCH_ID FROM LIMR5LDTA.sample WHERE Upper(JM_BATCH_ID) Like ('" + ConstRepItem.PR + "s" + ConstRepItem.PR + "" + ConstRepItem.PR + "') AND PRODUCT IN (" + ConstRepItem.PR + "s) AND STATUS='A'"),
	AS400_RESULT_QUERY_UK ("ResultQueryUK", "SELECT NAME, UNITS, FORMATTED_ENTRY FROM LIMR5LDTA.result WHERE SAMPL00001=" + ConstRepItem.PR + "s AND ANALYSIS='" + ConstRepItem.PR + "s' AND STATUS='A'"),
	AS400_SAMPLE_SERVER_NAME_US ("SampleSeverNameUS", "172.30.4.5"),
	AS400_RESULT_SERVER_NAME_US ("ResultServerNameUS", "172.30.4.5"),
	AS400_SAMPLE_DB_NAME_US ("SampleDBNameUS", "LIMR5LDTA"),
	AS400_RESULT_DB_NAME_US ("ResultDBNameUS", "LIMR5LDTA"),
	AS400_SAMPLE_USER_US ("SampleUserNameUS", "openlink"),
	AS400_RESULT_USER_US ("ResultUserNameUS", "openlink"),
	AS400_SAMPLE_PASSWORD_US ("SamplePasswordUS", "olremote"),
	AS400_RESULT_PASSWORD_US ("ResultPasswordUS", "olremote"),
	AS400_SAMPLE_QUERY_US ("SampleQueryUS", "SELECT SAMPLE_NUMBER, PRODUCT, JM_BATCH_ID FROM LIMR5LDTA.sample WHERE Upper(JM_BATCH_ID) Like ('" + ConstRepItem.PR + "s" + ConstRepItem.PR + "" + ConstRepItem.PR + "') AND PRODUCT IN (" + ConstRepItem.PR + "s) AND STATUS='A'"),
	AS400_RESULT_QUERY_US ("ResultQueryUS", "SELECT NAME, UNITS, FORMATTED_ENTRY FROM LIMR5LDTA.result WHERE SAMPL00001=" + ConstRepItem.PR + "s AND ANALYSIS = '" + ConstRepItem.PR + "s' AND STATUS='A'"),
	;

	public static final String CONST_REP_CONTEXT="Interfaces";
	public static final String CONST_REP_SUBCONTEXT="LIM";
	
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

}
