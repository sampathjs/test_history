//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.01.07 at 10:40:56 AM GMT 
//


package com.jm.accountingfeed.jaxbindings.shanghai;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 			Accounting document tax details.
 * 			
 * 
 * <p>Java class for TaxDetailsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TaxDetailsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TaxCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DocumentCurrencyBaseAmount" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}AccountingDocumentAmountType" minOccurs="0"/>
 *         &lt;element name="LocalCurrencyBaseAmount" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}AccountingDocumentAmountType" minOccurs="0"/>
 *         &lt;element name="DocumentCurrencyTaxAmount" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}AccountingDocumentAmountType" minOccurs="0"/>
 *         &lt;element name="LocalCurrencyTaxAmount" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}AccountingDocumentAmountType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TaxDetailsType", propOrder = {
    "taxCode",
    "documentCurrencyBaseAmount",
    "localCurrencyBaseAmount",
    "documentCurrencyTaxAmount",
    "localCurrencyTaxAmount"
})
public class TaxDetailsType {

    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "TaxCode", required = true)
    protected String taxCode;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "DocumentCurrencyBaseAmount")
    protected AccountingDocumentAmountType documentCurrencyBaseAmount;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "LocalCurrencyBaseAmount")
    protected AccountingDocumentAmountType localCurrencyBaseAmount;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "DocumentCurrencyTaxAmount")
    protected AccountingDocumentAmountType documentCurrencyTaxAmount;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "LocalCurrencyTaxAmount")
    protected AccountingDocumentAmountType localCurrencyTaxAmount;

    /**
     * Gets the value of the taxCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaxCode() {
        return taxCode;
    }

    /**
     * Sets the value of the taxCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaxCode(String value) {
        this.taxCode = value;
    }

    /**
     * Gets the value of the documentCurrencyBaseAmount property.
     * 
     * @return
     *     possible object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public AccountingDocumentAmountType getDocumentCurrencyBaseAmount() {
        return documentCurrencyBaseAmount;
    }

    /**
     * Sets the value of the documentCurrencyBaseAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public void setDocumentCurrencyBaseAmount(AccountingDocumentAmountType value) {
        this.documentCurrencyBaseAmount = value;
    }

    /**
     * Gets the value of the localCurrencyBaseAmount property.
     * 
     * @return
     *     possible object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public AccountingDocumentAmountType getLocalCurrencyBaseAmount() {
        return localCurrencyBaseAmount;
    }

    /**
     * Sets the value of the localCurrencyBaseAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public void setLocalCurrencyBaseAmount(AccountingDocumentAmountType value) {
        this.localCurrencyBaseAmount = value;
    }

    /**
     * Gets the value of the documentCurrencyTaxAmount property.
     * 
     * @return
     *     possible object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public AccountingDocumentAmountType getDocumentCurrencyTaxAmount() {
        return documentCurrencyTaxAmount;
    }

    /**
     * Sets the value of the documentCurrencyTaxAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public void setDocumentCurrencyTaxAmount(AccountingDocumentAmountType value) {
        this.documentCurrencyTaxAmount = value;
    }

    /**
     * Gets the value of the localCurrencyTaxAmount property.
     * 
     * @return
     *     possible object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public AccountingDocumentAmountType getLocalCurrencyTaxAmount() {
        return localCurrencyTaxAmount;
    }

    /**
     * Sets the value of the localCurrencyTaxAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccountingDocumentAmountType }
     *     
     */
    public void setLocalCurrencyTaxAmount(AccountingDocumentAmountType value) {
        this.localCurrencyTaxAmount = value;
    }

}
