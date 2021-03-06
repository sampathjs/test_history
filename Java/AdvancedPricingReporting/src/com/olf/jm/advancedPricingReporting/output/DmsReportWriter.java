package com.olf.jm.advancedPricingReporting.output;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openjvs.DocGen;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.OLFDOC_OUTPUT_TYPE_ENUM;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DmsReportWritter. Outputs the report data using DMS
 */
public class DmsReportWriter implements ReportWriter {

	/** The writer parameters. */
	private ReportWriterParameters writerParameters;
	
	/** The report parameters. */
	private ReportParameters reportParameters;
	
	/** The context. */
	private Context context;
	
	/**
	 * Instantiates a new dms report writer.
	 *
	 * @param writerParameters the writer parameters
	 * @param reportParameters the report parameters
	 * @param context the context
	 */
	public DmsReportWriter(ReportWriterParameters writterParameters, ReportParameters reportParameters, Context context) {
		this.writerParameters = writterParameters;
		this.reportParameters = reportParameters;		
		this.context = context;
		
		validateParameters();
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.output.ReportWriter#generateReport(java.lang.String)
	 */
	@Override
	public void generateReport(String xml) {
		
		writeXMLToFile(xml);
		
		String templateName = writerParameters.getTemplateName();
		
		String outputFilename = getFileName();
		
		try {
			DocGen.generateDocument(templateName, outputFilename, xml, null, OLFDOC_OUTPUT_TYPE_ENUM.OLFDOC_OUTPUT_TYPE_PDF.toInt(), 0, null, null, null, null);
		} catch (OException e) {
			String errorMessage = "Error generating report output. " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}
	
	/**
	 * Write xml to file.
	 *
	 * @param xml the xml to write to the file
	 */
	private void writeXMLToFile(String xml) {
		try {
			Files.write(Paths.get(getFileName() + ".xml"), prettyFormat(xml, "4").getBytes(), StandardOpenOption.CREATE_NEW);
		} catch (Exception e) {
			String errorMessage = "Error generating xml output. " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}	
	}
	
	/**
	 * Pretty format xml string.
	 *
	 * @param input the xml to format
	 * @param indent the indentation to use
	 * @return the formatted xml
	 * @throws TransformerException the transformer exception
	 */
	private String prettyFormat(String input, String indent)
			throws TransformerException {
		Source xmlInput = new StreamSource(new StringReader(input));
		StringWriter stringWriter = new StringWriter();

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", indent);
		transformer.transform(xmlInput, new StreamResult(stringWriter));

		String pretty = stringWriter.toString();
		pretty = pretty.replace("\r\n", "\n");
		return pretty;
	}
	
	/**
	 * Validate parameters.
	 */
	private void validateParameters() {
		// Check that the template name is set
		if(writerParameters.getTemplateName() == null || writerParameters.getTemplateName().length() == 0) {
			throw new RuntimeException("Template name is invalid.");
		}
	}
	
	/**
	 * Gets the output file name. Output filename format
	 * 
	 * AdvancedDeferredPricingReport_[External BU short name]_[run time dd-MM-yyyy_HHmmss].pdf
	 * 
	 * @return the file name
	 */
	private String getFileName() {
		
		Calendar cal = Calendar.getInstance();
		
		DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
		
		int externalBU = reportParameters.getExternalBu();
		
		return writerParameters.getOutputLocation() + "\\AdvancedDeferredPricingReport_" + getBuName(externalBU) + "_" + sdf.format(cal.getTime());
	}
	
	/**
	 * Gets the bu name.
	 *
	 * @param buId the bu id
	 * @return the bu name
	 */
	private String getBuName(int buId) {
		StaticDataFactory sdf = context.getStaticDataFactory();
		
		BusinessUnit bu = (BusinessUnit) sdf.getReferenceObject(EnumReferenceObject.BusinessUnit, buId);
		
		if(bu == null) {
			throw new RuntimeException("Error loading business unit " + buId);
		}
				
		return bu.getName();
	}
		

}
