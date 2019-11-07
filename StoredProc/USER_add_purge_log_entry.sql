if exists
(
      select *
      from sysobjects
      where name = 'USER_add_purge_log_entry' and type = 'P'
)
begin
      drop proc USER_add_purge_log_entry
end
go

create procedure dbo.USER_add_purge_log_entry
	@purge_name			varchar(255),
	@table_name			varchar(255),
	@days_to_retain			int,
	@purged_from_date		datetime,
	@purge_elapsed_time_secs	int,
	@number_rows_purged		int,
	@remaining_rows			int

as
	insert into USER_jm_purge_log
		select 
			@purge_name, 
			@table_name, 
			@days_to_retain, 
			date_column, 
			CURRENT_USER, 
			GETDATE(), 
			@purged_from_date, 
			@purge_elapsed_time_secs, 
			@number_rows_purged, 
			@remaining_rows
		from 
			USER_jm_purge_tables 
		where 
			purge_name = @purge_name and 
			table_name = @table_name
go

GRANT EXEC ON USER_add_purge_log_entry TO olf_user;
GRANT EXEC ON USER_add_purge_log_entry TO olf_user_manual;      

go

