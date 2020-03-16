begin tran

select count(*) from USER_bo_auto_doc_process

go

insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (2,20001,'Auto Confirms: To Send (failed output script)',7,5,1,0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (2,20001,'Auto Confirms_01: To Send (failed output script)',7,5,1,1)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (2,20001,'Auto Confirms_02: To Send (failed output script)',7,5,1,2)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (2,20001,'Auto Confirms_03: To Send (failed output script)',7,5,1,3)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (2,20001,'Auto Confirms_04: To Send (failed output script)',7,5,1,4)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (1,20002,'Auto Invoices All: To Cancelled (failed op script)',4,16,1,0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (1,20002,'Auto Invoices All: To Send (failed op script)',7,14,1,0)

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 15 WHERE query_name = 'Auto Invoices All: To Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 17 WHERE query_name = 'Auto Invoices All: To New Doc' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Confirms: Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 8 WHERE query_name = 'Auto Confirms: Re-Send' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Confirms_01: Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Confirms_02: Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Confirms_03: Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Confirms_04: Cancelled' AND active = 1

go
select count(*) from USER_bo_auto_doc_process

go
commit tran

go


