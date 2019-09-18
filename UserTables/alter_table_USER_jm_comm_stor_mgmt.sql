-- This statement will add a new column called "system_default" to USER_jm_comm_stor_mgmt
-- This is used to provide a last modified field for when the metal statements are actually run
DECLARE @SQL NVARCHAR(4000)

SELECT * FROM USER_jm_comm_stor_mgmt

 IF COL_LENGTH('USER_jm_comm_stor_mgmt','system_default') IS NULL
 BEGIN
	ALTER TABLE USER_jm_comm_stor_mgmt ADD system_default [int] NULL;
	SET @SQL='UPDATE USER_jm_comm_stor_mgmt SET system_default = 1 '
	EXEC(@SQL)
 END
 
SELECT * FROM USER_jm_comm_stor_mgmt