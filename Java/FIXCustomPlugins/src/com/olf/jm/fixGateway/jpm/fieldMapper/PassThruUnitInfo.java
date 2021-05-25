package com.olf.jm.fixGateway.jpm.fieldMapper;

import java.util.HashMap;
import java.util.Map;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2020-05-13 - V0.1 - jwaechter - Initial Version
 */


/**
 *  Class responsible for mapping the Internal Bunit.
 */
public class PassThruUnitInfo extends FieldMapperBase {
	private static final String JM_PMM_UK = "JM PMM UK";
	
	private final Map<String, String> personnelNameToDefaultBuName = new HashMap<>();
	
	@Override
	public String getTagFieldName() {
		return null; // complex logic
	}
	
	@Override
	public String infoFieldName() throws FieldMapperException {
		return "PassThrough Unit";
	}


	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_TRAN_INFO;
	}

	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		InternalContact internalContact = new InternalContact();
		String contact = internalContact.getTranFieldValue(message);
		loadDefaultBunitsForAllPersonnelNames();
		String defaultInternalBunit =  personnelNameToDefaultBuName.get(contact);
		if (defaultInternalBunit.equalsIgnoreCase(JM_PMM_UK)) {
			return "";
		} else {
			return defaultInternalBunit;
		}
	}
	
	@Override
	public boolean isInfoField() {
		return true;
	}
	
	
	public boolean isPassThru (Table message) throws FieldMapperException, OException {
		InternalContact internalContact = new InternalContact();
		String contact = internalContact.getTranFieldValue(message);
		loadDefaultBunitsForAllPersonnelNames();
		String defaultInternalBunit =  personnelNameToDefaultBuName.get(contact);

		if (defaultInternalBunit.equalsIgnoreCase(JM_PMM_UK)) {
			return false;
		} else {
			return true;
		}
	}
	private void loadDefaultBunitsForAllPersonnelNames () {
		personnelNameToDefaultBuName.clear();
		String sql = 
				    "SELECT p.short_name AS party_name, personnel.name AS personnel_name"
				+ "\nFROM party p"
				+ "\n  INNER JOIN party_personnel pp"
				+ "\n    ON pp.party_id = p.party_id"
				+ "\n  INNER JOIN personnel"
				+ "\n    ON personnel.id_number = pp.personnel_id"
				+ "\nWHERE pp.default_flag = 1" // 1 = true
				;
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL '" + sql + "':"));
			}
			for (int row = sqlResult.getNumRows(); row >= 1; row--) {
				personnelNameToDefaultBuName.put(sqlResult.getString("personnel_name", row), sqlResult.getString("party_name", row));
			}
		} catch (OException e) {
			Logging.error("Error executing SQL '" + sql + "':" );
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}			
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}
	
	/**
	 * The the seq num 2 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum2() {
		return -1;
	}
	
	/**
	 * The the seq num 3 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum3() {
		return -1;
		
	}
	
	/**
	 * The the seq num 4 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum4() {
		return -1;
		
	}
	
	/**
	 * The the seq num 5 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum5() {
		return -1;		
	}
}