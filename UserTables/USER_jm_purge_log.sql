if exists
(
                select *
                from sysobjects
                where name = 'USER_jm_purge_log' and type = 'U'
)
begin
                drop table USER_jm_purge_log
end
go

create table dbo.USER_jm_purge_log
(
      purge_name     					varchar(255),
      table_name     					varchar(255),
      days_to_retain 					int,
      date_column 					varchar(255),
      last_ran_by_username				varchar(255),
      last_update					datetime,
      purged_from_date					datetime,
      purge_elapsed_time_secs				int,
      number_rows_purged				int,
      remaining_rows					int
)

go

CREATE UNIQUE CLUSTERED INDEX [USER_jm_purge_log_idx] ON [dbo].[USER_jm_purge_log]
(
	[last_update] ASC,
	[purge_name] ASC,
	[table_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 95)
GO


grant update, insert, delete, select on dbo.USER_jm_purge_log to olf_user,olf_user_manual
grant select on dbo.USER_jm_purge_log to olf_readonly
go
