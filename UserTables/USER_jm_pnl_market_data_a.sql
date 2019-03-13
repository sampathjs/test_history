IF OBJECT_ID('dbo.USER_jm_pnl_market_data_a' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_pnl_market_data_a] 

GO

CREATE TABLE [dbo].[USER_jm_pnl_market_data_a]
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



GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_pnl_market_data_a] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_pnl_market_data_a] to olf_readonly;


IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_pnl_market_data_a')
    AND name='USER_jm_pnl_market_data_a')
DROP INDEX USER_jm_pnl_market_data_a ON USER_jm_pnl_market_data_a
GO
CREATE CLUSTERED INDEX USER_jm_pnl_market_data_a ON [dbo].[USER_jm_pnl_market_data_a] 
(
 [deal_num], [deal_leg] ASC
)
GO


select * from USER_jm_pnl_market_data_a
select * from USER_jm_pnl_market_data 


