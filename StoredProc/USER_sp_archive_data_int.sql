--This procedure moves data from table_name to archive_table
IF OBJECT_ID('USER_sp_archive_data_int', 'P') IS NOT NULL
       DROP PROC USER_sp_archive_data_int

GO
CREATE procedure [dbo].[USER_sp_archive_data_int]
          @table_name SYSNAME,
          @archive_table SYSNAME,
          @purge_from INT,
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
       

	   SET @criteria = 'FROM '+ @table_name +' WHERE '+@column_name + ' < ' + cast(@purge_from as varchar(100)) + ' AND '+@column_name +' <> ''''';
	   
            SET @sql_query_to_archive = 'INSERT INTO '+@archive_table 
                +' SELECT * '+@criteria;
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

GRANT EXEC ON USER_sp_archive_data_int TO olf_user;
GRANT EXEC ON USER_sp_archive_data_int TO olf_user_manual;