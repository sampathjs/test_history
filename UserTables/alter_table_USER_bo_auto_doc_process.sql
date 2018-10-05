-- This statement will add a new column called "processing_iteration" to USER_bo_auto_doc_process
-- This is used to provide a multi procssing support to the automation process

IF COL_LENGTH('USER_bo_auto_doc_process','processing_iteration') IS NULL
 BEGIN
	ALTER TABLE USER_bo_auto_doc_process ADD processing_iteration int;

	DECLARE @SQL NVARCHAR(4000)
	SET @SQL='UPDATE USER_bo_auto_doc_process SET processing_iteration = 0 '
	EXEC(@SQL)

 END


--  ALTER TABLE USER_bo_auto_doc_process DROP COLUMN processing_iteration 