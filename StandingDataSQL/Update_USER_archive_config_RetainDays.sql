UPDATE USER_archive_config SET retain_days=120 WHERE tablename='user_jm_ledger_extraction';
UPDATE USER_purge_config SET retain_days=150 WHERE tablename='user_jm_ledger_extraction_archive';