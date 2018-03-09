package com.olf.jm.tradeListing;

import com.olf.embedded.application.Context;
import com.olf.embedded.trading.AbstractTradeListing;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ColumnFormatter;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.JoinSpecification;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2016-08-19	V1.0	scurran	- initial version                               
 */

/**
 * Trade Listing data load script.
 * 
 * For Metal Swaps add the volume unit from leg 1.
 * 
 * 
 * @author curras01
 *
 */
@ScriptCategory({ EnumScriptCategory.TradeListingLoad })
public class FixingTradesTradeListing extends AbstractTradeListing {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "FrontOffice";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "FixingTradesTradeListing";
	
	private static final String COLUMN_NAME = "Volume Unit";
	
	@Override
	public void append(Context context, QueryResult tradeUpdates,
			QueryResult tradeNotifications, QueryResult originalTrades,
			ConstTable originalTradeDetails, Table tradeDetailUpdates) {
		try {
			init();
			
			PluginLog.debug("Running trade listing script append method");
			addData( context, tradeUpdates, tradeDetailUpdates);
		
		} catch (Exception e) {
			PluginLog.error("Error in trade listing script. " + e.getMessage());
		}
	}

	@Override
	public void load(Context context, QueryResult queryResult,
			Table tradeDetails) {

		try {
			init();
			
			PluginLog.debug("Running trade listing script load method");
			addData( context, queryResult, tradeDetails);
		
		} catch (Exception e) {
			PluginLog.error("Error in trade listing script. " + e.getMessage());
		}		
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	
	
	private void addData(Context context, QueryResult queryResult, Table tradeDetails) {
        Table additionalData = getAdditionalData(context, queryResult);
 
        // Add the extra data to tradeDetails
        JoinSpecification js = context.getTableFactory().createJoinSpecification("tran_num", "tran_num");
        tradeDetails.addColumn(COLUMN_NAME, EnumColType.Int);
        
        TableFormatter tableFormatter = tradeDetails.getFormatter();
        ColumnFormatter columnFormatter = tableFormatter.createColumnFormatterAsRef(EnumReferenceTable.IdxUnit);
        
        tradeDetails.getColumn(COLUMN_NAME).setFormatter(columnFormatter);
        
        
        js.fill(tradeDetails, COLUMN_NAME, additionalData, "unit");
		
	}
	
	private String getTradeListingSql(QueryResult queryResult) {
		String tableName = queryResult.getDatabaseTableName();
		int queryId = queryResult.getId();
		
		String sql =  " select tran_num, deal_tracking_num, p.unit " 
					+ " from " + tableName + " qr  "
					+ " join ab_tran ab on ab.tran_num = qr.query_result "
					+ " join parameter p on ab.ins_num = p.ins_num and param_seq_num = 1  "
					+ " join instruments i on i.id_number = ins_type and name = 'METAL-SWAP' "
					+ " where qr.unique_id = " + queryId;
		
		return sql;
	}
	
	private Table getAdditionalData(Context context, QueryResult queryResult) {
		
		String sql = getTradeListingSql(queryResult);
		
		PluginLog.debug("About to run SQL " + sql);
		
		Table date = context.getIOFactory().runSQL(sql);
		
		return date;
	}
	


}
