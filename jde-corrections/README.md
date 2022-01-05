# TODO

* exclude GL/SL entries where doc/deal num = 0
* retrieve run logs joined by the deal num when tran num = 0
* fix the issue that when deal amended, the GL status is still "sent"
* fix the issue that the flag for the new invoice for the amended deal is not updated 

# Release Steps

* change AB_AMEND_DEAL_COPIES_DOCINFO to false
* deploy the latest COMMON_JARS (currently COMMON_JARS_08_MTM_REPORTING)
* run SQL "jde_corrections.sql"
* import project "jde_corrections"
* import CMM package - scripts
* import CMM package - tasks
* import CMM package - queries
* import CMM package - op services
* delete exiting op services for trade amendments

# Configurations

* Op Service "Deal Amendment Checker"
* Tasks "JDE Corrections Generator UK/US/CN/HK"

# Test Steps

1. Amend/cancel the deals
2. Run TPM workflow "Auto Invoice Processing All Intraday_00"
3. Run TPM workflow "Automated Confirmation Processing"
4. Run TPM workflow "[UK|US|HK] Pre-EOD and Fixings"/"CN Pre-EOD" depends on the region
5. Run TPM workflow "Endur Accounting Interface [UK|US|HK|CN]" depends on the region
6. Run Task "JDE Corrections Generator [UK|US|HK|CN]" depends on the region

** TPM workflow "Auto Invoice Processing EOD *" is not needed because relevant steps are already included in "Endur Accounting Interface"

# Relevant Tasks/Scripts

* JM_AutomatedDocumentProcessing (for Task "BO_AutoInvoiceProcessing"/"BO_AutoConfirmationProcessing"/"BO_AutomatedInvoiceEOD_[UK|US|HK|CN]_Processing")
* StampDocuments (for Task "StampSalesLedger") - for new/cancelled invoices, called after BO processing, before reports
* StampTransactions (for Task "StampGeneralLedger") - only for locked deals
* GeneralLedgerStamping - after reports (via checking boundary tables)
* SalesLedgerStamping - after reports (via checking boundary tables)

# Relevant Reports

* Endur Accounting Feed - General Ledger Extract for UK
* Endur Accounting Feed - Sales Ledger Extract UK (query: "SL Extract UK SA - Current Month"/"SL Extract UK SA - Next Month")
* Endur Accounting Feed - General Ledger Extract for Shanghai
* Endur Accounting Feed - Sales Ledger Extract for Shanghai
* Endur Accounting Feed - General Ledger Extract (query: "GL Extract HK")
* Endur Accounting Feed - Sales Ledger Extract
* Endur Accounting Feed - General Ledger Extract for US
* Endur Accounting Feed - Sales Ledger Extract US

** HK might not work