--tabletitle ext_account_balance "External Account Balance"
--argument $$account$$ "" "account"
--argument $$ReportDate$$ "" "ReportDate"
SET ANSI_WARNINGS OFF

SELECT account_number, '$$ReportDate$$' as "Date",  ccy.name as currency_id,
	sum(-ate.para_position * COALESCE(uc.factor, 1)) as balance, 
	CASE WHEN SUM(CASE WHEN toolset = 36 THEN 1 ELSE 0 END) = 0 THEN 'N' ELSE 'Y' END as hasSpecifications,
	CASE WHEN SUM(CASE WHEN toolset = 36 THEN 0 ELSE 1 END) = 0 THEN 'N' ELSE 'Y' END as hasTransactions,
	iu.unit_label as weightUnit
FROM ab_tran ab
	join ab_tran_event ate on ate.tran_num = ab.tran_num
	join ab_tran_event_settle ates on ates.event_num = ate.event_num
	join account acc on acc.account_id = ates.ext_account_id 
	join currency ccy on ccy.id_number = ates.currency_id
	left outer join account_info ai on ai.account_id = acc.account_id and info_type_id = 20003
	left outer join idx_unit iu on iu.unit_label = ai.info_value
	left outer join unit_conversion uc on uc.dest_unit_id= COALESCE(iu.unit_id, 55) and uc.src_unit_id = 55
WHERE acc.account_number = '$$account$$'
	and ab.tran_status in (3,4)
	and event_date <= '$$ReportDate$$'
GROUP BY  account_number,  ccy.name, iu.unit_label, ccy.id_number
ORDER BY ccy.id_number