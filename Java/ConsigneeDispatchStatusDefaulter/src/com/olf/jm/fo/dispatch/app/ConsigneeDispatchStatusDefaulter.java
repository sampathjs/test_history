package com.olf.jm.fo.dispatch.app;

import com.openlink.util.constrepository.ConstRepository;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-03-24	V1.0	jwaechter	- Initial Version
 */

/**
 * This tranfield notification plugin should be triggered by changes to tran field 
 * {@link TRANF_FIELD#TRANF_EXTERNAL_BUNIT}. Upon being triggered by changes to this field
 * it is going to default the tran info field {@value #DISPATCH_STATUS_INFO_FIELD} to "None"
 * and the tran info field {@value #CONSIGNEE_INFO_FIELD} to the new value being selected by the user
 * and and the tran info field #{@value #CONSIGNEE_ADDRESS_INFO_FIELD} to (if present) the consignee address 
 * having the least address id or (if not consignee address is present) to the address of the main address of the
 * new external business unit.
 * @author jwaechter
 * @version 1.0
 */
public class ConsigneeDispatchStatusDefaulter implements IScript {
	
	public static final String CREPO_CONTEXT = "FrontOffice";
	public static final String CREPO_SUBCONTEXT = "ConsigneeAndDispatch";
	private static final String MAIN_ADDRESS_TYPE = "Main";
	private static final String CONSIGNEE_ADDRESS_TYPE = "Consignee";
	private static final String CONSIGNEE_INFO_FIELD = "Consignee";
	private static final String CONSIGNEE_ADDRESS_INFO_FIELD = "Consignee Address";
	private static final String DISPATCH_STATUS_INFO_FIELD = "Dispatch Status";
	
	
    public void execute(IContainerContext context) throws OException {
		initLogging ();	
		Table argt = context.getArgumentsTable();
		if (argt.getNumRows() != 1) {
			throw new OException ("Class " + getClass().getName() + " has to be executed as a Tranfield OPS");
		}
		
		boolean isPost = argt.getInt ("post_process", 1)==1;
		Transaction tran = argt.getTran("tran", 1);
		int fieldId = argt.getInt("field", 1);
		TRANF_FIELD field = TRANF_FIELD.fromInt(fieldId);
		int side = argt.getInt("side", 1);
		int seqNum2 = argt.getInt("seq_num_2", 1);
		int seqNum3 = argt.getInt("seq_num_3", 1);
		int seqNum4 = argt.getInt("seq_num_4", 1);
		int seqNum5 = argt.getInt("seq_num_5", 1);
		String name = argt.getString("name", 1);
		String value = argt.getString ("value", 1);
		String oldValue = argt.getString("old_value", 1);

		try {

			if (field == TRANF_FIELD.TRANF_EXTERNAL_BUNIT) {
				tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, CONSIGNEE_INFO_FIELD, "");
				tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, CONSIGNEE_ADDRESS_INFO_FIELD, "");
				tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, DISPATCH_STATUS_INFO_FIELD, "None");
			}
			if (field == TRANF_FIELD.TRANF_TRAN_INFO && name.equals(CONSIGNEE_INFO_FIELD)) {
				String consignee = retrieveConsignee (value);
				tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, CONSIGNEE_ADDRESS_INFO_FIELD, consignee);
			}
		} catch (Throwable t) {
			PluginLog.error("Error executing " + getClass().getName() + ": " + t.toString());
			throw t;
		}
    }
    
	private String retrieveConsignee(String partyName) throws OException {
		String consignee = getMainAddress (partyName);
		return consignee;
	}

	private String getMainAddress(String partyName) throws OException {
		String sql = 
				"\nSELECT CONCAT(pa.addr1, ', ', pa.addr2) AS consAddr"
			+	"\nFROM party p "
			+ 	"\nINNER JOIN party_address_type pat"
			+	"\n  ON pat.address_type_name = '" + MAIN_ADDRESS_TYPE + "'"
			+   "\nINNER JOIN party_address pa "
			+	"\n	 ON pa.party_id = p.party_id"
			+   "\n    AND pa.party_address_id = (SELECT MIN(pa2.party_address_id) FROM party_address pa2 "
			+	"\n 							  WHERE pa2.party_id = p.party_id AND pa2.address_type = pat.address_type_id)"
			+	"\n    AND pa.address_type = pat.address_type_id"
			+ 	"\nWHERE p.short_name = '" + partyName + "'"		
			;
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
				throw new OException (message);
			}
			if (sqlResult.getNumRows() == 0) {
				return "";
			}
			return sqlResult.getString("consAddr", 1);
		} finally {
			if (sqlResult != null) {
				sqlResult.destroy();
			}
		}
	}


	private String getLowestIdConsigneeAddress(String partyName) throws OException {
		String sql = 
				"\nSELECT CONCAT(pa.addr1, ', ', pa.addr2)"
			+	"\nFROM party p "
			+ 	"\nINNER JOIN party_address_type pat"
			+	"\n  ON pat.address_type_name = '" + CONSIGNEE_ADDRESS_TYPE + "'"
			+   "\nINNER JOIN party_address pa "
			+	"\n	 ON pa.party_id = p.party_id"
			+   "\n    AND pa.party_address_id = (SELECT MIN(pa2.party_address_id) FROM party_address pa2 "
			+	"\n 							  WHERE pa2.party_id = p.party_id AND pa2.address_type = pat.address_type_id)"
			+	"\n    AND pa.address_type = pat.address_type_id"
			+ 	"\nWHERE p.party_id = '" + partyName + "'"
			;
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
				throw new OException (message);
			}
			if (sqlResult.getNumRows() == 0) {
				return "";
			}
			return sqlResult.getString("addr1", 1);
		} finally {
			if (sqlResult != null) {
				sqlResult.destroy();
			}
		}
	}


	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", getClass()
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {

			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			String errMsg = getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}