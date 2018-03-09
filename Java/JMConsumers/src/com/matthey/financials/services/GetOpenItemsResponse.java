
package com.matthey.financials.services;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.matthey.financials.beans.OpenFinancialItemResult;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="getOpenItemsReturn" type="{http://beans.financials.matthey.com}OpenFinancialItemResult"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getOpenItemsReturn"
})
@XmlRootElement(name = "getOpenItemsResponse")
public class GetOpenItemsResponse {

    @XmlElement(required = true)
    protected OpenFinancialItemResult getOpenItemsReturn;

    /**
     * Gets the value of the getOpenItemsReturn property.
     * 
     * @return
     *     possible object is
     *     {@link OpenFinancialItemResult }
     *     
     */
    public OpenFinancialItemResult getGetOpenItemsReturn() {
        return getOpenItemsReturn;
    }

    /**
     * Sets the value of the getOpenItemsReturn property.
     * 
     * @param value
     *     allowed object is
     *     {@link OpenFinancialItemResult }
     *     
     */
    public void setGetOpenItemsReturn(OpenFinancialItemResult value) {
        this.getOpenItemsReturn = value;
    }

}
