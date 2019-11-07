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
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 			Enterprise message header.
 * 			
 * 
 * <p>Java class for EnterpriseMessageHeaderType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EnterpriseMessageHeaderType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MessageExchangeID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="CreationDateTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="AcknowledgementRequest" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="BusinessScope" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}MessageHeaderBusinessScopeType" minOccurs="0"/>
 *         &lt;element name="Sender" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}MessageHeaderSenderType"/>
 *         &lt;element name="Receiver" type="{http://johnsonmatthey.com/xmlns/enterpise_message/v01}MessageHeaderReceiverType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EnterpriseMessageHeaderType", propOrder = {
    "messageExchangeID",
    "creationDateTime",
    "acknowledgementRequest",
    "businessScope",
    "sender",
    "receiver"
})
public class EnterpriseMessageHeaderType {

    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "MessageExchangeID", required = true)
    protected String messageExchangeID;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "CreationDateTime", required = true)
    protected String creationDateTime;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "AcknowledgementRequest")
    protected String acknowledgementRequest;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "BusinessScope")
    protected MessageHeaderBusinessScopeType businessScope;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "Sender", required = true)
    protected MessageHeaderSenderType sender;
    @XmlElement(namespace="http://johnsonmatthey.com/xmlns/enterpise_message/v01",name = "Receiver")
    protected List<MessageHeaderReceiverType> receiver;

    /**
     * Gets the value of the messageExchangeID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessageExchangeID() {
        return messageExchangeID;
    }

    /**
     * Sets the value of the messageExchangeID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessageExchangeID(String value) {
        this.messageExchangeID = value;
    }

    /**
     * Gets the value of the creationDateTime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreationDateTime() {
        return creationDateTime;
    }

    /**
     * Sets the value of the creationDateTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreationDateTime(String value) {
        this.creationDateTime = value;
    }

    /**
     * Gets the value of the acknowledgementRequest property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAcknowledgementRequest() {
        return acknowledgementRequest;
    }

    /**
     * Sets the value of the acknowledgementRequest property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAcknowledgementRequest(String value) {
        this.acknowledgementRequest = value;
    }

    /**
     * Gets the value of the businessScope property.
     * 
     * @return
     *     possible object is
     *     {@link MessageHeaderBusinessScopeType }
     *     
     */
    public MessageHeaderBusinessScopeType getBusinessScope() {
        return businessScope;
    }

    /**
     * Sets the value of the businessScope property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageHeaderBusinessScopeType }
     *     
     */
    public void setBusinessScope(MessageHeaderBusinessScopeType value) {
        this.businessScope = value;
    }

    /**
     * Gets the value of the sender property.
     * 
     * @return
     *     possible object is
     *     {@link MessageHeaderSenderType }
     *     
     */
    public MessageHeaderSenderType getSender() {
        return sender;
    }

    /**
     * Sets the value of the sender property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageHeaderSenderType }
     *     
     */
    public void setSender(MessageHeaderSenderType value) {
        this.sender = value;
    }

    /**
     * Gets the value of the receiver property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the receiver property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReceiver().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MessageHeaderReceiverType }
     * 
     * 
     */
    public List<MessageHeaderReceiverType> getReceiver() {
        if (receiver == null) {
            receiver = new ArrayList<MessageHeaderReceiverType>();
        }
        return this.receiver;
    }

}
