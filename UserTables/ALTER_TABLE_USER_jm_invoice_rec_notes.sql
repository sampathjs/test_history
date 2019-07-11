-- This statement will add a new column called "requested_by, comment_added_by, ticket_numbers, timestamp  to user_jm_invoice_rec_notes
-- This is used to provide a multi procssing support to the automation process


Select * from user_jm_invoice_rec_notes

IF COL_LENGTH('user_jm_invoice_rec_notes','requested_by') IS NULL
 BEGIN
	ALTER TABLE user_jm_invoice_rec_notes ADD requested_by varchar(255);

	DECLARE @SQL NVARCHAR(4000)
	SET @SQL='UPDATE user_jm_invoice_rec_notes SET requested_by = '' '' '
	EXEC(@SQL)

 END

IF COL_LENGTH('user_jm_invoice_rec_notes','comment_added_by') IS NULL
 BEGIN
	ALTER TABLE user_jm_invoice_rec_notes ADD comment_added_by varchar(255);
	
	SET @SQL='UPDATE user_jm_invoice_rec_notes SET comment_added_by = '' '''
	EXEC(@SQL)

 END

IF COL_LENGTH('user_jm_invoice_rec_notes','ticket_numbers') IS NULL
 BEGIN
	ALTER TABLE user_jm_invoice_rec_notes ADD ticket_numbers varchar(255);
	
	SET @SQL='UPDATE user_jm_invoice_rec_notes SET ticket_numbers = '' '' '
	EXEC(@SQL)

 END

IF COL_LENGTH('user_jm_invoice_rec_notes','timestamp') IS NULL
 BEGIN
	ALTER TABLE user_jm_invoice_rec_notes ADD timestamp datetime;
		
	SET @SQL='UPDATE user_jm_invoice_rec_notes SET timestamp = GetDate() '
	EXEC(@SQL)

 END

 Select * from user_jm_invoice_rec_notes

--  ALTER TABLE user_jm_invoice_rec_notes DROP COLUMN requested_by 
--  ALTER TABLE user_jm_invoice_rec_notes DROP COLUMN comment_added_by 
--  ALTER TABLE user_jm_invoice_rec_notes DROP COLUMN ticket_numbers 
--  ALTER TABLE user_jm_invoice_rec_notes DROP COLUMN timestamp 
