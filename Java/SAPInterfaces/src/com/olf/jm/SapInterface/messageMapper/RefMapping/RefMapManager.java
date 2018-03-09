package com.olf.jm.SapInterface.messageMapper.RefMapping;

import com.olf.openjvs.ConnexRefMapManager;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Util;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.OC_REF_MAP_DIRECTION_ENUM;
import com.olf.openjvs.enums.OC_REF_MAP_MAPPING_METHOD_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;


/**
 * The Class RefMapManager. wrapper around the JVS functions to interact with the 
 * Connex ref map manager.
 */
public class RefMapManager implements IRefMapManager {

	/** The mapping definition. */
	private com.olf.openjvs.Table mappingDefinition; 
	
	/** Table containing the columns name and values to apply the lookup to. */
	private com.olf.openjvs.Table lookupValues;
	
	/**
	 * Instantiates a new reference map manager.
	 *
	 * @param definitionName the definition name
	 * @throws RefMapException the ref map exception
	 */
	public RefMapManager(final String definitionName) throws RefMapException {

		PluginLog.debug("Init ref map manager.");
		XString jvsErrorMessage = null;
		try {
			jvsErrorMessage = Str.xstringNew();
			
			mappingDefinition =
			  ConnexRefMapManager.retrieveMapDefinitionsByUser(
			  OC_REF_MAP_DIRECTION_ENUM.OC_REF_MGR_MAP_DIRECTION_EXT_TO_INT, 	// The Map's Direction
			  OC_REF_MAP_MAPPING_METHOD_ENUM.OC_REF_MGR_MAP_METHOD_VAL_TO_VAL, 	// The Mapping Method 
			  -1, 																// Map User ID 
			  definitionName, 													// Map  User Name 
			  -1, 																// Map Type Id 
			  "", 																// Map Type Name
			  jvsErrorMessage);
			
			if (mappingDefinition == Util.NULL_TABLE) {

				String errorMessage = "Error loading reference map for definition " 
						+ definitionName 
						+ ". " 
						+ Str.xstringGetString(jvsErrorMessage);
				
				PluginLog.error(errorMessage);
					
				throw new RefMapException(errorMessage);
			}
		} catch (OException e) {
			String errorMessage = "Error loading reference map for definition " 
					+ definitionName 
					+ ". "
					+ e.getMessage();
			
			PluginLog.error(errorMessage);
				
			throw new RefMapException(errorMessage);
		} finally {
			if (jvsErrorMessage != null) {
				try {
					Str.xstringDestroy(jvsErrorMessage);
				} catch (OException e) {
				}
			}
		}
		
		try {
			lookupValues = ConnexRefMapManager.createTokenValueTbl();
		} catch (OException e) {
			String errorMessage = "Error creating token table for definition " 
					+ definitionName 
					+ ". "
					+ e.getMessage();
			
			PluginLog.error(errorMessage);
				
			throw new RefMapException(errorMessage);
		}
		
		PluginLog.debug("Ref map manager init complete.");
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.RefMapping.IRefMapManager#addMappingLookUp(java.lang.String, java.lang.String)
	 */
	@Override
	public final void addMappingLookUp(final String columnName, final String value) throws RefMapException {

		XString jvsErrorMessage = null;
		
		// Skip empty columns
		if (value == null || value.length() == 0) {
			return;
		}

		try {
			jvsErrorMessage = Str.xstringNew();
			
			int retval = ConnexRefMapManager.addTokenVal(lookupValues, 
					columnName, // TokenName
					value, // ValueToBeMapped
					-1, // IDToBeMapped
					jvsErrorMessage);
			
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String errorMessage = "Error adding token " 
						+ columnName 
						+ ". value "
						+ value + ". " 
						+  Str.xstringGetString(jvsErrorMessage);
				
				PluginLog.error(errorMessage);
				
				throw new RefMapException(errorMessage);		

			}
		} catch (OException e) {
			String errorMessage = "Error adding token " 
					+ columnName 
					+ ". value "
					+ value + ". " 
					+ e.getMessage();
			
			PluginLog.error(errorMessage);
			throw new RefMapException(errorMessage);
		} finally {
			if (jvsErrorMessage != null) {
				try {
					Str.xstringDestroy(jvsErrorMessage);
				} catch (OException e) {
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.RefMapping.IRefMapManager#calculateMapping()
	 */
	@Override
	public final void calculateMapping() throws RefMapException {
		XString jvsErrorMessage = null;

		try {
			jvsErrorMessage = Str.xstringNew();
			
			// Now lets apply the map given the lookupValues table,
			int retval = ConnexRefMapManager.applyReferenceMapDefinition(
					mappingDefinition, lookupValues, jvsErrorMessage);

			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String errorMessage = "Error applying reference map. "  
						+  Str.xstringGetString(jvsErrorMessage);
				
				PluginLog.error(errorMessage);
				
				throw new RefMapException(errorMessage);		
			}
		} catch (OException e) {
			String errorMessage = "Error applying reference map. "  
					+ e.getMessage();
			
			PluginLog.error(errorMessage);
			throw new RefMapException(errorMessage);
		} finally {
			if (jvsErrorMessage != null) {
				try {
					Str.xstringDestroy(jvsErrorMessage);
				} catch (OException e) {
				}
			}
		}
		

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.RefMapping.IRefMapManager#lookUp(java.lang.String)
	 */
	@Override
	public final String lookUp(final String columnName) {
		XString jvsErrorMessage = null;
		String mappedValue = null;
		try {
			jvsErrorMessage = Str.xstringNew();
			
			mappedValue = ConnexRefMapManager.oCRetrieveMappedValue(
				lookupValues, columnName, jvsErrorMessage);
		} catch (OException e) {
			String errorMessage = "No mapping found when looking up value for column "  + columnName + ". "  
					+ e.getMessage();
			
			PluginLog.info(errorMessage);
		} finally {
			if (jvsErrorMessage != null) {
				try {
					Str.xstringDestroy(jvsErrorMessage);
				} catch (OException e) {
				}
			}
		}
		
		return mappedValue;
	}

}
