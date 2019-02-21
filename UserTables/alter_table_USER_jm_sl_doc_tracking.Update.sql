UPDATE s SET s.sap_status = '' FROM
(
	SELECT d.document_num,d.sap_status
		,CASE WHEN EXISTS(SELECT 1 from ab_tran ab 
							JOIN ab_tran_info ti ON ab.tran_num = ti.tran_num 
							JOIN stldoc_details_hist sdh ON ab.tran_num = sdh.tran_num   
							WHERE ti.type_id = 20021 and ti.value = 'Yes' and sdh.document_num = d.document_num )   
		THEN 'Yes' ELSE 'No' END as is_coverage  
	FROM USER_jm_sl_doc_tracking d WHERE d.sap_status IN ('NOT Sent', 'Pending Sent', 'Pending Cancelled')
) s WHERE s.is_coverage = 'No'