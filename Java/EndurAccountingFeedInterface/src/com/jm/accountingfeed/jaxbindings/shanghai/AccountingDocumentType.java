//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.01.07 at 10:40:56 AM GMT 
//


package com.jm.accountingfeed.jaxbindings.shanghai;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 			Accounting document.
 * 			
 * 
 * <p>Java class for AccountingDocumentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AccountingDocumentType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Header" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}AccountingDocumentHeaderType"/>
 *         &lt;element name="Item" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}AccountingDocumentItemType" maxOccurs="999" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AccountingDocumentType", propOrder = {
    "header",
    "item"
})
@XmlRootElement
public class AccountingDocumentType {

    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "Header", required = true)
    protected AccountingDocumentHeaderType header;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "Item")
    protected List<AccountingDocumentItemType> item;

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link AccountingDocumentHeaderType }
     *     
     */
    public AccountingDocumentHeaderType getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccountingDocumentHeaderType }
     *     
     */
    public void setHeader(AccountingDocumentHeaderType value) {
        this.header = value;
    }

    /**
     * Gets the value of the item property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the item property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccountingDocumentItemType }
     * 
     * 
     */
    public List<AccountingDocumentItemType> getItem() {
        if (item == null) {
            item = new ArrayList<AccountingDocumentItemType>();
        }
        return this.item;
    }

}