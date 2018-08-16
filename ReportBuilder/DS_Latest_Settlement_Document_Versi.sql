--tabletitle JM_Latest_Version_document "Latest Version Invoice"
--join document_num TO stldoc.document_num

SELECT

MAX(doc_version) as doc_version,
document_num,
doc_status,
doc_issue_date

FROM stldoc_header_hist
---WHERE doc_status IN (4,7) /*CANCELLED & SENT TO CP*/
WHERE doc_status = 7 /*SENT TO CP*/
GROUP BY document_num, doc_status, doc_issue_date
ORDER BY document_num