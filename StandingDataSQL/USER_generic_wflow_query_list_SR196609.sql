Delete from dbo.USER_generic_wflow_query_list where query_name in('Emir Empty File Check')
GO
declare @maxId as int;

select @maxId = max(id) from USER_generic_wflow_query_list


INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+1,'Emir Empty File Check'
,'select deal_count.total as deal_count ,file_count.total  as file_count from (SELECT COUNT(u.tran_num) AS total FROM  USER_jm_emir_log  u  '
,1,1,0,0)


INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+2,'Emir Empty File Check'
,', (select  convert(datetime,convert(char(19),date_value,126)) as date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''LastRunTime'') lastRunTime '
,2,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+3,'Emir Empty File Check'
,', (select  convert(datetime,convert(char(19),date_value,126)) as date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''secondLastRunTime'') secondLastRunTime '
,3,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+4,'Emir Empty File Check'
,'WHERE 1=1 AND u.last_update >= secondLastRunTime.date_value AND u.last_update <= lastRunTime.date_value) as file_count '
,4,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+5,'Emir Empty File Check'
,',(SELECT COUNT(ab.tran_num) AS total FROM  ab_tran  ab  '
,5,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+6,'Emir Empty File Check'
,', (select date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''LastRunTime'') lastRunTime '
,6,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+7,'Emir Empty File Check'
,', (select date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''secondLastRunTime'') secondLastRunTime '
,7,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+8,'Emir Empty File Check'
,'WHERE ab.toolset = 17 AND ab.trade_time > secondLastRunTime.date_value AND ab.trade_time <= lastRunTime.date_value) as deal_count WHERE deal_count.total > 0  AND file_count.total = 0 '
,8,1,0,0)

go

select count(*) from dbo.USER_generic_wflow_query_list where query_name in('Emir Empty File Check')
