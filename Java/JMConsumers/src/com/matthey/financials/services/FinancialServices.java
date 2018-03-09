
package com.matthey.financials.services;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import com.matthey.financials.beans.OpenFinancialItemResult;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebService(name = "FinancialServices", targetNamespace = "http://services.financials.matthey.com")
@XmlSeeAlso({
    com.matthey.financials.beans.ObjectFactory.class,
    com.matthey.financials.services.ObjectFactory.class
})
public interface FinancialServices {


    /**
     * 
     * @param tradingLocation
     * @param account
     * @return
     *     returns com.matthey.financials.beans.OpenFinancialItemResult
     */
    @WebMethod
    @WebResult(name = "getOpenItemsReturn", targetNamespace = "http://services.financials.matthey.com")
    @RequestWrapper(localName = "getOpenItems", targetNamespace = "http://services.financials.matthey.com", className = "com.matthey.financials.services.GetOpenItems")
    @ResponseWrapper(localName = "getOpenItemsResponse", targetNamespace = "http://services.financials.matthey.com", className = "com.matthey.financials.services.GetOpenItemsResponse")
    public OpenFinancialItemResult getOpenItems(
        @WebParam(name = "tradingLocation", targetNamespace = "http://services.financials.matthey.com")
        String tradingLocation,
        @WebParam(name = "account", targetNamespace = "http://services.financials.matthey.com")
        String account);

}