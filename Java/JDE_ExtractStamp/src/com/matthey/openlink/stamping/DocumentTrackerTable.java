package com.matthey.openlink.stamping;

/**
 * The Enum DocumentTrackerTable.
 */
public enum DocumentTrackerTable {
    DOCUMENT_NUM("document_num"),
    SL_STATUS("sl_status"),
    LAST_UPDATE("last_update"),
    PERSONNEL_ID("personnel_id"),
    DOC_STATUS("doc_status"),
    LAST_DOC_STATUS("last_doc_status"),
    DOC_VERSION("doc_version"),
    STLDOC_HDR_HIST_ID("stldoc_hdr_hist_id"),
    SAP_STATUS("sap_status");

    private final static String DOC_TRACKING_TABLE = "USER_jm_sl_doc_tracking";
    private String columnName;

    /**
     * Instantiates  Doc Tracking user table columns.
     *
     * @param columnName the column name
     */
    DocumentTrackerTable(final String columnName) {
        this.columnName = columnName;    
    }

    /**
     * Gets the column name.
     *
     * @return the column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Gets the Doc Tracking user table name.
     *
     * @return the table name
     */
    public static String getTableName(){
        return DOC_TRACKING_TABLE;
    }
}
