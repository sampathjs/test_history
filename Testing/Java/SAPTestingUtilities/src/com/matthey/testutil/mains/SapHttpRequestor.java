package com.matthey.testutil.mains;

import java.io.IOException;
import java.util.List;

import com.matthey.testutil.HttpRequestor;
import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.exception.SapTestUtilException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

/**
 * Utility to create POST request of the using with provided CSV file.
 * This utility parses the CSV file and creates XML request and post to the URL provided
 * @author SharmV04
 *
 */
public class SapHttpRequestor extends HttpRequestor
{
	@Override
	public Table executeWebServiceRequest(String postUrl, String inputCSVPath) throws SapTestUtilException, OException
	{
		final String DEAL_NUMBER_COLUMN_IN_RESULT_TABLE = "Deal Number";
		final String ERROR_MESSAGE_COLUMN_IN_RESULT_TABLE = "Error message";
		final String HTTP_RESPONSE_CODE_COLUMMN_IN_RESULT_TABLE = "HTTP Response Code";

		String request;

		List<String> xmlMessageList;

		SapHttpRequestorHelper sapHttpRequestorHelper = null;
		Table resultTable;
		try
		{

			resultTable = Table.tableNew();
			sapHttpRequestorHelper = new SapHttpRequestorHelper(inputCSVPath);
			xmlMessageList = sapHttpRequestorHelper.getXmlMessages();

			resultTable.addCol(DEAL_NUMBER_COLUMN_IN_RESULT_TABLE, COL_TYPE_ENUM.COL_STRING);
			resultTable.addCol(ERROR_MESSAGE_COLUMN_IN_RESULT_TABLE, COL_TYPE_ENUM.COL_CLOB);
			resultTable.addCol(HTTP_RESPONSE_CODE_COLUMMN_IN_RESULT_TABLE, COL_TYPE_ENUM.COL_INT);

			PluginLog.debug("URL path for post request: " + postUrl);

			for (int xmlItem = 0; xmlItem < xmlMessageList.size(); xmlItem++)
			{
				request = xmlMessageList.get(xmlItem);
				PluginLog.debug("Request:");
				PluginLog.debug(request);

				sendPost(postUrl, request, xmlItem, resultTable);
				sapHttpRequestorHelper.updateInfoFieldValue(resultTable, xmlItem + 1);

			}
		}
		catch (OException oException)
		{
			Util.printStackTrace(oException);
			throw new SapTestUtilException(oException.getMessage());
		}
		catch (IOException ioException)
		{
			Util.printStackTrace(ioException);
			throw new SapTestUtilException(ioException.getMessage());
		}
		catch (Exception exception)
		{
			Util.printStackTrace(exception);
			throw new SapTestUtilException(exception.getMessage());
		}
		finally
		{
			if (sapHttpRequestorHelper != null)
			{
				sapHttpRequestorHelper.cleanup();
			}
		}
		return resultTable;
	}

	@Override
	public void updateResultTable(int xmlItemNumber, Table resultTable, String request, StringBuilder response) throws OException
	{
		final String STARTING_ERROR_TAG = "<ERROR>";
		final String CLOSING_ERROR_TAG = "</ERROR>";
		final String ERROR_MESSAGE_COLUMN_IN_RESULT_TABLE = "Error message";
		final String INFO_FIELD_NAME_COLUMN_IN_RESULT_TABLE = "Info Field Name";
		final String INFO_FIELD_VALUE_COLUMN_IN_RESULT_TABLE = "Info Field Value";
		final String SAP_ORDER_ID = "SAP_Order_ID";
		final String METAL_TRANSFER_REQUEST_NO = "Metal Transfer Request No.";
		final String STARTING_TRADE_REFERENCE_ID = "<cdres:TradeReferenceID datatype=\"str\">";
		final String CLOSING_TRADE_REFERENCE_ID = "</cdres:TradeReferenceID>";
		final String STARTING_METAL_TRANSFER_TRADE_REFERENCE_ID = "<mtres:TradeReferenceID datatype=\"str\">";
		final String CLOSING_METAL_TRANSFER_TRADE_REFERENCE_ID = "</mtres:TradeReferenceID>";
		final String DEAL_NUMBER_COLUMN_IN_RESULT_TABLE = "Deal Number";

		int lastIndexOfStartingErrorTag;
		int lengthOfStartingErrorTag;
		int endIndex;
		int lengthOfResponse;
		int indexOfStartingTradeReferenceID;
		int indexOfStartingMetalTransferTradeReferenceID;

		if (request.contains("getCoverageDealRequest") && resultTable.getColNum(SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER) <= 0)
		{
			resultTable.addCol(SAPTestUtilitiesConstants.COVERAGE_INSTRUCTION_NUMBER, COL_TYPE_ENUM.COL_STRING);
		}
		else if (request.contains("getMetalTransferRequest") && resultTable.getColNum(SAPTestUtilitiesConstants.METAL_TRANSFER_REQUEST_NUMBER) <= 0)
		{
			resultTable.addCol(SAPTestUtilitiesConstants.METAL_TRANSFER_REQUEST_NUMBER, COL_TYPE_ENUM.COL_STRING);
		}

		indexOfStartingTradeReferenceID = response.lastIndexOf(STARTING_TRADE_REFERENCE_ID);
		PluginLog.debug("indexOfStartingTradeReferenceID: " + indexOfStartingTradeReferenceID);
		indexOfStartingMetalTransferTradeReferenceID = response.lastIndexOf(STARTING_METAL_TRANSFER_TRADE_REFERENCE_ID);
		PluginLog.debug("indexOfStartingMetalTransferTradeReferenceID: " + indexOfStartingMetalTransferTradeReferenceID);
		if (indexOfStartingTradeReferenceID > -1)
		{
			response.delete(0, response.lastIndexOf(STARTING_TRADE_REFERENCE_ID) + STARTING_TRADE_REFERENCE_ID.length());
			response.delete(response.lastIndexOf(CLOSING_TRADE_REFERENCE_ID), response.length());

			int dealNumber = Integer.valueOf(response.toString());
			resultTable.setString(DEAL_NUMBER_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, response.toString());
			Transaction transaction = Transaction.retrieve(dealNumber);
			int instrumentType = transaction.getInsType();

			PluginLog.debug("Instrument type: " + instrumentType);
			resultTable.setString(INFO_FIELD_NAME_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, SAP_ORDER_ID);
			resultTable.setString(INFO_FIELD_VALUE_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, transaction.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, SAP_ORDER_ID));
			resultTable.setClob(ERROR_MESSAGE_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, "NA");

		}
		else if (indexOfStartingMetalTransferTradeReferenceID > -1)
		{

			response.delete(0, response.lastIndexOf(STARTING_METAL_TRANSFER_TRADE_REFERENCE_ID) + STARTING_METAL_TRANSFER_TRADE_REFERENCE_ID.length());
			response.delete(response.lastIndexOf(CLOSING_METAL_TRANSFER_TRADE_REFERENCE_ID), response.length());

			int dealNumber = Integer.valueOf(response.toString());
			resultTable.setString(DEAL_NUMBER_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, response.toString());
			Transaction transaction = Transaction.retrieve(dealNumber);
			int instrumentType = transaction.getInsType();

			PluginLog.debug("Instrument type: " + instrumentType);

			resultTable.setString(INFO_FIELD_NAME_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, METAL_TRANSFER_REQUEST_NO);
			resultTable.setString(INFO_FIELD_VALUE_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, transaction.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, "SAP-MTRNo"));

			resultTable.setClob(ERROR_MESSAGE_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, "NA");

		}
		else
		{
			lastIndexOfStartingErrorTag = response.lastIndexOf(STARTING_ERROR_TAG);
			PluginLog.debug("Last index of starting error tag: " + lastIndexOfStartingErrorTag);
			lengthOfStartingErrorTag = STARTING_ERROR_TAG.length();
			endIndex = lastIndexOfStartingErrorTag + lengthOfStartingErrorTag;
			response.delete(0, endIndex);
			PluginLog.debug("Response before deleting closing error tag: ");
			PluginLog.debug(response.toString());
			lengthOfResponse = response.length();
			response.delete(response.lastIndexOf(CLOSING_ERROR_TAG), lengthOfResponse);

			resultTable.setString(DEAL_NUMBER_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, "NA");
			resultTable.setClob(ERROR_MESSAGE_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, response.toString());
		}
	}

	@Override
	public String getOperationName()
	{
		return "Web Service Injection";
	}

}
