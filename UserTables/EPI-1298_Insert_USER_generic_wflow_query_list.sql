Delete from dbo.USER_generic_wflow_query_list where query_name in('Undo Hold Status Doc with Netting Document')
GO
declare @maxId as int;

select @maxId = max(id) from USER_generic_wflow_query_list


insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 1, 'Undo Hold Status Doc with Netting Document' , 'Select Distinct sh.document_num Invoice_Doc_Num,si.value Our_Doc_Num,sds.doc_status_desc Invoice_Doc_Status, sdd.document_num Netting_Doc_Num,sdd.pymt_due_date Netting_Doc_Pymt_Date',1,1,0,0)

insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 2,'Undo Hold Status Doc with Netting Document' , ',sds1.doc_status_desc Netting_Doc_status from stldoc_header sh join stldoc_details sd on sh.document_num = sd.document_num join',2,1,0,0)


insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 3, 'Undo Hold Status Doc with Netting Document' , '(select distinct std.tran_num ,sh.document_num,sh.pymt_due_date,sh.doc_status from ab_tran_event ate, ab_tran ab, stldoc_header sh, stldoc_details std where  (ab.tran_num = ate.tran_num)',3,1,0,0)


insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 4, 'Undo Hold Status Doc with Netting Document' , 'and ate.event_num = std.event_num and std.document_num = sh.document_num  and ab.tran_status = 3 and ab.internal_bunit in(20008,20006)  and ate.event_type in (14,98)',4,1,0,0)


insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 5, 'Undo Hold Status Doc with Netting Document' , 'and sh.doc_type = 20001  and sh.doc_status in (20,21)  and sh.pymt_due_date < (select business_date from configuration)) sdd on sd.tran_num = sdd.tran_num',5,1,0,0)



insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 6, 'Undo Hold Status Doc with Netting Document' , 'join stldoc_document_status sds on sh.doc_status = sds.doc_status join stldoc_document_status sds1 on sdd.doc_status = sds1.doc_status',6,1,0,0)


insert into dbo.USER_generic_wflow_query_list (id,query_name , query, sequence, active, exact_expected_rows,max_expected_rows ) values 
(@maxId + 7, 'Undo Hold Status Doc with Netting Document' , 'join stldoc_info si on sh.document_num = si.document_num and si.type_id = 20003 where  sh.doc_status = 16 and sh.doc_type =1',7,1,0,0)


GO
select count(*) from dbo.USER_generic_wflow_query_list where query_name in('Undo Hold Status Doc with Netting Document')