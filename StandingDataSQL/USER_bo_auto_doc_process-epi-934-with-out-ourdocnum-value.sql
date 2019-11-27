begin tran

select count(*) from USER_bo_auto_doc_process

go

insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (1, 20002, 'Auto Invoices All: To Generated (Without OurDocNum)', 5, 10, 1, 0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (20012, 20007, 'Auto Invoices CN: To Generated (Without OurDocNum)', 5, 3, 1, 0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (20005, 20002, 'Auto Invoices HK: To Generated (Without OurDocNum)', 5, 3, 1, 0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (20003, 20002, 'Auto Invoices UK+SA: To Generated (Without OurDocNum)', 5, 4, 1, 0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (20004, 20002, 'Auto Invoices US: To Generated (Without OurDocNum)', 5, 3, 1, 0)


UPDATE dbo.USER_bo_auto_doc_process SET sequence = 11 WHERE query_name = 'Auto Invoices All: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 12 WHERE query_name = 'Auto Invoices All: To Send' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 13 WHERE query_name = 'Auto Invoices All: To Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 14 WHERE query_name = 'Auto Invoices All: To New Doc' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 4 WHERE query_name = 'Auto Invoices CN: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 5 WHERE query_name = 'Auto Invoices CN: To Send' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Invoices CN: Completed' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 4 WHERE query_name = 'Auto Invoices HK: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 5 WHERE query_name = 'Auto Invoices HK: To Send' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 5 WHERE query_name = 'Auto Invoices UK+SA: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Invoices UK+SA: To Send' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 7 WHERE query_name = 'Auto Invoices UK+SA: To Ignore for Pymt' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 8 WHERE query_name = 'Auto Invoices UK+SA: Completed' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 4 WHERE query_name = 'Auto Invoices US: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 5 WHERE query_name = 'Auto Invoices US: To Send' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Invoices US: To Completed' AND active = 1


go
select count(*) from USER_bo_auto_doc_process

go
commit tran

go


