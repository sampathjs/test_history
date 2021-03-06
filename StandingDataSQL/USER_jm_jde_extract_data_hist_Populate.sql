BEGIN TRANSACTION

declare @userid int = 22735, @lastupdate datetime
set @lastupdate = getdate()
truncate table [dbo].[USER_jm_jde_extract_data_hist]
insert into  [dbo].[USER_jm_jde_extract_data_hist]
(
	   [entry_date]
      ,[entry_time]
      ,[deal_num]
      ,[fixings_complete]
      ,[from_currency]
      ,[to_currency]
      ,[delivery_date]
      ,[metal_volume_uom]
      ,[settlement_value]
      ,[spot_equiv_value]
      ,[interest]
      ,[uom]
      ,[metal_volume_toz]
      ,[trade_price]
      ,[spot_equiv_price]
      ,[conv_factor]
      ,[fx_fwd_rate]
      ,[hist_user_id]
      ,[hist_last_update]
      ,[hist_update_type]
)
select 
[entry_date]
      ,[entry_time]
      ,[deal_num]
      ,[fixings_complete]
      ,[from_currency]
      ,[to_currency]
      ,[delivery_date]
      ,[metal_volume_uom]
      ,[settlement_value]
      ,[spot_equiv_value]
      ,[interest]
      ,[uom]
      ,[metal_volume_toz]
      ,[trade_price]
      ,[spot_equiv_price]
      ,[conv_factor]
      ,[fx_fwd_rate]
      ,@userid [hist_user_id]
      ,@lastupdate [hist_last_update]
      ,0 [hist_update_type]
from [dbo].[USER_jm_jde_extract_data]


COMMIT;  

Select COUNT(entry_date) from USER_jm_jde_extract_data

Select COUNT(entry_date) from USER_jm_jde_extract_data_hist

Select * from USER_jm_jde_extract_data_hist