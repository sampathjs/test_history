package com.matthey.pmm.ejm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractEJMController {

    protected final EndurConnector endurConnector;
    protected final XmlMapper xmlMapper;

    public AbstractEJMController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        this.endurConnector = endurConnector;
        this.xmlMapper = xmlMapper;
    }

    protected String genResponse(Object[] objects, Class<?> dataClass) {
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        if (objects.length == 0) {
            return header;
        }
        var wrapper = getWrapperElement(dataClass);
        return Stream.of(objects)
                .map(this::toXml)
                .collect(Collectors.joining("",
                                            header + System.lineSeparator() + genXmlTag(wrapper, false),
                                            genXmlTag(wrapper, true)));
    }

    private String genXmlTag(String name, boolean isClose) {
        return "<" + (isClose ? "/" : "") + name + ">";
    }

    private String getWrapperElement(Class<?> dataClass) {
        switch (dataClass.getSimpleName()) {
            case "AccountBalance":
            case "DailyAccountBalance":
                return "Response";
            case "Account":
                return "AccountExists";
            case "Statement":
            case "Specification":
                return "Documents";
            case "BSTransaction":
            case "DTRTransaction":
                return "Transactions";
            case "Transaction":
                return "Listing";
            case "SpecificationSummary":
                return "Specifications";
            default:
                throw new RuntimeException("invalid data class: " + dataClass.getSimpleName());
        }
    }

    private String toXml(Object object) {
        try {
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
