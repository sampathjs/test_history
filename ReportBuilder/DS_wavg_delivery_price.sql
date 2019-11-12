-- SWAP DEALS NON-UNIFY
select
u.deal_num
,sum(u.delivery_price*u.delivery_volume)/sum(u.delivery_volume) as wavg_delivery_price
from 
USER_jm_trading_pnl_history u 
inner join ab_tran ab on u.deal_num = ab.deal_tracking_num and ab.current_flag = 1
inner join parameter pm on ab.ins_num = pm.ins_num and pm.param_seq_num = u.deal_leg
inner join ab_tran abt on ab.template_tran_num = abt.tran_num and abt.current_flag = 1
where
ab.toolset = 15
and pm.fx_flt = 1
and abt.reference != 'Coverage_Benchmarks'
group by
deal_num
-- SWAP DEALS UNIFY (BUG IN SAVING CORRECT DEAL_LEG TO PNL_HISTORY NEEDS ALTERNATIVE JOIN CONDITIONS
UNION
select
u.deal_num
,sum(u.delivery_price*u.delivery_volume)/sum(u.delivery_volume) as wavg_delivery_price
from 
USER_jm_trading_pnl_history u 
inner join ab_tran ab on u.deal_num = ab.deal_tracking_num and ab.current_flag = 1
inner join ab_tran abt on ab.template_tran_num = abt.tran_num and abt.current_flag = 1
inner join currency ccy on ccy.id_number = u.metal_ccy
where
ab.toolset = 15
and abt.reference = 'Coverage_Benchmarks'
and ccy.precious_metal = 1
group by
deal_num
-- NON-SWAP DEALS 
union 
select
u.deal_num
,sum(u.delivery_price*u.delivery_volume)/sum(u.delivery_volume) as wavg_delivery_price
from 
USER_jm_trading_pnl_history u 
inner join ab_tran ab on u.deal_num = ab.deal_tracking_num and ab.current_flag = 1
inner join parameter pm on ab.ins_num = pm.ins_num and pm.param_seq_num = u.deal_leg 
where
ab.toolset != 15
and pm.param_seq_num = 0
group by
deal_num
-- CN SWAP DEALS
union
select
u.deal_num
,sum(u.delivery_price*u.delivery_volume)/sum(u.delivery_volume) as wavg_delivery_price
from 
USER_jm_trading_pnl_history_cn u 
inner join ab_tran ab on u.deal_num = ab.deal_tracking_num and ab.current_flag = 1
where
ab.toolset = 15
group by
deal_num
-- CN NON SWAP DEALS
union 
select
u.deal_num
,sum(u.delivery_price*u.delivery_volume)/sum(u.delivery_volume) as wavg_delivery_price
from 
USER_jm_trading_pnl_history_cn u 
inner join ab_tran ab on u.deal_num = ab.deal_tracking_num and ab.current_flag = 1
inner join parameter pm on ab.ins_num = pm.ins_num and pm.param_seq_num = u.deal_leg 
where
ab.toolset != 15
and pm.param_seq_num = 0
group by
deal_num
