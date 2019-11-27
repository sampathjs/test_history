------------------------------------------------------------------
--- UPDATE THE STAMPS ON CASH ITEMS INVOICED WAY BACK IN THE PAST
---------------------------------------------------------------------------

UPDATE USER_jm_sl_doc_tracking 
SET sl_status = 'Cancelled Sent'  --------------- FIX LEGACY STAMPS ON Cancelled Pending

WHERE 1 = 1
AND (sl_status IN ('Pending Cancelled'))
AND document_num IN
(
SELECT DISTINCT d.document_num
FROM stldoc_details d 
INNER JOIN ab_tran t ON t.tran_num = d.tran_num
WHERE 1 = 1
AND t.tran_status = 5 --------------- FIX LEGACY STAMPS ON Cancelled Pending
AND d.tran_settle_date < '16-NOV-2019'
)
