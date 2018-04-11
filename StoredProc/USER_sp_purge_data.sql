--This procedure deletes data from table_name
IF OBJECT_ID('USER_sp_purge_data', 'P') IS NOT NULL
	DROP PROC USER_sp_purge_data

GO
CREATE procedure [dbo].[USER_sp_purge_data]
          @table_name SYSNAME,
          @retain_days INT,
          @column_name SYSNAME
as
BEGIN
	DECLARE @retain_date DATETIME;
	DECLARE @sql_query_to_delete VARCHAR(500);
	DECLARE @criteria VARCHAR(500);
	DECLARE @business_date_in_stored_procedure DATETIME;
	
	BEGIN tran
	BEGIN TRY
	
	SET @retain_days = @retain_days * -1;
	SELECT @business_date_in_stored_procedure=business_date FROM system_dates;
	SET @retain_date = DATEADD( dd, @retain_days, @business_date_in_stored_procedure );

	SET @criteria = 'FROM '+ @table_name +' WHERE '+@column_name + ' < ''' +CONVERT(VARCHAR(20),@retain_date)+ ''' AND '+@column_name +' <> ''''';

	
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
commit tran
GO

GRANT EXEC ON USER_sp_purge_data TO olf_user;
GRANT EXEC ON USER_sp_purge_data TO olf_user_manual;