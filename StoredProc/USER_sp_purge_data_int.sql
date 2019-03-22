GO

/****** Object:  StoredProcedure [dbo].[USER_sp_purge_data_int]    Script Date: 07/03/2019 10:25:58 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE procedure [dbo].[USER_sp_purge_data_int]
          @table_name SYSNAME,
          @purge_from INT,
          @column_name SYSNAME
as
BEGIN

	DECLARE @sql_query_to_delete VARCHAR(500);
	DECLARE @criteria VARCHAR(500);
	

	BEGIN tran
	BEGIN TRY
	
	
	SET @criteria = 'FROM '+ @table_name +' WHERE '+@column_name + ' < ' + cast(@purge_from as varchar(100)) + ' AND '+@column_name +' <> ''''';

	
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

GRANT EXEC ON [USER_sp_purge_data_int] TO olf_user;
GRANT EXEC ON [USER_sp_purge_data_int] TO olf_user_manual;




