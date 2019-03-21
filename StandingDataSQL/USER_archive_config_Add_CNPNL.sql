INSERT INTO USER_archive_config(tablename, archive_table, email_recipients ,retain_days, time_comparison_column)
SELECT 'USER_jm_pnl_market_data_cn' tablename
	, 'USER_jm_pnl_market_data_cna' archive_table
	, ''  email_recipients
	, 400 retain_days
	, 'entry_date' time_comparison_column
EXCEPT
SELECT tablename
	, archive_table
	, email_recipients
	, retain_days
	, time_comparison_column
FROM USER_archive_config


INSERT INTO USER_archive_config(tablename, archive_table, email_recipients, retain_days , time_comparison_column)
SELECT 'USER_jm_pnl_market_data_hist_cn' tablename
	, 'USER_jm_pnl_market_data_hist_cna' archive_table
	, '' email_recipients
	, 400 retain_days
	, 'entry_date' time_comparison_column
EXCEPT
SELECT tablename
	, archive_table
	, email_recipients
	, retain_days
	, time_comparison_column
FROM USER_archive_config


Select * from USER_archive_config