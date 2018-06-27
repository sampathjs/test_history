--SQL script to create USER_jm_bt_out_sl_sap table
--DROP TABLE USER_jm_bt_out_sl_sap;

BEGIN TRANSACTION

IF OBJECT_ID('dbo.USER_jm_bt_out_sl_sap' , 'U') IS NULL
BEGIN

CREATE TABLE USER_jm_bt_out_sl_sap
(
extraction_id INT,
endur_doc_num INT,
endur_doc_status INT,
region NVARCHAR(255),
time_in DATETIME DEFAULT GETDATE(),
last_update DATETIME,
payload VARCHAR(MAX), -- clob in Oracle,
process_status CHAR(1) NOT NULL, -- N = New, E = Error (during file transmission), P = Processed
error_msg VARCHAR(4000),
message_key_1 VARCHAR(255),
message_key_2 VARCHAR(255),
message_key_3 VARCHAR(255), 
CONSTRAINT [PK_USER_jm_bt_out_sap_s1] PRIMARY KEY CLUSTERED (extraction_id, endur_doc_num, endur_doc_status),
CONSTRAINT fk_extraction_id_sap_sl FOREIGN KEY (extraction_id) REFERENCES USER_jm_ledger_extraction (extraction_id),
);

END

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_bt_out_sl_sap')
    AND name='bt_out_sl_sap_idx1')
DROP INDEX bt_out_sl_sap_idx1 ON USER_jm_bt_out_sl_sap
GO
CREATE nonclustered INDEX bt_out_sl_sap_idx1 ON USER_jm_bt_out_sl_sap (extraction_id);
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_bt_out_sl_sap')
    AND name='bt_out_sl_sap_idx3')
DROP INDEX bt_out_sl_sap_idx3 ON USER_jm_bt_out_sl_sap
GO
CREATE nonclustered INDEX bt_out_sl_sap_idx3 ON USER_jm_bt_out_sl_sap (process_status, region);
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_bt_out_sl_sap')
    AND name='bt_out_sl_sap_idx4')
DROP INDEX bt_out_sl_sap_idx4 ON USER_jm_bt_out_sl_sap
GO
CREATE nonclustered INDEX bt_out_sl_sap_idx4 ON USER_jm_bt_out_sl_sap (time_in, region); 
GO

exec master.dbo.AssignEndurDefaultUserTablePermissions 'OLEME04D','USER_jm_bt_out_sl_sap'

GRANT SELECT, INSERT, UPDATE, DELETE ON USER_jm_bt_out_sl_sap  TO olf_user; 
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_jm_bt_out_sl_sap TO olf_USER_manual ; 
GRANT SELECT ON USER_jm_bt_out_sl_sap TO olf_readonly;


COMMIT
--ROLLBACK TRANSACTION