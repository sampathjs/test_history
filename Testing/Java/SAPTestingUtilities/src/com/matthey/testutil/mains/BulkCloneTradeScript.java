package com.matthey.testutil.mains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.matthey.testutil.BaseScript;
import com.matthey.testutil.toolsetupdate.DealDelta;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.toolsetupdate.ToolsetFactory;
import com.matthey.testutil.toolsetupdate.ToolsetI;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * Jira issue number :JMU-32 Main Script for Cloning Deals. It reads a csv file
 * path from argT. This csv contains old deal number, test reference, date
 * fields to be overwritten in new deal, market data for new deal. Supported
 * toolsets : Cash, ComFut, Commodity, ComSwap, FX, LoanDep This script stops a
 * list of OpServices (const repo field 'OpservicesToStopBeforeCloneTask')
 * before cloning deals and starts them back after cloning is done This script
 * (1) creates copy of old deal (2) overwrites value of date fields (3) set
 * value of tran info fields to default (4) creates clone in VALIDATED status
 * This script unlock all the locked deals This script adds output columns
 * (status, comment, new deal num) in the same csv table and saves a csv file
 * under the today's report dir
 * 
 * @author SharmV03
 *
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class BulkCloneTradeScript extends BaseScript
{
	protected final String OUR_UNIT_COLUMN = "our_unit";
	protected final String OUR_PFOLIO_COLUMN = "our_pfolio";
	protected final String C_E_UNIT_COLUMN = "c/e_unit";
	protected final String TRADER_COLUMN = "trader";
	protected final String BUY_SELL_COLUMN = "buy_sell";
	protected final String TRADE_DATE_COLUMN = "trade_date";
	protected final String FX_DATE_COLUMN = "fx_date";
	protected final String CFLOW_TYPE_COLUMN = "cflow_type";
	protected final String METAL_WEIGHT_COLUMN = "metal_weight";
	protected final String METAL_UOM_COLUMN = "metal_uom";
	protected final String TRADE_PRICE_COLUMN = "trade_price";
	protected final String TICKER_COLUMN = "ticker";
	protected final String DEAL_COMMENT_COLUMN = "deal_comment";
	protected final String STATUS_IN_ENDUR_COLUMN = "status_in_endur";
	protected final String DEAL_NUM_COLUMN = "deal_num";
	protected final String TEST_REFERENCE_COLUMN = "test_reference";

	protected final String STATUS_COLUMN = "status";
	protected final String COMMENTS_COLUMN = "comments";
	protected final String NEW_DEAL_NUMBER_COLUMN = "new_deal_num";
	protected final String REFERENCE_COLUMN = "reference";
	private final String EMPTY_STRING = "";
	private final int ONE = 1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olf.openjvs.IScript#execute(com.olf.openjvs.IContainerContext)
	 */
	@Override
	public void execute(IContainerContext context) throws OException
	{
		try
		{
			int csvParseResponseCode;

			String ticker;
			String ourUnit;
			String ourPfolio;
			String ceUnit;
			String trader;
			String buySell;
			String tradeDate;
			String fxDate;
			String cflowType;
			String metalWeight;
			String metalUom;
			String tradePrice;
			String dealComment;
			String statusInEndur;
			int dealNum;
			String testReference;

			String csvFileAbsolutePath;
			String comments = EMPTY_STRING;
			Table argumentTable = context.getArgumentsTable();
			Table returnT = context.getReturnTable();
			setupLog();
			csvFileAbsolutePath = argumentTable.getString("file_name", 1);
			initializeInputTable(returnT);
			
			/*Table tempTable = Table.tableNew();
			csvParseResponseCode = tempTable.inputFromCSVFile(csvFileAbsolutePath);
			if (csvParseResponseCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				Logging.error("Unable to load the csv file " + csvFileAbsolutePath + " in tempTable");
				throw new OException("Unable to load the csv file " + csvFileAbsolutePath + " in tempTable");
			}
			Util.updateTableWithColumnNames( returnT );
			Logging.debug("tempTable:");
			Util.printTableOnLogTable( tempTable);*/
			
			csvParseResponseCode = returnT.inputFromCSVFile(csvFileAbsolutePath);
			if (csvParseResponseCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				Logging.error("Unable to load the csv file " + csvFileAbsolutePath);
				throw new OException("Unable to load the csv file " + csvFileAbsolutePath);
			}
			//Util.updateTableWithColumnNames( returnT );
			returnT.delRow(1);
			Logging.debug("returnT:");
			Util.printTableOnLogTable( returnT);
			
			int numberOfRows = returnT.getNumRows();
			addOutputColumns(returnT);

			Map<String, String> infoFieldDefaultValues = getTranInfoDefaultValue();

			List<String> listOfOpservices = stopOpservices();
			ToolsetFactory tFactory = new ToolsetFactory();
			Logging.info("Cloning " + numberOfRows + " deals");

			for (int row = ONE; row <= numberOfRows; row++)
			{
				comments = EMPTY_STRING;
				dealNum = returnT.getInt(DEAL_NUM_COLUMN, row);
				// String originalRef =
				// returnT.getString(ORIGINAL_REFERENCE_COLUMN, row);
				String searchOldTranQuery = null;
				if (dealNum > 0)
				{
					searchOldTranQuery = "select tran_num,deal_tracking_num,reference from ab_tran where deal_tracking_num =" + dealNum + " and current_flag = 1";
				}
				/*
				 * else { searchOldTranQuery =
				 * "select tran_num,deal_tracking_num,reference from ab_tran where reference ='"
				 * + originalRef + "' and tran_type=" +
				 * TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.toInt() +
				 * " and current_flag = 1"; }
				 */

				Logging.debug("searchOldTranQuery=" + searchOldTranQuery);
				Table tranNumberTable = Table.tableNew();
				int iRetVal = DBaseTable.execISql(tranNumberTable, searchOldTranQuery);
				if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new OException(" Unable to execute query SQL. Return code= " + iRetVal + "." + searchOldTranQuery);
				}

				if (tranNumberTable.getNumRows() == 0)
				{
					Logging.warn("Original deal not found. dealNumber = "
							+ dealNum /* + ", originalRef=" + originalRef */);
					returnT.setString(STATUS_COLUMN, row, "Failure");
					returnT.setString(COMMENTS_COLUMN, row, "No deal found");
				}
				else
				{
					if (tranNumberTable.getNumRows() > 1)
					{
						Logging.warn("More than one Transaction found for originalRef=" /* + originalRef */);
					}
					Transaction copyTransaction = com.olf.openjvs.Util.NULL_TRAN;
					Transaction clone = com.olf.openjvs.Util.NULL_TRAN;

					try
					{
						int transactionNumber = tranNumberTable.getInt("tran_num", 1);
						dealNum = tranNumberTable.getInt("deal_tracking_num", 1);
						// originalRef = tranNumberTable.getString("reference",
						// 1);
						returnT.setInt(DEAL_NUM_COLUMN, row, dealNum);
						// returnT.setString(ORIGINAL_REFERENCE_COLUMN, row,
						// originalRef);
						Logging.debug("Started cloning. transactionNumber=" + transactionNumber + ",dealNumber="
								+ dealNum /* + ",originalRef=" + originalRef */);
						copyTransaction = Transaction.retrieveCopy(transactionNumber);

						DealDelta dealDelta = new DealDelta();

						ourUnit = returnT.getString(OUR_UNIT_COLUMN, row);
						dealDelta.setOurUnit(ourUnit);

						ourPfolio = returnT.getString(OUR_PFOLIO_COLUMN, row);
						dealDelta.setOurPfolio(ourPfolio);

						ceUnit = returnT.getString(C_E_UNIT_COLUMN, row);
						dealDelta.setCeUnit(ceUnit);

						trader = returnT.getString(TRADER_COLUMN, row);
						dealDelta.setTrader(trader);

						buySell = returnT.getString(BUY_SELL_COLUMN, row);
						dealDelta.setBuySell(buySell);

						tradeDate = returnT.getString(TRADE_DATE_COLUMN, row);
						dealDelta.setTradeDate(tradeDate);

						fxDate = returnT.getString(FX_DATE_COLUMN, row);
						dealDelta.setFxDate(fxDate);

						cflowType = returnT.getString(CFLOW_TYPE_COLUMN, row);
						dealDelta.setCflowType(cflowType);

						metalWeight = returnT.getString(METAL_WEIGHT_COLUMN, row);
						dealDelta.setMetalWeight(metalWeight);

						metalUom = returnT.getString(METAL_UOM_COLUMN, row);
						dealDelta.setMetalUom(metalUom);

						tradePrice = returnT.getString(TRADE_PRICE_COLUMN, row);
						dealDelta.setTradePrice(tradePrice);

						ticker = returnT.getString(TICKER_COLUMN, row);
						dealDelta.setTicker(ticker);

						dealComment = returnT.getString(DEAL_COMMENT_COLUMN, row);
						dealDelta.setDealComment(dealComment);
						
						statusInEndur = returnT.getString(STATUS_IN_ENDUR_COLUMN, row);
						dealDelta.setStatusInEndur(statusInEndur);

						dealNum = returnT.getInt(DEAL_NUM_COLUMN, row);
						dealDelta.setDealNum(dealNum);

						testReference = returnT.getString(TEST_REFERENCE_COLUMN, row);
						dealDelta.setTestReference(testReference);
						
						dealDelta.setTranNum(transactionNumber);

						ToolsetI toolset = tFactory.createToolset(copyTransaction, dealDelta);
						Logging.debug("ToolsetI=" + toolset.getClass().getName() + " for originalTranNum=" + transactionNumber);
						toolset.updateToolset();

						Logging.debug("Updating TransactionInfo");
						toolset.updateTransactionInfo(infoFieldDefaultValues);
						
						//updateTransactionField(TRANF_FIELD.tranf_, 0, dealDelta.getFrom());updateTransactionField(TRANF_FIELD.tranf_, 0, dealDelta.getFrom());

						toolset.copySettlementInstructions();
						clone = toolset.createClone();
						int newDealNumber = clone.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
						String refText = clone.getField(TRANF_FIELD.TRANF_REFERENCE.toInt());

						returnT.setInt(NEW_DEAL_NUMBER_COLUMN, row, newDealNumber);
						returnT.setString(STATUS_COLUMN, row, "Success");
						returnT.setString(COMMENTS_COLUMN, row, comments);
						returnT.setString(REFERENCE_COLUMN, row, refText);
					}
					catch (OException oException)
					{
						Util.printStackTrace(oException);
						Logging.error("Transaction with deal number " + dealNum + " cloning failed." + oException.getMessage());
						comments += "Unable to clone.Exception occurred:" + oException.getMessage();
						returnT.setString(STATUS_COLUMN, row, "Failure");
						returnT.setString(COMMENTS_COLUMN, row, comments.toString());
					}
					finally
					{
						if (Transaction.isNull(copyTransaction) != 1)
						{
							copyTransaction.destroy();
						}
						if (Transaction.isNull(clone) != 1)
						{
							clone.destroy();
						}
						if (Table.isTableValid(tranNumberTable) == 1)
						{
							tranNumberTable.destroy();
						}
					}
				}
			}
			Logging.info("Starting Op Services");
			startOpservices(listOfOpservices);

			Logging.info("Creating output results csv");
			Util.generateCSVFile(returnT, "BulkTradeClone", null);
			Util.unLockDeals();
		}
		catch (Throwable throwable)
		{
			Util.printStackTrace(throwable);
			com.olf.openjvs.Util.exitFail();
		}finally{
			Logging.close();
		}
	}

	/**
	 * @return
	 * @throws OException
	 */
	protected Table initializeInputTable(Table argTable) throws OException
	{
		argTable.addCol(TICKER_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(OUR_UNIT_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(OUR_PFOLIO_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(C_E_UNIT_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(TRADER_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(BUY_SELL_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(TRADE_DATE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(FX_DATE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(CFLOW_TYPE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(METAL_WEIGHT_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(METAL_UOM_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(TRADE_PRICE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(DEAL_COMMENT_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(STATUS_IN_ENDUR_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argTable.addCol(DEAL_NUM_COLUMN, COL_TYPE_ENUM.COL_INT);
		argTable.addCol(TEST_REFERENCE_COLUMN, COL_TYPE_ENUM.COL_STRING);

		return argTable;
	}

	protected Table addOutputColumns(Table argumentTable) throws OException
	{
		argumentTable.addCol(STATUS_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argumentTable.addCol(COMMENTS_COLUMN, COL_TYPE_ENUM.COL_STRING);
		argumentTable.addCol(NEW_DEAL_NUMBER_COLUMN, COL_TYPE_ENUM.COL_INT);
		argumentTable.addCol(REFERENCE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		return argumentTable;
	}

	/**
	 * Return a maps of Tran Info field name and default value. Only fields
	 * where default value is defined are included in the response
	 * 
	 * @return
	 * @throws OException
	 */
	private Map<String, String> getTranInfoDefaultValue() throws OException
	{
		String infoFieldValuesQuery = "SELECT type_name,default_value FROM tran_info_types WHERE (default_value IS NOT NULL) AND (default_value NOT LIKE '') ";
		Logging.debug("infoFieldValuesQuery=" + infoFieldValuesQuery);

		Table infoFieldDefaultValuesTable = com.olf.openjvs.Util.NULL_TABLE;
		Map<String, String> infoFieldDefaultValues = new HashMap<String, String>();
		try
		{
			infoFieldDefaultValuesTable = Table.tableNew();
			int iRetVal = DBaseTable.execISql(infoFieldDefaultValuesTable, infoFieldValuesQuery);
			if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new OException(" Unable to execute query SQL. Return code= " + iRetVal + "." + infoFieldValuesQuery);
			}
			int numberOfRows = infoFieldDefaultValuesTable.getNumRows();

			for (int currentRow = 1; currentRow <= numberOfRows; currentRow++)
			{
				infoFieldDefaultValues.put(infoFieldDefaultValuesTable.getString("type_name", currentRow), infoFieldDefaultValuesTable.getString("default_value", currentRow));
			}
		}
		finally
		{
			infoFieldDefaultValuesTable.destroy();
		}
		return infoFieldDefaultValues;
	}

	/**
	 * This method stops opservices provided by user in constant repository
	 * 
	 * @return
	 * @throws OException
	 */
	private List<String> stopOpservices() throws OException
	{
		int i;
		String opservice;
		List<String> listOfOpservicesToStop = determineOpservicesToStop();
		Logging.info("Stopping these opservices:" + listOfOpservicesToStop);
		for (i = 0; i < listOfOpservicesToStop.size(); i++)
		{
			opservice = listOfOpservicesToStop.get(i);
			OpService.stopMonitoring(opservice);
		}

		return listOfOpservicesToStop;
	}

	/**
	 * This method reads a comma separated name of OpServices from Constants
	 * repository. Filters and returns the Opservices that currently running.
	 * 
	 * @return
	 * @throws OException
	 */
	private List<String> determineOpservicesToStop() throws OException
	{
		String opservice;
		int iRetVal;
		Table opservicesToStopTable = com.olf.openjvs.Util.NULL_TABLE;
		ConstRepository constRepository = new ConstRepository("SAPTestUtil", "BulkTradeClone");

		String opservicesNamesCSV = constRepository.getStringValue("OpservicesToStopBeforeCloneTask");
		List<String> listOfOpservicesToStop = new ArrayList<String>();
		try
		{
			if (null != opservicesNamesCSV && !opservicesNamesCSV.isEmpty())
			{
				// This quotes each opservice name present in constant
				// repository variable
				String opservicesForSqlQuery = "'" + opservicesNamesCSV.replace(",", "\',\'") + "'";
				Logging.debug("opservicesForSqlQuery=" + opservicesForSqlQuery);

				String query = "SELECT q1.definition_name AS online_definition_name" + " FROM \n" + "(SELECT red.defn_name AS definition_name,\n" + "MAX(rmdh.last_update) AS latest_update_date,\n" + "red.exp_defn_id AS definition_id" + " FROM \n"
						+ "rsk_monitor_default_history rmdh \n" + "inner join  \n" + "rsk_exposure_defn red" + " ON (rmdh.exp_defn_id = red.exp_defn_id)\n" + "WHERE   red.defn_name IN (" + opservicesForSqlQuery + ") \n"
						+ "GROUP BY red.defn_name,red.exp_defn_id)\n" + "AS q1 \n" + "INNER JOIN rsk_monitor_default_history q2 \n" + "ON (q1.definition_id=q2.exp_defn_id AND q1.latest_update_date=q2.last_update \n" + "AND q2.online_flag=1)";
				opservicesToStopTable = Table.tableNew();
				Logging.debug("query=" + query);
				iRetVal = DBaseTable.execISql(opservicesToStopTable, query);
				if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new OException(" Unable to execute query SQL. Return code= " + iRetVal + "." + query);
				}
			}

			int totalRowsInArgOpservicesToStopTable = opservicesToStopTable.getNumRows();
			for (int rowNum = ONE; rowNum <= totalRowsInArgOpservicesToStopTable; rowNum++)
			{
				opservice = opservicesToStopTable.getString("online_definition_name", rowNum);
				listOfOpservicesToStop.add(opservice);
			}
		}
		catch (OException oe)
		{
			Logging.error("Exception in determining Ops service to stop." + oe.getMessage());
			throw oe;
		}
		finally
		{
			opservicesToStopTable.destroy();
		}
		return listOfOpservicesToStop;
	}

	/**
	 * @param listOfOpservices
	 * @throws OException
	 */
	private void startOpservices(List<String> listOfOpservices) throws OException
	{
		for (String opservice : listOfOpservices)
		{
			OpService.startMonitoring(opservice);
		}
	}
}