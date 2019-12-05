USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[TPM_Support_Working]    Script Date: 28/11/2019 14:07:16 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[TPM_Support_Working] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')  
-------------------------------------------------------  
-- APPSupport.TPM_Support_Working  
-- Description: Used to track when the TPM service has stopped from which everything else hangs off  
--     when this happens and manual restart fails we will need to use the backup TPM process.  
-- Email:  Support Team  
-- Frequency: Every 10 minutes throughout the from 2 am to 9.00 pm  
-- Jira/Ivanti:  Author    Date:    Version:  Reason:   
-- I618114/EPI937   C Badcock  Sept 2019  00   Added First Version  
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic  
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables  
-- Jira989          C Badcock     Dec 2019     03            Correction to is_working
-------------------------------------------------------  
  
AS   
BEGIN  
  
 SET NOCOUNT ON  
  
 DECLARE @rowcount INT  
  
 IF OBJECT_ID('tempdb..##TPM_Support_Working') IS NOT NULL DROP TABLE ##TPM_Support_Working  
  
 -- Dynamic SQL to pick up the correct database name for cross-server deployment  
 -- Works only if there is a database beginning with "OLEM" on the DB Server  
 -- If no such database is found, the query runs for the prod database "OLEME00P"  
  
 DECLARE @db_name varchar(20)  
 declare @sql_stmt nvarchar(4000)  
  
 -- Top 1 as test/dev DB servers have multiple databases  
 -- If no such databases found, use the PROD DB name  
  
 SELECT TOP 1 @db_name = ISNULL(name,'OLEME00P')  
 FROM sys.databases  
 WHERE name like 'olem%'  
  
 DECLARE @is_working INT   
 DECLARE @paramlist NVARCHAR(4000)

 -- Construct and execute dynamic SQL  
 SET @sql_stmt = 'SELECT @is_working =  CASE WHEN ISNULL(jr1.status,0) = 18 THEN 1 ELSE (CASE WHEN ISNULL(jr2.status,0) = 18 THEN 1 ELSE 0 END) END        
     FROM ' + @db_name + '.dbo.job_cfg jc_h   
     JOIN ' + @db_name + '.dbo.job_cfg jc_1 ON (jc_1.wflow_id=jc_h.wflow_id AND jc_1.sub_id=18)   
     LEFT JOIN ' + @db_name + '.dbo.job_cfg jc_2 ON (jc_2.wflow_id=jc_h.wflow_id AND jc_2.sub_id=23)   
     LEFT JOIN ' + @db_name + '.dbo.job_running  jr1 ON (jr1.job_cfg_id = jc_1.job_id )  
     LEFT JOIN ' + @db_name + '.dbo.job_running  jr2 ON (jr2.job_cfg_id = jc_2.job_id )  
     WHERE jc_h.name = ''TPM_Support'''  
  
 SET @paramlist = N'@is_working INT OUTPUT'  
  
  
 EXEC sp_executesql @sql_stmt, @paramlist, @is_working OUTPUT
  
 SELECT @rowcount = @@ROWCOUNT  
  
 IF @debug = 1 PRINT @rowcount  
  
 IF @is_working = 0 BEGIN  
  
  SET @sql_stmt = 'SELECT jc_h.name , CASE WHEN ISNULL(jr1.status,0) = 18 THEN 1 ELSE (CASE WHEN ISNULL(jr2.status,0) = 18 THEN 1 ELSE 0 END) END as working ,  
      jc_1.job_id  primary_job_id , jc_2.job_id  secondary_job_id , jr1.status primary_status , jr2.status secondary_status,  
      ( CASE WHEN ISNULL(jr1.status, -1) = -1 THEN ''Not Found'' ELSE ( CASE WHEN jr1.status= 18 THEN ''Running'' ELSE  js1.name END) END)  primary_status,   
      ( CASE WHEN ISNULL(jr2.status, -1) = -1 THEN ''Not Found'' ELSE ( CASE WHEN jr2.status= 18 THEN ''Running'' ELSE  js2.name END) END)  secondary_status  
      INTO ##TPM_Support_Working  
      FROM ' + @db_name + '.dbo.job_cfg jc_h   
      JOIN ' + @db_name + '.dbo.job_cfg jc_1 ON (jc_1.wflow_id=jc_h.wflow_id AND jc_1.sub_id=18)   
       JOIN ' + @db_name + '.dbo.job_cfg jc_2 ON (jc_2.wflow_id=jc_h.wflow_id AND jc_2.sub_id=23)   
      LEFT JOIN ' + @db_name + '.dbo.job_running  jr1 ON (jr1.job_cfg_id = jc_1.job_id )  
      LEFT JOIN ' + @db_name + '.dbo.job_running  jr2 ON (jr2.job_cfg_id = jc_2.job_id )  
      LEFT  JOIN ' + @db_name + '.dbo.job_status js1 ON (js1.id_number= jr1.status)  
      LEFT  JOIN ' + @db_name + '.dbo.job_status js2 ON (js2.id_number= jr2.status)  
      WHERE jc_h.name = ''TPM_Support'''  
  
  
  
  EXEC sp_executesql @sql_stmt  
  
  
  DECLARE @email_subject NVARCHAR(100)  
  DECLARE @email_query   NVARCHAR(2000)  
  DECLARE @profile_name SYSNAME  
  DECLARE @email_db_name varchar(20)  
  DECLARE @email_count int  
  DECLARE @email_address_new VARCHAR(1000)  
  DECLARE @proc_name VARCHAR(500)  
   
    
  IF @db_name = 'OLEME00P'   
   SET @email_db_name = 'Production - '  
  ELSE   
   SET @email_db_name = 'UAT - '  
   
  SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Trade Process Mgmt service appears offline - try to restart manually'  
    
      
  SET @email_query = 'SELECT * from ##TPM_Support_Working'  
  
  IF @debug = 1 PRINT @email_query  
  
  SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1  
    
  -- Check if there are any valid active users mapped to the alert. If not, default to Endur Support email id  
  SET @proc_name = OBJECT_NAME(@@PROCID)  
  SELECT @email_count = COUNT(*)  
   FROM AppSupport.Uvw_AlertUserMapping  
   WHERE AlertName = @proc_name AND UserActive = 1  
  
  IF @email_count = 0  
   SET @email_address_new = 'GRPEndurSupportTeam@matthey.com'  
  ELSE  
   SELECT @email_address_new = stuff(list,1,1,'')  
    FROM(  
     SELECT ';' + CAST(UserEmail AS varchar(500))  
     FROM AppSupport.Uvw_AlertUserMapping  
     WHERE AlertName = @proc_name AND UserActive = 1  
     FOR XML PATH('')  
    ) Sub(list)  
  
  
  IF @debug = 1 PRINT @email_address_new  
  
  
  IF @debug = 0 BEGIN  
    EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address_new,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1  
    IF OBJECT_ID('tempdb..##TPM_Support_Working') IS NOT NULL DROP TABLE ##TPM_Support_Working  
    RAISERROR ('TPM_Support_Working',16,1)   
    RETURN(1)  
  END  
  
 END  
  
 IF OBJECT_ID('tempdb..##TPM_Support_Working') IS NOT NULL DROP TABLE ##TPM_Support_Working  
  
END 