IF OBJECT_ID('dbo.USER_jm_pnl_market_data_cn_hist' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_pnl_market_data_cn_hist] 

GO

CREATE TABLE [dbo].[USER_jm_pnl_market_data_cn_hist]
(
entry_date int,
entry_time int,
deal_num int,
deal_leg int,
deal_pdc int,
deal_reset_id int,
trade_date int,
fixing_date int,
index_id int,
metal_ccy int,
spot_rate float,
fwd_rate float,
usd_df float,
hist_user_id int,
hist_last_update datetime,
hist_update_type int
)

GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_pnl_market_data_cn_hist] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_pnl_market_data_cn_hist] to olf_readonly;


IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_pnl_market_data_cn_hist')
    AND name='USER_jm_pnl_market_data_cn_hist')
DROP INDEX USER_jm_pnl_market_data_cn_hist ON USER_jm_pnl_market_data_cn_hist
GO
CREATE CLUSTERED INDEX USER_jm_pnl_market_data_cn_hist ON [dbo].[USER_jm_pnl_market_data_cn_hist] (
 [entry_date], [entry_time] ASC
);
GO




