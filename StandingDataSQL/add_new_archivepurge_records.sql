INSERT INTO USER_archive_config(tablename, archive_table, retain_days, time_comparison_column)
SELECT 'USER_jm_bt_out_sl_sap' tablename
	, 'USER_jm_bt_out_sl_sap_archive' archive_table
	, 60 retain_days
	, 'last_update' time_comparison_column
EXCEPT
SELECT tablename
	, archive_table
	, retain_days
	, time_comparison_column
FROM USER_archive_config



INSERT INTO USER_purge_config(tablename, retain_days, time_comparison_column)
SELECT 'USER_jm_bt_out_sl_sap_archive' tablename
	, 90 retain_days
	, 'last_update' time_comparison_column
EXCEPT
SELECT tablename
	, retain_days
	, time_comparison_column
FROM USER_purge_config