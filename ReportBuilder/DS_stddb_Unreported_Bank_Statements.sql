--Files No loaded
--argument $$START_DATE$$ "20-Feb-2014 00:00:00"  "Starting day to selectt" mandatory
--argument $$END_DATE$$ "20-Feb-2014 00:00:00"  "Ending day to selectt" mandatory
-----select exp_account_id from input_exp_account where exp_account_id not in (select account_id from input_file_ref where  loaded_date between '$$START_DATE$$' and  '$$END_DATE$$' and file_status_id =3)

select exp_account_id from input_exp_account where exp_account_id not in (select account_id from input_file_ref where  official_system_date between '$$START_DATE$$' and  '$$END_DATE$$' and file_status_id =3)