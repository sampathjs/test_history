package com.openlink.jm.bo;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OutboundDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

/*
 * History:
 * 2016-04-05	V1.0	jwaechter	- Initial version
 */

/**
 * This plugin adds the field {@value #OLF_DIV_CUSTOMER} to the gen data. The field is mandatory
 * but is going to be empty in case it's source (Tran info field {@value #TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER})
 * is missing.
 * @author jwaechter
 * @version 1.0
 */
@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_GENERATE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_GEN_Output_Param implements IScript {
	private static final String OLF_DIV_CUSTOMER = "olfDivCustomer";
	private static final String TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER = "Divisional Customer";

	@Override
	public void execute(IContainerContext arg0) throws OException {
		// TODO Auto-generated method stub
		Table tranData = OutboundDoc.getTranDataTable();
		Table eventData = tranData.getTable("event_table", 1);
		String olfDivCustomer;
		
		String olfDivCustomerColName = getOlfDivCustomerColName ();
		int olfDivCustomerColId = eventData.getColNum(olfDivCustomerColName);
		if (olfDivCustomerColId <= 0) {
			olfDivCustomer = "";
		} else {
			String olfDivCustomerValue = eventData.getString(olfDivCustomerColId, 1);
			if (olfDivCustomerValue == null || olfDivCustomerValue.trim().length() == 0) {
				olfDivCustomer = "";
			} else {
				olfDivCustomer = "/ " + olfDivCustomerValue;
			}
		}
		OutboundDoc.setField(OLF_DIV_CUSTOMER, olfDivCustomer);
	}

	private String getOlfDivCustomerColName() throws OException {
		String sql = "SELECT type_id FROM tran_info_types WHERE type_name = '" + 
				TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER + "'";
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");
				throw new OException (message);
			}
			if (sqlResult.getNumRows() == 0) {
				throw new OException ("Could not find tran info type '" + TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER
						+ "'");
			}
			return "tran_info_type_" + sqlResult.getInt ("type_id", 1);
		} finally {
			if (sqlResult != null) {
				sqlResult.destroy();
			}
		}
	}

}
