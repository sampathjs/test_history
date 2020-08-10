USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[EndurServiceCheck]    Script Date: 18/05/2020 09:32:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO


-------------------------------------------------------
-- APPSupport.EndurServiceCheck
-- Description: Identifies Endur Service is running correctly
-- Email:  Support Team
-- Frequency: Mon-Fri, 10pm, Daily
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- 1017 			C Badcock 	Aug 2020 	00			Initial Version - always in PROD but not captured.
-- Jira145			C Badcock	Aug 2020	03			Made compatible for v17 Production Database
-------------------------------------------------------


ALTER PROC [AppSupport].[EndurServiceCheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')

AS 
BEGIN

SET NOCOUNT ON

DECLARE @rowcount INT

DECLARE @db_name varchar(20)  
declare @sql_stmt nvarchar(4000)  
  


 -- Top 1 as test/dev DB servers have multiple databases  
 -- If no such databases found, use the PROD DB name  
  
SELECT TOP 1 @db_name = ISNULL(MIN(name),'END_V17PROD')     FROM sys.databases     WHERE name like '%PROD'  


IF OBJECT_ID('tempdb..##EndurServiceCheck') IS NOT NULL
	DROP TABLE ##EndurServiceCheck

SET @sql_stmt = '
	SELECT service_name, workstation_name, deactivation_time
		INTO ##EndurServiceCheck
		FROM ' + @db_name + '.dbo.service_mgr sm
	INNER JOIN  ' + @db_name + '.dbo.service_mgr_run_h  smh
		ON smh.runsite_id = sm.id
	WHERE smh.run_id IN (SELECT MAX(run_id) FROM  ' + @db_name + '.dbo.service_mgr_run_h GROUP BY runsite_id)
		AND smh.deactivation_time <> ''1900-01-01 00:00:00.000''
	ORDER BY service_name'

IF @debug = 1 PRINT @sql_stmt 

EXEC sp_executesql @sql_stmt  

IF @debug = 1 PRINT @rowcount  

SELECT @rowcount = @@ROWCOUNT



IF @rowcount > 0  BEGIN

	DECLARE @email_subject NVARCHAR(100)
	DECLARE @email_query   NVARCHAR(2000)
	DECLARE @profile_name SYSNAME
	DECLARE @email_db_name varchar(20)  
	DECLARE @email_count int  
	DECLARE @email_address_new VARCHAR(1000)  
	DECLARE @proc_name VARCHAR(500)  


	IF @db_name = 'END_V17PROD'   
		SET @email_db_name = 'Production v17- '  
	ELSE   
		SET @email_db_name = 'UAT v17- '  
  
	SET @email_subject = 'Endur Alert : Priority = 3 :' + @email_db_name + ' DBA Warning - Check all prod Endur Services are running'  


	  
	SET @email_query = 'SELECT * from ##EndurServiceCheck'

	IF @debug = 1 PRINT @email_query

	SELECT  @profile_name =    name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

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
  
	SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1  
  
	IF @debug = 0 BEGIN  
  
		EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address_new,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
		DROP TABLE ##EndurServiceCheck
		RAISERROR ('EndurServiceCheck',16,1) 
		RETURN(1) 
   
	END  


	

 


END

DROP TABLE ##EndurServiceCheck

END
GO

