package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

abstract class Migr_INC_Maintenance implements IScript
{
	public abstract void execute(IContainerContext context) throws OException;

	void setTranfField(Transaction tran, int fieldId, String value, String errorMsg) throws OException
	{
		// return code '1': succeed; return code '2': succeed but no change
		if (tran.setField(fieldId, 0, "", value) < OLF_RETURN_SUCCEED)
			OConsole.oprint("\n" + errorMsg + tran.getTranNum());
	}

	void setTranfInfoField(Transaction tran, String fieldName, String value, String errorMsg) throws OException
	{
		// return code '1': succeed; return code '2': succeed but no change
		if (tran.setField(TRANF_TRAN_INFO, 0, fieldName, value) < OLF_RETURN_SUCCEED)
			OConsole.oprint("\n" + errorMsg + tran.getTranNum());
	}

	void saveTranInfo(Transaction tran, int bypass_opservices, String errorMsg) throws OException
	{
		if (tran.saveTranInfo(bypass_opservices) != OLF_RETURN_SUCCEED)
			OConsole.oprint("\n" + errorMsg + tran.getTranNum());
	}

	void insertTranByStatus(Transaction tran, TRAN_STATUS_ENUM status, String errorMsg) throws OException
	{
		if (tran.insertByStatus(status) != OLF_RETURN_SUCCEED)
			OConsole.oprint("\n" + errorMsg + tran.getTranNum());
	}

	final static int TRANF_TRAN_INFO = TRANF_FIELD.TRANF_TRAN_INFO.toInt(); // don't change!
	final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt(); // ='1'
}
