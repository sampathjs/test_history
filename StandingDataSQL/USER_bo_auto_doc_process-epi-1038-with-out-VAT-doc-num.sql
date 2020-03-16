begin tran

select count(*) from USER_bo_auto_doc_process

go

insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (1, 20002, 'Auto Invoices All: To Generated (Without VATInvDocNum)', 5, 11, 1, 0)
insert into dbo.USER_bo_auto_doc_process([doc_type],[def_id],[query_name],[next_doc_status],[sequence], [active], [processing_iteration]) values (20003, 20002, 'Auto Invoices UK+SA: To Generated (Without VATInvDocNum)', 5, 5, 1, 0)


UPDATE dbo.USER_bo_auto_doc_process SET sequence = 12 WHERE query_name = 'Auto Invoices All: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 13 WHERE query_name = 'Auto Invoices All: To Send' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 14 WHERE query_name = 'Auto Invoices All: To Cancelled' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 15 WHERE query_name = 'Auto Invoices All: To New Doc' AND active = 1

UPDATE dbo.USER_bo_auto_doc_process SET sequence = 6 WHERE query_name = 'Auto Invoices UK+SA: To Approval Required' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 7 WHERE query_name = 'Auto Invoices UK+SA: To Send' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 8 WHERE query_name = 'Auto Invoices UK+SA: To Ignore for Pymt' AND active = 1
UPDATE dbo.USER_bo_auto_doc_process SET sequence = 9 WHERE query_name = 'Auto Invoices UK+SA: Completed' AND active = 1

go
select count(*) from USER_bo_auto_doc_process

go
commit tran

go


