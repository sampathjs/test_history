--This procedure deletes data from table_name
IF OBJECT_ID('USER_purge_jm_open_tp', 'P') IS NOT NULL
	DROP PROC USER_purge_jm_open_tp

GO
CREATE procedure [dbo].[USER_purge_jm_open_tp]
          @table_name SYSNAME,
          @extract_date INT,
          @extract_time INT,
		  @archive_table_name SYSNAME
as
BEGIN

	DECLARE @sql_query_to_delete VARCHAR(500);
	DECLARE @criteria VARCHAR(500);


	BEGIN tran
		BEGIN TRY

			SET @criteria = 'FROM '+ @table_name +' WHERE extract_date = ' + CONVERT(VARCHAR(20),@extract_date)  + ' AND extract_time = '+ CONVERT(VARCHAR(20),@extract_time);
		
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

GRANT EXEC ON USER_purge_jm_open_tp TO olf_user;
GRANT EXEC ON USER_purge_jm_open_tp TO olf_user_manual;