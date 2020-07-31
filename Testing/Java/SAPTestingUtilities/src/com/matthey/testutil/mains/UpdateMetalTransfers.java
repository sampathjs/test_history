package com.matthey.testutil.mains;

import com.matthey.testutil.BulkOperationScript;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.enums.EndurTranInfoField;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Update metal transfers (Strategy deals) by setting a number of trade attributes and
 * moving the Strategy to New to kick off OLF TPM workflows
 * @author KailaM01
 */
public class UpdateMetalTransfers extends BulkOperationScript
{

	
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_updating_transfers", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		Table tblCsvData = Table.tableNew("CSV data");
		int ret = tblCsvData.inputFromCSVFile(csvPath);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + csvPath);
		}
		
		Util.updateTableWithColumnNames(tblCsvData);
		
		return tblCsvData;
	}

	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		com.matthey.testutil.common.Util.unLockDeals();
		
		int numRows = tblInputData.getNumRows();

		tblInputData.addCols("I(deal_num) I(tran_num) I(tran_status) I(toolset) I(ins_type) S(status)");
		
		for (int row = 1; row <= numRows; row++)
		{
			String tranInfoValue = tblInputData.getString("metal_transfer_request_number", row);

			Table tblTempData = null;
			
			try
			{
				tblTempData = Table.tableNew("Temp data");
				
				String sqlQuery = 
						"SELECT \n" +
							"ab.deal_tracking_num AS deal_num, \n" +
							"ab.tran_num, \n" +
							"ab.tran_status, \n" +
							"ab.toolset, \n" +
							"ab.ins_type, \n" +
							"ati.value \n" +
						"FROM \n" +
							"ab_tran_info ati, \n" +
							"ab_tran ab \n" +
						"WHERE ati.tran_num = ab.tran_num \n" +
						"AND ati.type_id = " + EndurTranInfoField.SAP_METAL_TRANSFER_REQUEST_NUMBER.toInt() + " \n" +
						"AND ati.value = '" + tranInfoValue + "' \n" +
						"AND ab.current_flag = 1";
				
				int ret = DBaseTable.execISql(tblTempData, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new SapTestUtilRuntimeException("Unable to load query: " + sqlQuery);
				}
				
				tblTempData.copyRowByColName(1, tblInputData, row);
			}
			finally
			{	
				if (tblTempData != null)
				{
					tblTempData.destroy();
				}
			}
		}

		int numDeals = tblInputData.getNumRows();		
		for (int row = 1; row <= numDeals; row++)
		{
			int tranNum = tblInputData.getInt("tran_num", row);
			
			if (tranNum > 0)
			{
				Transaction tran = null;
			
				
				try
				{
					tran = Transaction.retrieve(tranNum);

					String toAccLoco = tblInputData.getString("to_acc_loco", row);
					String toAccForm = tblInputData.getString("to_acc_form", row);
					String toAcc = tblInputData.getString("to_acc", row);
					String toAccBu = tblInputData.getString("to_acc_bu", row);
					String charges = tblInputData.getString("charges", row);
					String chargesinUSD = tblInputData.getString("charges_in_USD", row);

					/* We set via tran.setField so instrument builder scripts get triggered (event/field notifications) */
					updateTranInfoField(tran, EndurTranInfoField.TO_ACC_LOCO.toString(), toAccLoco);
					updateTranInfoField(tran, EndurTranInfoField.TO_ACC_FORM.toString(), toAccForm);
					updateTranInfoField(tran, EndurTranInfoField.TO_ACC.toString(), toAcc);
					updateTranInfoField(tran, EndurTranInfoField.TO_ACC_BU.toString(), toAccBu);
					updateTranInfoField(tran, EndurTranInfoField.CHARGES.toString(), charges);
					updateTranInfoField(tran, EndurTranInfoField.CHARGES_IN_USD.toString(), chargesinUSD);
					tran.loadDealComments();
					int numDealComments = tran.getNumRows(0, TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt());
					Logging.debug("numDealComments: " + numDealComments);
					addDealComment("From Account"+" "+tran.getTranNum(), tran, "From Account",numDealComments);
					//tran.loadDealComments();
					addDealComment("To Account"+" "+tran.getTranNum(), tran, "To Account",numDealComments+1);
					tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_NEW);
					
					/*Table tblTempData = Table.tableNew();
					String sqlQuery = 
							"SELECT \n" +
								"ab.deal_tracking_num AS deal_num \n" +
							"FROM ab_tran ab \n" +
							"WHERE tran_num="+tranNum;
					Logging.debug("sqlQuery:"+sqlQuery);
					int ret = DBaseTable.execISql(tblTempData, sqlQuery);
					int dealNumber = tblTempData.getInt("deal_num", 1);
					
					Table tblTempDataWithDealNumber = Table.tableNew();
					
					sqlQuery = 
							"SELECT \n" +
								"ab.tran_num AS tran_num \n" +
							"FROM ab_tran ab \n" +
							"WHERE deal_tracking_num="+dealNumber+" and "+
							"ab.current_flag=1";
					Logging.debug("sqlQuery:"+sqlQuery);
					DBaseTable.execISql(tblTempDataWithDealNumber, sqlQuery);
					Logging.debug("tblTempDataWithDealNumber:");
					Util.printTableOnLogTable(tblTempDataWithDealNumber);
					tranNum = tblTempDataWithDealNumber.getInt("tran_num", 1);
					Logging.debug("tranNum: " + tranNum);
					
					tran = Transaction.retrieve(tranNum);
					*/
					//tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_NEW);
					tblInputData.setString("status", row, "Succeeded");
				}
				catch (Exception e)
				{
					Logging.error("Error occured during update of tran_num: " + tranNum +", " + e.getMessage());
					Util.printStackTrace(e);
					tblInputData.setString("status", row, e.getMessage());
				}
				finally
				{
					if (tran != null)
					{
						tran.destroy();
					}
				}
			}
		}
		
		return tblInputData;
	}

	@Override
	public String getOperationName() 
	{
		return "Update Metal Transfers";
	}
	
	
	/**
	 * @param tran
	 * @param tranInfoName
	 * @param newValue
	 * @throws OException
	 */
	private void updateTranInfoField(Transaction tran, String tranInfoName, String newValue) throws OException 
	{
		int returnedValue = 1;
				
		Logging.debug("Updating transaction field " + tranInfoName + " with value " + newValue);
		
		if (null != newValue && !newValue.isEmpty()) {
			returnedValue = tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, tranInfoName, newValue);
			
			com.matthey.testutil.common.Util.setupLog();
		}
		
		if (returnedValue == 0) 
		{
			com.matthey.testutil.common.Util.setupLog();
			Logging.error("Value for field " + tranInfoName + " not set");
			throw new SapTestUtilRuntimeException("Value for field " + tranInfoName + " not set");
		}
	}
	
	
	private void addDealComment(String dealComment,Transaction transaction,String commentType,int numDealComments) throws OException
	{
		if(dealComment != null && !dealComment.isEmpty())
		{
			
			transaction.addDealComment();
			transaction.insertDealComment(numDealComments);
			transaction.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_COMMENT.toInt(), 0, "", dealComment, numDealComments);
			transaction.setField(TRANF_FIELD.TRANF_DEAL_COMMENTS_NOTE_TYPE.toInt(), 0, "", commentType, numDealComments);
			 
		}
	}
	
	
	
	
	
	
	
	
	
}
