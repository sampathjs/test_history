--tabletitle JM_Latest_Version_document "Latest Version Invoice"
--join document_num TO stldoc.document_num

SELECT

MAX(sh.doc_version) as doc_version,
sh.document_num,
sh.doc_status,
sh.doc_issue_date,
sdi.value as our_doc_num

FROM stldoc_header_hist sh
INNER JOIN stldoc_info sdi on (sdi.document_num=sh.document_num and (sdi.type_id=(select type_id from stldoc_info_types where type_name='Our Doc Num')))
WHERE sh.doc_status = 7 /*2 Sent to CP*/
GROUP BY sh.document_num, sh.doc_status, sh.doc_issue_date, sdi.value
ORDER BY sh.document_num