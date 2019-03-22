package com.matthey.webservice.consumer;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import com.matthey.financials.beans.OpenFinancialItem;
import com.matthey.financials.beans.OpenFinancialItemResult;
import com.matthey.financials.services.FinancialServices;
import com.matthey.financials.services.FinancialServicesService;
import com.matthey.openlink.utilities.Repository;
import com.olf.jm.credit.LogMessageHandler;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;
import com.sun.xml.ws.client.BindingProviderProperties;

/*
 * History:
 * 201Y-MM-DD	V1.0	<unknown> 	- Initial Version
 * 2017-03-15	V1.1	jwaechter  	- defect fix in error handling
 *                                  - refactored import
 *                                  - added history
 * 2019-02-05	V1.2	jwaechter	- Added call to the Shanghai REST service but integrating 
 * 									  everything into the existing classes.
 * */

/**
 * 
 *	@version $Revision: $
 * <p>interface to JM Financial Service webservice
 * <br>
 * <p>
 *  The {@code ConstRepository} can be used to redefine variables names and values to suite workflow requirements.
 *  
 *  <p><table border=0 style="width:15%;">
 *  <caption style="background-color:#0070d0;color:white"><b>ConstRepository</caption>
 *	<th><b>context</b></th>
 *	<th><b>subcontext</b></th>
 *	</tr><tbody>
 *	<tr>
 *	<td align="center">{@value #CONST_REPO_CONTEXT}</td>
 *	<td align="center">{@value #CONST_REPO_SUBCONTEXT}</td>
 *  </tbody></table></p>
 *	<p>
 *	<table border=2 bordercolor=black>
 *	<tbody>
 *	<tr>
 *	<th><b>Variable</b></th>
 *	<th><b>Default</b></th>
 *	<th><b>Description</b></th>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #HOST_NAME}</b></font></td>
 *	<td>{@code 10.16.1.246}</td>
 *	<td>The name or IP of the machine which hosts this service....
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #HOST_PORT}</b></td>
 *	<td>{@code 8080}</td>
 *	<td>The port to target on the given host</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #TIMEOUT}<font color="red"><sup>*</sup></font></b></td>
 *	<td>2½</td>
 *	<td>The time in seconds to wait before expiring the request
 *	<p style="font-size:xx-small">
 *	*Only change if there is some technical issues with the connectivity.
 *	</tr>
 *  <tr>
 *	<td><b>{@value #SIMULATOR_ACTIVE}<font color="orange"><sup>*</sup></font></b></td>
 *	<td>N/A</td>
 *	<td>If populated with "1" or "true" the simulated results will be returned instead of calling the micro service
 *	</tr>
 *	</p>
 *  </td>
 *	</tr>
 *	</tbody>
 *	</table>
 * </p>
 * <br>
 *  @see "JM Finannical Services"
 *  @see "FS ???  - Credit Risk"
 *  @see "FS D422 - Dispatch Workflow"
 */
public class FinancialService {
	
	public static final String BASE_VALUE = "base_amount";
	public static final String SPOT_RATE = "fx_spot_rate";
	public static final String VALUE = "amount";
	public static final String CURRENCY = "currency";
	
	private static final String TIMEOUT = "ws_timeout";
	private static final String HOST_PORT = "host_port";
	private static final String HOST_NAME = "host_name";
	public static final String SHANGHAI_HOST_NAME = "shanghai_host_name";
	public static final String SHANGHAI_SERVICE_URL = "shanghai_service_url";
	public static final String SHANGHAI_ENDPOINT_URL = "shanghai_endpoint_url";
	public static final String SHANGHAI_QUERY_URL = "shanghai_query_url";
	public static final String SHANGHAI_QUERY_PARAM_CCY_CODE = "shanghai_query_param_ccy_code";
	public static final String SHANGHAI_QUERY_PARAM_CONTROLLING_AREA = "shanghai_query_param_controlling_area";
	public static final String SHANGHAI_PARAM_CONSUMER_APP = "shanghai_param_consumer_app";
	public static final String SHANGHAI_PARAM_TARGET_APP = "shanghai_param_target_app";
	public static final String SHANGHAI_BASIC_AUTH_USERNAME = "shanghai_basic_auth_username";
	public static final String SHANGHAI_BASIC_AUTH_PASSWORD = "shanghai_basic_auth_password";
	public static final String SHANGHAI_JSON_TAG_UNPAID_CUSTOMER_INVOICES = "shanghai_json_tag_customer_invoices";
	public static final String SHANGHAI_JSON_TAG_UNPAID_VENDOR_INVOICES = "shanghai_json_tag_vendor_invoices";
	public static final String SHANGHAI_CONNECTION_TIMEOUT = "shanghai_connection_timeout";
	public static final String SHANGHAI_READ_TIMEOUT = "shanghai_read_timeout";
	private static final String SIMULATOR_ACTIVE = "SimulatorActive";
	private static final String CONST_REPO_SUBCONTEXT = "MTM_AR";
	private static final String CONST_REPO_CONTEXT = "Credit_Risk";
	private static Properties properties;
	
	static {
		Map<String,String> config = new HashMap<String,String>();
		config.put(HOST_NAME, "10.16.1.246");
		config.put(HOST_PORT, "8080");
		config.put(TIMEOUT, "2.5");
		config.put(SIMULATOR_ACTIVE, "False");
		config.put(SHANGHAI_HOST_NAME, "https://vdx1wd.johnsonmatthey.com");
		config.put(SHANGHAI_SERVICE_URL, "/RESTAdapter/api/financials/v1");
		config.put(SHANGHAI_ENDPOINT_URL, "/customers/{customer_id}/credit-check-report");
		config.put(SHANGHAI_QUERY_URL, "?filter[credit-check-report.currency]={currency_code}&filter[credit-check-report.credit-controlling-area]={controlling_area}");
		config.put(SHANGHAI_QUERY_PARAM_CCY_CODE, "RMB");
		config.put(SHANGHAI_QUERY_PARAM_CONTROLLING_AREA, "CNR1");
		config.put(SHANGHAI_PARAM_CONSUMER_APP, "endur");
		config.put(SHANGHAI_PARAM_TARGET_APP, "sap-ecc-enr-pmpd");
		config.put(SHANGHAI_BASIC_AUTH_USERNAME, "ENDUR_PMPD_RTR0012");
		config.put(SHANGHAI_BASIC_AUTH_PASSWORD, "Aed#7umU");
		config.put(SHANGHAI_JSON_TAG_UNPAID_CUSTOMER_INVOICES, "unpaidCustomerInvoicesTotalAmount");
		config.put(SHANGHAI_JSON_TAG_UNPAID_VENDOR_INVOICES, "unpaidVendorInvoicesTotalAmount");
		config.put(SHANGHAI_CONNECTION_TIMEOUT, "10000");
		config.put(SHANGHAI_READ_TIMEOUT, "10000");
		properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, config);
	}
	
	/**
	 * Call the micro service {@link FinancialService} to get the currency details for a specified location
	 * 
	 * @param location
	 * @param accountNo	if this is empty, just return an empty table without calling the service
	 * @return a table of currencies, <i>which could be empty!</i>
	 */
	public static Table getOpenItems(final Session session,
			final String location, final String accountNo) {

		long tStart = System.currentTimeMillis();
		String hostName = properties.getProperty(HOST_NAME);
		String hostPort = properties.getProperty(HOST_PORT);
		int timeOut = getTimeout();

		Table openItems = getEmptyResultsTable(session);
		
		try {
			// Check if accountNo is valid
			if (accountNo == null || accountNo.isEmpty()) {
				return openItems;
			}

			if ("1".equals(properties.getProperty(SIMULATOR_ACTIVE))
					|| "true".equalsIgnoreCase(properties.getProperty(SIMULATOR_ACTIVE))
					|| "yes".equalsIgnoreCase(properties.getProperty(SIMULATOR_ACTIVE))) {
				return getOpenItemsSimulator(session, openItems, location, accountNo);				
			} 
			
			OpenFinancialItemResult ofi = null;
			if (!location.equals("Shanghai")) {
				String wsdlAddress = String.format("%s%s:%s%s%s", 
						"http://",
						hostName, 
						hostPort, 
						"/JMFinancialServices/services/",
						"FinancialServices?wsdl");

				Logger.log(LogLevel.DEBUG, LogCategory.Trading,
						FinancialService.class, String.format(
								"Calling %s with timeout %s",
								wsdlAddress, properties.getProperty(TIMEOUT)));
				URL url = new URL(wsdlAddress);
				FinancialServicesService service = new FinancialServicesService(url);
				FinancialServices port = service.getFinancialServices();

				BindingProvider bindingProvider = (BindingProvider) port;
				bindingProvider.getRequestContext().put(
						BindingProviderProperties.REQUEST_TIMEOUT,
						timeOut);
				bindingProvider.getRequestContext().put(
						BindingProviderProperties.CONNECT_TIMEOUT,
						timeOut);

				//			if (session.getDebug().getDebugLevel().getValue() >= EnumDebugLevel.Low
//						.getValue()) {
					Binding binding = bindingProvider.getBinding();
					List<Handler> handlerChain = binding.getHandlerChain();
					handlerChain.add(new LogMessageHandler());
					binding.setHandlerChain(handlerChain);
//				}

				// Making the call
				ofi = port.getOpenItems(location, accountNo);				
			} else { // if we are processing shanghai location
				ofi = ShanghaiFinancialService.getOpenItems(properties, accountNo);
			}

			if (ofi == null) {
				Logger.log(LogLevel.ERROR,  LogCategory.Trading, FinancialService.class, 
						"Port (FinancialServices) returned null pointer. Skipping processing.");
			} else  if (ofi.getItems() == null) {
				String message = "Port (FinancialServices) returned null pointer instead of items. Skipping processing.";
				message += "\nError code: " + ofi.getErrorCode() + "\nError description: " + ofi.getErrorDescription();
				Logger.log(LogLevel.ERROR,  LogCategory.Trading, FinancialService.class, 
						message);
			} else {
				for (OpenFinancialItem item : ofi.getItems().getItem()) {
					int row = openItems.addRows(1);
					openItems.setString(CURRENCY, row, item.getCurrency());
					String total = item.getTotal();
//					if (total.contains("-")) {
//						total = "-" + total.split("-")[0];
//					}
					openItems.setDouble(VALUE, row, Double.parseDouble(total));
				}				
			}			
		} catch (RuntimeException e) {

			Logger.log(
					LogLevel.ERROR,
					LogCategory.Trading,
					FinancialService.class,
					String.format(
							"Failed calling %s for Location: %s, account: %s"
									+ "\nHost:%s, Port:%s, Timeout:%s",
							FinancialService.class.getName(), location,
							accountNo, properties.getProperty(HOST_NAME),
							properties.getProperty(HOST_PORT), properties.getProperty(TIMEOUT)),
					e);
			throw e;
			
		} catch(Exception e) {
			Logger.log(
					LogLevel.FATAL,
					LogCategory.Trading,
					FinancialService.class,
					String.format(
							"Failed calling %s for Location: %s, account: %s"
									+ "\nHost:%s, Port:%s, Timeout:%s",
							FinancialService.class.getName(), location,
							accountNo, properties.getProperty(HOST_NAME),
							properties.getProperty(HOST_PORT), properties.getProperty(TIMEOUT)),
					e);
			
		}	finally {
		
			Logger.log(
					LogLevel.INFO,
					LogCategory.Trading,
					FinancialService.class,
					String.format(
							"%s call for Location: %s , account: %s took %d ms\n",
							FinancialService.class.getName(), location,
							accountNo, System.currentTimeMillis() - tStart));

		}
		
		return openItems;

	}

/**
 * Return empty initialised instance of the results table to support result aggregation
 * @param session
 * @return
 */
	private static Table getEmptyResultsTable(final Session session) {
		Table openItems = session.getTableFactory().createTable("OpenItems");
		openItems.addColumns("String["+CURRENCY+"], Double["+VALUE+"], Double["+SPOT_RATE+"], Double["+BASE_VALUE+"]");
		return openItems;
	}

/**
 * recover the effective Timeout configured for the webservice
 */
	private static int getTimeout() {
		// set timeout
		int timeOut = -1;
		try {

			
			timeOut =  (int) (Double.parseDouble(properties.getProperty(TIMEOUT)) * TimeUnit.SECONDS.toMillis(1));
			
		} catch (Exception e) {
			Logger.log(
					LogLevel.ERROR,
					LogCategory.Trading,
					FinancialService.class,
					String.format("Timeout period invalid(%s)",
							properties.getProperty(TIMEOUT)), e);
			throw e;
		}
		return timeOut;
	}
	
	
/**
 * Simulate results of calling the webservice	
 * @return a table with fixed currencies and random values
 */
	private static Table getOpenItemsSimulator(final Session session,
			final Table table, final String location, final String accountNo) {

		String[] currencies = {"CHF", "GBP", "USD", "EUR"};
		Random random = new Random();
		
		table.addRows(random.nextInt(3) + 1);
		for (int row = 0 ; row < table.getRowCount(); row++){
			table.setString(CURRENCY, row, currencies[random.nextInt(currencies.length)]);
			table.setDouble(VALUE, row, random.nextDouble() * 100);
		}
		Logger.log(LogLevel.INFO, LogCategory.Trading, FinancialService.class, "providing SIMULATED results!");
		return table;
	}

}
