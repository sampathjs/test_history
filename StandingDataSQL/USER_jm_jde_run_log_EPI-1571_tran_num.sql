IF COL_LENGTH('USER_jm_jde_interface_run_log', 'tran_num') IS NULL
BEGIN
  ALTER TABLE USER_jm_jde_interface_run_log 
  ADD tran_num int;
END