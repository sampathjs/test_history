-- This statement will add a new column called "last_modified" to USER_jm_monthly_metal_statement
-- This is used to provide a last modified field for when the metal statements are actually run
DECLARE @SQL NVARCHAR(4000)

 IF COL_LENGTH('USER_jm_monthly_metal_statement','output_path') IS NULL
 BEGIN
	ALTER TABLE USER_jm_monthly_metal_statement ADD output_path varChar(255);
	SET @SQL='UPDATE USER_jm_monthly_metal_statement SET output_path = '' '' '
	EXEC(@SQL)
 END
 
 IF COL_LENGTH('USER_jm_monthly_metal_statement','output_files') IS NULL
 BEGIN
	ALTER TABLE USER_jm_monthly_metal_statement ADD output_files varChar(255);
	SET @SQL='UPDATE USER_jm_monthly_metal_statement SET output_files = '' '' '
	EXEC(@SQL)
 END

 IF COL_LENGTH('USER_jm_monthly_metal_statement','files_generated') IS NULL
 BEGIN
	ALTER TABLE USER_jm_monthly_metal_statement ADD files_generated [int] NULL;
	SET @SQL='UPDATE USER_jm_monthly_metal_statement SET files_generated = 0 '
	EXEC(@SQL)
 END
 
IF COL_LENGTH('USER_jm_monthly_metal_statement','last_modified') IS NULL
 BEGIN
	ALTER TABLE USER_jm_monthly_metal_statement ADD last_modified datetime;
	
	
	SET @SQL='UPDATE USER_jm_monthly_metal_statement SET last_modified = metal_statement_production_date'
	EXEC(@SQL)
 END
 
 IF COL_LENGTH('USER_jm_monthly_metal_statement','run_detail') IS NULL
 BEGIN
	ALTER TABLE USER_jm_monthly_metal_statement ADD run_detail varChar(255);
	
	
	SET @SQL='UPDATE USER_jm_monthly_metal_statement  SET run_detail = statement_period  '
	EXEC(@SQL)
 END
 
 
 
    


select * from USER_jm_monthly_metal_statement