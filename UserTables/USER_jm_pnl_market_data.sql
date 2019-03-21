IF OBJECT_ID('dbo.USER_jm_pnl_market_data' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_pnl_market_data] 

GO

CREATE TABLE [dbo].[USER_jm_pnl_market_data]
(
	[entry_date] [int] NULL,
	[entry_time] [int] NULL,
	[deal_num] [int] NULL,
	[deal_leg] [int] NULL,
	[deal_pdc] [int] NULL,
	[deal_reset_id] [int] NULL,
	[trade_date] [int] NULL,
	[fixing_date] [int] NULL,
	[index_id] [int] NULL,
	[metal_ccy] [int] NULL,
	[spot_rate] [float] NULL,
	[fwd_rate] [float] NULL,
	[usd_df] [float] NULL
);



GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_pnl_market_data] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_pnl_market_data] to olf_readonly;


IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_pnl_market_data')
    AND name='USER_jm_pnl_market_data')
DROP INDEX USER_jm_pnl_market_data ON USER_jm_pnl_market_data
GO
CREATE CLUSTERED INDEX USER_jm_pnl_market_data ON [dbo].[USER_jm_pnl_market_data] 
(
 [deal_num], [deal_leg] ASC
)
GO



 


