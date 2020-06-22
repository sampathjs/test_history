begin tran

select * from USER_jm_pte_pto_form_loco
go
update USER_jm_pte_pto_form_loco set form_on_pti_pto = 'Ingot' where loco_on_pte = 'Zurich' and form_on_pte = 'Ingot'
go
update USER_jm_pte_pto_form_loco set form_on_pti_pto = 'Ingot' where loco_on_pte = 'Tanaka Japan' and form_on_pte = 'Ingot'
go
update USER_jm_pte_pto_form_loco set form_on_pti_pto = 'Ingot' where loco_on_pte = 'Hong Kong' and form_on_pte = 'Ingot'
go

select * from USER_jm_pte_pto_form_loco
go
commit tran