IF OBJECT_ID('dbo.USER_jm_open_trading_position_cn' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_open_trading_position_cn] 

GO

CREATE TABLE [dbo].[USER_jm_open_trading_position_cn]
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
close_date int
)


GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_open_trading_position_cn] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_open_trading_position_cn] to olf_readonly;

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_open_trading_position_cn')
    AND name='USER_jm_open_trading_position_cn')
DROP INDEX USER_jm_open_trading_position_cn ON USER_jm_open_trading_position_cn
GO

CREATE CLUSTERED INDEX [USER_jm_open_trading_position_cn] ON [dbo].[USER_jm_open_trading_position_cn]
(
       [open_date] ASC
)


