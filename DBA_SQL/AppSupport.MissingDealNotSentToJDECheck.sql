USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[MissingDealNotSentToJDECheck]    Script Date: 25/07/2018 10:26:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO


ALTER PROC [AppSupport].[MissingDealNotSentToJDECheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.MissingDealNotSentToJDECheck
-- Description:  Missing Deal Not Sent to JDE Check
-- Email:  Support Team
-- Frequency: 
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira145			C Badcock	Aug 2020	04			Made compatible for v17 Production Database
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##MissingDealNotSentToJDECheck') IS NOT NULL
		DROP TABLE ##MissingDealNotSentToJDECheck


	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(MIN(name),'END_V17PROD')     FROM sys.databases     WHERE name like '%PROD'

	SET @sql_stmt = 
	'SELECT Issue,DealNum,TradeDate,StartDate,MaturityDate,TranStatus,InsType,CflowType,LastUpdated, GLStamping
	INTO ##MissingDealNotSentToJDECheck
	FROM(
		SELECT Issue,DealNum,TradeDate,StartDate,MaturityDate,TranStatus,InsType,CflowType,LastUpdated, GLStamping
		FROM(
			SELECT DISTINCT ''GL Stamping not updated after Cancellation'' As Issue,
			ab.deal_tracking_num DealNum,
			cast(ab.trade_date as date) TradeDate,
			cast(ab.start_date as date) StartDate,
			cast(ab.maturity_date as date) MaturityDate,
			ts.name TranStatus,
			i.name InsType,
			ct.name CflowType,
			ab.last_update LastUpdated, 
			abiv.value GLStamping
			FROM ' + @db_name + '.dbo.stldoc_header sh
			JOIN ' + @db_name + '.dbo.stldoc_details sd on sd.document_num = sh.document_num and sh.doc_status = 4 and sh.last_doc_status not in (5,6,22) and sh.doc_type in (1,2)
			LEFT JOIN ' + @db_name + '.dbo.ab_tran ab on ab.deal_tracking_num = sd.deal_tracking_num and ab.tran_status in (5)
			join ' + @db_name + '.dbo.ab_tran_info_view abiv on abiv.tran_num = ab.tran_num and abiv.type_id = 20024
			JOIN ' + @db_name + '.dbo.trans_status ts on ts.trans_status_id = ab.tran_status
			JOIN ' + @db_name + '.dbo.instruments i on i.id_number=ab.ins_type
			JOIN ' + @db_name + '.dbo.cflow_Type ct on ct.id_number= ab.cflow_type
			WHERE sd.ins_type != 32007 
			and abiv.value = ''Sent''
			and ab.offset_tran_num is null
			and ab.internal_bunit in (20001,20016) and ab.ins_sub_type != 10001
			AND ab.trade_date >=DATEADD(m, DATEDIFF(m, 0, GETDATE()-1), 0)
		union 
			SELECT DISTINCT ''GL Stamping not updated after Validation'' As Issue,
			ab.deal_tracking_num DealNum,
			cast(ab.trade_date as date) TradeDate,
			cast(ab.start_date as date) StartDate,
			cast(ab.maturity_date as date) MaturityDate,
			ts.name TranStatus,
			i.name InsType,
			ct.name CflowType,
			ab.last_update LastUpdated, 
			abiv.value GLStamping
			FROM ' + @db_name + '.dbo.stldoc_header sh
			JOIN ' + @db_name + '.dbo.stldoc_details sd on sd.document_num = sh.document_num and sh.doc_status = 5 and sh.last_doc_status = 0 and sh.doc_type in (1,2)
			LEFT JOIN ' + @db_name + '.dbo.ab_tran ab on ab.deal_tracking_num = sd.deal_tracking_num and ab.tran_status in (3)
			join ' + @db_name + '.dbo.ab_tran_info_view abiv on abiv.tran_num = ab.tran_num and abiv.type_id = 20024
			JOIN ' + @db_name + '.dbo.trans_status ts on ts.trans_status_id = ab.tran_status
			JOIN ' + @db_name + '.dbo.instruments i on i.id_number=ab.ins_type
			JOIN ' + @db_name + '.dbo.cflow_Type ct on ct.id_number= ab.cflow_type
			WHERE sd.ins_type != 32007 
			and abiv.value != ''Sent''
			and ab.offset_tran_num is null
			and ab.internal_bunit in (20001,20016) and ab.ins_sub_type != 10001
			AND ab.trade_date >=DATEADD(m, DATEDIFF(m, 0, GETDATE()-1), 0)
		)x
	)x'

	EXEC sp_executesql @sql_stmt


	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME
		DECLARE @email_count int
		DECLARE @email_address_new VARCHAR(1000)
		DECLARE @proc_name VARCHAR(500)
		DECLARE @email_db_name varchar(20)
		
		IF @db_name = 'END_V17PROD' 
			SET @email_db_name = 'Production v17- '
		ELSE 
			SET @email_db_name = 'UAT v17- '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Missing Deal Not Sent To JDE Check - FAILED'

		  
		SET @email_query = 'SELECT * from ##MissingDealNotSentToJDECheck'

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

		IF @debug = 0  BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address_new,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##MissingDealNotSentToJDECheck
			RAISERROR ('MissingDealNotSentToJDECheck',16,1) 
			RETURN(1)
		END

	END

	IF OBJECT_ID('tempdb..##MissingDealNotSentToJDECheck') IS NOT NULL
	DROP TABLE ##MissingDealNotSentToJDECheck

END

