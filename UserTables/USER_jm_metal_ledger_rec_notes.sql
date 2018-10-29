create table USER_jm_metal_ledger_rec_notes
(
deal_num int,
break_description varchar(255)
)

grant select, insert, update, delete on USER_jm_metal_ledger_rec_notes to olf_user

grant select on USER_jm_metal_ledger_rec_notes to olf_readonly

grant select, insert, update, delete on USER_jm_metal_ledger_rec_notes to olf_user_manual
go  

