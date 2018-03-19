USE [END_UNI01]
GO
/****** Object:  StoredProcedure [dbo].[USER_sp_archive_data]    Script Date: 19/03/2018 11:47:32 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER procedure [dbo].[USER_sp_archive_data]
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
       
       
       SET @retain_days = @retain_days * -1;
       SELECT @business_date_in_stored_procedure=business_date FROM system_dates;
       SET @retain_date = DATEADD( dd, @retain_days, @business_date_in_stored_procedure );

       SET @criteria = 'FROM '+ @table_name +' WHERE '+@column_name + ' < ''' +CONVERT(VARCHAR(20),@retain_date)+ ''' AND '+@column_name +' <> '''''; 

       begin tran
              SET @sql_query_to_archive = 'INSERT INTO '+@archive_table
                     +' SELECT * '+@criteria;
              exec(@sql_query_to_archive);
       

              SET @sql_query_to_delete = 'DELETE '+@criteria;
              exec(@sql_query_to_delete);
       commit tran
END
