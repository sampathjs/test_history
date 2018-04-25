BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_dp_margin_percn' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_dp_margin_percn] 

GO

create Table [dbo].USER_jm_ap_dp_margin_percn 
( 

customer_id int,
metal varchar(255),
min_vol_kgs float,
max_vol_kgs float,
percentage float,
price_type varchar(255),
start_date datetime,
end_date datetime


); 


grant select, insert, update, delete on [dbo].[USER_jm_ap_dp_margin_percn] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_dp_margin_percn] to olf_readonly

GO

CREATE CLUSTERED INDEX [USER_jm_ap_dp_margin_percn] ON [dbo].[USER_jm_ap_dp_margin_percn]
(
       [customer_id] ASC
)
GO

 
COMMIT; 

