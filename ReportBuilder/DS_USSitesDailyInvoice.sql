
-- Handle Invoices/Credit Notes currently in sent2ctpy
SELECT sh.doc_issue_date, ab.deal_tracking_num ,ab.trade_date ,ab.buy_sell ,ab.external_bunit,ab.reference,ati.value AS EndUser, ab.external_contact ,ab.currency,ab.personnel_id,ab.price ,
ab.positiON,abe.currency as Currency1,abe.ins_para_seq_num ,
(CASE WHEN ab.price=0.0 THEN 'Transfer' ELSE 'Trade' END) AS DealType,
abe.event_date ,abe1.event_date AS CashSettleDate , sti.value ,sh.document_num ,sd.settle_amount ,sti.last_update ,ab.settle_date 
FROM ab_Tran ab 
LEFT OUTER JOIN ab_tran_info ati ON ab.tran_num=ati.tran_num AND ati.type_id IN (20065) -- EndUser
JOIN ab_tran_event abe ON ab.tran_num=abe.tran_num 
LEFT OUTER JOIN ab_tran_event abe1 ON ab.tran_num=abe1.tran_num
JOIN stldoc_details sd ON sd.tran_num=ab.tran_num 
JOIN stldoc_header sh ON sd.document_num=sh.document_num 
JOIN stldoc_info sti ON sti.document_num=sh.document_num
WHERE abe.ins_para_seq_num=0 AND abe.event_type=14 
AND abe1.ins_para_seq_num=1 AND abe1.event_type=14 
AND ab.current_flag=1 AND ab.internal_bunit in (20001) --PMM US
AND ab.toolset in (9,10,15)		-- Cash, FX,COMSWAP
AND sh.doc_status=7  -- Sent2ctpy
AND sh.doc_issue_date='$$ISSUE_DATE$$'
AND ab.external_bunit in ($$EXTERNAL_BUNIT$$)
AND sh.stldoc_def_id=20002 -- Invoice
AND sti.type_id=20003  -- JM Invoice Number

UNION 

-- Cancelled Inovices/Sent2Ctpy
SELECT sh.doc_issue_date, ab.deal_tracking_num ,ab.trade_date ,ab.buy_sell ,ab.external_bunit,ab.reference,ati.value as EndUser, ab.external_cONtact ,ab.currency,ab.persONnel_id,ab.price ,
ab.positiON,abe.currency as Currency1,abe.ins_para_seq_num ,
(CASE WHEN ab.price=0.0 THEN 'Transfer' ELSE 'Trade' END) as DealType,
abe.event_date ,abe1.event_date as CashSettleDate , sti.value ,sh.document_num ,sd.settle_amount ,sti.last_update ,ab.settle_date 
FROM ab_Tran ab 
LEFT OUTER JOIN ab_tran_info ati ON ab.tran_num=ati.tran_num AND ati.type_id IN (20065)
JOIN ab_tran_event abe ON ab.tran_num=abe.tran_num 
LEFT OUTER JOIN ab_tran_event abe1 ON ab.tran_num=abe1.tran_num
JOIN stldoc_details sd ON sd.tran_num=ab.tran_num 
JOIN stldoc_header sh ON sd.document_num=sh.document_num 
JOIN stldoc_info sti ON sti.document_num=sh.document_num
WHERE abe.ins_para_seq_num=0 AND abe.event_type=14 
AND abe1.ins_para_seq_num=1 AND abe1.event_type=14 
AND ab.current_flag=1  AND ab.internal_bunit in (20001) --PMM US
AND ab.toolset in (9,10,15)  -- Cash, FX,COMSWAP
AND sh.doc_status=4 -- Cancelled
AND sh.last_update>='$$ISSUE_DATE$$'
AND sh.doc_issue_date <> '$$ISSUE_DATE$$'
AND ab.external_bunit in ($$EXTERNAL_BUNIT$$)
AND sh.stldoc_def_id=20002 ---- Invoice
AND sti.type_id=20007 -- CancellatiON Number