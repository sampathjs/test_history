package com.matthey.webservice.consumer;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.matthey.financials.beans.OpenFinancialItem;
import com.matthey.financials.beans.OpenFinancialItemResult;
import com.matthey.financials.services.ArrayOfTns1OpenFinancialItem;
import com.openlink.util.logging.PluginLog;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/*
 * History:
 * 2019-02-05		V1.0		jwaechter		- Initial Version
 */

/**
 * This class calls the Shanghai specific REST service to retrieve credit limits.
 * It uses several configurable values to build the URL and the parameters taken
 * from the ConstantsRepository. The constants repository values used
 * can be reviewed in class {@link FinancialService}.
 * 
 * @author jwaechter
 * @version 1.0
 */

public class ShanghaiFinancialService {
	public static OpenFinancialItemResult getOpenItems(final Properties properties,
			final String accountNo) {
		// see https://stackoverflow.com/questions/6353849/received-fatal-alert-handshake-failure-through-sslhandshakeexception for details
		System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
		
        ClientConfig config = configureClient();
        Client client = Client.create(config);
        client.setConnectTimeout(Integer.parseInt(properties.getProperty(FinancialService.SHANGHAI_CONNECTION_TIMEOUT)));
        client.setReadTimeout(Integer.parseInt(properties.getProperty(FinancialService.SHANGHAI_READ_TIMEOUT)));
        client.addFilter(new HTTPBasicAuthFilter(
        		properties.getProperty(FinancialService.SHANGHAI_BASIC_AUTH_USERNAME),
        		properties.getProperty(FinancialService.SHANGHAI_BASIC_AUTH_PASSWORD)));
        client.addFilter(new LoggingFilter(System.out));
		WebResource webResource = client
		   .resource(properties.getProperty(FinancialService.SHANGHAI_HOST_NAME)
				   + properties.getProperty(FinancialService.SHANGHAI_SERVICE_URL)
				   + properties.getProperty(FinancialService.SHANGHAI_ENDPOINT_URL).replaceAll("\\{customer_id\\}", accountNo)
				   + properties.getProperty(FinancialService.SHANGHAI_QUERY_URL)
				   		.replaceAll("\\{currency_code\\}", properties.getProperty(FinancialService.SHANGHAI_QUERY_PARAM_CCY_CODE))
				   		.replaceAll("\\{controlling_area\\}", properties.getProperty(FinancialService.SHANGHAI_QUERY_PARAM_CONTROLLING_AREA))
				   				);
		
		ClientResponse response = webResource.accept("application/json")
					.header("consumer-app", properties.getProperty(FinancialService.SHANGHAI_PARAM_CONSUMER_APP))
					.header("target-app", properties.getProperty(FinancialService.SHANGHAI_PARAM_TARGET_APP))
	                .get(ClientResponse.class);

		String output = response.getEntity(String.class);
		PluginLog.info(output);
		if (response.getStatus() >= 200 && response.getStatus() <= 299) {
			return processOkResponse(properties, response, output);			
		} else {
			return processErrorResponse (response, output);
		}
	}

	private static OpenFinancialItemResult processErrorResponse(ClientResponse response, String output) {
		OpenFinancialItemResult result = new OpenFinancialItemResult();
		result.setErrorCode("" + response.getStatus());
		result.setErrorDescription(output);
		return result;
	}

	private static OpenFinancialItemResult processOkResponse(
			final Properties properties, ClientResponse response, String output) {
		String unpaidCustomerAmt = getJSONDoubleValueForUniqueTag(output, properties.getProperty(FinancialService.SHANGHAI_JSON_TAG_UNPAID_CUSTOMER_INVOICES));
		PluginLog.info("Unpaid Customer Amt: " + unpaidCustomerAmt);
		OpenFinancialItem unpaidCustomerAmtItem = new OpenFinancialItem();
		unpaidCustomerAmtItem.setCurrency(properties.getProperty(FinancialService.SHANGHAI_QUERY_PARAM_CCY_CODE));
		unpaidCustomerAmtItem.setTotal(unpaidCustomerAmt);

		String unpaidVendorAmt = getJSONDoubleValueForUniqueTag(output, properties.getProperty(FinancialService.SHANGHAI_JSON_TAG_UNPAID_VENDOR_INVOICES));
		PluginLog.info("Unpaid Vendor Amt: " + unpaidVendorAmt);
		OpenFinancialItem unpaidVendorAmtItem = new OpenFinancialItem();
		unpaidVendorAmtItem.setCurrency(properties.getProperty(FinancialService.SHANGHAI_QUERY_PARAM_CCY_CODE));
		unpaidVendorAmtItem.setTotal(unpaidVendorAmt);

		OpenFinancialItemResult result = new OpenFinancialItemResult();
		result.setErrorCode("" + response.getStatus());
		result.setErrorDescription("");
		ArrayOfTns1OpenFinancialItem items = new ArrayOfTns1OpenFinancialItem();
		items.getItem().add(unpaidCustomerAmtItem);
		items.getItem().add(unpaidVendorAmtItem);
		result.setItems(items);
		return result;
	}
	
	private static String getJSONDoubleValueForUniqueTag(String output, String jsonTag) {
		int indexUniqueJsonTag=output.indexOf(jsonTag);
		int indexColon=output.indexOf(":", indexUniqueJsonTag);
		StringBuilder doubleValueAsString = new StringBuilder();
		boolean lookingForFirstNumber = true;
		for (int i = indexColon+1; i < output.length(); i++) {
			char ch = output.charAt(i);
			if (Character.isDigit(ch) || ch == '.' || ch == '-') {
				doubleValueAsString.append(ch);
				lookingForFirstNumber = false;
			} else {
				if (!lookingForFirstNumber) {
					break;
				}
			}
		}
		System.out.println (doubleValueAsString.toString());
		return doubleValueAsString.toString();
	}

	private static ClientConfig configureClient() {
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

}
