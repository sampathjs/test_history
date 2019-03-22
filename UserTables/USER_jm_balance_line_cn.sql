BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_balance_line_cn' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_balance_line_cn] 

GO

create table USER_jm_balance_line_cn
(
id int,
balance_line varchar(255),
description varchar(255),
formula varchar(255),
display_order int,
display_in_drilldown varchar(255)
)

grant select, insert, update, delete on USER_jm_balance_line_cn to olf_user , olf_user_manual

grant select on USER_jm_balance_line_cn to olf_readonly


COMMIT;  
