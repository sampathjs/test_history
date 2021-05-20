IF COL_LENGTH('USER_jm_jde_extract_data_hist', 'tran_num') IS NULL
BEGIN
  ALTER TABLE USER_jm_jde_extract_data_hist
  ADD tran_num int;
END