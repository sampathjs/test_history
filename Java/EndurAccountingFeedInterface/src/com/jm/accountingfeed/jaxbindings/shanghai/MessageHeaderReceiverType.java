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
 * 			Enterprise message receiver details.
 * 			
 * 
 * <p>Java class for MessageHeaderReceiverType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MessageHeaderReceiverType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LogicalID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ComponentID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MessageHeaderReceiverType", propOrder = {
    "logicalID",
    "componentID"
})
public class MessageHeaderReceiverType {

    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "LogicalID", required = true)
    protected String logicalID;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "ComponentID")
    protected String componentID;

    /**
     * Gets the value of the logicalID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLogicalID() {
        return logicalID;
    }

    /**
     * Sets the value of the logicalID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLogicalID(String value) {
        this.logicalID = value;
    }

    /**
     * Gets the value of the componentID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComponentID() {
        return componentID;
    }

    /**
     * Sets the value of the componentID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComponentID(String value) {
        this.componentID = value;
    }

}
