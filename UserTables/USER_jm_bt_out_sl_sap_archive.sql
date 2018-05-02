--This script creates archive table for sales ledger sap
BEGIN TRANSACTION

IF OBJECT_ID('dbo.USER_jm_bt_out_sl_sap_archive' , 'U') IS NULL
BEGIN

CREATE TABLE USER_jm_bt_out_sl_sap_archive
(
extraction_id INT,
endur_doc_num INT,
endur_doc_status INT,
region NVARCHAR(255),
time_in DATETIME,
last_update DATETIME,
payload VARCHAR(MAX), -- clob in Oracle,
process_status CHAR(1) NOT NULL, -- N = New, E = Error (during file transmission), P = Processed
error_msg VARCHAR(4000),
message_key_1 VARCHAR(255),
message_key_2 VARCHAR(255),
message_key_3 VARCHAR(255), 
CONSTRAINT [PK_USER_jm_bt_out_sl_sap_archive] PRIMARY KEY CLUSTERED (extraction_id, endur_doc_num, endur_doc_status),
);

END

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_bt_out_sl_sap_archive')
    AND name='USER_jm_bt_out_sls_archive_idx1')
DROP INDEX USER_jm_bt_out_sls_archive_idx1 ON USER_jm_bt_out_sl_sap_archive
GO
CREATE nonclustered INDEX USER_jm_bt_out_sls_archive_idx1 ON USER_jm_bt_out_sl_sap_archive (extraction_id);
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_bt_out_sl_sap_archive')
    AND name='USER_jm_bt_out_sls_archive_idx3')
DROP INDEX USER_jm_bt_out_sls_archive_idx3 ON USER_jm_bt_out_sl_sap_archive
GO
CREATE nonclustered INDEX USER_jm_bt_out_sls_archive_idx3 ON USER_jm_bt_out_sl_sap_archive (process_status, region);
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_bt_out_sl_sap_archive')
    AND name='USER_jm_bt_out_sls_archive_idx4')
DROP INDEX USER_jm_bt_out_sls_archive_idx4 ON USER_jm_bt_out_sl_sap_archive
GO
CREATE nonclustered INDEX USER_jm_bt_out_sls_archive_idx4 ON USER_jm_bt_out_sl_sap_archive (time_in, region);
GO

exec master.dbo.AssignEndurDefaultUserTablePermissions 'END_UNI01','USER_jm_bt_out_sl_sap_archive'

GRANT SELECT, INSERT, UPDATE, DELETE ON USER_jm_bt_out_sl_sap_archive  TO olf_user; 
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_jm_bt_out_sl_sap_archive TO olf_user_manual ; 
GRANT SELECT ON USER_jm_bt_out_sl_sap_archive TO olf_readonly;

COMMIT;
--Rollback Transaction