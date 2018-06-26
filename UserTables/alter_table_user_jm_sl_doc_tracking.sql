-- This statement will add a new column called "sap_status" to user_jm_sl_doc_tracking
-- This is used to provide a flag for invoices that are to be sent to SAP

IF COL_LENGTH('USER_jm_sl_doc_tracking','sap_status') IS NULL
 BEGIN
ALTER TABLE USER_jm_sl_doc_tracking ADD sap_status VARCHAR(255);
 END