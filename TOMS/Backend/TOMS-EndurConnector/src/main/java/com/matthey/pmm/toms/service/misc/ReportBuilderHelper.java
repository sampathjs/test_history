package com.matthey.pmm.toms.service.misc;

import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;

public class ReportBuilderHelper {
	public static Table runReport (TableFactory tf, String reportName) {
		com.olf.openjvs.Table outputTable = null; 
		try {
			outputTable = com.olf.openjvs.Table.tableNew();
			ReportBuilder rb = ReportBuilder.createNew(reportName);
			// the report builder output is going to get stored in outputTable:
			rb.setOutputTable(outputTable);	
			rb.runReport();
			return tf.fromOpenJvs(outputTable, true);
		} catch (OException e) {
			
			throw new RuntimeException (e);
		} finally {
			try {
				if (outputTable != null) {
					outputTable.destroy();					
				}
			} catch (OException e) {
				
			}
		}
	}

}
