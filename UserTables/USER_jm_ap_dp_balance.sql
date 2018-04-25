BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_dp_balance' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_dp_balance] 

GO

create Table [dbo].USER_jm_ap_dp_balance 
( 
customer_id int,
open_date datetime,
close_date datetime,
prevdaydollar_balance float,
todaysdollar_balance float

); 


grant select, insert, update, delete on [dbo].[USER_jm_ap_dp_balance] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_dp_balance] to olf_readonly

GO

CREATE CLUSTERED INDEX [USER_jm_ap_dp_balance] ON [dbo].[USER_jm_ap_dp_balance]
(
       [customer_id] ASC
)
GO

 
COMMIT; 

