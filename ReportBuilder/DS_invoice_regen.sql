SELECT distinct s2.deal_Tracking_num, value JDE_Invoice_Number FROM
(SELECT deal_Tracking_num, ins_para_seq_num, count(*) count FROM
(SELECT distinct sdh2.deal_Tracking_num, CAST(sdh2.para_position as decimal (18,4)) para_position, sdh2.ins_para_seq_num FROM stldoc_header_hist shh
INNER JOIN stldoc_details_hist sdh ON sdh.document_num=shh.document_num
INNER JOIN ab_tran ab ON ab.deal_tracking_num=sdh.deal_Tracking_num
INNER JOIN stldoc_details_hist sdh2 ON sdh2.deal_tracking_num=sdh.deal_Tracking_num
WHERE ab.tran_status=3
AND shh.doc_status=4
AND shh.last_update> EOMONTH(GETDATE(),-1)
AND shh.last_update<=EOMONTH(GETDATE())
AND shh.doc_type=1
AND ab.internal_bunit IN (20006, 20008)
AND sdh2.event_type=14
) s1 
GROUP BY deal_Tracking_num, ins_para_seq_num
HAVING count(*)>1
) s2
INNER JOIN stldoc_details_hist sdh3 ON sdh3.deal_Tracking_num=s2.deal_Tracking_num
INNER JOIN stldoc_header_hist shh2 ON sdh3.document_num=shh2.document_num
INNER JOIN stldoc_info_h sih ON shh2.document_num=sih.document_num
WHERE  sih.type_id IN (20003, 20007)
AND shh2.doc_type=1
AND sdh3.doc_version=1
AND sdh3.event_type=14