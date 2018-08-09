SELECT DISTINCT ate.ins_num, ate.ins_para_seq_num - 1 AS param_seq_num, acc.*
FROM account acc
JOIN ab_tran_event_settle ates ON (ates.ext_account_id = acc.account_id)
JOIN ab_tran_event ate ON (ate.event_num = ates.event_num)
WHERE ate.event_type = 14 /* Cash event */
AND ate.pymt_type = 107 /* Commodity payment */
