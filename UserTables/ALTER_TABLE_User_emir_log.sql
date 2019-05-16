ALTER TABLE user_jm_emir_log
ADD filename varchar(255);

go

update user_jm_emir_log set err_desc = 'OK' --where last_update <= getdate()

go 

select * from user_jm_emir_log