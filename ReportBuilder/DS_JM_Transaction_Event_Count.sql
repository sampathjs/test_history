--tabletitle JM_Transaction_Event_Count "Daily Account Transactions to Account Balance"
--argument $$Reporting_Start$$ "" "Start Date"
--argument $$Reporting_End$$ "" "End Date"
--join account_id TO account.account_id

SELECT
acc.account_number,
acc.account_class,
acc.account_id,
ates.ext_account_id,
ates.delivery_type,
ates.currency_id,
ab.internal_portfolio,
ccy.description as metal,
ates.nostro_date as report_date,
case count(abe.event_type)
  WHEN 0 THEN 'No' 
ELSE 'Yes' 
END as 'hasEvents',
count(abe.event_type) as NoEvent

from ab_tran_event_settle ates 
inner join currency ccy on (ccy.id_number=ates.currency_id and (ccy.precious_metal=1))
inner join account acc on (acc.account_id=ates.ext_account_id and (acc.account_class=20002))
inner join ab_tran_event abe on(abe.event_num=ates.event_num and (abe.event_type=14))
inner join ab_tran ab on (ab.tran_num=abe.tran_num and (ab.tran_status=3))

WHERE  abe.event_type=14
AND ates.nostro_date BETWEEN '$$Reporting_Start$$' AND '$$Reporting_End$$' 
GROUP BY acc.account_number, acc.account_class, acc.account_id,ates.ext_account_id, ates.delivery_type, ates.currency_id, ab.internal_portfolio, ccy.description, ates.nostro_date, abe.event_type