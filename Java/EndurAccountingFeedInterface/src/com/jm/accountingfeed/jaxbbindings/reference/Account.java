//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.06.27 at 10:26:31 AM BST 
//


package com.jm.accountingfeed.jaxbbindings.reference;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


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
 *         &lt;element name="party_id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="pmmAccount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="metAcName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="businessUnit" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="country" type="{http://www.w3.org/2001/XMLSchema}NCName"/>
 *         &lt;element name="region" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="lglEntity" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="group" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="jmAccNo" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="lppmMbr" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="lglEntityDesc" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="lbmaMbr" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="busUnitDesc" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "partyId",
    "pmmAccount",
    "metAcName",
    "businessUnit",
    "country",
    "region",
    "lglEntity",
    "group",
    "jmAccNo",
    "lppmMbr",
    "lglEntityDesc",
    "lbmaMbr",
    "busUnitDesc"
})
@XmlRootElement(name = "account")
public class Account {

    @XmlElement(required = true)
    protected String deskLocation;
    @XmlElement(name = "party_id", required = true)
    protected String partyId;
    protected int pmmAccount;
    @XmlElement(required = true)
    protected String metAcName;
    @XmlElement(required = true)
    protected String businessUnit;
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String country;
    @XmlElement(required = true)
    protected String region;
    @XmlElement(required = true)
    protected String lglEntity;
    @XmlElement(required = true)
    protected String group;
    @XmlElement(required = true)
    protected String jmAccNo;
    @XmlElement(required = true)
    protected String lppmMbr;
    @XmlElement(required = true)
    protected String lglEntityDesc;
    @XmlElement(required = true)
    protected String lbmaMbr;
    @XmlElement(required = true)
    protected String busUnitDesc;

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
     * Gets the value of the partyId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPartyId() {
        return partyId;
    }

    /**
     * Sets the value of the partyId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPartyId(String value) {
        this.partyId = value;
    }

    /**
     * Gets the value of the pmmAccount property.
     * 
     */
    public int getPmmAccount() {
        return pmmAccount;
    }

    /**
     * Sets the value of the pmmAccount property.
     * 
     */
    public void setPmmAccount(int value) {
        this.pmmAccount = value;
    }

    /**
     * Gets the value of the metAcName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMetAcName() {
        return metAcName;
    }

    /**
     * Sets the value of the metAcName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMetAcName(String value) {
        this.metAcName = value;
    }

    /**
     * Gets the value of the businessUnit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBusinessUnit() {
        return businessUnit;
    }

    /**
     * Sets the value of the businessUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBusinessUnit(String value) {
        this.businessUnit = value;
    }

    /**
     * Gets the value of the country property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the value of the country property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountry(String value) {
        this.country = value;
    }

    /**
     * Gets the value of the region property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRegion() {
        return region;
    }

    /**
     * Sets the value of the region property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRegion(String value) {
        this.region = value;
    }

    /**
     * Gets the value of the lglEntity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLglEntity() {
        return lglEntity;
    }

    /**
     * Sets the value of the lglEntity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLglEntity(String value) {
        this.lglEntity = value;
    }

    /**
     * Gets the value of the group property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the value of the group property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroup(String value) {
        this.group = value;
    }

    /**
     * Gets the value of the jmAccNo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJmAccNo() {
        return jmAccNo;
    }

    /**
     * Sets the value of the jmAccNo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJmAccNo(String value) {
        this.jmAccNo = value;
    }

    /**
     * Gets the value of the lppmMbr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLppmMbr() {
        return lppmMbr;
    }

    /**
     * Sets the value of the lppmMbr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLppmMbr(String value) {
        this.lppmMbr = value;
    }

    /**
     * Gets the value of the lglEntityDesc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLglEntityDesc() {
        return lglEntityDesc;
    }

    /**
     * Sets the value of the lglEntityDesc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLglEntityDesc(String value) {
        this.lglEntityDesc = value;
    }

    /**
     * Gets the value of the lbmaMbr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLbmaMbr() {
        return lbmaMbr;
    }

    /**
     * Sets the value of the lbmaMbr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLbmaMbr(String value) {
        this.lbmaMbr = value;
    }

    /**
     * Gets the value of the busUnitDesc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBusUnitDesc() {
        return busUnitDesc;
    }

    /**
     * Sets the value of the busUnitDesc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBusUnitDesc(String value) {
        this.busUnitDesc = value;
    }

}
