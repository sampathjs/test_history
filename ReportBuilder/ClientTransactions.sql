--tabletitle Client_Stmt_SQL "Client Statment SQL v2"
--argument $$accountNumbers$$ "" "accounts"
--argument $$subsidBunits$$ "" "subsid_bunits"

select distinct ab.deal_tracking_num as deal_num, trani.value as JM_tran_ID, REPLACE(REPLACE(ab.reference, CHAR(13), ''), CHAR(10), '') as reference, ab.trade_date, 
ate1.event_date as delivery_date, ate2.event_date as payment_date, ccy1.name as metal, bs.name as buysell, 
ate1.para_position* COALESCE(uc.factor, 1) as mtl_pos, acci.info_value as acc_uom, 
CASE WHEN ab.external_bunit in ($$subsidBunits$$) THEN CASE WHEN ab.toolset IN (36) THEN 0.0 ELSE Sum(ate2.para_position) END ELSE 0.0 END as sett_amt, 
CASE ate2.internal_conf_status WHEN 2 THEN 'Fixed' ELSE 'Unfixed' END as fixed, ccy2.name as ccy, 
CASE WHEN ab.external_bunit in ($$subsidBunits$$) THEN CASE WHEN ab.toolset IN (36) THEN 0.0 ELSE CASE WHEN abs(ab.price+Sum(ate2.para_position)/(ate1.para_position*COALESCE(uc.factor, 1))) < 0.01 THEN ab.price ELSE -Sum(ate2.para_position)/(ate1.para_position*COALESCE(uc.factor, 1)) END END ELSE 0.0 END as price, 
acc2.account_name as fr_acc, acc2.account_number as fr_acc_num, acc1.account_name as to_acc, acc1.account_number as to_acc_num, ts.name as deal_status, '' as your_ref, 0 as xfer_deal_id
from ab_tran ab
inner join ab_tran_event ate1 on ate1.tran_num = ab.tran_num and ate1.event_type = 14 and ate1.unit <> 0
inner join ab_tran_event_settle ates1 on ates1.event_num = ate1.event_num 
inner join account acc1 on acc1.account_id = ates1.ext_account_id
inner join ab_tran_event ate2 on ate2.tran_num = ab.tran_num and ate2.event_type = 14 and ate1.event_num <> ate2.event_num and ate2.unit = 0
inner join account acc2 on acc2.account_id = ates1.int_account_id
inner join buy_sell bs on bs.id_number = ab.buy_sell
inner join trans_status ts on ts.trans_status_id = ab.tran_status
inner join system_dates sd on 1 = 1
inner join currency ccy1 on ate1.currency = ccy1.id_number
inner join currency ccy2 on ate2.currency = ccy2.id_number
left outer join account_info acci on acci.account_id = ates1.ext_account_id and info_type_id = 20003
left outer join idx_unit iu on iu.unit_label = acci.info_value 
left outer join unit_conversion uc on uc.src_unit_id = 55 and uc.dest_unit_id =  iu.unit_id
left outer join ab_tran_info trani on trani.tran_num=ab.tran_num and trani.type_id = 20019
where left(acc1.account_number, 6) in ($$accountNumbers$$)
and acc2.account_name not like '%@JM TREASURY%'
and acc2.account_name not like '%@TREASURY%'
and ab.tran_status in (3,4,5)
and ab.tran_type = 0
and ab.toolset in (9,15,36)
and ate1.event_date >= CONVERT(varchar,dateadd(d,-60-(day(sd.business_date)),sd.business_date),106)
group by ab.deal_tracking_num, ab.reference, ab.trade_date, ate1.event_date, ate2.event_date, ccy1.name, ate1.para_position, uc.factor, acci.info_value, ate2.internal_conf_status, 
ccy2.name, acc2.account_name, acc1.account_name,  acc2.account_number, acc1.account_number, ab.toolset, ts.name, bs.name, trani.value, ab.external_bunit, ab.price
UNION ALL
select distinct ab.deal_tracking_num as deal_num, trani.value as JM_tran_ID,  REPLACE(REPLACE(ab.reference, CHAR(13), ''), CHAR(10), '') as reference, ab.trade_date, ate1.event_date as delivery_date, ''as payment_date, ccy1.name as metal, bs.name as buysell,
ate1.para_position* COALESCE(uc.factor, 1) as mtl_pos, acci.info_value as acc_uom, 0 as sett_amt,
'Fixed'  as fixed, '' as ccy, 0 as price, COALESCE(acc3.account_name, acc2.account_name) as from_acc, COALESCE(acc3.account_number, acc2.account_number) as fr_acc_num, acc1.account_name as to_acc, 
acc1.account_number as to_acc_num, ts.name as deal_status, 

"your_ref" = 
CASE
  WHEN ab.buy_sell=0 THEN 
	CASE 
		WHEN REPLACE(REPLACE(tn_from.line_text, CHAR(13), ''), CHAR(10), '') is NULL or len(REPLACE(REPLACE(tn_from.line_text, CHAR(13), ''), CHAR(10), ''))=0 THEN REPLACE(REPLACE(tn_to.line_text, CHAR(13), ''), CHAR(10), '') 
		ELSE REPLACE(REPLACE(tn_from.line_text, CHAR(13), ''), CHAR(10), '')
	END
  WHEN ab.buy_sell=1 THEN  
    CASE 
		WHEN REPLACE(REPLACE(tn_to.line_text, CHAR(13), ''), CHAR(10), '') is NULL or len(REPLACE(REPLACE(tn_to.line_text, CHAR(13), ''), CHAR(10), ''))=0 THEN REPLACE(REPLACE(tn_from.line_text, CHAR(13), ''), CHAR(10), '') 
		ELSE REPLACE(REPLACE(tn_to.line_text, CHAR(13), ''), CHAR(10), '')
	END

END  


, trani_strategy.value as xfer_deal_id

from ab_tran ab
inner join ab_tran_event ate1 on ate1.tran_num = ab.tran_num and ate1.unit <> 0
inner join ab_tran_event_settle ates1 on ates1.event_num = ate1.event_num and ate1.event_type = 14 and ates1.unit <> 0
inner join account acc1 on acc1.account_id = ates1.ext_account_id
inner join account acc2 on acc2.account_id = ates1.int_account_id
inner join currency ccy1 on ate1.currency = ccy1.id_number
inner join buy_sell bs on bs.id_number = ab.buy_sell
inner join trans_status ts on ts.trans_status_id = ab.tran_status
inner join system_dates sd on 1 = 1
left outer join account_info acci on acci.account_id = ates1.ext_account_id and info_type_id = 20003
left outer join idx_unit iu on iu.unit_label = acci.info_value 
left outer join unit_conversion uc on uc.src_unit_id = 55 and uc.dest_unit_id =  iu.unit_id
--left outer join ab_tran ab2 on ab2.reference = ab.reference and ab2.tran_num <> ab.tran_num and ab2.tran_status in (3,4,5) and ab2.toolset in (8) and ab2.tran_type = 39
left outer join tran_notepad tn_from on tn_from.tran_num = ab.tran_num and tn_from.note_type = 20001
left outer join tran_notepad tn_to on tn_to.tran_num = ab.tran_num and tn_to.note_type = 20002
left outer join ab_tran ab3 on ab3.reference = ab.reference and ab3.tran_num <> ab.tran_num and ab3.tran_status in (3,4,5) and ab3.toolset in (10) and ab3.tran_type = 0 and ab3.ins_sub_type in (10001) and ab3.tran_status = ab.tran_status and ab3.internal_portfolio = ab.internal_portfolio
left outer join ab_tran_event ate3 on ate3.tran_num = ab3.tran_num and ate3.event_type = 14 and ate3.unit <> 0
left outer join ab_tran_event_settle ates3 on ates3.event_num = ate3.event_num
left outer join account acc3 on acc3.account_id = ates3.ext_account_id
left outer join ab_tran_info trani on trani.tran_num=ab.tran_num and trani.type_id = 20019
left outer join ab_tran_info trani_strategy on trani_strategy.tran_num=ab.tran_num and trani_strategy.type_id = 20044
--left outer join ab_tran ab4 on ab4.reference = ab.reference and ab4.tran_type = 39 and ab4.tran_status in (3,4)
where left(acc1.account_number, 6) in ($$accountNumbers$$)
and acc2.account_name not like '%@JM TREASURY%'
and acc2.account_name not like '%@TREASURY%'
and ab.tran_status in (3,4,5)
and ab.toolset in (10)
and ab.tran_type = 0
and ate1.event_date >= CONVERT(varchar,dateadd(d,-60-(day(sd.business_date)),sd.business_date),106)
