--argument $$ACCOUNT_NUMBER$$ "0" " " 
--tabletitle EJM_Account "AccountExists"
SET ANSI_WARNINGS OFF

IF EXISTS (
	SELECT
	* 
	FROM account
	WHERE account_number = '$$ACCOUNT_NUMBER$$'
) (
	SELECT 'True' as account_exists, 
	   COALESCE(p.long_name ,'NOT Linked') as account_name,
	   COALESCE((select ai.info_value from account_info_type ait where ait.type_id=ai.info_type_id AND  ait.type_name = 'GT Acct Number') ,'' ) as account_code
	FROM account a
		LEFT JOIN party_account pa ON a.account_id=pa.account_id 
		LEFT JOIN party p ON pa.party_id=p.party_id
		LEFT JOIN account_info ai ON a.account_id=ai.account_id AND  ai.info_type_id=(select type_id from account_info_type where type_name = 'GT Acct Number')
	WHERE a.account_number = '$$ACCOUNT_NUMBER$$' 
 ) ELSE (
	SELECT 'False' as account_exists, '' as account_name ,'' as account_code 
)
