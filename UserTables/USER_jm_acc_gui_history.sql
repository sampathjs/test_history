IF OBJECT_ID('dbo.USER_jm_acc_gui_history' , 'U') IS NOT NULL
      DROP TABLE [dbo].[USER_jm_acc_gui_history]
GO
----


create table USER_jm_acc_gui_history
(
type varchar(255),
entry_no int,
value varchar(255),
personnel_id varchar(255),
last_update datetime
)

grant select, insert, update, delete on USER_jm_acc_gui_history to olf_user
grant select on USER_jm_acc_gui_history to olf_readonly
grant select, insert, update, delete on USER_jm_acc_gui_history to olf_user_manual

select * from USER_jm_acc_gui_history