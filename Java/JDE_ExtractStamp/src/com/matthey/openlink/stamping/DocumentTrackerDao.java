package com.matthey.openlink.stamping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.logging.PluginLog;

/**
 * DAO layer for document tracker.
 * 
 */
public class DocumentTrackerDao {
    private static final String DOC_TRACKING_TABLE = DocumentTrackerTable.getTableName();
    private static final Integer STLDOC_TYPE_INVOICE = 1;
    private static final Integer STDOC_STATUS_CANCELLED = 4;
    private static final Integer STDOC_STATUS_1_GENERATED = 5;
    private static final Integer STLDOC_STATUS_2_SENT_TO_CP = 7;
    private static final Integer TRAN_INFO_TYPE_IS_COVERAGE = 20021;
    
    private final Session session;
    
    /**
     * Instantiates a new document tracking DAO.
     *
     * @param session the session
     */
    public DocumentTrackerDao(Session session){
        this.session = session; 
    }

    /**
     * Gets the new invoices by running a SQL query.
     *
     * @return list of document tracker
     */
    public List<DocumentTracker> getNewInvoices(String stlDocLastUpdate){
        List<DocumentTracker> invoices = new ArrayList<DocumentTracker>();
        
        String sqlQuery = "SELECT d.document_num, d.last_update, d.personnel_id, d.doc_status, d.last_doc_status, d.doc_version, d.stldoc_hdr_hist_id, \n"
                +" CASE WHEN EXISTS(SELECT 1 from ab_tran ab join ab_tran_info ti on ab.tran_num = ti.tran_num join stldoc_details_hist sdh on ab.tran_num = sdh.tran_num \n"
                +" WHERE ti.type_id = "+ TRAN_INFO_TYPE_IS_COVERAGE +" and ti.value = 'Yes' and sdh.document_num = d.document_num and sdh.doc_version = d.doc_version ) \n"
                +" THEN 'Yes' ELSE 'No' END is_coverage \n"
                +" FROM (SELECT shh.document_num, shh.last_update, shh.personnel_id, shh.doc_status, shh.last_doc_status, shh.doc_version, shh.stldoc_hdr_hist_id,\n"  
                +" RANK() OVER (PARTITION BY shh.document_num ORDER BY shh.doc_version desc ) AS RowRank \n"
                +" FROM  stldoc_header_hist shh \n" 
                +" WHERE shh.doc_status = " + STDOC_STATUS_1_GENERATED + " \n" 
                +" AND NOT EXISTS (SELECT 1 FROM "+ DOC_TRACKING_TABLE +" dt WHERE dt.document_num = shh.document_num) \n"
                +" AND shh.doc_type = "+ STLDOC_TYPE_INVOICE +" AND shh.last_update > '"+ stlDocLastUpdate +"' \n"
                +" ) d WHERE d.RowRank = 1 ";
        PluginLog.debug(String.format("New invoices tracking Sql query: \n%s",sqlQuery));

        try {
            IOFactory ioFactory = session.getIOFactory();
            Table result = ioFactory.runSQL(sqlQuery);
            PluginLog.debug(String.format("Total number of row returned by new invoice Sql query: %d",result.getRowCount()));

            Date lastUpdate = session.getServerTime();
            Integer personnelId = session.getUser().getId();
            String personnelName = session.getUser().getName();

            for(TableRow row : result.getRows()) {
                Integer documentNumber = row.getInt(DocumentTrackerTable.DOCUMENT_NUM.getColumnName());
                Integer docStatus = row.getInt(DocumentTrackerTable.DOC_STATUS.getColumnName());
                Integer lastDocStatus = row.getInt(DocumentTrackerTable.LAST_DOC_STATUS.getColumnName());
                Integer docVersion = row.getInt(DocumentTrackerTable.DOC_VERSION.getColumnName());
                Integer docHistoryId = row.getInt(DocumentTrackerTable.STLDOC_HDR_HIST_ID.getColumnName());
                String isCoverage = row.getString("is_coverage");

                DocumentTracker invoice = new DocumentTracker.DocumentTrackerBuilder(documentNumber, docStatus, docVersion)
                                            .LastDocStatus(lastDocStatus)
                                            .DocHistoryId(docHistoryId)
                                            .LastUpdate(lastUpdate)
                                            .PersonnelId(personnelId)
                                            .PersonnelName(personnelName)
                                            .Coverage(isCoverage)
                                            .build();
                invoices.add(invoice);
            }
        } catch(Exception ex) {
            throw new StampingException(String.format("An exception occurred while retrieving new invoices. %s", ex.getMessage()), ex);
        }
        return invoices;
    }

    /**
     * Gets the cancelled invoices by running a SQL query.
     *
     * @return list of document tracker
     */
    public List<DocumentTracker> getCancelledInvoices(String stlDocLastUpdate){
        List<DocumentTracker> invoices = new ArrayList<DocumentTracker>();
        
        String sqlQuery = " SELECT d.document_num, d.last_update, d.personnel_id, d.doc_status, d.last_doc_status, d.doc_version, d.stldoc_hdr_hist_id,d.sl_status,d.sap_status,d.sent_to_cp FROM \n"
                +" (SELECT shh.document_num, shh.last_update, shh.personnel_id, shh.doc_status, shh.last_doc_status, shh.doc_version, shh.stldoc_hdr_hist_id,dt.sl_status,dt.sap_status, \n"
        		+ "CASE WHEN EXISTS(SELECT 1 FROM stldoc_header_hist WHERE doc_status = " + STLDOC_STATUS_2_SENT_TO_CP + " and document_num = shh.document_num ) THEN 'Yes' ELSE 'No' END sent_to_cp, \n" 
                +" RANK() OVER (PARTITION BY shh.document_num ORDER BY shh.doc_version desc ) AS RowRank \n"
                +" FROM  stldoc_header_hist shh \n"
                +" JOIN " + DOC_TRACKING_TABLE + " dt \n"
                +" ON dt.document_num = shh.document_num \n"
                +" WHERE shh.doc_status = " + STDOC_STATUS_CANCELLED + " \n"
                +" AND dt.sl_status IN (" + getValidStatues() +") \n"
                +" AND shh.doc_type = " + STLDOC_TYPE_INVOICE + " AND shh.last_update > '"+ stlDocLastUpdate +"' \n"
                +" ) d WHERE d.RowRank = 1 ";
        PluginLog.debug(String.format("Cancelled invoices tracking Sql query: \n%s",sqlQuery));

        try {
            IOFactory ioFactory = session.getIOFactory();
            Table result = ioFactory.runSQL(sqlQuery);
            PluginLog.debug(String.format("Total number of row returned by cancelled invoice Sql query: %d",result.getRowCount()));

            Date lastUpdate = session.getServerTime();
            Integer personnelId = session.getUser().getId();
            String personnelName = session.getUser().getName();

            for(TableRow row : result.getRows()) {
                Integer documentNumber = row.getInt(DocumentTrackerTable.DOCUMENT_NUM.getColumnName());
                Integer docStatus = row.getInt(DocumentTrackerTable.DOC_STATUS.getColumnName());
                Integer lastDocStatus = row.getInt(DocumentTrackerTable.LAST_DOC_STATUS.getColumnName());
                Integer docVersion = row.getInt(DocumentTrackerTable.DOC_VERSION.getColumnName());
                Integer docHistoryId = row.getInt(DocumentTrackerTable.STLDOC_HDR_HIST_ID.getColumnName());
                String slStatus = row.getString(DocumentTrackerTable.SL_STATUS.getColumnName());
                String sapStatus = row.getString(DocumentTrackerTable.SAP_STATUS.getColumnName());
                String sentToCP = row.getString("sent_to_cp");
                
                DocumentTracker invoice = new DocumentTracker.DocumentTrackerBuilder(documentNumber, docStatus, docVersion)
                                            .SlStatus(slStatus)
                                            .SapStatus(sapStatus)
                                            .LastDocStatus(lastDocStatus)
                                            .DocHistoryId(docHistoryId)
                                            .LastUpdate(lastUpdate)
                                            .PersonnelId(personnelId)
                                            .PersonnelName(personnelName)
                                            .SentToCP(sentToCP)
                                            .build();
                invoices.add(invoice);
            }
        } catch(Exception exception) {
            throw new StampingException(String.format("An exception occurred while retrieving cancelled invoices. %s", exception.getMessage()), exception);
        }
        return invoices;
    }

    /**
     * Add or update invoice data in document tracking table.
     *
     * @param invoices the list of invoice tracking
     * @param newInvoices flag to indicate if new invoices
     */
    public void updateTrackingData(List<DocumentTracker> invoices, boolean newInvoices) {
        try{
        Table invoiceData = createTableStructure(DOC_TRACKING_TABLE);
        UserTable docTrackingTable = session.getIOFactory().getUserTable(DOC_TRACKING_TABLE);
        int invoiceCount = invoices.size();
        invoiceData.addRows(invoiceCount);

        int rowNum = 0;
        for (DocumentTracker invoice : invoices) {
            invoiceData.setInt(DocumentTrackerTable.DOCUMENT_NUM.getColumnName(), rowNum, invoice.getDocumentNum());
            invoiceData.setDate(DocumentTrackerTable.LAST_UPDATE.getColumnName(), rowNum, invoice.getlastUpdate());
            invoiceData.setInt(DocumentTrackerTable.PERSONNEL_ID.getColumnName(), rowNum, invoice.getPersonnelId()); 
            invoiceData.setInt(DocumentTrackerTable.DOC_STATUS.getColumnName(), rowNum, invoice.getDocStatus());
            invoiceData.setInt(DocumentTrackerTable.LAST_DOC_STATUS.getColumnName(), rowNum, invoice.getLastDocStatus());
            invoiceData.setInt(DocumentTrackerTable.DOC_VERSION.getColumnName(), rowNum, invoice.getDocVersion());
            invoiceData.setInt(DocumentTrackerTable.STLDOC_HDR_HIST_ID.getColumnName(), rowNum, invoice.getDocHistoryId());
            invoiceData.setString(DocumentTrackerTable.SL_STATUS.getColumnName(), rowNum, invoice.getSlStatus());
            invoiceData.setString(DocumentTrackerTable.SAP_STATUS.getColumnName(), rowNum, invoice.getSapStatus());
            rowNum++;
            PluginLog.debug(String.format("Doc tracking data #%d : %s",rowNum, invoice.toString()));
        }

        if(newInvoices) {
            docTrackingTable.insertRows(invoiceData);
            PluginLog.info(String.format("Total number of new invoices added in tracking table : %d",rowNum));
        }else {
            docTrackingTable.updateRows(invoiceData, DocumentTrackerTable.DOCUMENT_NUM.getColumnName());
            PluginLog.info(String.format("Total number of cancelled invoices updated in tracking table : %d",rowNum));
        }
        } catch (Exception ex) {
            throw new StampingException(String.format("An exception occurred while %s data into tracking table. %s"
                    ,newInvoices == true ? "inserting" : "updating", ex.getMessage()), ex);
        }
    }

    /**
     * Creates an empty table with the same structure as the user table.
     *
     * @param userTableName the user table name
     * @return the empty table structure
     */
    private Table createTableStructure(String userTableName) {
        Table userTable = session.getIOFactory().getUserTable(userTableName).getTableStructure();
        if (userTable == null) {
            String errorMessage = String.format("Error creating structure for user table. %s", userTableName);
            throw new StampingException(errorMessage);               
        }
        return userTable;
    }   
    
    /**
     * Gets the comma separated list of valid statues for cancellation.
     *
     * @return the valid statues
     */
    private String getValidStatues() {
        return  "'" + LedgerStatus.PENDING_SENT.getValue() + "','" + LedgerStatus.SENT.getValue()+ "'";
    }
    
}
