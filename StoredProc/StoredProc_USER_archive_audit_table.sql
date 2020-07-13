--This procedure moves data from table_name to archive_table
IF OBJECT_ID('USER_archive_audit_table', 'P') IS NOT NULL
       DROP PROC USER_archive_audit_table

GO
CREATE procedure [dbo].[USER_archive_audit_table]
          @table_name SYSNAME,
          @archive_table SYSNAME,
          @retain_days INT,
          @column_name SYSNAME
		  
as
BEGIN
	DECLARE @retain_date DATETIME;
	DECLARE @sql_query_to_delete VARCHAR(500);
	DECLARE @sql_query_to_archive VARCHAR(500);
	DECLARE @criteria VARCHAR(500);
	DECLARE @business_date_in_stored_procedure DATETIME;

	BEGIN tran
	BEGIN TRY

	SET @retain_days = @retain_days * -1;
	SELECT @business_date_in_stored_procedure=business_date FROM system_dates;
	SET @retain_date = DATEADD( dd, @retain_days, @business_date_in_stored_procedure );

	SET @criteria = 'FROM '+ @table_name +' WHERE '+@column_name + ' < ''' +CONVERT(VARCHAR(20),@retain_date)+ ''' AND LEN(explanation) >0  ';    

	SET @sql_query_to_archive = 'INSERT INTO '+@archive_table +' SELECT * '+@criteria;
	exec(@sql_query_to_archive);


	SET @sql_query_to_delete = 'DELETE '+@criteria;
	exec(@sql_query_to_delete);

	END TRY
	BEGIN CATCH
	IF @@TRANCOUNT > 0
		ROLLBACK TRAN
	DECLARE @ErrorMessage NVARCHAR(4000);
	DECLARE @ErrorSeverity INT;
	DECLARE @ErrorState INT;
 
	SELECT
		@ErrorMessage=ERROR_MESSAGE(),
		@ErrorSeverity=ERROR_SEVERITY(),
		@ErrorState=ERROR_STATE();

	RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
	END CATCH
       
END
IF @@TRANCOUNT > 0
	COMMIT TRAN

GO

GRANT EXEC ON USER_archive_audit_table TO olf_user;
GRANT EXEC ON USER_archive_audit_table TO olf_user_manual;