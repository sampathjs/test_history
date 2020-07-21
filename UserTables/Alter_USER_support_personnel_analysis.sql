

DECLARE @SQL NVARCHAR(4000)


IF COL_LENGTH('USER_support_personnel_analysis','personnel_diff') IS NULL
 BEGIN
	ALTER TABLE USER_support_personnel_analysis ADD personnel_diff varchar(255);
	SET @SQL='UPDATE USER_support_personnel_analysis SET personnel_diff = ''No'' ';
	EXEC(@SQL)

 END

IF COL_LENGTH('USER_support_personnel_analysis','functional_diff') IS NULL
 BEGIN
	ALTER TABLE USER_support_personnel_analysis ADD functional_diff varchar(255);
	SET @SQL='UPDATE USER_support_personnel_analysis SET functional_diff = ''No'' ';
	EXEC(@SQL)

 END


Select * from USER_support_personnel_analysis






