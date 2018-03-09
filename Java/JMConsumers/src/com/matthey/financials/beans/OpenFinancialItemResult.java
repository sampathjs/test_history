
package com.matthey.financials.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.matthey.financials.services.ArrayOfTns1OpenFinancialItem;


/**
 * <p>Java class for OpenFinancialItemResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OpenFinancialItemResult">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="errorCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="errorDescription" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="items" type="{http://services.financials.matthey.com}ArrayOf_tns1_OpenFinancialItem"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpenFinancialItemResult", propOrder = {
    "errorCode",
    "errorDescription",
    "items"
})
public class OpenFinancialItemResult {

    @XmlElement(required = true, nillable = true)
    protected String errorCode;
    @XmlElement(required = true, nillable = true)
    protected String errorDescription;
    @XmlElement(required = true, nillable = true)
    protected ArrayOfTns1OpenFinancialItem items;

    /**
     * Gets the value of the errorCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the value of the errorCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorCode(String value) {
        this.errorCode = value;
    }

    /**
     * Gets the value of the errorDescription property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Sets the value of the errorDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorDescription(String value) {
        this.errorDescription = value;
    }

    /**
     * Gets the value of the items property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfTns1OpenFinancialItem }
     *     
     */
    public ArrayOfTns1OpenFinancialItem getItems() {
        return items;
    }

    /**
     * Sets the value of the items property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfTns1OpenFinancialItem }
     *     
     */
    public void setItems(ArrayOfTns1OpenFinancialItem value) {
        this.items = value;
    }

}
