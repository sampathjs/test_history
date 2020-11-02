package com.openlink.jm.gen.holdinginstrument;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranType;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-01-23 - V0.1 - jwaechter - Initial Version
 * 2015-01-28 - V1.0 - jwaechter - added surrounding try catch 
 *                                 runtime exception in main
 */

/**
 * Param script to pick MPX indices and end date.
 * Based on AVS script "TR_CreateFutureHoldingIns_P.mls"
 * @author jwaechter
 * @version 1.0
 * @see {@link CreateHoldingInstrumentsMain}
 */
@ScriptCategory( { EnumScriptCategory.Generic } )
public class CreateHoldingInstrumentsParam extends AbstractGenericScript {
	public static final String INDEX_DB_STATUS_VALIDATED = "Validated"; 

	public static final String CONST_REPO_CONTEXT = "EOD"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "CreateHoldingInstruments"; // subcontext of constants repository
	
	private List<Integer> eodIndexList;
	private String eodSymbolicDate;

	public Table execute(final Context context, final ConstTable table) {
		final TableFactory tf = context.getTableFactory();
		final StaticDataFactory sdf = context.getStaticDataFactory();
		final IOFactory iof =  context.getIOFactory();
		final CalendarFactory cf = context.getCalendarFactory();
		final Date tradingDate= context.getTradingDate(); // get trading date to be used as base for end date calculation

		init (context); // init constants repository and plugin log 
		try {
			Logging.info(this.getClass().getSimpleName() + " started\n"); // log with level info 		
			Table argt;
			if (context.hasDisplay()) {
				argt = askUser(tf, sdf, iof, cf, tradingDate);				
			} else {
				argt = setupEODParams (tf, sdf, iof, cf, tradingDate);
			}
			Logging.info(this.getClass().getSimpleName() + " finished\n");
			return argt;
		} catch (RuntimeException ex) {
			Logging.error(ex.toString());
			throw ex;
		}finally{
			Logging.close();
		}
	}


	private Table setupEODParams(TableFactory tf, StaticDataFactory sdf,
			IOFactory iof, CalendarFactory cf, Date tradingDate) {
		Table argt = tf.createTable("TR Create Holding Instruments Parameters");
		argt.addColumn("index_list", EnumColType.Table);
		argt.addColumn("end_date", EnumColType.Int);
		argt.addColumn("user_exit", EnumColType.Int);
		
		Table indexList = createIndexListFromCR (tf, iof, sdf);		
		SymbolicDate plus3Months = cf.createSymbolicDate(eodSymbolicDate);
		int endDate = cf.getJulianDate(plus3Months.evaluate(tradingDate));	
		
		argt.addRow();
		argt.setTable("index_list", 0, indexList);	
		argt.setInt("end_date", 0, endDate);
		argt.setInt("user_exit", 0, 0);

		return argt;
	}


	private Table createIndexListFromCR(TableFactory tf, IOFactory iof,
			StaticDataFactory sdf) {
		Table indexList = tf.createTable("index_list");
		indexList.addColumn("index_id", EnumColType.Int);
		indexList.addColumn("index_name", EnumColType.String);
		for (int indexId : eodIndexList) {
			int row = indexList.addRows(1);
			indexList.setInt("index_id", row, indexId);
			String indexName = sdf.getName(EnumReferenceTable.Index, indexId);
			indexList.setString ("index_name", row, indexName);
		}
		return indexList;
	}


	private Table askUser(final TableFactory tf,
			final StaticDataFactory sdf, final IOFactory iof, CalendarFactory cf, Date tradingDate) {
		Table argt = tf.createTable("TR Create Holding Instruments Parameters");
		argt.addColumn("index_list", EnumColType.Table);
		argt.addColumn("end_date", EnumColType.Int);
		argt.addColumn("user_exit", EnumColType.Int);

		Table indices = createIndexList (tf, iof, sdf);
		Table selectedIndices = null;
		int endDate;
		indices.setName("Index List for Ask Table");

		SymbolicDate plus3Months = cf.createSymbolicDate(eodSymbolicDate);
		endDate = cf.getJulianDate(plus3Months.evaluate(tradingDate));	

		try {
			com.olf.openjvs.Table askTable = com.olf.openjvs.Table.tableNew("JVS Ask Table");
			Ask.setAvsTable(askTable, tf.toOpenJvs(indices, true),"MPX Curves", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1);
			Ask.setTextEdit(askTable, "End Date", cf.getDateDisplayString(plus3Months.evaluate(tradingDate)), ASK_TEXT_DATA_TYPES.ASK_DATE, "Select an end date");
			int ret = Ask.viewTable(askTable, "Future Holding Instrument Creation", "");
			if (ret <= 0) {
				Logging.info("User pressed cancel. Aborting...\n");
				Ask.ok("You cancelled execution of the task, no instruments were created.");
				argt.setInt("user_exit", 0, 1);
				askTable.destroy();
				indices.dispose();
				return argt;
			}			
			selectedIndices = tf.fromOpenJvs(askTable.getTable("return_value", 1), true);
			String endDateUnparsed = askTable.getTable ("return_value", 2).getString("return_value", 1);
			SymbolicDate sd = cf.createSymbolicDate(endDateUnparsed);
			endDate = cf.getJulianDate(sd.evaluate());			
			askTable.destroy();
		} catch (OException e) {
			throw new RuntimeException (e);
		}
		selectedIndices.setName("Index List");
		selectedIndices.setColumnName(0, "index_id");
		selectedIndices.removeColumn("ted_str_value");
		selectedIndices.removeColumn("return_value");
		selectedIndices.select(indices, "index_id, index_name", "IN.index_id == OUT.index_id");
		argt.addRow();
		argt.setTable("index_list", 0, selectedIndices);	
		argt.setInt("end_date", 0, endDate);
		argt.setInt("user_exit", 0, 0);
		indices.dispose();
		return argt;
	}	

	private void init(Context context) {
		try {
			String abOutdir = context.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);

			eodSymbolicDate = constRepo.getStringValue("symbolicDate", "3m");
			eodIndexList = new ArrayList<>();
			String indexNameList = constRepo.getStringValue("indexNames", "NMX_PA");
			for (String index : indexNameList.split(",") ) {
				int indexId = context.getStaticDataFactory().getId(EnumReferenceTable.Index, index.trim());
				eodIndexList.add(indexId);
			}
			try {
				Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
	}

	/**
	 * Retrieves the list of all MBX indices from database
	 */
	private Table createIndexList (TableFactory tf, IOFactory iof, StaticDataFactory sdf) {
		int indexDbStatusValidated = sdf.getId(EnumReferenceTable.IdxDbStatus, INDEX_DB_STATUS_VALIDATED);
		String sql="SELECT DISTINCT im.index_id, im.index_name "
			+ "   FROM     ab_tran a "
			+ "   JOIN parameter p ON a.ins_num = p.ins_num AND p.param_seq_num = 0 "
			+ "   JOIN idx_def im ON im.index_id = p.proj_index AND im.db_status = " + indexDbStatusValidated 
			+ " WHERE "
			+ "   a.toolset in (" + EnumToolset.ComFut.getValue() + ", " + EnumToolset.ComOptFut.getValue() + ") "
			+ "   AND a.tran_type = " + EnumTranType.Holding.getValue()
			+ "   AND a.tran_status = " + EnumTranStatus.Validated.getValue()
			+ " ORDER BY im.index_name ASC";

		Table indexList = iof.runSQL(sql);

		return indexList;
	}
}