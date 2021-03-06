//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.09.04 at 07:54:32 AM BST 
//


package com.jm.accountingfeed.jaxbbindings.metalledger;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="deskLocation" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tradeRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tradeStatus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="leg" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="portfolio" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ins_type" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="pmmAccount" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="intPmmAccount" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tradeType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tradeDate" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tradingLoc" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="fromCurrency" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="fromValue" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="unitOfMeasure" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="baseWeight" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="customerRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="site" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="location" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="form" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="purity" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="valueDate" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="returndate" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="pricingType" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "deskLocation",
    "tradeRef",
    "tradeStatus",
    "leg",
    "portfolio",
    "insType",
    "pmmAccount",
    "intPmmAccount",
    "tradeType",
    "tradeDate",
    "tradingLoc",
    "fromCurrency",
    "fromValue",
    "unitOfMeasure",
    "baseWeight",
    "customerRef",
    "site",
    "location",
    "form",
    "purity",
    "valueDate",
    "returndate",
    "pricingType"
})
@XmlRootElement(name = "trade")
public class Trade {

    @XmlElement(required = true)
    protected String deskLocation;
    @XmlElement(required = true)
    protected String tradeRef;
    @XmlElement(required = true)
    protected String tradeStatus;
    @XmlElement(required = true)
    protected String leg;
    @XmlElement(required = true)
    protected String portfolio;
    @XmlElement(name = "ins_type", required = true)
    protected String insType;
    @XmlElement(required = true)
    protected String pmmAccount;
    @XmlElement(required = true)
    protected String intPmmAccount;
    @XmlElement(required = true)
    protected String tradeType;
    @XmlElement(required = true)
    protected String tradeDate;
    @XmlElement(required = true)
    protected String tradingLoc;
    @XmlElement(required = true)
    protected String fromCurrency;
    @XmlElement(required = true)
    protected BigDecimal fromValue;
    @XmlElement(required = true)
    protected String unitOfMeasure;
    @XmlElement(required = true)
    protected BigDecimal baseWeight;
    @XmlElement(required = true)
    protected String customerRef;
    @XmlElement(required = true)
    protected String site;
    @XmlElement(required = true)
    protected String location;
    @XmlElement(required = true)
    protected String form;
    @XmlElement(required = true)
    protected String purity;
    @XmlElement(required = true)
    protected String valueDate;
    @XmlElement(required = true)
    protected String returndate;
    @XmlElement(required = true)
    protected String pricingType;
    
    /**
     * Gets the value of the deskLocation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeskLocation() {
        return deskLocation;
    }

    /**
     * Sets the value of the deskLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeskLocation(String value) {
        this.deskLocation = value;
    }

    /**
     * Gets the value of the tradeRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTradeRef() {
        return tradeRef;
    }

    /**
     * Sets the value of the tradeRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTradeRef(String value) {
        this.tradeRef = value;
    }

    /**
     * Gets the value of the tradeStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTradeStatus() {
        return tradeStatus;
    }

    /**
     * Sets the value of the tradeStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTradeStatus(String value) {
        this.tradeStatus = value;
    }

    /**
     * Gets the value of the leg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLeg() {
        return leg;
    }

    /**
     * Sets the value of the leg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLeg(String value) {
        this.leg = value;
    }

    /**
     * Gets the value of the portfolio property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPortfolio() {
        return portfolio;
    }

    /**
     * Sets the value of the portfolio property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPortfolio(String value) {
        this.portfolio = value;
    }

    /**
     * Gets the value of the insType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInsType() {
        return insType;
    }

    /**
     * Sets the value of the insType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInsType(String value) {
        this.insType = value;
    }

    /**
     * Gets the value of the pmmAccount property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPmmAccount() {
        return pmmAccount;
    }

    /**
     * Sets the value of the pmmAccount property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPmmAccount(String value) {
        this.pmmAccount = value;
    }

    /**
     * Gets the value of the intPmmAccount property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIntPmmAccount() {
        return intPmmAccount;
    }

    /**
     * Sets the value of the intPmmAccount property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIntPmmAccount(String value) {
        this.intPmmAccount = value;
    }

    /**
     * Gets the value of the tradeType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTradeType() {
        return tradeType;
    }

    /**
     * Sets the value of the tradeType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTradeType(String value) {
        this.tradeType = value;
    }

    /**
     * Gets the value of the tradeDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTradeDate() {
        return tradeDate;
    }

    /**
     * Sets the value of the tradeDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTradeDate(String value) {
        this.tradeDate = value;
    }

    /**
     * Gets the value of the tradingLoc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTradingLoc() {
        return tradingLoc;
    }

    /**
     * Sets the value of the tradingLoc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTradingLoc(String value) {
        this.tradingLoc = value;
    }

    /**
     * Gets the value of the fromCurrency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFromCurrency() {
        return fromCurrency;
    }

    /**
     * Sets the value of the fromCurrency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFromCurrency(String value) {
        this.fromCurrency = value;
    }

    /**
     * Gets the value of the fromValue property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getFromValue() {
        return fromValue;
    }

    /**
     * Sets the value of the fromValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setFromValue(BigDecimal value) {
        this.fromValue = value;
    }

    /**
     * Gets the value of the unitOfMeasure property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    /**
     * Sets the value of the unitOfMeasure property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnitOfMeasure(String value) {
        this.unitOfMeasure = value;
    }

    /**
     * Gets the value of the baseWeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getBaseWeight() {
        return baseWeight;
    }

    /**
     * Sets the value of the baseWeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setBaseWeight(BigDecimal value) {
        this.baseWeight = value;
    }

    /**
     * Gets the value of the customerRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustomerRef() {
        return customerRef;
    }

    /**
     * Sets the value of the customerRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustomerRef(String value) {
        this.customerRef = value;
    }

    /**
     * Gets the value of the site property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSite() {
        return site;
    }

    /**
     * Sets the value of the site property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSite(String value) {
        this.site = value;
    }

    /**
     * Gets the value of the location property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the value of the location property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocation(String value) {
        this.location = value;
    }

    /**
     * Gets the value of the form property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForm() {
        return form;
    }

    /**
     * Sets the value of the form property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForm(String value) {
        this.form = value;
    }

    /**
     * Gets the value of the purity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPurity() {
        return purity;
    }

    /**
     * Sets the value of the purity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPurity(String value) {
        this.purity = value;
    }

    /**
     * Gets the value of the valueDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValueDate() {
        return valueDate;
    }

    /**
     * Sets the value of the valueDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValueDate(String value) {
        this.valueDate = value;
    }

    /**
     * Gets the value of the returndate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReturndate() {
        return returndate;
    }

    /**
     * Sets the value of the returndate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReturndate(String value) {
        this.returndate = value;
    }
    
    /**
     * Gets the value of the pricingType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPricingType() {
        return pricingType;
    }

    /**
     * Sets the value of the pricingType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPricingType(String value) {
        this.pricingType = value;
    }
}
