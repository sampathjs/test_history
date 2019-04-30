IF OBJECT_ID('dbo.USER_jm_trading_pnl_history_cn' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_trading_pnl_history_cn] 

GO

CREATE TABLE [dbo].[USER_jm_trading_pnl_history_cn]
(
extract_id int,
extract_date int,
extract_time int,
bunit int,
metal_ccy int,
open_date int,
open_volume float,
open_price float,
open_value float,
deal_date int,
deal_num int,
deal_leg int,
deal_pdc int,
deal_reset_id int,
buy_sell int,
delivery_volume float,
delivery_price float,
delivery_value float,
deal_profit float,
accum_profit float,
close_date int,
close_volume float,
close_price float,
close_value float
)



GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_trading_pnl_history_cn] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_trading_pnl_history_cn] to olf_readonly;

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_trading_pnl_history_cn')
    AND name='USER_jm_trading_pnl_history_cn')
DROP INDEX USER_jm_trading_pnl_history_cn ON USER_jm_trading_pnl_history_cn
GO


CREATE CLUSTERED INDEX [USER_jm_trading_pnl_history_cn] ON [dbo].[USER_jm_trading_pnl_history_cn]
(
       [deal_date] ASC
)

GO




