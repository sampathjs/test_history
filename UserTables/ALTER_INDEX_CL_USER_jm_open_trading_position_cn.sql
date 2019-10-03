IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_open_trading_position_cn')
    AND name='CL_USER_jm_open_trading_position_cn')
DROP INDEX CL_USER_jm_open_trading_position_cn ON USER_jm_open_trading_position_cn
GO


CREATE CLUSTERED INDEX CL_USER_jm_open_trading_position_cn
ON USER_jm_open_trading_position_cn(extract_date, extract_time)
WITH (DATA_COMPRESSION=PAGE) 

------------
IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_open_trading_position')
    AND name='CL_USER_jm_open_trading_position')
DROP INDEX CL_USER_jm_open_trading_position ON USER_jm_open_trading_position
GO


CREATE CLUSTERED INDEX CL_USER_jm_open_trading_position
ON USER_jm_open_trading_position(extract_date, extract_time)
WITH (DATA_COMPRESSION=PAGE) 

