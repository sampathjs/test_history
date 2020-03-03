------------------------------------------------------------------
--- UPDATE USER_jm_open_trading_position PNL for EUR SR#342535
---------------------------------------------------------------------------

UPDATE USER_jm_open_trading_position SET open_volume = 3471704.70163055, open_price = 1.104293
WHERE extract_date = 43891 AND bunit = 20006 AND metal_ccy = 51 AND open_date = 43860 AND extract_time=(
   SELECT MAX(extract_time) FROM USER_jm_open_trading_position
   WHERE extract_date = 43891 AND bunit = 20006 AND metal_ccy = 51 AND open_date = 43860 
);