package com.matthey.testutil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.matthey.testutil.common.Util;
import com.matthey.testutil.exception.SapTestUtilException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

/**
 * Web service HTTP requestor, initially developed to support POST requests to
 * the Openlink Connex webservice gateway
 * 
 * @author SharmV04
 */
public abstract class HttpRequestor extends BulkOperationScript
{
	boolean isInfoColumnAdded = false;

	@Override
	public Table performBulkOperation(Table tblInputData) throws OException, SapTestUtilException
	{
		try
		{
			final String POST_URL = tblInputData.getString("url_path", 1);
			String inputCSVPath = tblInputData.getString("csv_path", 1);
			String csvInputDirectoryPath = tblInputData.getString("csv_directory", 1);
			Table resultTable;

			setupLog();
			PluginLog.info("Started executing " + this.getClass().getSimpleName());
			PluginLog.debug("csvPath: " + inputCSVPath);
			if (inputCSVPath == null)
			{
				inputCSVPath = Util.getLatestFilePath(csvInputDirectoryPath);
			}

			PluginLog.debug("Latest input CSV file path: " + inputCSVPath);
			resultTable = executeWebServiceRequest(POST_URL, inputCSVPath);
			PluginLog.info("Completed executing " + this.getClass().getSimpleName());
			return resultTable;
		}
		catch (IOException ioException)
		{
			com.matthey.testutil.common.Util.printStackTrace(ioException);
			PluginLog.info("Completed executing " + this.getClass().getSimpleName());
			throw new SapTestUtilException("IOException occurred");
		}
		catch (Exception e)
		{
			com.matthey.testutil.common.Util.printStackTrace(e);
			PluginLog.info("Completed executing " + this.getClass().getSimpleName());
			throw new SapTestUtilException("Exception occurred");
		}
	}

	/**
	 * Send xml post {@code request} to {@code url}
	 * 
	 * @param url
	 * @param request
	 * @param xmlItemNumber
	 * @param resultTable
	 * @throws Exception
	 */
	protected void sendPost(String url, String request, int xmlItemNumber, Table resultTable) throws Exception
	{
		URL obj;
		HttpURLConnection con = null;

		obj = new URL(url);
		con = (HttpURLConnection) obj.openConnection();
		int responseCode;

		final String DEAL_NUMBER_COLUMN_IN_RESULT_TABLE = "Deal Number";
		final String HTTP_RESPONSE_CODE_COLUMMN_IN_RESULT_TABLE = "HTTP Response Code";

		String inputLine;

		StringBuilder response = null;

		BufferedReader bufferedReader;

		con.setRequestMethod("POST");

		// Send post request
		con.setDoOutput(true);

		try (DataOutputStream wr = new DataOutputStream(con.getOutputStream());)
		{

			wr.writeBytes(request);
			wr.flush();

			responseCode = con.getResponseCode();
			PluginLog.debug("\nSending 'POST' request to URL : " + url);
			PluginLog.debug("Response Code : " + responseCode);

			bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

			resultTable.addRow();
			resultTable.setInt(HTTP_RESPONSE_CODE_COLUMMN_IN_RESULT_TABLE, xmlItemNumber + 1, responseCode);
			if (responseCode == HttpURLConnection.HTTP_OK)
			{
				response = new StringBuilder();
				while ((inputLine = bufferedReader.readLine()) != null)
				{
					response.append(inputLine);
				}

				PluginLog.debug("Response:");
				PluginLog.debug(response.toString());

				updateResultTable(xmlItemNumber, resultTable, request, response);
			}
			else
			{
				PluginLog.warn("POST request not worked");
				resultTable.setString(DEAL_NUMBER_COLUMN_IN_RESULT_TABLE, xmlItemNumber + 1, "NA");
			}

			PluginLog.debug(response.toString());
		}
		catch (Exception e)
		{
			Util.printStackTrace(e);
			throw new SapTestUtilException(e.getMessage());
		}
		finally
		{
			con.disconnect();
		}
	}

	@Override
	public Table generateInputData() throws OException
	{
		return getArgt();
	}

	/**
	 * Prepare XML request from {@code inputCSVPath}
	 * 
	 * @param resultTable
	 * @param con
	 * @param bufferedWriter
	 * @param inputCSVPath
	 * @throws Exception
	 */
	protected abstract Table executeWebServiceRequest(String postUrl, String inputCSVPath) throws Exception;

	/**
	 * Update {@code resultTable} after parsing {@code response}
	 * 
	 * @param i
	 * @param resultTable
	 * @param request
	 * @param response
	 * @throws OException
	 */
	protected abstract void updateResultTable(int i, Table resultTable, String request, StringBuilder response) throws OException;
}