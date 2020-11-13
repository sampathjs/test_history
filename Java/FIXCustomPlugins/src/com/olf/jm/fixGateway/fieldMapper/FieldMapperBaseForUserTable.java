package com.olf.jm.fixGateway.fieldMapper;

import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;


/*
 * History:
 * 2020-05-13 - V0.1 - jwaechter - Initial Version
 */


/**
 * This class implements the complete mapping using the user table "USER_jm_connex_fix_mapping" 
 * instead of the Connex Reference Data Mapping.
 */
public abstract class FieldMapperBaseForUserTable extends FieldMapperBase {
	public static final String USER_TABLE_CUSTOM_MAPPING = "USER_jm_connex_fix_mapping";
	public static final String COMPLEX_TAG_FLAG = "*";
	
	private final String mapName;
	private final String mapType;
	private Map<String, String> externalToInternal = new HashMap<>();
	
	public FieldMapperBaseForUserTable (final String mapName, final String mapType) {
		this.mapName = mapName;
		this.mapType = mapType;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		String tagValue = null;
		String mappedTagValue = null;
		try {
			if(message == null || message.getNumRows() != 1) {
				String errorMessage = "Invalid message table, table is null or wrong number of rows.";
				Logging.error(errorMessage);
				throw new FieldMapperException(errorMessage);				
			}
		} catch (OException e1) {
			String errorMessage = "Error validating the message table. " + e1.getMessage();
			Logging.error(errorMessage);
			for (StackTraceElement ste : e1.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw new FieldMapperException(errorMessage);	
		}
				
		try {
			if (getTagFieldName() != null) {
				tagValue = message.getString(getTagFieldName(), 1);				
			} else {
				tagValue = getComplexTagValue(message);
			}
			Logging.info("Mapping field " + getTagFieldName() + "/ map type='" + mapType + 
					"'  raw value " + tagValue + " to Endur field " + getTranFieldName().toString());
			retrieveMappings();
			mappedTagValue = customMapTagValue (tagValue);
			Logging.info("Mapping field " + getTagFieldName()+ "/ map type='" + mapType +  
					"' mapped value " + mappedTagValue);
		} catch (OException e) {
			String errorMessage = "Error reading field " + getTagFieldName() + ". " + e.getMessage();
			Logging.error(errorMessage);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw new FieldMapperException(errorMessage);
		}
		return mappedTagValue;
	}
	
	protected abstract String getComplexTagValue(Table message) throws FieldMapperException;
	
	private void retrieveMappings () {
		String fixColumnName = (getTagFieldName()!=null)?getTagFieldName():"*";
		String sqlMetadata = 
				"\nSELECT DISTINCT m.endur_table_name, m.endur_id_column, m.endur_value_column"
			+	"\nFROM " + USER_TABLE_CUSTOM_MAPPING + " m"
			+   "\nWHERE m.fix_column_name = '" + fixColumnName + "'"
			+   "\n  AND m.map_name = '" + mapName + "'"
			+   "\n  AND m.map_type = '" + mapType + "'"
			;
		Table metadata = null;
		try {
			metadata = Table.tableNew(sqlMetadata); 
			int ret = DBaseTable.execISql(metadata, sqlMetadata);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL '" + sqlMetadata + "':"));
			}
			// now retrieve the mappings for the FIX Tag and the mapping type grouped
			for (int row=metadata.getNumRows();row >= 1; row--) {
				String endurTableName = metadata.getString("endur_table_name", row);
				String endurIdColumn = metadata.getString("endur_id_column", row);
				String endurValueColumn = metadata.getString("endur_value_column", row);
				String sqlMapping = 
						   "\nSELECT m.external_value, r." + endurValueColumn + " endur_value "
						+  "\nFROM " + USER_TABLE_CUSTOM_MAPPING + " m"
						+  "\n  INNER JOIN " + endurTableName + " r"
						+  "\n    ON r."  + endurIdColumn + " = m.endur_id"
						+  "\nWHERE m.fix_column_name = '" + fixColumnName + "'"
						+  "\n  AND m.map_name = '" + mapName + "'"		
						+  "\n  AND m.map_type = '" + mapType + "'"
						;
				Table mapping = null;
				try {
					mapping = Table.tableNew(sqlMapping);
					ret = DBaseTable.execISql(mapping, sqlMapping);
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
						throw new RuntimeException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL '" + sqlMapping + "':"));
					}
					for (int rowMapping = mapping.getNumRows(); rowMapping >= 1; rowMapping--) {
						String external = mapping.getString("external_value", rowMapping);
						String internal = mapping.getString("endur_value", rowMapping);
						externalToInternal.put(external, internal);
					}
				} catch (OException e) {
					Logging.error("Error executing SQL '" + sqlMetadata + "':" );
					Logging.error(e.toString());
					for (StackTraceElement ste : e.getStackTrace()) {
						Logging.error(ste.toString());
					}
				} finally {
					mapping = TableUtilities.destroy(mapping);
				}
			}
		} catch (OException e) {
			Logging.error("Error executing SQL '" + sqlMetadata + "':" );
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
		} finally {
			metadata = TableUtilities.destroy(metadata);
		}
	}

	private String customMapTagValue(String tagValue) throws FieldMapperException {
		if (externalToInternal.containsKey(tagValue)) {
			return externalToInternal.get(tagValue);
		} else {
			String message = "The custom mapping in table '" + USER_TABLE_CUSTOM_MAPPING + "' "
					+ " for map_name = '" + mapName + "' "
					+ " and fix column '" + getTagFieldName() + "'"
					+ " does not contain a mapping for value '" + tagValue + "'"
					+ " or the provided endur_id is invalid";
			Logging.warn(message);
			// note: the FIX Gateway automatically maps certain values, e.g. OrdStatus, OrdType
			// and side (buy/sell). As those values are already premapped you would not find 
			// a corresponding value in the map. The Connex Reference Mappings are ignoring premapped
			// values. This custom mapper is following the same approach.
			// As a result, a missing mapping is going to result in an error message stating
			// the original value of the incoming FIX message can't be set to the Endur value.
			return tagValue; 
		}
	}
}
