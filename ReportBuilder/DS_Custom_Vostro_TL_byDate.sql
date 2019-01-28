--tabletitle D960_VostroTL "Vostro Transaction Listing(By Date)"
--argument $$accountNumber$$ "" "Account Number"
--join account_id TO account.account_id
--argument $$fromDate$$ "" "Events From"
--argument $$toDate$$ "" "Events To"
--argument $$eventRange$$ "" "Event Date Range"
--argument $$tradeRef$$ "" "Trades "
--argument $$tradeRange$$ "" "Trades To Filter"
--argument $$METAL$$ "" "For Metals"

 
 
SELECT  a.account_number
     , ates.ext_account_id as VostroAccount_id
    , a.account_type
    , a.account_class
    , ab.deal_tracking_num , ab.tran_num
    , ab.tran_group
    , ab.tran_status
    , ativ.value JM_Tran_Id
   , ab.internal_portfolio
   , ab.ins_type
   , ate.event_num
    , ab.trade_date
    , (CASE WHEN ab.ins_type= 27001 THEN tn.line_text ELSE ab.reference END) reference
    , ate.ins_seq_num
    , ab.start_date
    , ab.maturity_date  end_date
    , ate.event_date value_date
    , ab.buy_sell
    , ab.price price
    ,  ate.currency Currency
    , -ate.para_position IndexPosition
    ,  CASE  WHEN ate.currency  IN((select id_number from currency where precious_metal=1)) AND ate.event_type=14 THEN 'TOz' ELSE 'Currency' END  'index_unit'
    , 'UConv' UnitConv
    , 'TP' TradePosition
    , ate.unit TradedUnit
--    , ai.info_value ReportingUnit
    , unit.unit_id ReportingUnit
    , 'RP Pos' ReportingPosition
--TODO     , ai.reporting_unit
    , CASE  WHEN ate.currency  IN((select id_number from currency where precious_metal=1)) AND ate.event_type=14 THEN 0 ELSE ate.currency END  settlementCurrency
--    , CASE  WHEN ate.currency  IN((select id_number from currency where precious_metal=0)) AND ate.event_type=14 THEN ate.para_position ELSE -4 END  DealValue
    , COALESCE ( ativL.value, aiL.info_value ) Loco
 
--                                                         , '$$Reporting_Date$$' as report_date
   FROM ab_tran ab 
   INNER JOIN ab_tran_event ate on ate.tran_num=ab.tran_num AND  ate.event_type=14  $$eventRange$$ 
   INNER JOIN ab_tran_event_settle ates ON (ate.event_num=ates.event_num) 
   INNER JOIN account a ON a.account_id=ates.ext_account_id AND a.account_number = '$$accountNumber$$'
   INNER JOIN trans_status abs ON (abs.trans_status_id=ab.tran_status AND abs.name IN ('Validated', 'Matured', 'Closeout'))
   LEFT JOIN tran_notepad tn ON tn.tran_num = ab.tran_num and tn.note_type =(CASE  WHEN ab.buy_sell= 0 THEN 20001 ELSE 20002 END)
   LEFT JOIN ab_tran_info_view ativ ON ab.tran_num=ativ.tran_num AND ativ.type_name='JM_Transaction_Id'
   LEFT JOIN ab_tran_info_view ativL ON ab.tran_num=ativL.tran_num AND ativL.type_name='Loco'
   LEFT JOIN account_info ai ON a.account_id=ai.account_id AND ai.info_type_id in (SELECT ait.type_id FROM account_info_type ait WHERE ait.type_name='Reporting Unit' )
   LEFT JOIN account_info aiL ON a.account_id=aiL.account_id AND aiL.info_type_id in (SELECT ait.type_id FROM account_info_type ait WHERE ait.type_name='Loco' )
   LEFT JOIN idx_unit unit ON unit.unit_label=ai.info_value
 
   WHERE  ab.tran_status IN (SELECT trans_status_id FROM trans_status WHERE name in ('Validated', 'Matured', 'Closeout'))
      AND ab.current_flag=1
      AND ab.offset_tran_num<1
      $$tradeRange$$
