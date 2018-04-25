BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_dp_balance_hist' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_dp_balance_hist] 

GO

create Table [dbo].USER_jm_ap_dp_balance_hist 
( 
customer_id int,
open_date datetime,
close_date datetime,
prevdaydollar_balance float,
todaysdollar_balance float,
hist_user_id int,
hist_last_update datetime,
hist_update_type smallint
); 


grant select, insert, update, delete on [dbo].[USER_jm_ap_dp_balance_hist] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_dp_balance_hist] to olf_readonly

GO
 
COMMIT; 

