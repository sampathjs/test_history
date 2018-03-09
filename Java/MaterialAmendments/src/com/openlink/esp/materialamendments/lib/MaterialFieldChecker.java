/*
 * History:
 * 1.0   - 15.01.2013 eikesass - initial version 
 * 1.1   - 05.03.2013 jbonetzk - fixed 'getTranFields'
 *                             - don't initialize logging in constructor
 */

package com.openlink.esp.materialamendments.lib;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


/**
 * This plug-in is an OPS post processing plug-in. 
 * It checks if an relevant field (see material check user tables) on the deal
 * has changed. If no changes are found existing documents are linked to the new transaction.
 * Otherwise a plug-in that is set up in const repository will be triggered.
 * Prerequisites: 
 * The current transaction must have an amended version to compare to.
 * 
 * @author eikesass
 * @version 1.0
 * @category none
 */

public class MaterialFieldChecker implements IScript
{
	private final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

	private static final String CONTEXT = "BackOffice";
	private static final String SUBCONTEXT = "Material Amendments";

	// constants set be const repository
	private final ConstRepository constRepo;
	private final String scriptName;

	/**
	 * This constructor is called from the plug-in class
	 * 
	 * @return
	 * @throws OException
	 */
	public MaterialFieldChecker() throws OException
	{
		constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);

		scriptName = constRepo.getStringValue("Document Processing Script", null);
	}

	/**
	 */
	@Override
	final public void execute(IContainerContext context) throws OException
	{
		initLogging();

		try
		{
			process(context);
		}
		catch (Throwable t)
		{
			PluginLog.error(t.toString());
		}

		PluginLog.exitWithStatus();
	}

	private void process(IContainerContext context) throws OException
	{
		int tranNum;
		Table checkedFields = null;
		Transaction newTran = null;
		Transaction oldTran = null;
		boolean transAreEqual;

		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		try
		{
			tranNum = argt.getTable("Deal Info", 1).getInt("tran_num", 1);

			PluginLog.debug("Retrieving new transaction #" + tranNum);
			newTran = Transaction.retrieve(tranNum);
			PluginLog.debug("Retrieving new transaction done!");

			int dealNum = newTran.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
			int insType = newTran.getFieldInt(TRANF_FIELD.TRANF_INS_TYPE.toInt());

			PluginLog.debug("Retrieving old transaction of deal #" + dealNum);
			oldTran = retrieveOldTransaction(dealNum);
			PluginLog.debug("Retrieving old transaction done!");

			if (oldTran != null)
			{
				checkedFields = getTranFields(insType);

				PluginLog.debug("Comparing transactions");
				transAreEqual = areTransactionsEqual(newTran, oldTran, checkedFields);
				if (transAreEqual)
				{
					PluginLog.debug("Compared transactions are equal");
					PluginLog.debug("Assign events to settlement document");
					putOldDocumentsOnNewTransaction(newTran, oldTran);
					PluginLog.debug("Assign events to settlement document done!");
				}
				else
				{
					PluginLog.debug("Compared transactions are not equal");
					if (scriptName != null && scriptName.length() > 0)
					{
						int scriptId = Ref.getValue(SHM_USR_TABLES_ENUM.SCRIPT_TABLE, scriptName);

						PluginLog.debug("Calling script '" + scriptName + "' (Id " + scriptId + ")");
						Util.runScript(scriptId, argt.copyTable(), returnt.copyTable());
						PluginLog.debug("Script '" + scriptName + "' (Id " + scriptId + ") started");
					}
				}
				oldTran.destroy();
			}
			newTran.destroy();
		}
		catch (Exception e)
		{
			PluginLog.fatal(e.getMessage());
			AlertBroker.sendAlert("BO-MAT-001", e.getMessage());
		}
	}

	private void initLogging() throws OException
	{
		String  logLevel = constRepo.getStringValue("logLevel", "Error"),
				logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName()+".log"),
				logDir = constRepo.getStringValue("logPath", null);

		try
		{
			if (logDir == null)
				PluginLog.init(logLevel);
			else
				PluginLog.init(logLevel, logDir, logFile);
		}
		catch (Exception e)
		{
			OConsole.oprint("Error initializing PluginLog: " + e.getMessage());
		}
	}

	/**
	 * Returns a map of instrument types and material tran fields as defined in
	 * the material check user tables
	 * 
	 * @return map of instrument types and tran fields
	 * @throws OException
	 */
	public Table getTranFields(int insType) throws OException
	{
		String
		sql = "SELECT *"
			+ "  FROM user_material_check_ins_types mcit"
			+ "     , user_material_check_rules mcr"
			+ " WHERE mcit.ins_grp = mcr.ins_grp"
			+ "   AND mcit.ins_type = " + insType;

		return execISql(sql);
	}

	/**
	 * Checks if two versions of a deal are identical in their material tran
	 * fields as defined in the material check user tables
	 * 
	 * @param newTran
	 *            the modified/new transaction
	 * @param oldTran
	 *            the existing/old transaction
	 * @param materialFields
	 *            map of instrument types and material tran fields
	 * @return true if material fields have not been changed, false otherwise
	 * @throws OException
	 */
	public boolean areTransactionsEqual(Transaction newTran, Transaction oldTran, Table tranFields) throws OException
	{
		PluginLog.debug("Comparing Transaction #" + newTran.getTranNum() + " and Transaction #" + oldTran.getTranNum());
		int numLegs = newTran.getNumParams();

		if (numLegs != oldTran.getNumParams())
		{
			PluginLog.debug("Number of Legs differ");
			return false;
		}

	//	for (int row = 1; row <= tranFields.getNumRows(); row++)
		for (int row = tranFields.getNumRows(); row > 0; --row)
		{
			int leg = tranFields.getInt("leg", row);
			String name = tranFields.getString("field_name", row);
			int fieldId = tranFields.getInt("field_id", row);

			if (leg >= 0)
			{
				String newValue = newTran.getField(fieldId, leg, name);
				String oldValue = oldTran.getField(fieldId, leg, name);
				if (!areEqual(newValue, oldValue))
				{
					PluginLog.debug("Values of field #" + fieldId + " (leg " + leg + ") differ (" + oldValue + " -> " + newValue + ")");
					return false;
				}
				else
				{
					PluginLog.debug("Values of field #" + fieldId + " (leg " + leg + ") are equal.");
				}
			}
			else
			{
			//	for (int currLeg = 0; currLeg < numLegs; currLeg++)
				for (int currLeg = numLegs; --currLeg >= 0; )
				{
					String newValue = newTran.getField(fieldId, currLeg, name);
					String oldValue = oldTran.getField(fieldId, currLeg, name);
					if (!areEqual(newValue, oldValue))
					{
						PluginLog.debug("Values of field #" + fieldId + " (leg " + currLeg + ") differ (" + oldValue + " -> " + newValue + ")");
						return false;
					}
					else
					{
						PluginLog.debug("Values of field #" + fieldId + " (leg " + currLeg + ") are equal.");
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check if values are equal
	 * 
	 * @param newValue
	 *            - new value
	 * @param oldValue
	 *            - old value
	 * @return true/false
	 */
	private boolean areEqual(String newValue, String oldValue)
	{
		if (newValue == null)
		{
			return (oldValue == null);
		}
		else
		{
			return newValue.equals(oldValue);
		}
	}

	/**
	 * Retrieves the latest amended version of a deal.
	 * 
	 * @param dealNum
	 *            the deal number
	 * @return the latest amended transaction. null if not exists
	 * @throws OException
	 */
	public Transaction retrieveOldTransaction(int dealNum) throws OException
	{
		Transaction tran = null;

		String
		sql = "SELECT max(tran_num) tran_num"
			+ "  FROM ab_tran"
			+ " WHERE deal_tracking_num = " + dealNum
			+ "   AND tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED.toInt();

		Table table = execISql(sql);

		if (table.getNumRows() > 0)
		{
			int oldTranNum = table.getInt("tran_num", 1);
			tran = Transaction.retrieve(oldTranNum);
		}

		return tran;
	}

	/**
	 * takes the documents from the status EVENT_TYPE_AMENDED_OPEN and
	 * EVENT_TYPE_OPEN from the old tran Num and puts in to the new one
	 * 
	 * @param newTran
	 * @param oldTran
	 * @throws OException
	 */
	public static void putOldDocumentsOnNewTransaction(Transaction newTran, Transaction oldTran) throws OException
	{
		Table oldEvents = Table.tableNew();
		Table newEvents = Table.tableNew();

		// old transaction
		Transaction.eventRetrieveEvents(oldTran.getTranNum(), EVENT_TYPE_ENUM.EVENT_TYPE_AMENDED_OPEN, oldEvents);
		Transaction.eventRetrieveEvents(oldTran.getTranNum(), EVENT_TYPE_ENUM.EVENT_TYPE_OPEN, oldEvents);

		// new transaction
		Transaction.eventRetrieveEvents(newTran.getTranNum(), EVENT_TYPE_ENUM.EVENT_TYPE_AMENDED_OPEN, newEvents);
		Transaction.eventRetrieveEvents(newTran.getTranNum(), EVENT_TYPE_ENUM.EVENT_TYPE_OPEN, newEvents);

		if (oldEvents.getNumRows() != newEvents.getNumRows())
		{
			String error = "The transactions " + oldTran.getTranNum() + " and " + newTran.getTranNum() 
						 + " doen't have the same amount of events. The documents can't be moved to the new transaction.";
			throw new OException(error);
		}

		Table eventPairs = Table.tableNew();

		eventPairs.addCol("old_event_num", COL_TYPE_ENUM.COL_INT);
		eventPairs.addCol("new_event_num", COL_TYPE_ENUM.COL_INT);

		int numRows = oldEvents.getNumRows();
		PluginLog.debug("Matching " + numRows + " events");
		for (int count = 1; count <= numRows; count++)
		{
			int oldEventNum = oldEvents.getInt("event_num", count);
			int newEventNum = newEvents.getInt("event_num", count);
			eventPairs.addRow();
			eventPairs.setInt("old_event_num", count, oldEventNum);
			eventPairs.setInt("new_event_num", count, newEventNum);
		}
		newEvents.destroy();
		oldEvents.destroy();

		int numEvents = eventPairs.getNumRows();
		PluginLog.debug(eventPairs);
		if (numEvents > 0)
		{
			int ret = StlDoc.updateAmendedDealEvents(eventPairs);
			if (ret != OLF_RETURN_SUCCEED)
			{
				String error = "Could not set the documents to transaction " + newTran.getTranNum() + ".";
				throw new OException(error);
			}
		}
		PluginLog.debug("" + numEvents + " event pairs updated in settlement document");

		eventPairs.destroy();
	}

	/**
	 * Execute and SQL statement and handles errors.
	 * 
	 * @param sql
	 *            sql statement to execute
	 * @param results
	 *            table to contain results
	 */
	private Table execISql(String sql)
	{
		Table results = null;
		try
		{
			results = Table.tableNew();
			PluginLog.debug("Execuing SQL statement:\n" + sql);
			int retVal = DBaseTable.execISql(results, sql);
			if (retVal != OLF_RETURN_SUCCEED)
			{
				PluginLog.error("Failed !!");
				String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
				throw new RuntimeException(error);
			}
		}
		catch (OException e)
		{
			throw new RuntimeException(e);
		}
		return results;
	}
}
