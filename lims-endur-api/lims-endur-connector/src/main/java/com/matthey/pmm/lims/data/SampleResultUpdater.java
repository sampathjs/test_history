package com.matthey.pmm.lims.data;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

public class SampleResultUpdater {

    final Session session;
    private static final String USER_TABLE_USER_JM_LIMS_SAMPLES = "USER_jm_lims_samples";
    private static final String USER_TABLE_USER_JM_LIMS_RESULT = "USER_jm_lims_result";
    
    private static final Logger logger = LogManager.getLogger(SampleResultUpdater.class);
    
    public SampleResultUpdater(Session session) {
        this.session = session;
        logger.info("SampleResultUpdater constructor started");
    }

    public String process(String sampleResultXML) {
    	
    	Boolean status = true;
    	logger.info("Start processing XML");
    	
    	UserTable userTableJMLimsSamples = session.getIOFactory().getUserTable(USER_TABLE_USER_JM_LIMS_SAMPLES);
		Table userJMLimsSamples = userTableJMLimsSamples.getTableStructure();
		Table userJMLimsSamplesToDelete = session.getTableFactory().createTable();
		
		UserTable userTableJMLimsResults = session.getIOFactory().getUserTable(USER_TABLE_USER_JM_LIMS_RESULT);
		Table userJMLimsResults = userTableJMLimsResults.getTableStructure();
		Table userJMLimsResultsToDelete = session.getTableFactory().createTable();
		
    	try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document = dBuilder.parse(new InputSource(new StringReader(sampleResultXML)));
			document.getDocumentElement().normalize();
			
			Element root = document.getDocumentElement();

			// Loop through samples
			NodeList samples = document.getElementsByTagName("Sample");
			logger.info("number of samples: " + samples.getLength());
			for (int sampleCount = 0; sampleCount < samples.getLength(); sampleCount++) {
				
				Node sampleNode = samples.item(sampleCount);
				if (sampleNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}

				Element sample = (Element) sampleNode;
				String productId = sample.getElementsByTagName("ProductID").item(0).getTextContent();
				String batchId = sample.getElementsByTagName("LotID").item(0).getTextContent();
				String sampleId = sample.getElementsByTagName("SampleID").item(0).getTextContent();
				
				int sampleRow = userJMLimsSamples.addRows(1);
				userJMLimsSamples.setString("jm_batch_id", sampleRow, batchId);
				userJMLimsSamples.setString("sample_number", sampleRow, sampleId);
				userJMLimsSamples.setString("product", sampleRow, productId);
				userJMLimsSamples.setDate("last_updated", sampleRow, new java.util.Date());
				
				logger.info("Parsing XML for batchId = " + batchId + ", sampleNumber = " + sampleId + ", product = " + productId);
				
				//Loop through Test
				NodeList tests = sample.getElementsByTagName("Test");
				logger.info("number of tests: " + tests.getLength());
		        for (int testCount = 0; testCount < tests.getLength(); testCount++) {
		        	Node testNode = tests.item(testCount);
					if (testNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					Element test = (Element) testNode;
		        	String analysisName = test.getElementsByTagName("AnalysisName").item(0).getTextContent();
		        	
		        	// Loop through result
		        	NodeList results = test.getElementsByTagName("Result");
		        	logger.info("number of results: " + results.getLength());
		            for (int resultCount = 0; resultCount < results.getLength(); resultCount++) {
		            	
		            	Node resultNode = results.item(resultCount);
						if (resultNode.getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}
						Element result = (Element) resultNode;
						
		            	String resultName = result.getElementsByTagName("ResultName").item(0).getTextContent();
		            	String resultUnit = result.getElementsByTagName("ResultUnit").item(0).getTextContent();
		                String resultValue = result.getElementsByTagName("ResultValue").item(0).getTextContent();
		                logger.info("Parsing XML for sample: " + sampleId + ", analysis: " + analysisName + ", result: " + resultName);
		                
		                int resultrowNum = userJMLimsResults.addRows(1);
				    	userJMLimsResults.setString("sample_number", resultrowNum, sampleId);
				    	userJMLimsResults.setString("analysis", resultrowNum, analysisName);
				    	userJMLimsResults.setString("name", resultrowNum, resultName);
				    	userJMLimsResults.setString("units", resultrowNum, resultUnit);
				    	userJMLimsResults.setString("formatted_entry", resultrowNum, resultValue);
				    	userJMLimsResults.setString("status", resultrowNum, "A");
				    	
		            }
		        }
			}
			
			userJMLimsSamplesToDelete.select(userJMLimsSamples, "jm_batch_id, sample_number, product", "[IN.jm_batch_id]  != '0'");
			// For update messages, get the list of sample numbers to be deleted.
			userJMLimsResultsToDelete.select(userJMLimsResults, "sample_number", "[IN.sample_number] != '0'");
			
		} catch (DOMException e1) {
			logger.error("DOMException : " + e1.getMessage());
			status = false;
		} catch (ParserConfigurationException e1) {
			logger.error("DOMException : " + e1.getMessage());
			status = false;
		} catch (SAXException e1) {
			logger.error("DOMException : " + e1.getMessage());
			status = false;
		} catch (IOException e1) {
			logger.error("DOMException : " + e1.getMessage());
			status = false;
		}
        
    	if(status) {
    		status = updateUserTables(userTableJMLimsSamples, userJMLimsSamples, userTableJMLimsResults, userJMLimsResultsToDelete, userJMLimsResults);
    	}
		
		logger.info("Method completed");
		return status ? "Success" : "Failure";
    }

	private boolean updateUserTables(UserTable userTableJMLimsSamples, Table userJMLimsSamples, UserTable userTableJMLimsResults,
			Table userJMLimsResultsToDelete, Table userJMLimsResults) {

		try {
			
			// Delete existing rows from user table
			logger.info("Rows in user table " + USER_TABLE_USER_JM_LIMS_SAMPLES + " before deletion: " + userTableJMLimsSamples.getRowCount());
			logger.info("Delete existing rows from user table " + USER_TABLE_USER_JM_LIMS_SAMPLES);
			userTableJMLimsSamples.deleteMatchingRows(userJMLimsSamples);
			logger.info("Rows in user table " + USER_TABLE_USER_JM_LIMS_SAMPLES + " after deletion: " + userTableJMLimsSamples.getRowCount());
			
			// Delete existing rows from user table
			logger.info("Rows in user table " + USER_TABLE_USER_JM_LIMS_RESULT + " before deletion: " + userTableJMLimsResults.getRowCount());
			logger.info("Delete existing rows from user table " + USER_TABLE_USER_JM_LIMS_RESULT);
			userTableJMLimsResults.deleteMatchingRows(userJMLimsResultsToDelete);
			logger.info("Rows in user table " + USER_TABLE_USER_JM_LIMS_RESULT + " after deletion: " + userTableJMLimsResults.getRowCount());
			
			logger.info("Updating user table " + USER_TABLE_USER_JM_LIMS_SAMPLES);
			userTableJMLimsSamples.insertRows(userJMLimsSamples);
			
			logger.info("Updating user table " + USER_TABLE_USER_JM_LIMS_RESULT);
			userTableJMLimsResults.insertRows(userJMLimsResults);
			
		} catch (Exception e) {
			logger.error("Failed to upload user table : " + e.getMessage());
			return false;
		}
		
		return true;
	}
}
