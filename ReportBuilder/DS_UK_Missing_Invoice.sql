SELECT DISTINCT doc.value missing_doc_num, doc.field_name, doc.document_num endur_doc_num,doc.doc_issue_date,sds.doc_status_desc doc_status, sds1.doc_status_desc last_doc_status,doc.last_update, sdh.deal_tracking_num, sdh.tran_status,
(CASE WHEN doc.scenario=1 THEN 'Doc is in Generated/Approval Required or moved from Generated to Cancelled status' 
 WHEN doc.scenario=2 THEN 'Doc Num present on older version of document, can be ignored'
 WHEN doc.scenario=3 THEN 'Doc Num for Currency trade having maturity date > doc generation date'
 WHEN doc.scenario=4 THEN 'Doc Num for Base Metals which are currently excluded from JDE Extract' ELSE 'None' END) comment
FROM (
SELECT 1 scenario, si.value, (CASE WHEN si.type_id=20003 THEN 'OurDocNum' WHEN si.type_id=20007 THEN 'CancelDocNum' END) field_name, 
sh.doc_version, sh.document_num, sh.doc_status,sh.last_doc_status,sh.last_update,sh.doc_issue_date
FROM stldoc_header sh
JOIN stldoc_info si ON sh.document_num=si.document_num AND si.type_id IN (20003,20007)
WHERE sh.doc_type=1 AND ((sh.doc_status=15) OR (sh.doc_status=5) OR (sh.doc_status=4 AND sh.last_doc_status=5))
AND sh.doc_issue_date>= '$$Reporting_Date$$'
AND sh.doc_issue_date< (SELECT trading_date FROM configuration)
  
UNION
SELECT 2 scenario, sih.value, 'VATInvoiceDocNum' field_name, sh.doc_version, sh.document_num, sh.doc_status, sh.last_doc_status,sh.last_update ,sh.doc_issue_date
FROM stldoc_info si
JOIN stldoc_info_h sih ON si.document_num=sih.document_num AND sih.type_id=20005 AND si.value != sih.value
JOIN stldoc_header sh ON sh.document_num=si.document_num
WHERE si.type_id=20005
AND sh.doc_issue_date>= '$$Reporting_Date$$'
AND sh.doc_issue_date< (SELECT trading_date FROM configuration)

UNION
SELECT DISTINCT 3 scenario, si.value, 'OurDocNum' field_name, sh.doc_version, sh.document_num, sh.doc_status,sh.last_doc_status,sh.last_update,sh.doc_issue_date
FROM stldoc_header sh
JOIN stldoc_details sd ON sh.document_num=sd.document_num
JOIN stldoc_info si ON sh.document_num=si.document_num AND si.type_id=20003
JOIN ab_tran ab ON ab.tran_num = sd.tran_num AND ab.tran_status = 3
WHERE sd.portfolio_id IN(20054,20001) AND ab.maturity_date > (SELECT trading_date FROM configuration)
AND sh.doc_issue_date>= '$$Reporting_Date$$'
AND sh.doc_issue_date< (SELECT trading_date FROM configuration)

UNION
SELECT DISTINCT 4 scenario, si.value, 'OurDocNum' field_name, sh.doc_version, sh.document_num, sh.doc_status,sh.last_doc_status,sh.last_update,sh.doc_issue_date
FROM stldoc_header sh
JOIN stldoc_details sd ON sh.document_num=sd.document_num
JOIN stldoc_info si ON sh.document_num=si.document_num AND si.type_id=20003
WHERE sd.idx_group = 3
AND sh.doc_issue_date>= '$$Reporting_Date$$'
AND sh.doc_issue_date< (SELECT trading_date FROM configuration)

) doc
JOIN stldoc_details_hist sdh ON doc.document_num=sdh.document_num AND sdh.doc_version=doc.doc_version AND sdh.internal_bunit IN (20006,20008)
JOIN stldoc_document_status sds ON doc.doc_status = sds.doc_status 
JOIN stldoc_document_status sds1 ON doc.last_doc_status = sds1.doc_status
ORDER BY deal_tracking_num