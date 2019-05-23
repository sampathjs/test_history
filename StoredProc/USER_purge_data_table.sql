if exists
(
      select *
      from sysobjects
      where name = 'USER_purge_data_table' and type = 'P'
)
begin
      drop proc USER_purge_data_table
end
go

create procedure dbo.USER_purge_data_table
	@table_name		varchar(255),
	@where_clause		varchar(255),
	@date_column 		varchar(255),
	@date_column_type 	varchar(255),
	@purge_date		varchar(255)
as
	declare @deleted_count int
	declare @SqlStr nvarchar(256)
	declare @ParamStr nvarchar(256)
	declare @purge_date_int int
	declare @remaining_count int
	declare @err int, @rcount int
	declare @start_time datetime
	declare @end_time datetime

	begin tran

	select @start_time = GETDATE()

	set @ParamStr = N' @rc int output'

 	if @where_clause is not null AND LEN(@where_clause) > 1  begin
		if @date_column_type = 'datetime' begin
			set @SqlStr = N'delete from dbo.' + @table_name + ' where ' + @where_clause + ' and ' +  @date_column + ' != ''''' + ' and ' +  @date_column + ' <= '''+@purge_date+'''; set @rc = @@ROWCOUNT'
			end
		else if @date_column_type = 'string' begin
			set @SqlStr = N'delete from dbo.' + @table_name + ' where ' + @where_clause + ' and ' +  @date_column + ' <= '+@purge_date+'; set @rc = @@ROWCOUNT'
			end
		else if @date_column_type = 'int' begin
			select @purge_date_int = CONVERT(INT,@purge_date)
			set @SqlStr = N'delete from dbo.' + @table_name + ' where ' + @where_clause + ' and ' +  @date_column + ' <= '+@purge_date_int+'; set @rc = @@ROWCOUNT'
			end
	end
	else begin
		if @date_column_type = 'datetime' begin
			set @SqlStr = N'delete from dbo.' + @table_name + ' where ' +  @date_column + ' != ''''' + ' and ' + @date_column + ' <= '''+@purge_date+'''; set @rc = @@ROWCOUNT'
			end
		else if @date_column_type = 'string' begin
			set @SqlStr = N'delete from dbo.' + @table_name + ' where ' + @date_column + ' <= '+@purge_date+'; set @rc = @@ROWCOUNT'
			end
		else if @date_column_type = 'int' begin
			select @purge_date_int = CONVERT(INT,@purge_date)
			set @SqlStr = N'delete from dbo.' + @table_name + ' where ' + @date_column + ' <= '+@purge_date_int+'; set @rc = @@ROWCOUNT'
			end
	end
	
	execute sp_executesql
    		@SqlStr,
    		@ParamStr, 
    		@rc = @deleted_count output
		
       	select	@err=@@error, @rcount=@@rowcount
        if @err != 0 begin
                rollback tran
       		raiserror ('25002:SQL Transaction Error', 16, -1 )
       		return -1
       	end
		    
	commit tran

go

GRANT EXEC ON USER_purge_data_table TO olf_user;
GRANT EXEC ON USER_purge_data_table TO olf_user_manual;      

go


