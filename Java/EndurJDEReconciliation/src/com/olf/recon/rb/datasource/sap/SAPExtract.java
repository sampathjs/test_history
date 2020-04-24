package com.olf.recon.rb.datasource.sap;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.olf.jm.pricewebservice.persistence.CryptoImpl;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.recon.enums.SAPReconFieldsEnum;
import com.olf.recon.enums.SAPReconOutputFieldsEnum;
import com.olf.recon.rb.datasource.ReportEngine;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/**
 * Calls a Restful Webservice to get data from SAP
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class SAPExtract extends ReportEngine
{	
	/*** Constant for test with a dummy response ***/
	private boolean isTest;
	private String testFile;
	
	private ConstRepository constRepo;
	private Properties config;
	
	/*** Class constants ***/
	private final String CONST_REPO_CONTEXT = "Recon";
	private final String CONST_REPO_SUBCONTEXT = "Endur_SAP"; 
	
	/*** Constants for converting to TOz. Multiply by the constant to get TOz equivalent. ***/
	private final double GMS_TO_TOZ = 0.032150747;
	private final double KGS_TO_TOZ = 32.150746500;
	
	/*** Use the default date instead of null ***/
	private final String DEFAULT_DATE = "1960-01-01";
	
	@Override
	public void setOutputFormat(Table output) throws OException 
	{

		output.addCol(SAPReconOutputFieldsEnum.ID.toString(), SAPReconOutputFieldsEnum.ID.colType());
		output.addCol(SAPReconOutputFieldsEnum.NOTE.toString(), SAPReconOutputFieldsEnum.NOTE.colType());
		output.addCol(SAPReconOutputFieldsEnum.GL_DATE.toString(), SAPReconOutputFieldsEnum.GL_DATE.colType());
		output.addCol(SAPReconOutputFieldsEnum.GL_DATE_PLUGIN.toString(), SAPReconOutputFieldsEnum.GL_DATE_PLUGIN.colType());		
		output.addCol(SAPReconOutputFieldsEnum.ACCOUNT_NUMBER.toString(), SAPReconOutputFieldsEnum.ACCOUNT_NUMBER.colType());
		output.addCol(SAPReconOutputFieldsEnum.TYPE.toString(), SAPReconOutputFieldsEnum.TYPE.colType()); 
		output.addCol(SAPReconOutputFieldsEnum.ENDUR_DOC_NUM.toString(), SAPReconOutputFieldsEnum.ENDUR_DOC_NUM.colType());
		output.addCol(SAPReconOutputFieldsEnum.VALUEDATE.toString(),SAPReconOutputFieldsEnum.VALUEDATE.colType());
		output.addCol(SAPReconOutputFieldsEnum.VALUE_DATE_PLUGIN.toString(), SAPReconOutputFieldsEnum.VALUE_DATE_PLUGIN.colType());
		output.addCol(SAPReconOutputFieldsEnum.QUANTITY.toString(), SAPReconOutputFieldsEnum.QUANTITY.colType());
		output.addCol(SAPReconOutputFieldsEnum.AMOUNT.toString(), SAPReconOutputFieldsEnum.AMOUNT.colType());
		output.addCol(SAPReconOutputFieldsEnum.CURRENCY.toString(), SAPReconOutputFieldsEnum.CURRENCY.colType());
		output.addCol(SAPReconOutputFieldsEnum.BATCH_NUM.toString(), SAPReconOutputFieldsEnum.BATCH_NUM.colType());
		output.addCol(SAPReconOutputFieldsEnum.DEAL_NUM.toString(), SAPReconOutputFieldsEnum.DEAL_NUM.colType());
		output.addCol(SAPReconOutputFieldsEnum.KEY.toString(), SAPReconOutputFieldsEnum.KEY.colType());
		output.addCol(SAPReconOutputFieldsEnum.CREDITDEBIT.toString(), SAPReconOutputFieldsEnum.CREDITDEBIT.colType());
		output.addCol(SAPReconOutputFieldsEnum.TAX.toString(), SAPReconOutputFieldsEnum.TAX.colType());
	}

	@Override
	public Table generateOutput(Table output) throws OException
	{
		
		PluginLog.info("window_start_date: " + windowStartDateStr + ", window_end_date: " + windowEndDateStr);
			
		/*** Get config from constant repository***/
		PluginLog.info("Getting config from const repository. Context: " + CONST_REPO_CONTEXT + ". Subcontext: " + CONST_REPO_SUBCONTEXT);		
		constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		
		if (constRepo == null) {
			throw new OException ("Unable to get config from the repository. Please check..");			
		}
		
		/*** Add data to properties ***/
		PluginLog.info("Getting config");
		config = new Properties();
		getConfig ();
	
		/*** Connect to WebService ***/
		PluginLog.info("Trying to connect and fetch response");
		String response = (isTest ? null : connect ());
		
		/*** Call Webservice. Get and process response and add data to a table ***/
		PluginLog.info("Processing Response");
		processResponse (output, response);
	
		PluginLog.info ("Returning");
		
		return output;
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{

	}

	@Override
	public void groupOutputData(Table output) throws OException 
	{
		output.group(SAPReconOutputFieldsEnum.DEAL_NUM.toString());
		output.groupBy();
	}

	@Override
	public void formatOutputData(Table output) throws OException 
	{	
	}
	
	/*** 
	 * Process the response received from the external system
	 * 
	 * @param output Table containing the final output that will be sent to ReportBuilder
	 * @param response Response received from the external system
	 * 
	 * @throws OException
	 */
	private void processResponse (Table output, String response) throws OException {

		/*** If we are testing using a file then get data from the file. Else use the response received. ***/
		response = (isTest ? getTestJSON () : response);
		
		PluginLog.info("Response JSON: " + response);
		
		/*** If there is no response JSON then throw an exception ***/
		if (response == null || response.isEmpty() || response.length() <= 2) {
			throw new RuntimeException ("No response to process.");
		}
		
		Table valueDates = Table.tableNew();
		
		JSONParser parser = new JSONParser();
		try {
			
			Object object = parser.parse(response);	
			JSONObject jsonObject = (JSONObject)object;
			JSONArray jsonData = (JSONArray)jsonObject.get("ReconciliationReport");
			int length = jsonData.size();
			PluginLog.info("Length of array: " + length);
			for (int i=0; i < length; i++) {
				PluginLog.info ("Processing record: #" + (i+1) + " of " + length);
				
				JSONObject j = (JSONObject) jsonData.get(i);
				
				/*** Get the tag for the field from Constant Repository and then get value of the tag from the response ***/
				String endurDoc = getValue (j, SAPReconFieldsEnum.JSON_TAG_ENDUR_DOCUMENT_ID.toString());
				String note = getValue (j, SAPReconFieldsEnum.JSON_TAG_NOTE.toString());
				String glDate = getValue (j, SAPReconFieldsEnum.JSON_TAG_GLDATE.toString());
				String docType = getValue (j, SAPReconFieldsEnum.JSON_TAG_DOCUMENT_TYPE.toString());
				String creditDebit = getValue (j, SAPReconFieldsEnum.JSON_TAG_CREDIT_DEBIT.toString());
				String account = getValue (j, SAPReconFieldsEnum.JSON_TAG_ACCOUNT.toString());
				String account_type = getValue (j, SAPReconFieldsEnum.JSON_TAG_ACCOUNT_TYPE.toString());
				String valueDate = getValue (j, SAPReconFieldsEnum.JSON_TAG_VALUE_DATE.toString());
				String quantity = getValue (j, SAPReconFieldsEnum.JSON_TAG_QUANTITY.toString());
				String uom = getValue (j, SAPReconFieldsEnum.JSON_TAG_UOM.toString());
				String amount = getValue (j, SAPReconFieldsEnum.JSON_TAG_AMOUNT.toString());
				String taxAmount = getValue (j, SAPReconFieldsEnum.JSON_TAG_TAX_AMOUNT.toString());
				String currency = getValue (j, SAPReconFieldsEnum.JSON_TAG_CURRENCY.toString());				
				
				
				/*** Format values before adding to the table ***/
				int endurDocument = getInteger (endurDoc);  
				
				/*** If no endurDocument then no point in proceeding as this is endur deal number ***/
				if (endurDocument == 0) {
					PluginLog.error("Endur Deal Number " + endurDocument + " received from SAP is incorrect. Please check. Skipping this record.");
					continue;
				}
				
				PluginLog.info("Processing for Deal: " + endurDocument);
				
				/*** Get double values. If they are not in the response, set them to 0 ***/
				double qtyToz = getDouble(quantity);   
				double amt = getDouble (amount);   				
				double tax = getDouble(taxAmount); 		
				
				/*** From SAP, account number is received with leading 0s. Need to remove them. ****/
				int accountNumber = getInteger(account);
				account = (accountNumber == 0 ? "" : "" + accountNumber);
				
				SimpleDateFormat inputDateFormat = new SimpleDateFormat ("yyyy-MM-dd");
				SimpleDateFormat outputDateFormat = new SimpleDateFormat ("dd-MMM-yyyy HH:mm:ss");
				SimpleDateFormat yyyymmdd = new SimpleDateFormat ("yyyyMMdd");

				if (valueDate == null || valueDate.isEmpty()) {
					valueDate = DEFAULT_DATE;
				}
				
				Date valDate = inputDateFormat.parse(valueDate);
				String valDateStr = outputDateFormat.format(valDate);
				ODateTime oValDate = ODateTime.strToDateTime(valDateStr);
				int valDateJd = OCalendar.convertYYYYMMDDToJd(yyyymmdd.format(valDate));

				if (glDate == null || glDate.isEmpty()) {
					glDate = DEFAULT_DATE;
				}
				
				Date gldDate = inputDateFormat.parse(glDate);
				String glDateStr = outputDateFormat.format(gldDate);
				ODateTime oGlDate = ODateTime.strToDateTime(glDateStr);
				int glDateJd = OCalendar.convertYYYYMMDDToJd(yyyymmdd.format(gldDate));
				
				/*** Derive Credit/Debit ***/
				String crDr = getCreditDebit (creditDebit);
				
				/*** Convert to TOz. ***/
				Double quantityToz = convertToTOz(qtyToz, "TOz");
				
				String key = "" + (endurDocument != 0 ? ""+endurDocument : "") + "-" 
							+ ((account == null || account.isEmpty()) ? "" : account.trim()) + "-"
							+ crDr + "-"
							+ ((amount == null || amount.isEmpty()) ? "" : String.format("%.2f", amt));	

				
				/*** Add to output table and pass it on to the return table ***/
				int newRow = output.addRow();
				output.setString(SAPReconOutputFieldsEnum.ACCOUNT_NUMBER.toString(), newRow, account);
				output.setDouble(SAPReconOutputFieldsEnum.AMOUNT.toString(), newRow, amt);
				output.setString(SAPReconOutputFieldsEnum.BATCH_NUM.toString(), newRow, " ");
				output.setString(SAPReconOutputFieldsEnum.CURRENCY.toString(), newRow, currency);
				output.setString(SAPReconOutputFieldsEnum.CREDITDEBIT.toString(), newRow, crDr);
				output.setInt(SAPReconOutputFieldsEnum.ENDUR_DOC_NUM.toString(), newRow, 0);
				output.setInt(SAPReconOutputFieldsEnum.GL_DATE.toString(), newRow, glDateJd);
				output.setDateTime(SAPReconOutputFieldsEnum.GL_DATE_PLUGIN.toString(), newRow, oGlDate);
				output.setInt(SAPReconOutputFieldsEnum.ID.toString(), newRow, newRow);
				output.setString(SAPReconOutputFieldsEnum.KEY.toString(), newRow, key);
				output.setString(SAPReconOutputFieldsEnum.NOTE.toString(), newRow, note);
				output.setDouble(SAPReconOutputFieldsEnum.QUANTITY.toString(), newRow, quantityToz);
				output.setInt(SAPReconOutputFieldsEnum.DEAL_NUM.toString(), newRow, endurDocument);
				output.setString(SAPReconOutputFieldsEnum.TYPE.toString(), newRow, " ");
				output.setInt(SAPReconOutputFieldsEnum.VALUEDATE.toString(), newRow, valDateJd);
				output.setDateTime(SAPReconOutputFieldsEnum.VALUE_DATE_PLUGIN.toString(), newRow, oValDate);  
				output.setDouble(SAPReconOutputFieldsEnum.TAX.toString(), newRow, tax);
			}
			
			/*** Update value dates as only SL documents have it and not GL documents ***/
			if (output.getNumRows() > 0) {
				valueDates.select(output, "DISTINCT, tran_num, value_date, value_date_plugin", "value_date GT 21914");
				
				if (valueDates.getNumRows() > 0) {
					output.select(valueDates, "value_date, value_date_plugin", "tran_num EQ $tran_num");
				}				
			}

		}
		catch (Exception e) {
			PluginLog.info("Unable to parse the response. Exception: " + e.getMessage());
		}
		finally {
			if (valueDates != null)
				valueDates.destroy();
		}
		
		return;
	}
	
	/*** 
	 * Get config from constant repository and add to properties
	 * 
	 * @throws OException
	 */
	private void getConfig () throws OException {
		
		/*** The fields like HOST NAME etc that have values defined in the database 
		 *   will be saved as key in property along their values 
		 *   ***/
		String username = constRepo.getStringValue(SAPReconFieldsEnum.BASIC_AUTH_USERNAME.toString());
		config.put(SAPReconFieldsEnum.BASIC_AUTH_USERNAME.toString(), username);
		
		String password = constRepo.getStringValue(SAPReconFieldsEnum.BASIC_AUTH_PASSWORD.toString());
		config.put(SAPReconFieldsEnum.BASIC_AUTH_PASSWORD.toString(), password);
		
		int connectionTimeOut = constRepo.getIntValue(SAPReconFieldsEnum.CONNECTION_TIMEOUT.toString());
		config.put(SAPReconFieldsEnum.CONNECTION_TIMEOUT.toString(), ""+connectionTimeOut);

		String hostAddress = constRepo.getStringValue(SAPReconFieldsEnum.HOST_ADDRESS.toString());
		config.put(SAPReconFieldsEnum.HOST_ADDRESS.toString(), hostAddress);
		
		String hostName = constRepo.getStringValue(SAPReconFieldsEnum.HOST_NAME.toString());
		config.put(SAPReconFieldsEnum.HOST_NAME.toString(), hostName);
		
		int hostPort = constRepo.getIntValue(SAPReconFieldsEnum.HOST_PORT.toString());
		config.put(SAPReconFieldsEnum.HOST_PORT.toString(), ""+hostPort);
		
		String serviceUrl = constRepo.getStringValue(SAPReconFieldsEnum.SERVICE_URL.toString());
		config.put(SAPReconFieldsEnum.SERVICE_URL.toString(), serviceUrl);
		
		String endpointUrl = constRepo.getStringValue(SAPReconFieldsEnum.ENDPOINT_URL.toString());
		config.put(SAPReconFieldsEnum.ENDPOINT_URL.toString(), endpointUrl);
		
		String queryUrl = constRepo.getStringValue(SAPReconFieldsEnum.QUERY_URL.toString());
		config.put(SAPReconFieldsEnum.QUERY_URL.toString(), queryUrl);
		
		String paramConsumerApp = constRepo.getStringValue(SAPReconFieldsEnum.PARAM_CONSUMER_APP.toString());
		config.put(SAPReconFieldsEnum.PARAM_CONSUMER_APP.toString(), paramConsumerApp);
		
		String paramTargetApp = constRepo.getStringValue(SAPReconFieldsEnum.PARAM_TARGET_APP.toString());
		config.put(SAPReconFieldsEnum.PARAM_TARGET_APP.toString(), paramTargetApp);
		
		int readTimeOut = constRepo.getIntValue(SAPReconFieldsEnum.READ_TIMEOUT.toString());
		config.put(SAPReconFieldsEnum.READ_TIMEOUT.toString(), ""+readTimeOut);
		
		/*** JSON tags are configured with name only in the database. Set their name to the key in the property.
		 *   Their values will be assigned later when response is read.
		 ***/
		String tagEndurDocId = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_ENDUR_DOCUMENT_ID.toString());
		config.put(tagEndurDocId, "TBF");
		
		String tagNote = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_NOTE.toString());
		config.put(tagNote, "TBF");
		
		String tagCreditDebit = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_CREDIT_DEBIT.toString());
		config.put(tagCreditDebit, "TBF");
		
		String tagGLDate = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_GLDATE.toString());
		config.put(tagGLDate, "TBF");
		
		String tagDocType = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_DOCUMENT_TYPE.toString());
		config.put(tagDocType, "TBF");
		
		String tagAccount = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_ACCOUNT.toString());
		config.put(tagAccount, "TBF");
		
		String tagValueDate = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_VALUE_DATE.toString());
		config.put(tagValueDate, "TBF");
		
		String tagQuantity = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_QUANTITY.toString());
		config.put(tagQuantity, "TBF");
		
		String tagAmount = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_AMOUNT.toString());
		config.put(tagAmount, "TBF");
		
		String tagCurrency = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_CURRENCY.toString());
		config.put(tagCurrency, "TBF");
		
		String tagSapDocId = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_SAP_DOCUMENT_ID.toString());
		config.put(tagSapDocId, "TBF");
		
		String tagSapDocDate = constRepo.getStringValue(SAPReconFieldsEnum.JSON_TAG_SAP_DOCUMENT_DATE.toString());
		config.put(tagSapDocDate, "TBF");				

		/*** Check test flag. Do not throw exception if flag is not present. ***/
		try {
			String testFlag = constRepo.getStringValue(SAPReconFieldsEnum.TEST_FLAG.toString());
			if (testFlag != null && !testFlag.isEmpty() && testFlag.equalsIgnoreCase("Yes")) {
				isTest = true;
				testFile = constRepo.getStringValue(SAPReconFieldsEnum.TEST_FILE.toString());
			}
		} catch (Exception e) {
			PluginLog.error("No test related configs present.");
		}

	}

	/****
	 * Prepare URL and connect to the external system.
	 * 
	 * @return The response received from the external system.
	 * @throws OException
	 */
	private String connect () throws OException {
		
		System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
		
		/*** Configure date in format Restful API understands i.e. from DD-MMM-YYYY to YYYY-MM-DD ***/
		SimpleDateFormat inputDateFormat = new SimpleDateFormat ("dd-MMM-yyyy");
		SimpleDateFormat outputDateFormat = new SimpleDateFormat ("yyyy-MM-dd");
		
		/*** Make sure the dates are correct ***/
		String fromDateStr = null;
		String toDateStr = null;
		try {
			fromDateStr = outputDateFormat.format(inputDateFormat.parse(windowStartDateStr)); 
			toDateStr = outputDateFormat.format(inputDateFormat.parse(windowEndDateStr)); 
		}
		catch (java.text.ParseException e) {
			throw new OException ("Unable to parse date. \nException: " + e.getMessage() + "\n" + e.getStackTrace());
		} 
		
		/*** Configure client ***/
        ClientConfig clientConfigconfig = configureClient();
        Client client = Client.create(clientConfigconfig);
        int connectionTimeout = Integer.parseInt(config.getProperty(SAPReconFieldsEnum.CONNECTION_TIMEOUT.toString()));
        client.setConnectTimeout(connectionTimeout);
        
        int readTimeOut  = Integer.parseInt(config.getProperty(SAPReconFieldsEnum.READ_TIMEOUT.toString()));
        client.setReadTimeout(readTimeOut);
        
        /*** Decrypt password ***/
        String password = config.getProperty(SAPReconFieldsEnum.BASIC_AUTH_PASSWORD.toString());
        String decryptedPassword = new CryptoImpl().decrypt (password);

        client.addFilter(new HTTPBasicAuthFilter(
        		config.getProperty(SAPReconFieldsEnum.BASIC_AUTH_USERNAME.toString()),
        		decryptedPassword));
        client.addFilter(new LoggingFilter(System.out));
        
        /*** Prepare URL ***/
        String url = config.getProperty(SAPReconFieldsEnum.HOST_NAME.toString())
				   + config.getProperty(SAPReconFieldsEnum.SERVICE_URL.toString())
				   + config.getProperty(SAPReconFieldsEnum.ENDPOINT_URL.toString())
				   + config.getProperty(SAPReconFieldsEnum.QUERY_URL.toString()).replaceAll("\\{fromDate\\}", fromDateStr).replaceAll("\\{toDate\\}", toDateStr);
		PluginLog.info("URL: " + url);
        WebResource webResource = client.resource(url);
        
        /*** Connect and get response ***/
		ClientResponse response = webResource.accept("application/json")
				.header("consumer-app", config.getProperty(SAPReconFieldsEnum.PARAM_CONSUMER_APP.toString()))
				.header("target-app", config.getProperty(SAPReconFieldsEnum.PARAM_TARGET_APP.toString()))
                .get(ClientResponse.class);
		
		String responseJSON = response.getEntity(String.class);
		int responseStatus = response.getStatus();
		PluginLog.info("Status returned: " + responseStatus);
		if (responseStatus >= 200 && responseStatus <= 299) {
			return responseJSON;			
		} if ((responseStatus == 400) && (responseJSON.contains("No documents found")) ) {
			throw new OException ("No data found for the date range in SAP");
		}
		else {
			/*** Error returned. Put it into log and return ***/
			PluginLog.info("Error returned: " + responseJSON);
			if (!isTest)
				throw new OException ("Error while connecting to SAP. Check status and error description.");
			else 
				return null;
		}
	}
	
	private ClientConfig configureClient() {
		ClientConfig config = new DefaultClientConfig(); // SSL configuration
        try {
            TrustManager[] trustManagers = null;
            HostnameVerifier hostnameVerifier = null;
            TrustManager trustAll = new X509TrustManager() {
                 @Override
                 public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                 @Override
                 public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                 @Override
                 public X509Certificate[] getAcceptedIssuers() { return null; }
           };
           SSLContext sslContext = SSLContext.getInstance("TLS");
           trustManagers = new TrustManager[]{ trustAll };
           hostnameVerifier = new HostnameVerifier() {
               @Override
                 public boolean verify(String hostname, SSLSession session) { return true; }
               };
           // SSL configuration
           sslContext.init(null, trustManagers, new SecureRandom());
           config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, sslContext));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
		return config;
	}
	
	/****
	 * Gets value of the tag from the json output received
	 * 
	 * @param jsonMessage JSONObject containing the tag
	 * @param tag The tag for which value is desired
	 * @return Value of the tag
	 */
	private String getValue (JSONObject jsonMessage, String tag) {
		String value = null;
		try {
			value =  jsonMessage.get(constRepo.getStringValue(tag)).toString();
			
		} catch (Exception e) {
			PluginLog.error("Unable to get tag: " + tag + ". \nException: " + e.getMessage());
		}
		return value;
	}
	
	/***
	 * Gets Credit or Debit from the message
	 * 
	 * @param creditDebit Credit or Debit indicator
	 * @return S if Debit and H if credit
	 */
	private String getCreditDebit (String creditDebit) {
		String crDr = "Credit";
		if (creditDebit.equalsIgnoreCase("S"))
			crDr = "Debit";
		
		return crDr;
	}
	
	/*** Get JSON string from a test file. File name with full path is configured in constant repository.
	 * 
	 * @return Contents of test file
	 */
	private String getTestJSON () {
		String json = null;
		
		PluginLog.info("Reading file: " + testFile);
		try(BufferedReader br = new BufferedReader(new FileReader(testFile))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    
		    json = sb.toString();
		} catch (IOException e) {
			throw new RuntimeException("Unable to read data from file: " + testFile);
		}
		
		return json;
	}
	
	/*** 
	 * Convert quantity to Troy Ounce
	 * 
	 * @param qty Quantity to convert
	 * @param uom Unit of Measure
	 * @return Quantity in TOz
	 */
	private Double convertToTOz (Double qty, String uom) {
		Double convertedQty = qty;
		if ("KG".equalsIgnoreCase(uom.trim())) {
			convertedQty = qty * KGS_TO_TOZ;
		} else if ("G".equalsIgnoreCase(uom.trim())) {
			convertedQty = qty * GMS_TO_TOZ;
		}
		
		return convertedQty;
	}
	
	private int getInteger (String value) {
		Integer intValue = 0;
		
		try {
			intValue = (value == null || value.isEmpty()) ? 0 : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			PluginLog.error("Unable to derive integer value from: " + value);
		}
		
		return intValue;
	}
	
	private double getDouble (String value) {
		Double dblValue = 0.00;
		
		try {
			dblValue = (value == null || value.isEmpty())? 0.00 : Double.parseDouble(value);
		} catch (NumberFormatException e) {
			PluginLog.error("Unable to derive double value from: " + value);
		}
		
		return dblValue;
	}
}
