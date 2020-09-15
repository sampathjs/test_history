Delete from dbo.USER_generic_wflow_query_list where query_name in('Emir Empty File Check')
GO
declare @maxId as int;

select @maxId = max(id) from USER_generic_wflow_query_list



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+1,'Emir Empty File Check'
,'SELECT dc.*,fc.* FROM   '
,1,1,0,0)


INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+2,'Emir Empty File Check'
,'(SELECT count(*) AS deal_count FROM ab_tran ab,(SELECT dateadd(HOUR,6,prev_business_date)  AS prev_run_time ,dateadd(HOUR,6,business_date) AS curr_run_time FROM system_dates)  dates '
,2,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+3,'Emir Empty File Check'
,'WHERE toolset = 17 AND ab.trade_time > dates.prev_run_time AND ab.trade_time < dates.curr_run_time) dc '
,3,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+4,'Emir Empty File Check'
,',(SELECT count(*) AS file_count FROM user_jm_emir_log u,(SELECT dateadd(MINUTE,15,dateadd(HOUR,6,prev_business_date))  AS prev_run_time ,dateadd(HOUR,6,business_date) AS curr_run_time FROM system_dates)  dates '
,4,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+5,'Emir Empty File Check'
,'WHERE u.last_update > dates.prev_run_time AND u.last_update < dates.curr_run_time) fc  '
,5,1,0,0)



INSERT INTO USER_generic_wflow_query_list VALUES(
@maxId+6,'Emir Empty File Check'
,'WHERE CAST (GETDATE() AS TIME)  > ''07:00:00'' AND fc.file_count = 0  AND dc.deal_count > 0 '
,6,1,0,0)


go

select count(*) from dbo.USER_generic_wflow_query_list where query_name in('Emir Empty File Check')
