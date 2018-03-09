package com.olf.jm.pricewebservice.persistence;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.olf.openjvs.OException;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-04-23 	V1.0	jwaechter	- initial version
 */

/**
 * Helper class to deal with XML issues especially applying XSL transformation
 * to XML files.
 * @author jwaechter
 * @version 1.0
 */
public class XMLHelper {
	
	/**
	 * Retrieves the path to the stylesheet specified in the "href" attribute of the first xml-stylsheet tag
	 * of fileXML assuming the stylesheet referenced in the href attribute is specified with a 
	 * relative path. 
	 * 
	 * @param fileXML 
	 * @return
	 * @throws OException
	 */
	public static String retrieveStylesheetFromXML (String fileXML) throws OException {
		Path xmlfile = Paths.get(fileXML);
		Path parent = xmlfile.getParent();
		String styleSheetFileName="";
		try {
			List<String> allLines = java.nio.file.Files.readAllLines(xmlfile, Charset.defaultCharset());
			for (String line : allLines) {
				if (line.matches(".*xml-stylesheet.*")) {
					PluginLog.info("Stylesheet info found in line " + line);
					int hrefIndex =  line.lastIndexOf("href");
					int indexStartStylesheet = line.indexOf("\"", hrefIndex)+1;
					int indexEndStylesheet = line.indexOf("\"", indexStartStylesheet);
					styleSheetFileName = line.substring(indexStartStylesheet, indexEndStylesheet);
					break;
				}
			}
			if (!styleSheetFileName.isEmpty()) {
				Path pathToXml = Paths.get(parent.toString(), styleSheetFileName);
				styleSheetFileName = pathToXml.toString();
			}
			return styleSheetFileName;
		} catch (IOException e) {
			String message="Could not read from XML file " + fileXML + " :\n" + e.toString();
			throw new OException(message);
		}
	}
	


	/**
	 * Checks whether the file is an XML file or not by trying to build a DOM document through parsing.
	 * @param file the file to be checked.
	 * @return
	 */
	public static boolean isFileXML(String file) {
		try {
            File datafile = new File(file);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(datafile);
            if (document != null) {
            	return true;
            }
            return false;
        } catch (SAXException | ParserConfigurationException | IOException sxe ) {
        	PluginLog.warn(sxe.toString());
        	return false;
        }
	}

	/**
	 * Applies the stylesheet of an XML file.
	 * It renames fileXml and then applies the XSL transformation saving the result of the
	 * XSL transformation under the original filename.
	 * @param fileXml
	 * @throws OException 
	 * <ol>
	 *   <li> in case the source file could not be renamed </li> 
	 *   <li> in case the XSL parser can't be retrieved </li>
	 *   <li> in case an exception occurs during reading or writing <li>
	 *   <li> in case the transformation can't be retrieved </li>
	 * </ol> 
	 */
	public static void applyStylesheet(String fileXml, String stylesheetFileName) throws OException {
		String srcCopyFilename = createSrcCopyFilename (fileXml);
		File srcFile = new File (fileXml);
		File srcCopyFile = new File (srcCopyFilename);
		
		if (!srcFile.renameTo(srcCopyFile)) {
			String message = "Could not rename XML source file " + fileXml + " to " + srcCopyFilename;
			throw new OException (message);
		}
		PluginLog.info ("Renamed file " + fileXml + " to " + srcCopyFilename);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document;
        PluginLog.info("Start transformation");
        try {
             File stylesheet = new File(stylesheetFileName);
 
             DocumentBuilder builder = factory.newDocumentBuilder();
             document = builder.parse(srcCopyFilename);
             PluginLog.info("Creating transformer factory");
            // Use a Transformer for output
             TransformerFactory tFactory = TransformerFactory.newInstance();
             tFactory.setErrorListener(new ErrorListener() {  // use error listener to get details if error occurs
				@Override
				public void error(TransformerException arg0)
						throws TransformerException {
					PluginLog.error(arg0.toString());					
				}
				@Override
				public void fatalError(TransformerException arg0)
						throws TransformerException {
					PluginLog.error(arg0.toString());										
				}
				@Override
				public void warning(TransformerException arg0)
						throws TransformerException {
					PluginLog.warn(arg0.toString());	
				}            	 
             });
             StreamSource stylesource = new StreamSource(stylesheet);
             PluginLog.info("Creating transformer");
             Transformer transformer = tFactory.newTransformer(stylesource);
             PluginLog.info("Converting document to DOM source"); 
             DOMSource source = new DOMSource(document);
             PluginLog.info("Creating output stream"); 
             OutputStream output = new BufferedOutputStream (new FileOutputStream(fileXml));
             StreamResult result = new StreamResult(output);
             PluginLog.info("Applying transformation"); 
             transformer.transform(source, result);
             output.flush();
             output.close();
             PluginLog.info("Succesfully created transformed XML result " + fileXml);
         } catch (TransformerConfigurationException tce) {
             String message = "Could not create XSL Transformator with the requested configuration:\n " + tce.toString();
             PluginLog.info(message);
             throw new OException (message);            
         } catch (TransformerException te) {
             // Error generated by the parser
        	 String message = "Error occured while parsing files:\n " + te.toString();
             PluginLog.info(message);
        	 throw new OException (message);        	 
         } catch (SAXException sxe) {
             String message = "Could not create XSL parser with the requested configuration:\n " + sxe.toString();
             PluginLog.info(message);
             throw new OException (message); 	 
         } catch (ParserConfigurationException pce) {
             String message = "Could not create XSL parser with the requested configuration:\n " + pce.toString();
             PluginLog.info(message);
             throw new OException (message);
         } catch (IOException ioe) {
             PluginLog.info(ioe.toString());
             throw new OException (ioe);
         }
	}

	public static String createSrcCopyFilename(String fileXml) {
		
		int indexLastDot = fileXml.lastIndexOf(".");
		StringBuilder srcCopyFilename = new StringBuilder ();
		srcCopyFilename.append(fileXml.substring(0, indexLastDot)).append("_src").append(fileXml.substring(indexLastDot));
		return srcCopyFilename.toString();
	}
	
	/**
	 * To prevent instantiation
	 */
	private XMLHelper() {
		
	}
}
