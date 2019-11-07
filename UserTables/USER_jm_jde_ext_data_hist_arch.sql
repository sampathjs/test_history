
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_jde_ext_data_hist_arch' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_jde_ext_data_hist_arch] 

GO


create Table [dbo].[USER_jm_jde_ext_data_hist_arch] 
( 
      [entry_date] [int] NULL,
       [entry_time] [int] NULL,
       [deal_num] [int] NULL,
       [fixings_complete] [varchar](255) NULL,
       [from_currency] [int] NULL,
       [to_currency] [int] NULL,
       [delivery_date] [int] NULL,
       [metal_volume_uom] [float] NULL,
       [settlement_value] [float] NULL,
       [spot_equiv_value] [float] NULL,
       [interest] [float] NULL,
       [uom] [int] NULL,
       [metal_volume_toz] [float] NULL,
       [trade_price] [float] NULL,
       [spot_equiv_price] [float] NULL,
       [conv_factor] [float] NULL,
       [fx_fwd_rate] [float] NULL,
       [hist_user_id] [int] NULL,
       [hist_last_update] [datetime] NULL,
       [hist_update_type] [smallint] NULL
) ON [PRIMARY]


grant select, insert, update, delete on [dbo].[USER_jm_jde_ext_data_hist_arch] to olf_user, olf_user_manual 

grant select on [dbo].[USER_jm_jde_ext_data_hist_arch] to olf_readonly



 
COMMIT;  



Select * from USER_jm_jde_ext_data_hist_arch





