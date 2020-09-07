package standard.apm.utilities;

import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Services;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SERVICE_MGR_ENGINES_ENUM;

public class APM_DealScanParamScript implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
        Table askTable = Table.tableNew();
		String dealNumStr = "";
		String apmServiceName = "";

		/* Get the list of APM Services */
		Table apmServiceList = Services.getAppServicesConfiguredForGroupType(SERVICE_MGR_ENGINES_ENUM.APM_SERVICE_ID);
		int serviceNameColNum = apmServiceList.getColNum("service_name");
		for (int colNum = apmServiceList.getNumCols(); colNum > 0; colNum--)
		{
			if (colNum != serviceNameColNum)
				apmServiceList.delCol(colNum);
		}
		apmServiceList.sortCol(1);

		/* Pop up the viewer for user input */
		Ask.setTextEdit(askTable, "Deal Number", "0", ASK_TEXT_DATA_TYPES.ASK_INT, "Type In the Deal Number", 1);
		Ask.setAvsTable(askTable, apmServiceList, "APM Service", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, null, "Select APM Service", 1);

		int retval = Ask.viewTable(askTable, "APM Deal Scan Arguments", "Specify APM Deal Scan Arguments");
		if (retval == 1)
		{
			argt.addCol("ArgsTable", COL_TYPE_ENUM.COL_TABLE);
			argt.addCol("ErrorStr", COL_TYPE_ENUM.COL_STRING);
			argt.addRow();

			int returnValueCol = askTable.getColNum("return_value");
			if (returnValueCol > 0)
			{
				dealNumStr = askTable.getTable(returnValueCol, 1).getString(1, 1);
				apmServiceName = askTable.getTable(returnValueCol, 2).getString(1, 1);
			}

			/* If valid user input, pass it to the main script via argt */
			if (Integer.parseInt(dealNumStr) > 0 && !apmServiceName.isEmpty())
			{
				Table valuesTable = Table.tableNew("APM Deal Scan Arguments");
				valuesTable.addCol("dealNum", COL_TYPE_ENUM.COL_STRING, "Deal Number");
				valuesTable.addCol("apmServiceName", COL_TYPE_ENUM.COL_STRING, "APM Service");
				valuesTable.setRowValues(valuesTable.addRow(), "(" + dealNumStr + "), (" + apmServiceName + ")");

				argt.setTable("ArgsTable", 1, valuesTable);
			}
			else
			{
				argt.setString("ErrorStr", 1, "Invalid Input: Deal Num = " + dealNumStr + ", APM Service = '" + apmServiceName + "'.");
			}
		}
		
        askTable.destroy();
        apmServiceList.destroy();

        return;	
	}

}
