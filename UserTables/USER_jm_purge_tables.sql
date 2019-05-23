if exists
(
                select *
                from sysobjects
                where name = 'USER_jm_purge_tables' and type = 'U'
)
begin
                drop table USER_jm_purge_tables
end
go

create table dbo.USER_jm_purge_tables
(
      purge_name     					varchar(255),
      table_name     					varchar(255),
      date_column 					varchar(255),
      date_column_type 					varchar(255),
      where_clause 					varchar(255)
)

go

CREATE UNIQUE CLUSTERED INDEX [USER_jm_purge_tables_index] ON [dbo].[USER_jm_purge_tables]
(
	[purge_name] ASC,
	[table_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 95)
GO

grant update, insert, delete, select on dbo.USER_jm_purge_tables to olf_user,olf_user_manual
grant select on dbo.USER_jm_purge_tables to olf_readonly
go
