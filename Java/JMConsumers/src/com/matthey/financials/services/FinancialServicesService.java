
package com.matthey.financials.services;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "FinancialServicesService", targetNamespace = "http://services.financials.matthey.com", wsdlLocation = "META-INF/wsdl/FinancialServicesService.wsdl")
public class FinancialServicesService
    extends Service
{

    private final static URL FINANCIALSERVICESSERVICE_WSDL_LOCATION;
    private final static WebServiceException FINANCIALSERVICESSERVICE_EXCEPTION;
    private final static QName FINANCIALSERVICESSERVICE_QNAME = new QName("http://services.financials.matthey.com", "FinancialServicesService");

    static {
        FINANCIALSERVICESSERVICE_WSDL_LOCATION = com.matthey.financials.services.FinancialServicesService.class.getClassLoader().getResource("META-INF/wsdl/FinancialServicesService.wsdl");
        WebServiceException e = null;
        if (FINANCIALSERVICESSERVICE_WSDL_LOCATION == null) {
            e = new WebServiceException("Cannot find 'META-INF/wsdl/FinancialServicesService.wsdl' wsdl. Place the resource correctly in the classpath.");
        }
        FINANCIALSERVICESSERVICE_EXCEPTION = e;
    }

    public FinancialServicesService() {
        super(__getWsdlLocation(), FINANCIALSERVICESSERVICE_QNAME);
    }

    public FinancialServicesService(WebServiceFeature... features) {
        super(__getWsdlLocation(), FINANCIALSERVICESSERVICE_QNAME, features);
    }

    public FinancialServicesService(URL wsdlLocation) {
        super(wsdlLocation, FINANCIALSERVICESSERVICE_QNAME);
    }

    public FinancialServicesService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, FINANCIALSERVICESSERVICE_QNAME, features);
    }

    public FinancialServicesService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public FinancialServicesService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns FinancialServices
     */
    @WebEndpoint(name = "FinancialServices")
    public FinancialServices getFinancialServices() {
        return super.getPort(new QName("http://services.financials.matthey.com", "FinancialServices"), FinancialServices.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns FinancialServices
     */
    @WebEndpoint(name = "FinancialServices")
    public FinancialServices getFinancialServices(WebServiceFeature... features) {
        return super.getPort(new QName("http://services.financials.matthey.com", "FinancialServices"), FinancialServices.class, features);
    }

    private static URL __getWsdlLocation() {
        if (FINANCIALSERVICESSERVICE_EXCEPTION!= null) {
            throw FINANCIALSERVICESSERVICE_EXCEPTION;
        }
        return FINANCIALSERVICESSERVICE_WSDL_LOCATION;
    }

}