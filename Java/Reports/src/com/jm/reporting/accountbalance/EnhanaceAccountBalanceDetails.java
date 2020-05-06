/*
 * Purpose:This script is responsible for adding Deal reference column in Account balance drilldown list.
 * 
 * Version History:
 * 
 * Initial Version - Jyotsna -  SR 273139: Enrichment of Account Balances module drill-down listing fields 
 */

package com.jm.reporting.accountbalance;
import com.jm.reportbuilder.utils.ReportBuilderUtils;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class EnhanaceAccountBalanceDetails implements IScript{
	
	private static final String CONTEXT = "AccountBalance";
	private String taskName;
	
	public void execute(IContainerContext context) throws OException {
		
		Table task = Ref.getInfo();
		taskName = task.getString("task_name", 1);
		task.destroy();
		ConstRepository repository = new ConstRepository(CONTEXT, taskName);
		ReportBuilderUtils.initPluginLog(repository, taskName);
		
		PluginLog.info(taskName + " task triggered...");
		Table toView = Util.NULL_TABLE;
		
		try {
			
			Table argt = context.getArgumentsTable();
			
			if (Table.isTableValid(argt) != 1) {
				throw new OException("Invalid data retrieved from argt");
			}
					
			PluginLog.info("Copying argument table to a new table ..\n");
			toView = argt.copyTable();
			
			//fetching Deal reference 
			
			PluginLog.info("Retrieving deal reference details ..\n");
			toView.addCol("reference",COL_TYPE_ENUM.COL_STRING );
			int qId = Query.tableQueryInsert(toView, "Tran Num");
			
			String sql = "SELECT ab.tran_num, ab.reference "
						+ "from ab_tran ab "
						+ "inner join query_result qr "
						+ "ON ab.tran_num = qr.query_result "
						+ "where qr.unique_id = " + qId;
			
			Table dealRef = Table.tableNew();
			dealRef = ReportBuilderUtils.runSql(sql);
			toView.setColName("Tran Num", "tran_num");
			toView.select(dealRef, "reference", "tran_num EQ $tran_num");
			dealRef.destroy();
		
			//to make formatting of the new table same as original
			PluginLog.info("Starting formatting the new table..\n");
			
			//match column titles
			toView.setColTitle("Amount", "Settle Amount");
			toView.setColTitle("Base Amount", "Base Settle Amount");
			toView.setColTitle("Amount2", "Actual Amount");
			toView.setColTitle("tran_num", "Tran Num");
		
			//set column formatting
			
			toView.defaultFormat();
			toView.setColFormatAsRef("Account Name", SHM_USR_TABLES_ENUM.ACCOUNT_TABLE);		
			toView.setColFormatAsRef("Base Unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
			toView.setColFormatAsRef("Instrument Type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
			toView.setColFormatAsRef("External Account", SHM_USR_TABLES_ENUM.ACCOUNT_TABLE);
			
			toView.setColFormatAsDouble("Amount", 20, 4);
			toView.setColFormatAsDouble("Base Amount", 20, 4);
			toView.setColFormatAsDouble("Amount2", 20, 2);
			toView.setColFormatAsDouble("Settlement Difference", 20, 2);
			
			//delete extra row in the end
			toView.delRow(toView.getNumRows());
			
			//get sum total of all the double type columns
			PluginLog.info("calculating sum of double column types ..\n");
			toView.sum();		
			toView.viewTable();
			
	} catch (OException oe) {
		PluginLog.error(oe.getMessage());
		
		throw oe;
		
	} finally {
		PluginLog.info("Exiting Script...");
		toView.destroy();
	}

}

}
