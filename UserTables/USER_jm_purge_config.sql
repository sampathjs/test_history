if exists
(
                select *
                from sysobjects
                where name = 'USER_jm_purge_config' and type = 'U'
)
begin
                drop table USER_jm_purge_config
end
go

create table dbo.USER_jm_purge_config
(
      purge_name     				varchar(255),
      category 						varchar(64),
      sort_order					int,
      purge_type 					varchar(64),
      stored_procedure 				varchar(255),
      days_to_retain 				int,
      active_flag					int
)
go

grant update, insert, delete, select on dbo.USER_jm_purge_config to olf_user,olf_user_manual
grant select on dbo.USER_jm_purge_config to olf_readonly
go
