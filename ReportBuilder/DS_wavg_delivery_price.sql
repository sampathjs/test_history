-- SWAP DEALS NON-UNIFY
SELECT 	u.deal_num, SUM(u.delivery_price*u.delivery_volume)/SUM(u.delivery_volume) AS wavg_delivery_price
    FROM USER_jm_trading_pnl_history u 
	INNER JOIN ab_tran ab ON (u.deal_num = ab.deal_tracking_num AND ab.current_flag = 1)
	INNER JOIN parameter pm ON (ab.ins_num = pm.ins_num AND pm.param_seq_num = u.deal_leg)
	INNER JOIN ab_tran abt ON (ab.template_tran_num = abt.tran_num AND abt.current_flag = 1)
WHERE ab.toolset = 15	-- Swap
	AND pm.fx_flt = 1	-- Float
	AND abt.reference != 'Coverage_Benchmarks'
GROUP BY deal_num
-- SWAP DEALS UNIFY (BUG IN SAVING CORRECT DEAL_LEG TO PNL_HISTORY NEEDS ALTERNATIVE JOIN CONDITIONS
UNION
SELECT
	u.deal_num,SUM(u.delivery_price*u.delivery_volume)/SUM(u.delivery_volume) AS wavg_delivery_price
FROM 
	USER_jm_trading_pnl_history u 
	INNER JOIN ab_tran ab ON (u.deal_num = ab.deal_tracking_num AND ab.current_flag = 1)
	INNER JOIN ab_tran abt ON (ab.template_tran_num = abt.tran_num AND abt.current_flag = 1)
	INNER JOIN currency ccy ON (ccy.id_number = u.metal_ccy)
WHERE
	ab.toolset = 15
	AND abt.reference = 'Coverage_Benchmarks'
	AND ccy.precious_metal = 1
GROUP BY
	deal_num
-- NON-SWAP DEALS 
UNION 
SELECT
	u.deal_num
	,SUM(u.delivery_price*u.delivery_volume)/SUM(u.delivery_volume) AS wavg_delivery_price
FROM 
	USER_jm_trading_pnl_history u 
	INNER JOIN ab_tran ab ON (u.deal_num = ab.deal_tracking_num AND ab.current_flag = 1)
	INNER JOIN parameter pm ON (ab.ins_num = pm.ins_num AND pm.param_seq_num = u.deal_leg)
WHERE
	ab.toolset != 15 -- Swap
	AND pm.param_seq_num = 0
GROUP BY
	deal_num
-- CN SWAP DEALS
UNION
SELECT
	u.deal_num
	,SUM(u.delivery_price*u.delivery_volume)/SUM(u.delivery_volume) AS wavg_delivery_price
FROM 
	USER_jm_trading_pnl_history_cn u 
	INNER JOIN ab_tran ab ON (u.deal_num = ab.deal_tracking_num AND ab.current_flag = 1)
WHERE
	ab.toolset = 15 -- Swap
GROUP BY
	deal_num
-- CN NON SWAP DEALS
UNION 
SELECT
	u.deal_num
	,SUM(u.delivery_price*u.delivery_volume)/SUM(u.delivery_volume) AS wavg_delivery_price
FROM 
	USER_jm_trading_pnl_history_cn u 
	INNER JOIN ab_tran ab ON (u.deal_num = ab.deal_tracking_num AND ab.current_flag = 1)
	INNER JOIN parameter pm ON (ab.ins_num = pm.ins_num AND pm.param_seq_num = u.deal_leg)
WHERE
	ab.toolset != 15 -- Swap
	AND pm.param_seq_num = 0
GROUP BY deal_num
