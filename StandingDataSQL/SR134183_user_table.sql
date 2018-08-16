--use OLEME09T

BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_lbma_log' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_lbma_log] 

GO


create Table [dbo].[USER_jm_lbma_log] 
( 
deal_num int,
tran_num int,
price float,
qty float,
last_update DATETIME
); 


grant select, insert, update, delete on [dbo].[USER_jm_lbma_log] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_lbma_log] to olf_readonly

 
COMMIT;  

--exec master.dbo.AssignEndurDefaultUserTablePermissions 'OLEME00P','USER_jm_lbma_log'

