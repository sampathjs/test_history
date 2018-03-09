package com.openlink.esp.materialamendments.lib;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class TranfFieldsPublisher implements IScript
{
	private static final String UT_MATERIAL_CHECK_TRANF = "material_check_tranf";

	public void execute(IContainerContext context) throws OException
	{
		String tableName;
		if (DBase.getDbType() == DBTYPE_ENUM.DBTYPE_ORACLE.toInt())
			tableName = "user_";
		else
			tableName = "USER_";
		tableName += UT_MATERIAL_CHECK_TRANF;

		Table fieldsTable = Table.tableNew(tableName);
		DBUserTable.structure(fieldsTable);
		DBUserTable.clear(fieldsTable);
		for (TRANF_FIELD c : TRANF_FIELD.values())
		{
			fieldsTable.addRowsWithValues("" + c.toInt() + ",(" + c.toString() + ")");
//			com.olf.openjvs.OConsole.oprint("\n" + c.toInt() + " - " + c.toString());
		}
		DBUserTable.saveUserTable(fieldsTable, 1, 1, 0);
	}
}
