package com.matthey.testutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.matthey.testutil.common.Util;
import com.matthey.testutil.exception.SapTestUtilException;
import com.olf.openjvs.OException;
import com.openlink.util.logging.PluginLog;

/**
 * This utility creates consolidated XML from multiple XML from a directory
 * @author SharmV04
 *
 */
public class CreateConsolidatedXML
{
	public void createXML( String directoryPath, String rootElement)
	{
		try
		{
			File directory;
			File listOfFiles[];

			PluginLog.info("Started executing " + this.getClass().getSimpleName());
			PluginLog.debug("Directory path: " + directoryPath);

			PluginLog.debug("Root element: " + rootElement);

			directory = new File(directoryPath);
			listOfFiles = directory.listFiles();

			Document doc = merge("/" + rootElement, listOfFiles);
			print(doc);
			PluginLog.info("Completed executing " + this.getClass().getSimpleName());
		}
		catch (Throwable throwable)
		{
			Util.printStackTrace(throwable);
			com.olf.openjvs.Util.exitFail();
		}
	}

	/**
	 * @param expression
	 * @param listOfFiles
	 * @return
	 * @throws Exception
	 */
	private static Document merge(String expression, File listOfFiles[]) throws Exception
	{
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xpath = xPathFactory.newXPath();
		XPathExpression compiledExpression = xpath.compile(expression);
		return merge(compiledExpression, listOfFiles);
	}

	/**
	 * @param expression
	 * @param listOfFiles
	 * @return
	 * @throws Exception
	 */
	private static Document merge(XPathExpression expression, File listOfFiles[]) throws Exception
	{
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document base = docBuilder.parse(listOfFiles[0]);
		File currentFile;

		Node results = (Node) expression.evaluate(base, XPathConstants.NODE);
		if (results == null)
		{
			throw new IOException(listOfFiles[0] + ": expression does not evaluate to node");
		}

		for (int i = 1; i < listOfFiles.length; i++)
		{
			currentFile = listOfFiles[i];
			if (currentFile.isFile())
			{
				Document merge = docBuilder.parse(currentFile);
				Node nextResults = (Node) expression.evaluate(merge, XPathConstants.NODE);
				while (nextResults.hasChildNodes())
				{
					Node kid = nextResults.getFirstChild();
					nextResults.removeChild(kid);
					kid = base.importNode(kid, true);
					results.appendChild(kid);
				}
			}
		}

		return base;
	}

	/**
	 * @param doc
	 * @throws OException 
	 * @throws SapTestUtilException 
	 * @throws Exception
	 */
	private static void print(Document doc) throws OException, SapTestUtilException 
	{
		TransformerFactory transformerFactory;
		Transformer transformer;
		DOMSource source;
		String resultDirectoryPath;
		String resultFileName;
		String resultFilePath;
		Result result;
		

		resultFileName = Util.getFileNameWithTimeStamp("ConsolidatedConfirmsXML", "xml");
		resultDirectoryPath = Util.getOutputDirectoryPath("Consolidated XML");
		resultFilePath = resultDirectoryPath + File.separator + resultFileName;
		
		try(PrintStream printStream = new PrintStream(resultFilePath))
		{
			transformerFactory = TransformerFactory.newInstance();
			transformer = transformerFactory.newTransformer();
			source = new DOMSource(doc);
			result = new StreamResult(printStream);
			transformer.transform(source, result);
		}
		catch (FileNotFoundException e)
		{
			Util.printStackTrace(e);
			throw new SapTestUtilException(e.getMessage(), e);
		}
		catch (TransformerConfigurationException transformerConfigurationException)
		{
			Util.printStackTrace(transformerConfigurationException);
			throw new SapTestUtilException(transformerConfigurationException.getMessage(), transformerConfigurationException);
		}
		catch (TransformerFactoryConfigurationError transformerFactoryConfigurationError)
		{
			Util.printStackTrace(transformerFactoryConfigurationError);
			throw new SapTestUtilException(transformerFactoryConfigurationError.getMessage(), transformerFactoryConfigurationError);
		}
		catch (TransformerException transformerException)
		{
			Util.printStackTrace(transformerException);
			throw new SapTestUtilException(transformerException.getMessage(), transformerException);
		}
		
		
		PluginLog.debug("Consolidated XML generated at path: " + resultFilePath);
	}

}
