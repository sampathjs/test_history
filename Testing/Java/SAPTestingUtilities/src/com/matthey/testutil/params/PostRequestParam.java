package com.matthey.testutil.params;

import com.matthey.testutil.BaseScript;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

public class PostRequestParam extends BaseScript
{
	@Override
	public void execute(IContainerContext context) throws OException
	{
		Table tAsk = Util.NULL_TABLE;
		Table tFileSelected = Util.NULL_TABLE;
		Table argumentTable;
		Table tURLPath;
		String urlPath = null;
		String csvPath = null;
		final String CSV_PATH = "csv_path";
		final String URL_PATH = "url_path";
		try
		{
			if (Util.canAccessGui() == 1)
			{
				tAsk = Table.tableNew();
				Ask.setTextEdit(tAsk, "Input CSV file path", null, ASK_TEXT_DATA_TYPES.ASK_FILENAME, "Choose CSV file to fill input message", 1);
				Ask.setTextEdit(tAsk, "URL Path", null, ASK_TEXT_DATA_TYPES.ASK_STRING, "Choose URL for POST message", 1);
				Ask.viewTable(tAsk, "Input", "Please fill suitable values");
				
				tFileSelected = tAsk.getTable("return_value", 1);
				csvPath = tFileSelected.getString("return_value", 1);
				
				tURLPath = tAsk.getTable("return_value", 2);
				urlPath = tURLPath.getString("return_value", 1);
			}

			PluginLog.debug("csvFilePath=" + csvPath);
			argumentTable = context.getArgumentsTable();
			
			argumentTable.addCol(CSV_PATH, COL_TYPE_ENUM.COL_STRING);
			argumentTable.addCol(URL_PATH, COL_TYPE_ENUM.COL_STRING);
			
			argumentTable.addRow();
			
			argumentTable.setString(CSV_PATH, 1, csvPath);
			argumentTable.setString(URL_PATH, 1, urlPath);

		}
		catch (OException e)
		{
			PluginLog.error("Failure in choosing csv filename for upload. " + e.getMessage());
			Util.exitFail("Param script failure in choosing csv filename for upload");
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
