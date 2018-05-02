-- This query selects all documents that have transitioned through a "sent to Counterparty" and
-- "Cancelled" status, where the sl_status is in a "Pending" state. 

SELECT 
DISTINCT -- don't care about doc versions in history table, just interested in unique documents 
shh.document_num AS endur_doc_num,
udt.sl_status
FROM stldoc_details_hist sdh 
JOIN stldoc_header_hist shh ON sdh.document_num = shh.document_num AND sdh.doc_version = shh.doc_version 
JOIN user_jm_sl_doc_tracking udt ON shh.document_num = udt.document_num
WHERE shh.doc_type IN (select doc_type from stldoc_document_type where doc_type_desc = 'Invoice') 
AND shh.stldoc_template_id IN (20008, 20015, 20016, 20021) -- 'JM Invoice' templates 
AND shh.doc_status IN (select doc_status from stldoc_document_status where doc_status_desc in ('2 Sent to CP', 'Cancelled'))
AND sdh.settle_amount != 0
AND sdh.ins_type != 32007 -- Not PREC-EXCH-FUT (32007)
AND udt.sl_status IN ('Pending Sent', 'Pending Cancelled', 'NOT Sent')