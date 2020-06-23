

INSERT INTO USER_generic_wflow_query_list VALUES(
70,'Emir Empty File Check'
,'select deal_count.total as deal_count ,file_count.total  as file_count from (SELECT COUNT(u.tran_num) AS total FROM  USER_jm_emir_log  u  '
,1,1,0,0)

go
INSERT INTO USER_generic_wflow_query_list VALUES(
71,'Emir Empty File Check'
,', (select  convert(datetime,convert(char(19),date_value,126)) as date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''LastRunTime'') lastRunTime '
,2,1,0,0)

go

INSERT INTO USER_generic_wflow_query_list VALUES(
72,'Emir Empty File Check'
,', (select  convert(datetime,convert(char(19),date_value,126)) as date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''secondLastRunTime'') secondLastRunTime '
,3,1,0,0)

go

INSERT INTO USER_generic_wflow_query_list VALUES(
73,'Emir Empty File Check'
,'WHERE 1=1 AND u.last_update >= secondLastRunTime.date_value AND u.last_update <= lastRunTime.date_value) as file_count '
,4,1,0,0)

go

INSERT INTO USER_generic_wflow_query_list VALUES(
74,'Emir Empty File Check'
,',(SELECT COUNT(ab.tran_num) AS total FROM  ab_tran  ab  '
,5,1,0,0)

go

INSERT INTO USER_generic_wflow_query_list VALUES(
75,'Emir Empty File Check'
,', (select date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''LastRunTime'') lastRunTime '
,6,1,0,0)

go

INSERT INTO USER_generic_wflow_query_list VALUES(
76,'Emir Empty File Check'
,', (select date_value from user_const_repository where context = ''EMIR'' and sub_context = ''RegisTR'' and name = ''secondLastRunTime'') secondLastRunTime '
,7,1,0,0)

go 

INSERT INTO USER_generic_wflow_query_list VALUES(
77,'Emir Empty File Check'
,'WHERE ab.toolset = 17 AND ab.trade_time >= secondLastRunTime.date_value AND ab.trade_time <= lastRunTime.date_value) as deal_count WHERE deal_count.total > 0  AND file_count.total = 0 '
,8,1,0,0)

