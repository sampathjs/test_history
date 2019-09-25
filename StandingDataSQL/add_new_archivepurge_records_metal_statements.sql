INSERT INTO USER_archive_config(tablename, archive_table, retain_days, time_comparison_column)
SELECT 'USER_jm_monthly_metal_statement' tablename
	, 'USER_jm_monthly_metal_statement_archive' archive_table
	, 180 retain_days
	, 'last_modified' time_comparison_column
EXCEPT
SELECT tablename
	, archive_table
	, retain_days
	, time_comparison_column
FROM USER_archive_config



