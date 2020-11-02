package com.olf.jm.reportbuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.DocGen;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLFDOC_OUTPUT_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;


/**
 * 
 * Report builder output plugin that transforms the report builder output data into an xml structure and sends to DMS template. This plugin
 * will also link the generated DMS document to the deal if the deal tracking number or transaction number is available in the output data.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 05-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 07-Dec-2015 |               | G. Moore        | Remove existing document with the same document title from deal before adding   |
 * |     |             |               |                 | new document to deal.                                                           |
 * | 003 | 14-Aug-2020 |               | J. Wächter      | Removing existing document after the new document added and leaving latest doc  |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class ReportOutputToDms extends AbstractGenericScript {

    private static int PDF;

    Table reportParams;

    /** Stores the last value of group columns. */
    private HashMap<String, String> colLastValue = new HashMap<>();

    private ReportColumns reportCols;
    private GroupStructure groupStructure;

    @Override
    public Table execute(Session session, ConstTable argt) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "ReportBuilder");
        	process(session, argt);
            return null;
        }
        catch (RuntimeException e) {
        	Logging.error("Failed", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }
    
    /**
     * Main processing method.
     * 
     * @param session
     * @param argt
     * @return
     */
    private void process(Session session, ConstTable argt) {

        PDF = session.getStaticDataFactory().getId(EnumReferenceTable.FileObjectType, "PDF");

        // Enter into the column values  map some common values.
        colLastValue.put("GEN_DATE", new SimpleDateFormat("ddMMMYYYY").format(new Date()));
        colLastValue.put("RUN_TIME", new SimpleDateFormat("HHmmss").format(new Date()));
        colLastValue.put("OUTPUT_DIR", session.getIOFactory().getReportDirectory());

        reportParams = argt.getTable(0, 0);
        Table data = argt.getTable(1, 0);

        String docTitle = getReportParameterValue("DocumentTitle", "", false);

        Logging.info("Generating document " + docTitle);
        
        if (data.getRowCount() < 1) {
        	Logging.info("No data available to produce report for '" + docTitle + "'");
            return;
        }

        int tranNum = 0;
        int dealNum = 0;
        if (data.isValidColumn("tran_num")) {
            tranNum = data.getInt("tran_num", 0);
        }
        if (data.isValidColumn("deal_tracking_num")) {
            dealNum = data.getInt("deal_tracking_num", 0);
        }

        try {
            Document doc = generateReportXmlDocument(session, data, reportParams);

            // Fill hash map that maps column name to column title.
            reportCols = new ReportColumns(data, false);
            // Obtain the report grouping definition from the report parameters.
            groupStructure = new GroupStructure(session, reportParams, data, reportCols);

            docTitle = Utils.substituteValues(docTitle, false, colLastValue, reportCols);

            runLinkedReports(session, doc, reportParams);
            
            String xml = Utils.docAsXml(doc);
            saveXml(session, xml, docTitle, dealNum);
            
            String docPath = generateDocument(xml);
    
            if ("YES".equalsIgnoreCase(getReportParameterValue("SaveToDeal", "NO", false)) && tranNum > 0) {
                saveDocumentToTransaction(session, tranNum, docTitle, docPath);
            }
            else if (tranNum > 0) {
                Logging.info("Parameter SaveToDeal for document '" + docTitle + "' is set to No so document not saved to deal");
            }
        } catch (OException | ParserConfigurationException | TransformerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run any linked reports defined for the main report and add the output document to the main document supplied.
     * 
     * @param session Current session
     * @param document Main xml document
     * @param reportParameters Main report parameters
     */
    private void runLinkedReports(Session session, Document document, Table reportParameters) {
        // Loop through the report parameters looking for linked reports
        for (TableRow row : reportParameters.getRows()) {
            if (row.getString(0).startsWith("LinkedReportDefinition")) {
                // Linked report found, the report definition name and it's parent group are defined by the parameter value
                // and are delimited by a '|' character 
                String[] rowValues = row.getString(1).split("\\|");
                String reportName = rowValues[0].trim();
                String parentGroup = groupStructure.getGroupElementName(Integer.valueOf(rowValues[1].split("_")[1]));
                // Clone report parameters and use a copy as parameters to linked report
                try (Table linkedReportParameters = reportParameters.cloneData()) {
                    DmsOutputParameterUtil.removeDmsOutputParameters(linkedReportParameters);
                    Document output = runReport(session, reportName, linkedReportParameters);
                    
                    // From the output document get the children of the root element and then add these children to the main document
                    // under the required parent node
                    Node parent = document.getElementsByTagName(parentGroup).item(0);
                    NodeList nodes = output.getFirstChild().getChildNodes();
                    for (int idx = 0; idx < nodes.getLength(); idx++) {
                        Node node = document.importNode(nodes.item(idx), true);
                        parent.appendChild(node);
                    }
                }
            }
        }
    }

    /**
     * Run the given Report Builder report.
     * 
     * @param reportName Report name
     * @return output xml document
     * @throws OException
     */
    private Document runReport(Session session, String reportName, Table reportParameters) {
        HashMap<String, String> parameters = DmsOutputParameterUtil.parameterTableToHashMap(reportParameters);
        parameters.put("report_name", reportName);
        ReportParameters rptParams = new ReportParameters(session, parameters);
        
        GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
        generator.generate();

        try (Table output = generator.getResults();
             Table allParameters = DmsOutputParameterUtil.parameterSetToTable(session, generator.getDefinitonParameters())) {
            return generateReportXmlDocument(session, output, allParameters);
        }
        catch (OException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate the xml document from the report output data using the grouping configuration defined in the report parameters.
     * 
     * @param session Current session
     * @param data Report output data
     * @param reportParams Report parameters
     * @return xml document
     * @throws OException
     * @throws ParserConfigurationException
     */
    private Document generateReportXmlDocument(Session session, Table data, Table reportParams) throws OException, ParserConfigurationException {
        // Fill hash map that maps column name to column title.
        ReportColumns reportCols = new ReportColumns(data, true);
        // Obtain the report grouping definition from the report parameters.
        GroupStructure groupStructure = new GroupStructure(session, reportParams, data, reportCols);

        return DocumentBuilder.build(session, data, groupStructure, reportCols, colLastValue);
    }
    
    /**
     * Generate the PDF document.
     * 
     * @param xml Report data
     * @return generated document file path
     * @throws OException
     */
    private String generateDocument(String xml) {

        // Get the DMS template from the report parameters.
        String template = getReportParameterValue("Template", null, true);

        // Get the document output path from the report parameters.
        String output = getReportParameterValue("Output", null, true);
        output = Utils.substituteValues(output, false, colLastValue, reportCols);

        try {
            DocGen.generateDocument(template, output, xml, null, OLFDOC_OUTPUT_TYPE_ENUM.OLFDOC_OUTPUT_TYPE_PDF.toInt(), 0, null, null, 
                null, null);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    /**
     * Get the report parameter value for the parameter name.
     * 
     * @param parameterName Parameter name
     * @param defaultValue Default value to return if parameter does not exist
     * @param exceptionOnMissingParameter true if RuntimeException to be thrown on parameter not found
     * @return parameter value or empty string if parameter not found
     */
    private String getReportParameterValue(String parameterName, String defaultValue, boolean exceptionOnMissingParameter) {
    	/*** For v17 column name changed from expr_param_name to parameter_name ***/
    	String colValue = Utils.getColParamValue(reportParams);
    	int column = reportParams.getColumnId(Utils.getColParamName(reportParams));
    	
        int row = reportParams.find(column, parameterName, 0);
        if (row < 0 && exceptionOnMissingParameter) {
            throw new RuntimeException("The report parameter " + parameterName + " is missing from the Report Builder configuration");
        }
        else if (row < 0 && defaultValue != null) {
            return defaultValue;
        }
        else if (row < 0) {
            return "";
        }
        return reportParams.getString(colValue, row);
    }

    /**
     * Save the report xml document to the daily reporting folder.
     * 
     * @param xml Xml document to save
     * @param docTitle Document title
     * @param dealNum Deal number
     * @throws IOException
     */
    private void saveXml(Session session, String xml, String docTitle, int dealNum) throws IOException {
        String xmlPath = session.getIOFactory().getReportDirectory() + '/' + docTitle + '_' + dealNum + ".xml";
        Files.write(Paths.get(xmlPath), xml.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Save the document to the transaction. If the transaction is as amended status the document is saved to the current transaction.
     * 
     * @param session Current session object
     * @param tranNum Transaction number
     * @param docTitle Document title
     * @param docPath Document path
     * @throws OException
     */
    private void saveDocumentToTransaction(Session session, int tranNum, String docTitle, String docPath) throws OException {

        // Need to use OpenJVS transaction because OC transaction does not support adding deal documents 
        Transaction tran = Transaction.retrieve(tranNum);

        try {
            int dealNum = tran.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
            // If tran status is Amended the document cannot be saved to the transaction, so find the current transaction
            if (tran.getFieldInt(TRANF_FIELD.TRANF_TRAN_STATUS.toInt()) == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED.toInt()) {
                Transaction current = retrieveCurrentTran(session, dealNum);
                tran.destroy();
                tran = current;
            }

            Logging.info("Adding document '" + docTitle + "' to deal " + tran.getField(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
            int retval = tran.addDealDocument(docPath, PDF, 0, docTitle);
            retval = tran.saveDealDocumentTable();
            Thread.sleep(1000*5);
            Transaction current = com.olf.openjvs.Transaction.retrieve(tran.getTranNum());
            tran.destroy();
            tran = null;
            tran = current;
            // Delete existing documents of doc title from deal except the latest document
            deleteExistingDocumentExceptLatest(tran, docTitle);
            retval = tran.saveDealDocumentTable();            
        } catch (InterruptedException e) {
        	// do nothing
        } finally {
            try {
            	if (tran != null) {
                    tran.destroy();            		
            	}
            }
            catch (OException e) {
                // Nothing we can do!
            }
        }
        
    }

	private void deleteExistingDocumentExceptLatest(Transaction tran, String docTitle) throws OException {
        Logging.info("Removing older documents of type '" + docTitle + "' documents from deal " + tran.getField(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
        com.olf.openjvs.Table dealDocuments = tran.getDealDocumentTable();
        try {
            int rowCount = dealDocuments.getNumRows();
            
            int maxDocId = 0;
            for (int row = 1; row <= rowCount; row++) {
                String reference = dealDocuments.getString("reference", row);
                if (reference.equals(docTitle)) {
                    int docId = dealDocuments.getInt("doc_id", row);
                    if (docId > maxDocId) {
                    	maxDocId = docId;
                    }
                }
            }
            Logging.info("Max Doc ID found: " + maxDocId);
            for (int row = 1; row <= rowCount; row++) {
                String reference = dealDocuments.getString("reference", row);
                if (reference.equals(docTitle)) {
                    int docId = dealDocuments.getInt("doc_id", row);
                    if (docId < maxDocId) {
                    	Logging.info("Deleting document having doc id #" + docId);
                        tran.deleteDealDocument(docId);
                        // Delete from file system
                        String path = dealDocuments.getString("saved_link", row);
                        Files.deleteIfExists(Paths.get(path));                    	
                    }
                }
            }
        } catch (IOException e) {
            Logging.error("Failed to delete existing document from file system", e);
		}
        finally {
            dealDocuments.destroy();
        }
	}

    /**
     * Retrieve the current transaction for the deal number.
     * 
     * @param dealNum Deal number
     * @return current transaction
     * @throws OException
     */
    private com.olf.openjvs.Transaction retrieveCurrentTran(Session session, int dealNum) throws OException {
        try (Table current = session.getIOFactory().runSQL(
                "SELECT tran_num FROM ab_tran WHERE current_flag = 1 AND deal_tracking_num = " + dealNum)) {

            if (current.getRowCount() == 1) {
                int tranNum = current.getInt("tran_num", 0);
                return com.olf.openjvs.Transaction.retrieve(tranNum);
            }
            else if (current.getRowCount() > 1) {
                throw new RuntimeException("Unexpected error: More than one current transaction for deal " + dealNum);
            }
            throw new RuntimeException("Current transaction not found for deal " + dealNum);
        }
    }

}
