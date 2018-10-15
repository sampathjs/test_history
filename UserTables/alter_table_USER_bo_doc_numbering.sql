-- This statement will add a new column called "our_bu_id" to USER_bo_doc_numbering 
-- This is used to provide a flag for invoices that are to be sent to SAP

Select * from  USER_bo_doc_numbering 

IF COL_LENGTH('USER_bo_doc_numbering','our_bu_id') IS NULL
 BEGIN
	ALTER TABLE USER_bo_doc_numbering ADD our_bu_id int ;

	DECLARE @SQL NVARCHAR(4000)
	SET @SQL='UPDATE USER_bo_doc_numbering SET our_bu_id = 0 '
	EXEC(@SQL)


 END
 
 
 Select * from  USER_bo_doc_numbering 
 
grant select, insert, update, delete on USER_bo_doc_numbering to olf_user
GO
grant select, insert, update, delete on [dbo].[USER_bo_doc_numbering] to olf_user_manual
GO
grant select on USER_bo_doc_numbering to olf_readonly 
GO


 --  ALTER TABLE USER_bo_doc_numbering DROP COLUMN our_bu_id 