SELECT *
 FROM (SELECT    dxr1.deal_tracking_num deal_tracking_num  , dxr1.toolset toolset  , dxr1.ins_num, dxr1.currency Metal,
dxr1.internal_bunit internal_bunit  , dxr1.internal_portfolio internal_portfolio  ,
dxr1.external_bunit external_bunit  , dxr1.tran_status tran_status  ,
dxr1.buy_sell buy_sell  , dxr1.position Position , dxr1.trade_date trade_date  , 
 dxr1.ins_type ins_type  , dxr1.internal_contact internal_contact  , dxr19.contract_code contract_code  ,
dxr21.para_position para_position, dxr22.para_position para_position1, dxr21.unit unit, dxr20.unit ins_para_unit, dxr21.pymt_type pymt_type  , dxr21.event_date value_date  , 
 dxr22.currency currency  , dxr22.para_position settlement_amount  , dxr22.event_date value_date_1   , dxr26.value Form  , 
 dxr28.value JM_Group  , dxr29.currency_convention currency_convention  , dxr29.fx_rate_mid fx_rate_mid  , dxr33.value is_funding  ,
dxr34.value End_User  , dxr35.value Good_Home  , dxr20.notnl notional , '$$StartDate$$ to $$EndDate$$' as Period
FROM ab_tran dxr1  INNER JOIN 
$$QUERY_RESULT_TABLE$$ q ON (dxr1.tran_num = q.query_result and q.unique_id = ($$QUERY_RESULT_ID$$))  
LEFT OUTER JOIN misc_ins dxr19 ON (dxr19.ins_num = dxr1.ins_num)  LEFT 
OUTER JOIN ab_tran_event dxr21 ON (dxr21.tran_num = dxr1.tran_num and (dxr21.event_type = 14) and dxr21.ins_para_seq_num=0 $$Cflow_Type$$)  
LEFT 
OUTER JOIN ab_tran_event dxr22 ON (dxr22.tran_num = dxr1.tran_num 
and (dxr22.event_type = 14)  
and dxr22.ins_para_seq_num=1)  LEFT 
OUTER JOIN ab_tran_info dxr26 ON (dxr26.tran_num = dxr1.tran_num 
AND ( dxr26.type_id = 20014 $$FormVal$$))  LEFT 
OUTER JOIN party_info dxr28 ON (dxr28.party_id =  dxr1.external_lentity 
AND (dxr28.type_id=20019  $$Jm_GroupVal$$ ))  LEFT 
OUTER JOIN idx_historical_fx_rates dxr29 ON (dxr29.fx_rate_date = dxr1.trade_date 
and (dxr29.currency_id=dxr22.currency))  
INNER JOIN ab_tran_info dxr33 ON (dxr33.tran_num = dxr1.tran_num 
AND ( dxr33.type_id = 20067 $$Is_FundingVal$$) 
AND (dxr1.internal_bunit in ($$InternalUnit$$) ))  LEFT 
OUTER JOIN ab_tran_info dxr34 ON (dxr34.tran_num = dxr1.tran_num 
AND ( dxr34.type_id = 20065 ))  LEFT 
OUTER JOIN party_info dxr35 ON (dxr35.party_id =  dxr1.external_bunit 
AND (dxr35.type_id=20068 ))  LEFT 
OUTER JOIN ins_parameter dxr20 ON (dxr20.ins_num=dxr21.ins_num 
and dxr20.param_seq_num=dxr21.ins_para_seq_num)  
WHERE  dxr21.event_date >= '$$StartDate$$' and dxr21.event_date <= '$$EndDate$$'
UNION
SELECT   dxr1.deal_tracking_num deal_tracking_num  , dxr1.toolset toolset  , dxr1.ins_num, dxr1.currency Metal,
dxr1.internal_bunit internal_bunit  , dxr1.internal_portfolio internal_portfolio  ,
dxr1.external_bunit external_bunit  , dxr1.tran_status tran_status  ,
dxr1.buy_sell buy_sell  , dxr1.position Position , dxr1.trade_date trade_date  , 
 dxr1.ins_type ins_type  , dxr1.internal_contact internal_contact  , dxr19.contract_code contract_code  ,
dxr21.para_position para_position, dxr22.para_position para_position1, dxr21.unit unit, dxr20.unit ins_para_unit, dxr21.pymt_type pymt_type  , dxr21.event_date value_date  , 
 dxr22.currency currency  , dxr22.para_position settlement_amount  , dxr22.event_date value_date_1   , dxr26.value Form  , 
 dxr28.value JM_Group  , dxr29.currency_convention currency_convention  , dxr29.fx_rate_mid fx_rate_mid  , dxr33.value is_funding  ,
dxr34.value End_User  , dxr35.value Good_Home  , dxr20.notnl notional , '$$CompStartDate$$ to $$CompEndDate$$' as Period
FROM ab_tran dxr1  INNER JOIN 
$$QUERY_RESULT_TABLE$$ q ON (dxr1.tran_num = q.query_result and q.unique_id = ($$QUERY_RESULT_ID$$))   
LEFT OUTER JOIN misc_ins dxr19 ON (dxr19.ins_num = dxr1.ins_num)  LEFT 
OUTER JOIN ab_tran_event dxr21 ON (dxr21.tran_num = dxr1.tran_num and (dxr21.event_type = 14) and dxr21.ins_para_seq_num=0 $$Cflow_Type$$) 
 LEFT 
OUTER JOIN ab_tran_event dxr22 ON (dxr22.tran_num = dxr1.tran_num 
and (dxr22.event_type = 14)  
and dxr22.ins_para_seq_num=1)  LEFT 
OUTER JOIN ab_tran_info dxr26 ON (dxr26.tran_num = dxr1.tran_num 
AND ( dxr26.type_id = 20014  $$FormVal$$))  LEFT 
OUTER JOIN party_info dxr28 ON (dxr28.party_id =  dxr1.external_lentity 
AND (dxr28.type_id=20019  $$Jm_GroupVal$$ ))  LEFT 
OUTER JOIN idx_historical_fx_rates dxr29 ON (dxr29.fx_rate_date = dxr1.trade_date 
and (dxr29.currency_id=dxr22.currency))  
INNER JOIN ab_tran_info dxr33 ON (dxr33.tran_num = dxr1.tran_num 
AND ( dxr33.type_id = 20067 $$Is_FundingVal$$)
AND (dxr1.internal_bunit in ($$InternalUnit$$) ))  LEFT 
OUTER JOIN ab_tran_info dxr34 ON (dxr34.tran_num = dxr1.tran_num 
AND ( dxr34.type_id = 20065 ))  LEFT 
OUTER JOIN party_info dxr35 ON (dxr35.party_id =  dxr1.external_bunit 
AND (dxr35.type_id=20068 ))  LEFT 
OUTER JOIN ins_parameter dxr20 ON (dxr20.ins_num=dxr21.ins_num 
and dxr20.param_seq_num=dxr21.ins_para_seq_num)  
WHERE  dxr21.event_date >= '$$CompStartDate$$' and dxr21.event_date <= '$$CompEndDate$$'
) dxr WHERE 1=1
$$Group_Where_Clause$$
$$FORM_Where_Clause$$
$$Funding_Where_Clause$$


