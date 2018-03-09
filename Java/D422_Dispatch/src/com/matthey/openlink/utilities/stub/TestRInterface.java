package com.matthey.openlink.utilities.stub;

import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.apm_foundation.enums.OLF_RETURN_CODE;
import com.olf.openrisk.table.Table;


public class TestRInterface {
private Table processRBprocessCustomRequest(Table argumentsTable, String reportName) {
		
		Table output = null;
		Table resultData = null;
		int result = OLF_RETURN_CODE.OLF_RETURN_APP_FAILURE.toInt();
/*		try {
			ReportBuilder report = ReportBuilder.createNew(reportName);
			
			output = Table.tableNew("RESULT");
			report.setOutputTable(output);
			result = report.runReport();
			
			
		} catch (OException e) {
			e.printStackTrace();
		}
		
		if (OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() != result) {
			OConsole.message("\n\t AccountBalances FAILED!!!\n\n");
			return null;
		} else {
			OConsole.message("\n\t AccountBalances RESPONSE OK!!!\n\n");
			try {

				resultData = output.cloneTable();
				resultData.addRow();
				output.copyRow(1, resultData, 1);
				resultData.viewTable();
			} catch (OException e) {
				OConsole.message("\n\t AccountBalances error changing column names!\n\n");
				e.printStackTrace();
			}*/
			return output;
	/*	}*/
	}

}
