//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.01.18 at 02:43:26 PM GMT 
//


package com.matthey.testutil.successresponse.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.matthey.testutil.response.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ERROR_QNAME = new QName("urn:or-getCoverageDealResponse", "ERROR");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.matthey.testutil.response.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TradeReferenceID }
     * 
     */
    public TradeReferenceID createTradeReferenceID() {
        return new TradeReferenceID();
    }

    /**
     * Create an instance of {@link GetCoverageDealResponse }
     * 
     */
    public GetCoverageDealResponse createGetCoverageDealResponse() {
        return new GetCoverageDealResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:or-getCoverageDealResponse", name = "ERROR")
    public JAXBElement<String> createERROR(String value) {
        return new JAXBElement<String>(_ERROR_QNAME, String.class, null, value);
    }

}
