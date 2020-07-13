	DECLARE @SQL NVARCHAR(4000)

IF COL_LENGTH('USER_support_change_audit','auto_filled') IS NULL
 BEGIN
	ALTER TABLE USER_support_change_audit ADD auto_filled varchar(255);
	SET @SQL='UPDATE USER_support_change_audit SET auto_filled = ''NA'' ';
	EXEC(@SQL)

 END



Select * from USER_support_change_audit





