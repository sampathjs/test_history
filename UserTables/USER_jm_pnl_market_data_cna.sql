IF OBJECT_ID('dbo.USER_jm_pnl_market_data_cna' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_pnl_market_data_cna] 

GO

CREATE TABLE [dbo].[USER_jm_pnl_market_data_cna]
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
usd_df float
);



GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_pnl_market_data_cna] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_pnl_market_data_cna] to olf_readonly;


IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_pnl_market_data_cna')
    AND name='USER_jm_pnl_market_data_cna')
DROP INDEX USER_jm_pnl_market_data_cna ON USER_jm_pnl_market_data_cna
GO
CREATE CLUSTERED INDEX USER_jm_pnl_market_data_cna ON [dbo].[USER_jm_pnl_market_data_cna] 
(
 [deal_num], [deal_leg] ASC
)
GO

select * from USER_jm_pnl_market_data_cn

select * from USER_jm_pnl_market_data_cna
 


