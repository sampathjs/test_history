package com.matthey.testutil.params;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

/**
 * Param script to take a csv filepath as input from user.
 * Add the file path in the argument table (to be consumed by Main script)
 * @author jains03
 *
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class CsvFilenameParamCreateWebInjectorInput extends CsvFilenameParam {

	private String taskName = null;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		setupLog();
		
		Table argT = context.getArgumentsTable();
		try {

			argT.addCol("tempate_csv_path", COL_TYPE_ENUM.COL_STRING);
			argT.addRow();
			argT.setString("tempate_csv_path", 1, getCsvFilePath(argT));
		} catch (OException e) {
			Util.exitFail("Param script failure in choosing csv filename for upload");
		}finally{
			Logging.close();
		}
	}

	protected String getTaskName() {
		return taskName;
	}
}
