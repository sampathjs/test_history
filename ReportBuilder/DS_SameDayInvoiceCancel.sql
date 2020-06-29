SELECT ab.deal_tracking_num ,ab.trade_date ,ab.external_bunit, sh.doc_issue_date, sh.document_num, sti.value as JDE_InvoiceNum, sti1.value as JDE_CancellationNum
FROM ab_Tran ab  
JOIN stldoc_details_hist sd ON sd.tran_num=ab.tran_num 
JOIN stldoc_header_hist sh ON sd.document_num=sh.document_num 
JOIN stldoc_info_h sti ON sti.document_num=sh.document_num 
JOIN stldoc_info_h sti1 ON sti1.document_num=sh.document_num 
WHERE ab.current_flag=1 AND ab.internal_bunit in (20006,20008) --PMM UK, JM PM Ltd
AND ab.toolset in (9,10,15)		-- Cash, FX,COMSWAP
AND ab.cflow_type NOT IN (SELECT id_number FROM cflow_type WHERE name LIKE 'Metal Rentals%')
AND sh.doc_status=7   -- Sent2ctpy
AND sh.doc_issue_date IN (SELECT DATEADD(dd, 1, prev_business_date) from configuration)
AND sh.stldoc_def_id=20002 -- Invoice
AND sti.type_id=20003 
AND sti1.type_id = 20007 -- JM Invoice Number

INTERSECT 

-- Cancelled Inovices/Sent2Ctpy
SELECT ab.deal_tracking_num ,ab.trade_date ,ab.external_bunit, sh.doc_issue_date, sh.document_num, sti.value as JDE_InvoiceNum, sti1.value as JDE_CancellationNum
FROM ab_Tran ab 
JOIN stldoc_details_hist sd ON sd.tran_num=ab.tran_num 
JOIN stldoc_header_hist sh ON sd.document_num=sh.document_num 
JOIN stldoc_info_h sti ON sti.document_num=sh.document_num 
JOIN stldoc_info_h sti1 ON sti1.document_num=sh.document_num 
where ab.current_flag=1  AND ab.internal_bunit in (20006,20008) --PMM UK, JM PM Ltd
AND ab.toolset in (9,10,15)  -- Cash, FX,COMSWAP
AND ab.cflow_type NOT IN (SELECT id_number FROM cflow_type WHERE name LIKE 'Metal Rentals%')
AND sh.doc_status=4 -- Cancelled
AND sh.last_update >=(SELECT DATEADD(dd, 1, prev_business_date) from configuration)
AND sh.doc_issue_date = (SELECT DATEADD(dd, 1, prev_business_date) from configuration)
AND sh.stldoc_def_id=20002 ---- Invoice
AND sti.type_id=20003
AND sti1.type_id=20007 -- CancellatiON N