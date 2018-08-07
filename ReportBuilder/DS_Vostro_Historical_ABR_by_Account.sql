--tabletitle JM_VostroHistoricalAB "Vostro Historical Account Balances By Account"
--argument $$Reporting_Start$$ "" "Start Date"
--argument $$Reporting_End$$ "" "End Date"
--argument $$Reporting_Account_Number$$ "" "Account Number"
--join account_id TO account.account_id
 
SELECT  ab.account_id as account_id,
                                            ab.currency_id,
              ab.delivery_type,
                                            ab.portfolio_id,
              ab.position +
 isnull(( select sum(position) from user_jm_vostro_account_pos_adj  adj  where ab.account_id = adj.account_id
                AND ab.currency_id = adj.currency_id
                AND ab.delivery_type = adj.delivery_type
                AND ab.portfolio_id = adj.portfolio_id
                AND ab.unit = adj.unit
                AND ab.report_date BETWEEN adj.start_date AND adj.end_date) ,0) ohd_position,
            ab.report_date,
ab.unit
FROM USER_jm_vostro_acct_pos_hist ab
JOIN account a on a.account_id = ab.account_id and a.account_number =  '$$Reporting_Account_Number$$'
 
WHERE ab.report_date BETWEEN '$$Reporting_Start$$' AND '$$Reporting_End$$'