package com.matthey.testutil;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.common.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

/**
 * Converts XML file in CSV
 * @author SharmV04
 */
public class Xml2CsvConverter implements IScript
{
	@Override
	public void execute(IContainerContext context) throws OException
	{
		try
		{
			final String STYLE_SHEET_PATH_COLUMN_NAME = "style_sheet_path";
			final String XML_DIRECTORY_COLUMN_NAME = "xml_directory";
			final String RESULT_FILE_PREFIX = "CSVConvertedFromXML";
			
			String styleSheetPath;
			String xmlFilePath;
			String xmlDirectory;
			String resultFileName;
			String resultDirectoryPath;
			String resultFilePath;

			File stylesheet;
			File xmlFile;
			File resultFile;
			
			DocumentBuilderFactory factory;
			DocumentBuilder builder;
			Document document;
			StreamSource stylesource;
			Transformer transformer;
			Source source;
			Result outputTarget;
			
			Table argumentTable;

			Util.setupLog();
			PluginLog.info("Started executing " + this.getClass().getSimpleName());
			argumentTable = context.getArgumentsTable();
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();

			styleSheetPath = argumentTable.getString(STYLE_SHEET_PATH_COLUMN_NAME, 1);
			stylesheet = new File(styleSheetPath);

			xmlDirectory = argumentTable.getString( XML_DIRECTORY_COLUMN_NAME, 1);
			xmlDirectory = Util.getAbsolutePath(xmlDirectory);
			xmlFilePath = Util.getLatestFilePath(xmlDirectory);
			xmlFile = new File(xmlFilePath);

			document = builder.parse(xmlFile);

			stylesource = new StreamSource(stylesheet);
			transformer = TransformerFactory.newInstance().newTransformer(stylesource);
			source = new DOMSource(document);
			
			resultFileName = Util.getFileNameWithTimeStamp(RESULT_FILE_PREFIX, SAPTestUtilitiesConstants.CSV);
			resultDirectoryPath = Util.getOutputDirectoryPath( "Converted CSV from XML");
			resultFilePath = resultDirectoryPath + File.separator + resultFileName;
			
			resultFile = new File(resultFilePath);
			outputTarget = new StreamResult(resultFile);

			transformer.transform(source, outputTarget);
			PluginLog.info("Completed executing " + this.getClass().getSimpleName());
		}
		catch (Throwable throwable)
		{
			PluginLog.debug("No,You have not launched a ballistic missile. It's is an exception. Here is the stack trace.");
			Util.printStackTrace(throwable);
			PluginLog.info("Completed executing " + this.getClass().getSimpleName());
			throw new OException("Exception occurred." + throwable.getMessage());
		}
	}
}
