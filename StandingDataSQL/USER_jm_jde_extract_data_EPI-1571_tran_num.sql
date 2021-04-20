IF COL_LENGTH('USER_jm_jde_extract_data', 'tran_num') IS NULL
BEGIN
  ALTER TABLE USER_jm_jde_extract_data 
  ADD tran_num int;
END