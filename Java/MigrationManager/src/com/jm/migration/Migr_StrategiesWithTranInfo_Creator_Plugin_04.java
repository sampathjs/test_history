package com.jm.migration;





/*
 * History:
 * 1.0 - 25.06.2015 sehlert  	- initial version based on Standard Content implementation
 * 1.1 - 10.02.2016 jneufert  	- adjustments for JM to set required fields for metal transfers
 * 1.2 - 23.02.2016	jwaechter 	- added comment processing
 *                              - status after processing is now "BOOKED" instead of INSERTED
 * 1.3 - 16.03.2016	jwaechter	- added Thread.sleep after processing strategy
 * 1.4 - 06.04.2016 jwaechter	- added sort by row_id in line 200
 * 1.5 - 10.05.2016 jneufert    - add from and to reference to comments
 */

/**
 * Creates and Inserts Strategy Transactions into the data base.
 * 
 * @author sehlert
 * @version 1.4
 * 
 * @dependency Endur >= 100R2
 * 
 */
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.esp.migration.model.MIGR_PROCESS_STATUS;
import com.openlink.esp.migration.persistence.ApplicationScript;
import com.openlink.esp.migration.persistence.UserTableRepository;
import com.openlink.esp.migration.persistence.exception.InvalidArgumentException;
import com.olf.jm.logging.Logging;

public class Migr_StrategiesWithTranInfo_Creator_Plugin_04 extends ApplicationScript implements IScript{
	public static final String COL_LENTITY = "int_lentity";
	public static final String COL_BUNIT = "int_bu";
	public static final String COL_PORTFOLIO = "int_portfolio";
	public static final String COL_NAME = "strategy_name";
	public static final String COL_TRADE_DATE = "trade_date";
	public static final String COL_SETTLE_DATE = "settle_date";
	public static final String COL_START_DATE = "start_date";
	public static final String COL_END_DATE = "end_date";
		
	public static final String COL_STATUS_FLAG = "status_flag";
	public static final String COL_STATUS_MSG = "status_msg";
	public static final String COL_ID = "strategy_id";
	
	//JM specific fields
	public static final String TI_FROM_ACCT = "From A/C";
	public static final String TI_TO_ACCT = "To A/C";
	public static final String TI_FROM_BU = "From A/C BU";
	public static final String TI_TO_BU = "To A/C BU";
	public static final String TI_FROM_ACCT_LOCO = "From A/C Loco";
	public static final String TI_TO_ACCT_LOCO = "To A/C Loco";
	public static final String TI_FROM_ACCT_FORM = "From A/C Form";
	public static final String TI_TO_ACCT_FORM = "To A/C Form";
	public static final String TI_METAL = "Metal";
	public static final String TI_UNIT = "Unit";
	public static final String TI_QUANTITY = "Qty";
	public static final String TI_CHARGES = "Charges";
	public static final String TI_CHARGE_USD = "Charge (in USD)";
	public static final String TI_FORCE_VAT = "Force VAT";
	
	//public static final String COL_TI_FROM_ACCT = "from_acct";
	//public static final String COL_TI_TO_ACCT = "to_acct";
	public static final String COL_TI_FROM_ACCT = "from_si";
	public static final String COL_TI_TO_ACCT = "to_si";
	public static final String COL_TI_FROM_BU = "from_bu";
	public static final String COL_TI_TO_BU = "to_bu";
	public static final String COL_TI_FROM_ACCT_LOCO = "from_acct_loco";
	public static final String COL_TI_TO_ACCT_LOCO = "to_acct_loco";
	public static final String COL_TI_FROM_ACCT_FORM = "from_acct_form";
	public static final String COL_TI_TO_ACCT_FORM = "to_acct_form";
	public static final String COL_TI_METAL = "metal";
	public static final String COL_TI_UNIT ="unit";
	public static final String COL_TI_QUANTITY = "quantity";
	public static final String COL_TI_CHARGES = "charges";
	public static final String COL_TI_CHARGE_USD = "charges_usd";
	public static final String COL_TI_FORCE_VAT = "force_vat";

	public static final String[][] COL_AND_COMMENT_TYPES = {
	//      COL_NAME        Comment Type as type name in table deal_comments_type
		{"from_reference" , "From Account"},
		{"from_comment1"  , "From Account"},
		{"from_comment2"  , "From Account"},
		{"from_comment3"  , "From Account"},
		{"from_comment4"  , "From Account"},
		{"to_reference"   , "To Account"},
		{"to_comment1"    , "To Account"},
		{"to_comment2"    , "To Account"},
		{"to_comment3"    , "To Account"},
		{"to_comment4"    , "To Account"},
	};
	
	public void execute(IContainerContext context) throws OException {
		initLogging();
		
		/*
		Table argt = context.getArgumentsTable();
		String name = argt.getString("user_table", 1);
		if(name == null) {
			Logging.error("Unable to retrieve strategies user table name from argt, please check the param script.");
			return;
		}
		name = name.trim();
		*/
		
		String name = "USER_migr_us_strategies_04";
		try {
			process(name);
		} catch (Exception e) {
			Logging.error("Error creating strategies: " + e.getMessage());
		} finally{
			Logging.close();
		}
        // View Table? 
	}
	
	private void initLogging() throws OException {
		try {
			Logging.init(this.getClass(), "","");
		} catch (Exception e) {
			OConsole.oprint(this.getClass().getSimpleName() + ": Failed to initialize logging module.");
		}
	}
	
	/**
	 * Loads a user table with name tableName from the data base. Creates a
	 * Strategy Transaction and inserts the Transaction into the database by
	 * status validated for every strategy contained in the table. Writes the
	 * user table back to the data base.
	 * 
	 * @param tableName
	 *            User table name.
	 * @throws OException
	 * @throws InvalidObjectException
	 *             If table does not have the correct structure.
	 */
	public void process(String tableName) throws OException, InvalidObjectException {
		Table table = Table.tableNew(tableName);
		try {
			if (DBUserTable.load(table) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException("Error loading strategy table '" + tableName + "'.");
			}		
			process(table);
			UserTableRepository.clearAndSave(table);
		} finally {
			table.viewTable();
			table.destroy();
		}
	}

	/**
	 * Creates a Strategy Transaction and inserts the Transaction into the
	 * database by status validated for every strategy contained in table.
	 * 
	 * @param table
	 *            Table containing the strategies
	 * @throws OException
	 * @throws InvalidObjectException
	 *             If table does not have the correct structure.
	 */
	public void process(Table table) throws OException, InvalidObjectException {
		if(table == null || !isTableStructureValid(table)){
			throw new InvalidObjectException("The strategy table does not contain all mandatory columns.");
		}
		
		String name, portfolio, bunit, lentity, tradeDate, settleDate, startDate, endDate;
		
		//JM tran info fields
		String tiFromAC;
		String tiToAC;
		String tiFromBU;
		String tiToBU;
		String tiFromACLoco;
		String tiToACLoco;
		String tiFromACForm;
		String tiToACForm;
		String tiMetal;
		String tiUnit;
		String tiQty;
		String tiCharges;
		String tiChargeUSD;
		String tiForceVAT;
		
		String commentsAndTypes[][] = new String[COL_AND_COMMENT_TYPES.length][];
			
		int numRows = table.getNumRows();
		int dealId;
				
		table.sortCol("row_id");
		for (int i = 1; i <= numRows; i++) {
			try {
				//if (table.getString(COL_STATUS_FLAG, i) != MIGR_PROCESS_STATUS.INSERTED.name()) {
				if (table.getInt("row_type", i) == 2) {
					name = table.getString(COL_NAME, i);
					portfolio = table.getString(COL_PORTFOLIO, i);
					bunit = table.getString(COL_BUNIT, i);
					lentity = table.getString(COL_LENTITY, i);
					tradeDate = table.getString(COL_TRADE_DATE, i);
					settleDate = table.getString(COL_SETTLE_DATE, i);
					startDate = table.getString(COL_START_DATE, i);
					endDate = table.getString(COL_END_DATE, i);
					
					//jm tran info fields
					tiFromACLoco = table.getString(COL_TI_FROM_ACCT_LOCO, i);
					tiToACLoco = table.getString(COL_TI_TO_ACCT_LOCO, i);
					tiFromAC = table.getString(COL_TI_FROM_ACCT, i);
					tiToAC = table.getString(COL_TI_TO_ACCT, i);
					tiFromBU = table.getString(COL_TI_FROM_BU, i);
					tiToBU = table.getString(COL_TI_TO_BU, i);
					tiFromACForm = table.getString(COL_TI_FROM_ACCT_FORM, i);
					tiToACForm = table.getString(COL_TI_TO_ACCT_FORM, i);
					tiMetal = table.getString(COL_TI_METAL, i);
					tiUnit = table.getString(COL_TI_UNIT, i);
					tiQty = table.getString(COL_TI_QUANTITY, i);
					tiCharges = table.getString(COL_TI_CHARGES, i);
					tiChargeUSD = table.getString(COL_TI_CHARGE_USD, i);
					tiForceVAT = table.getString(COL_TI_FORCE_VAT, i);

					// comments
					for (int k=0; k < commentsAndTypes.length; k++) {
						commentsAndTypes[k] = new String[2];
						commentsAndTypes[k][0] = table.getString(COL_AND_COMMENT_TYPES[k][0], i);
						commentsAndTypes[k][1] = COL_AND_COMMENT_TYPES[k][1];
					}
					
					if (name != null) name = name.trim();
					if (portfolio != null) portfolio = portfolio.trim();
					if (bunit != null) bunit = bunit.trim();
					if (lentity != null) lentity = lentity.trim();
					if (tradeDate != null) tradeDate = tradeDate.trim();
					if (settleDate != null) settleDate = settleDate.trim();
					if (startDate != null) startDate = startDate.trim();
					if (endDate != null) endDate = endDate.trim();
					
					//jm tran info fields
					if (tiFromAC != null) tiFromAC	 = tiFromAC.trim();
					if (tiToAC	 != null) tiToAC	 = tiToAC.trim();
					if (tiFromBU != null) tiFromBU	 = tiFromBU.trim();
					if (tiToBU	 != null) tiToBU	 = tiToBU.trim();
					if (tiFromACLoco != null) tiFromACLoco	 = tiFromACLoco.trim();
					if (tiToACLoco	 != null) tiToACLoco	 = tiToACLoco.trim();
					if (tiFromACForm != null) tiFromACForm	 = tiFromACForm.trim();
					if (tiToACForm	 != null) tiToACForm	 = tiToACForm.trim();
					if (tiMetal	 != null) tiMetal	 = tiMetal.trim();
					if (tiUnit	 != null) tiUnit	 = tiUnit.trim();
					if (tiQty	 != null) tiQty	 = tiQty.trim();
					if (tiCharges	 != null) tiCharges	 = tiCharges.trim();
					if (tiChargeUSD	 != null) tiChargeUSD = tiChargeUSD.trim();
					if (tiForceVAT	 != null) tiForceVAT = tiForceVAT.trim();

					// comments
					for (int k=0; k < commentsAndTypes.length; k++) {
						if (commentsAndTypes[k][0] != null) {
							commentsAndTypes[k][0] = commentsAndTypes[k][0].trim();
						}
					}				
									
					dealId = insertTradingStrategyTransaction(name, portfolio, bunit, lentity, tradeDate, settleDate, startDate, endDate, tiFromAC, tiToAC, tiFromBU, tiToBU, tiFromACLoco, tiToACLoco, tiFromACForm, tiToACForm, tiMetal, tiUnit, tiQty, tiCharges, tiChargeUSD, tiForceVAT,
							commentsAndTypes);
					
					setStatusBooked (table, i, dealId);
					table.setInt("row_type", i, 6);
					
					//wait 5 secs 
					try { 
						Thread.sleep(1000);	// number in milliseconds
					} catch (InterruptedException ex) {
						throw new RuntimeException ("Thread was interrupted while sleeping: " + ex.toString(), ex);
					}
				}
			} catch (Exception e) {
				setStatusError(table, i, e);
			}
		}
		table.sortCol("row_id");
	}	
	
	/**
	 * Creates a Strategy Transaction and inserts the Transaction into the
	 * database by status validated.
	 * 
	 * @param name
	 *            Strategy name
	 * @param portfolio
	 *            Portfolio
	 * @param bunit
	 *            Business Unit
	 * @param lentity
	 *            Legal Entity
	 * @param commentsAndTypes 
	 * @return The strategy id
	 * @throws OException
	 *             If the Strategy Transaction could not be created/ inserted.
	 * @throws InvalidArgumentException
	 *             If one of the parameters name, portfolio, bunit or lentity is invalid.
	 */
	private int insertTradingStrategyTransaction(String name, String portfolio, String bunit, String lentity, 
										        String tradeDate, String settleDate, String startDate, String endDate,
												String tiFromAC, String tiToAC, String tiFromBU, String tiToBU, String tiFromACLoco, String tiToACLoco, 
												String tiFromACForm, String tiToACForm, String tiMetal, 
												String tiUnit, String tiQty,  
												String tiCharges, String tiChargeUSD, String tiForceVAT, String[][] commentsAndTypes) throws OException, InvalidArgumentException {
		int intPortfolio = getReferenceValue(portfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		int intBunit = getReferenceValue(bunit, SHM_USR_TABLES_ENUM.BUNIT_TABLE);
		int intLentity = getReferenceValue(lentity, SHM_USR_TABLES_ENUM.LENTITY_TABLE);
		
		return insertTradingStrategyTransaction(name, intPortfolio, intBunit, intLentity, tradeDate, settleDate, startDate, endDate, tiFromAC, tiToAC, tiFromBU, tiToBU, tiFromACLoco, tiToACLoco, tiFromACForm, tiToACForm, tiMetal, tiUnit, tiQty, tiCharges, tiChargeUSD, tiForceVAT, commentsAndTypes);
	}

	/**
	 * Creates a Strategy Transaction and inserts the Transaction into the
	 * database by status validated.
	 * 
	 * @param name
	 *            Strategy name
	 * @param portfolio
	 *            Portfolio
	 * @param bunit
	 *            Business Unit
	 * @param lentity
	 *            Legal Entity
	 * @return The strategy id
	 * @throws OException
	 *             If the Strategy Transaction could not be created/ inserted.
	 */
	private int insertTradingStrategyTransaction(String name, int portfolio, int bunit, int lentity, 
												String tradeDate, String settleDate, String startDate, String endDate,
												String tiFromAC, String tiToAC, String tiFromBU, String tiToBU, String tiFromACLoco, String tiToACLoco, 
												String tiFromACForm, String tiToACForm, String tiMetal, 
												String tiUnit, String tiQty, String tiCharges, String tiChargeUSD, String tiForceVAT, String[][] commentsAndTypes) throws OException {
		int ret = 0;
		Transaction transaction = null;
		try {
			// Transaction.createTradingStrategyTransaction - Endur Version >= 100R2
			transaction = Transaction.createTradingStrategyTransaction(name, portfolio, bunit, lentity);
			if (Transaction.isNull(transaction) != 0) {
				throw new OException("Error creating Trading Strategy Transaction.");
			}
			
			fillTranField(transaction, TRANF_FIELD.TRANF_TRADE_DATE, 0, tradeDate);
			fillTranField(transaction, TRANF_FIELD.TRANF_SETTLE_DATE, 0, settleDate);
			fillTranField(transaction, TRANF_FIELD.TRANF_START_DATE, 0, startDate);
			fillTranField(transaction, TRANF_FIELD.TRANF_MAT_DATE, 0, endDate);
						
			//JM tran info fields
			fillTranInfoField(transaction, TI_FROM_ACCT, tiFromAC);
			fillTranInfoField(transaction, TI_TO_ACCT, tiToAC);
			fillTranInfoField(transaction, TI_FROM_BU, tiFromBU);
			fillTranInfoField(transaction, TI_TO_BU, tiToBU);
			fillTranInfoField(transaction, TI_FROM_ACCT_LOCO, tiFromACLoco);
			fillTranInfoField(transaction, TI_TO_ACCT_LOCO, tiToACLoco);
			fillTranInfoField(transaction, TI_FROM_ACCT_FORM, tiFromACForm);
			fillTranInfoField(transaction, TI_TO_ACCT_FORM, tiToACForm);
			fillTranInfoField(transaction, TI_METAL, tiMetal);
			fillTranInfoField(transaction, TI_UNIT, tiUnit);
			fillTranInfoField(transaction, TI_QUANTITY, tiQty);
			fillTranInfoField(transaction, TI_CHARGES, tiCharges);
			fillTranInfoField(transaction, TI_CHARGE_USD, tiChargeUSD);
			fillTranInfoField(transaction, TI_FORCE_VAT, tiForceVAT);

			// merge comment types
			Map<String, String> processedCommentTypes = new HashMap<String, String> ();
			for (int k=0; k < commentsAndTypes.length; k++) {
				String commentType = commentsAndTypes[k][1];
				String comment = commentsAndTypes[k][0];
				if (comment == null || comment.equals("")) {
					continue;
				}
				if (processedCommentTypes.containsKey(commentType)) {
					String mergedComment = processedCommentTypes.get(commentType) + "\r\n" + comment;
					processedCommentTypes.put(commentType, mergedComment);
				} else {
					processedCommentTypes.put(commentType, comment);
				}
			}
			// set comments (one per comment type)
			for (String commentType : processedCommentTypes.keySet()) {
				String comment = processedCommentTypes.get(commentType);
				fillDealCommentField(transaction, commentType, comment);
			}
			
			if (transaction.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_NEW) != 1) {
				throw new OException("Error inserting Trading Strategy Transaction by status New.");
			}
			
			ret = transaction.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
		} finally {
			if (Transaction.isNull(transaction) == 0) {
				transaction.destroy();
			}
		}
		return ret;
	}
	
	/**
	 * adds a new deal comment to the provided transaction
	 * @param tran the transaction to which a comment should be added
	 * @param commentType value of type_name column in table deal_comments_type 
	 * @param comment the actual comment text
	 * @throws OException
	 */
	private void fillDealCommentField (Transaction tran, String commentType, String comment) throws OException {
		tran.addDealComment();
        tran.getNumRows(0, TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt());
        int currLength =  tran.getNumRows(0, TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt());
        
        int pos = currLength-1;
        tran.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_NOTE_TYPE.toInt(), 0, "",
                commentType, pos);       
        tran.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_MULTILINE.toInt(), 0, "",
                        comment, pos);

	}

	
	private void fillTranField(Transaction tran, TRANF_FIELD field, int leg, String value) throws OException {
		int ret = tran.setField(field.toInt(), leg, "", value);
		if (ret != 1 && ret != 2) {
			throw new OException("Failed to set tran field '" + field.name() + "' on strategy.");
		}
	}
	
	private void fillTranInfoField(Transaction tran, String tiName, String tiValue) throws OException {
		int ret = tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, tiName, tiValue);
		if (ret != 1 && ret != 2) {
			throw new OException("Failed to set tran info field '" + tiName + "' on strategy.");
		}
	}

	/**
	 * Returns the integer for name in the shared memory table specified by
	 * refTable.
	 * 
	 * @param name
	 *            The string to find the integer representation.
	 * @param refTable
	 *            The alias table enumeration.
	 * @return returns The integer id value for the name string.
	 * @throws OException
	 * @throws InvalidArgumentException
	 *             If no integer value can be found for the name.
	 */
	private int getReferenceValue(String name, SHM_USR_TABLES_ENUM refTable) throws OException, InvalidArgumentException {
		int ret = Ref.getValue(refTable, name);
		if (ret < 1) {
			throw new InvalidArgumentException("The value '" + name
					+ "' is not valid. (Reference table " + refTable.name()
					+ ")");
		}
		return ret;
	}
	
	/**
	 * Checks if a table contains the following String columns:
	 * <ul>
	 * <li>int_lentity</li>
	 * <li>int_bunit</li>
	 * <li>int_pfolio</li>
	 * <li>int_strategy_name</li>
	 * <li>status_flag</li>
	 * <li>status_msg</li>
	 * <li>strategy_id</li>
	 * </ul>
	 * 
	 * @param table
	 *            Table to be checked.
	 * @return True if the table contains the columns, false if not.
	 * @throws OException
	 */
	private boolean isTableStructureValid(Table table) throws OException {
		return isTableColumnValid(table, COL_BUNIT, COL_TYPE_ENUM.COL_STRING)
				&& isTableColumnValid(table, COL_LENTITY, COL_TYPE_ENUM.COL_STRING)
				&& isTableColumnValid(table, COL_PORTFOLIO, COL_TYPE_ENUM.COL_STRING)
				&& isTableColumnValid(table, COL_NAME, COL_TYPE_ENUM.COL_STRING)
				&& isTableColumnValid(table, COL_STATUS_MSG, COL_TYPE_ENUM.COL_STRING)
				&& isTableColumnValid(table, COL_STATUS_FLAG, COL_TYPE_ENUM.COL_STRING)
				&& isTableColumnValid(table, COL_ID, COL_TYPE_ENUM.COL_INT);
	}
	
	/**
	 * Checks if a table contains a specified column of a specified column type.
	 * 
	 * @param table
	 *            Table to be checked.
	 * @param column
	 *            Name of the column.
	 * @param colType
	 *            Type of the column.
	 * @return True if the table contains the column of type, false if not.
	 * @throws OException
	 */
	private boolean isTableColumnValid(Table table, String column, COL_TYPE_ENUM colType) throws OException {
		boolean ret = true;
		if(table.getColNum(column) < 1 ||
				table.getColType(column) != colType.toInt()){
			ret = false;
			//	throw new OException("Invalid column is " + column);
		}
		return ret;
	}
	
	/**
	 * Sets the status of the current strategy entry.
	 * 
	 * @param table
	 *            Table containing the strategies.
	 * @param row
	 *            Row of the current strategy.
	 * @param status
	 *            Status to be set.
	 * @param msg
	 *            Status message.
	 * @param id
	 *            Strategy id of the inserted strategy transaction.
	 * @throws OException
	 */
	private void setStatus(Table table, int row, MIGR_PROCESS_STATUS status, String msg, int id) throws OException {
		table.setString(COL_STATUS_FLAG, row, status.name());
		table.setString(COL_STATUS_MSG, row, msg);
		table.setInt(COL_ID, row, id);
	}
	
	/**
	 * Sets the status of the current strategy entry to status INSERTED.
	 * Logs the message with loglevel info.
	 * 
	 * @param table
	 *            Table containing the strategies.
	 * @param row
	 *            Row of the current strategy.
	 * @param id
	 *            Strategy id of the inserted strategy transaction.
	 * @throws OException
	 */
	private void setStatusInserted(Table table, int row, int id) throws OException {
		String msg = "Inserted Strategy Transaction # " + id;
		setStatus(table, row, MIGR_PROCESS_STATUS.INSERTED, msg, id);
		Logging.info(msg);
	}

	/**
	 * Sets the status of the current strategy entry to status BOOKED.
	 * Logs the message with loglevel info.
	 * 
	 * @param table
	 *            Table containing the strategies.
	 * @param row
	 *            Row of the current strategy.
	 * @param id
	 *            Strategy id of the inserted strategy transaction.
	 * @throws OException
	 */
	private void setStatusBooked(Table table, int row, int id) throws OException {
		String msg = "Inserted Strategy Transaction # " + id;
		setStatus(table, row, MIGR_PROCESS_STATUS.BOOKED, msg, id);
		Logging.info(msg);
	}

	
	/**
	 * Sets the status of the current strategy entry to status ERROR.
	 * Logs the message with loglevel error.
	 * 
	 * @param table
	 *            Table containing the strategies.
	 * @param row
	 *            Row of the current strategy.
	 * @param e
	 *            Error occasioned the failure.
	 * @throws OException
	 */
	private void setStatusError(Table table, int row, Throwable e) throws OException {
		setStatus(table, row, MIGR_PROCESS_STATUS.ERROR, e.getMessage(), 0);
		Logging.error(e.getMessage());
	}
}
