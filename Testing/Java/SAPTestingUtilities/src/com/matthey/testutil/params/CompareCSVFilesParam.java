package com.matthey.testutil.params;

import com.matthey.testutil.common.Util;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.openlink.util.logging.PluginLog;

public class CompareCSVFilesParam implements IScript
{

	@Override
	public void execute(IContainerContext context) throws OException
	{
		String csvPath = null;
		Table argumentTable = context.getArgumentsTable();

		Table tAsk = com.olf.openjvs.Util.NULL_TABLE;
		Table tFileSelected = com.olf.openjvs.Util.NULL_TABLE;
		try
		{
			if (com.olf.openjvs.Util.canAccessGui() == 1)
			{
				Util.setupLog();
				tAsk = Table.tableNew();
				Ask.setTextEdit(tAsk, "Input CSV file path", null, ASK_TEXT_DATA_TYPES.ASK_FILENAME, "Choose CSV file to fill input message", 1);
				Ask.viewTable(tAsk, "Input", "Please fill suitable values");

				tFileSelected = tAsk.getTable("return_value", 1);
				csvPath = tFileSelected.getString("return_value", 1);

			}

			PluginLog.debug("csvFilePath=" + csvPath);
			argumentTable = context.getArgumentsTable();

			/*
			 * argumentTable.addCol(CSV_PATH, COL_TYPE_ENUM.COL_STRING);
			 * 
			 * argumentTable.addRow();
			 * 
			 * argumentTable.setString(CSV_PATH, 1, csvPath);
			 */

			argumentTable.inputFromCSVFile(csvPath);
			Util.updateTableWithColumnNames(argumentTable);
			PluginLog.debug("Input CSV:");
			Util.printTableOnLogTable(argumentTable);
		}
		catch (OException e)
		{
			PluginLog.error("Failure in choosing csv filename for upload. " + e.getMessage());
			com.olf.openjvs.Util.exitFail("Param script failure in choosing csv filename for upload");
		}
		finally
		{
			if (tAsk != null && Table.isTableValid(tAsk) == 1)
			{
				tAsk.destroy();
			}
			if (tFileSelected != null && Table.isTableValid(tFileSelected) == 1)
			{
				tFileSelected.destroy();
			}

		}
	}

}
