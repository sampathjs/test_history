package com.matthey.testutil.params;

import com.matthey.testutil.BaseScript;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * Param script to take a csv filepath as input from user.
 * Add the file path in the argument table (to be consumed by Main script)
 * @author jains03
 *
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class CsvFilenameParam extends BaseScript {

	private String taskName = null;
	
	private ConstRepository constRepo;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		setupLog();
		
		Table argT = context.getArgumentsTable();
		try {

			argT.addCol("file_name", COL_TYPE_ENUM.COL_STRING);
			argT.addRow();
			argT.setString("file_name", 1, getCsvFilePath(argT));
		} catch (OException e) {
			Util.exitFail("Param script failure in choosing csv filename for upload");
		}finally{
			Logging.close();
		}
	}
	
	/**
	 * This method returns csv filepath as per following priotity -
	 * 1. User input
	 * 2. 'file_name' column of argT
	 * 3. ConstantRepositoty(context='TestUtil', sub-context='<task-name>') field 'csvFilePath'
	 * 4. Path = ReportDirForToday/<task-name>.csv
	 * @return
	 * @throws OException
	 */
	protected String getCsvFilePath(Table argT) throws OException {
		Table tAsk = Util.NULL_TABLE;
		Table tFileSelected = Util.NULL_TABLE;
		Table scriptInfo = Util.NULL_TABLE;
		int taskId = 0;
		String csvFilePath = null;
		try {
			scriptInfo = Ref.getInfo();
			this.taskName =  scriptInfo.getString("task_name", 1);
			constRepo= new ConstRepository("TestUtil", getTaskName());
			taskId =  scriptInfo.getInt("task_id", 1);
			String path = Util.reportGetDirForToday();

			Logging.debug("Choose the filename for upload. taskName = " + getTaskName() + ", taskId = " + taskId);
			int fileNameCol = argT.getColNum("file_name");
			if (Util.canAccessGui() == 1) {
				tAsk = Table.tableNew();
				Ask.setTextEdit(tAsk, "CSV file path", path ,ASK_TEXT_DATA_TYPES.ASK_FILENAME,"Choose CSV file for upload", 1);
				Ask.viewTable(tAsk, getTaskName(),"Please select a csv File" );
				tFileSelected = tAsk.getTable("return_value", 1);
				csvFilePath = tFileSelected.getString("return_value", 1);
			} 
			else if(fileNameCol > 0 && argT.getNumRows() > 0)
			{
				csvFilePath = argT.getString(fileNameCol, 1);
			} 
			else
			{
				csvFilePath = constRepo.getStringValue("csvFilePath", Util.reportGetDirForToday()+"/"+getTaskName()+".csv");								
			}
			Logging.debug("csvFilePath=" + csvFilePath);

		} catch (OException e) {
			Logging.error("Failure in choosing csv filename for upload. taskName = " + getTaskName() + ", taskId = " + taskId + ". " + e.getMessage());
			Util.exitFail("Param script failure in choosing csv filename for upload");
		} finally {
			if (tAsk != null && Table.isTableValid(tAsk) == 1) {
				tAsk.destroy();
			}
			if (tFileSelected != null && Table.isTableValid(tFileSelected) == 1) {
				tFileSelected.destroy();
			}
			if (scriptInfo != null && Table.isTableValid(scriptInfo) == 1) {
				scriptInfo.destroy();
			}

		}
		return csvFilePath;
	}

	protected String getTaskName() {
		return taskName;
	}
	
}
