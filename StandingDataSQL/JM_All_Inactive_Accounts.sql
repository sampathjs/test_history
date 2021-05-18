WITH active_accounts AS (
SELECT
ab.external_bunit, CONVERT(date, MAX(trade_date)) last_trade_date, ac.account_id, ab.currency
FROM
ab_tran ab, ab_tran_event abte, ab_tran_event_settle abtes, account ac
WHERE
ab.tran_num=abte.tran_num AND
abte.event_num=abtes.event_num AND
abtes.ext_account_id = ac.account_id AND
ab.tran_status = 3 AND
ab.trade_date >= CONVERT(varchar, '$$CutOffDate$$', 120) AND
abte.currency in (53, 54, 55, 56, 58, 61, 62, 63)
GROUP BY
ab.external_bunit, ac.account_id, ab.currency
),
all_accounts AS (
SELECT acc.account_id
FROM Account acc
),
inactive_accounts AS (
SELECT account_id FROM all_accounts
EXCEPT
SELECT account_id FROM active_accounts WHERE last_trade_date >= CONVERT(varchar, '$$CutOffDate$$', 120)
)
SELECT ab.external_bunit, acc.account_id, MAX(trade_date) last_trade_date, SUM(abtes.settle_amount) Account_Balances, abte.currency
FROM 
ab_tran ab, ab_tran_event abte, ab_tran_event_settle abtes, inactive_accounts ia, account acc 
WHERE
ab.tran_num=abte.tran_num AND
abte.event_num=abtes.event_num AND
abtes.ext_account_id = ia.account_id AND
ia.account_id = acc.account_id AND
abte.currency in (53, 54, 55, 56, 58, 61, 62, 63)
AND abtes.nostro_flag = 1
AND acc.account_type=0
GROUP BY ab.external_bunit, acc.account_id, abte.currency
