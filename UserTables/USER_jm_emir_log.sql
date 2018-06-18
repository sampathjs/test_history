--use OLEME09T

BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_emir_log' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_emir_log] 

GO


create Table [dbo].USER_jm_emir_log 
( 
deal_num int,
tran_num int,
price float,
lots int,
lot_size int,
message_ref varchar(255),
err_desc varchar(255),
last_update DATETIME
);

CREATE INDEX idx_emir ON USER_jm_emir_log (deal_num,last_update); 


grant select, insert, update, delete on [dbo].[USER_jm_emir_log] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_emir_log] to olf_readonly

 
COMMIT;  

--exec master.dbo.AssignEndurDefaultUserTablePermissions 'OLEME09T','USER_jm_emir_log'