--tabletitle ext_account_balances "External Account Balances"
--argument $$accountNumber$$ "" "account"
--argument $$metalCode$$ "" "metalCode"
--argument $$fromDate$$ "" "fromDate"
--argument $$toDate$$ "" "toDate"
SET ANSI_WARNINGS OFF


SELECT account_number,currency_id,
REPLACE(CONVERT(VARCHAR, reset_date, 106),' ','-') as report_date,position, numevent
FROM
	(SELECT DISTINCT account_number,currency_id, reset_date,  
	SUM(-para_position * COALESCE(factor, 1)) OVER (PARTITION BY account_number, currency_id ORDER BY reset_date) AS position,
	COUNT(para_position * COALESCE(factor, 1)) OVER (PARTITION BY reset_date ) AS numevent
	from 
		(SELECT COALESCE(abs.account_number, '$$account$$') as account_number, COALESCE(abs.name,'$$metalCode$$') as currency_id, gbds.reset_date, abs.para_position, abs.factor
		from 
			(select distinct reset_date from reset where reset_date > '01-Jan-2016') gbds 
		left outer join 
			(select  acc.account_number,ccy.name, ate.event_date, ate.para_position, uc.factor
			from ab_tran ab
				join ab_tran_event ate on ate.tran_num = ab.tran_num
				join ab_tran_event_settle ates on ates.event_num = ate.event_num
				join account acc on acc.account_id = ates.ext_account_id 
				join currency ccy on ccy.id_number = ates.currency_id
				left outer join account_info ai on ai.account_id = acc.account_id and info_type_id = 20003
				left outer join idx_unit iu on iu.unit_label = ai.info_value
				left outer join unit_conversion uc on uc.dest_unit_id= COALESCE(iu.unit_id, 55) and uc.src_unit_id = 55
			where acc.account_number = '$$account$$'
				and ab.tran_status in (3,4)
				and ccy.name =  '$$metalCode$$'
			) abs on gbds.reset_date = abs.event_date) as raw
	) bals
WHERE reset_date >= '$$fromDate$$'
	AND reset_date <= '$$toDate$$'
ORDER BY reset_date