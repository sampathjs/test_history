USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[MissingInvoiceNotSentToJDECheck]    Script Date: 12/10/2018 11:26:42 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[MissingInvoiceNotSentToJDECheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.
-- Description:  Missing Invoice Not Sent to JDE Check Missing Invoice Not Sent To JDE Check
-- Email:  Support Team 
-- Frequency: Mon-Fri, 1am-22pm, hourly
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##MissingInvoiceNotSentToJDECheck') IS NOT NULL
	DROP TABLE ##MissingInvoiceNotSentToJDECheck

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


	SET @sql_stmt = 'SELECT x.Issue,
		x.JDE_invoice_number,
		x.endur_doc_num,
		x.sl_status,
		x.deal_tracking_num,
		x.tran_status,
		x.ins_type,
		x.cflow_type,
		x.last_updated
	INTO ##MissingInvoiceNotSentToJDECheck
	FROM (
	SELECT DISTINCT ''Status_insert missing in user table'' As Issue,
		si.value JDE_invoice_number, 
		shh.document_num AS endur_doc_num,
		udt.sl_status AS sl_status,
		ab.deal_tracking_num deal_tracking_num,
		ts.name tran_status,
		i.name ins_type,
		ct.name cflow_type,
		udt.last_update last_updated
	FROM ' + @db_name + '.dbo.stldoc_header_hist shh 
	left JOIN ' + @db_name + '.dbo.stldoc_info si on si.document_num=shh.document_num and si.type_id = 20003 
	JOIN ' + @db_name + '.dbo.stldoc_details_hist sd on sd.document_num = shh.document_num
	JOIN ' + @db_name + '.dbo.ab_tran ab on ab.deal_tracking_num = sd.deal_tracking_num and ab.tran_status in (3)
	LEFT JOIN ' + @db_name + '.dbo.user_jm_sl_doc_tracking udt ON sd.document_num = udt.document_num
	JOIN ' + @db_name + '.dbo.trans_status ts on ts.trans_status_id = ab.tran_status
	JOIN ' + @db_name + '.dbo.instruments i on i.id_number=ab.ins_type
	JOIN ' + @db_name + '.dbo.cflow_Type ct on ct.id_number= ab.cflow_type
	WHERE shh.doc_type = 1 AND shh.stldoc_template_id IN (20008, 20015, 20016) 
		AND shh.doc_status = 7
		AND sd.settle_amount != 0
		AND sd.ins_type != 32007 
		AND udt.sl_status is null
		AND ab.trade_date >=DATEADD(m, DATEDIFF(m, 0, GETDATE())-2, 0)
	UNION
	SELECT DISTINCT ''Status_Update missing in user table'' As Issue,
		si.value JDE_invoice_number, shh.document_num AS endur_doc_num,
		udt.sl_status sl_status,
		ab.deal_tracking_num deal_tracking_num,
		ts.name tran_status,
		i.name ins_type,
		ct.name cflow_type,
		ab.last_update last_updated
	FROM ' + @db_name + '.dbo.stldoc_header_hist shh 
	left JOIN ' + @db_name + '.dbo.stldoc_info si on si.document_num=shh.document_num and si.type_id = 20003 
	JOIN ' + @db_name + '.dbo.stldoc_details_hist sd on sd.document_num = shh.document_num
	JOIN ' + @db_name + '.dbo.ab_tran ab on ab.deal_tracking_num = sd.deal_tracking_num and ab.tran_status in (5)
	LEFT JOIN ' + @db_name + '.dbo.user_jm_sl_doc_tracking udt ON sd.document_num = udt.document_num
	JOIN ' + @db_name + '.dbo.trans_status ts on ts.trans_status_id = ab.tran_status
	JOIN ' + @db_name + '.dbo.instruments i on i.id_number=ab.ins_type
	JOIN ' + @db_name + '.dbo.cflow_Type ct on ct.id_number= ab.cflow_type
	WHERE shh.doc_type = 1 AND shh.stldoc_template_id IN (20008, 20015, 20016) 
		AND shh.doc_status = 4
		AND sd.settle_amount != 0
		AND sd.ins_type != 32007 
		AND udt.sl_status not in  (''Pending Cancelled'',''Cancelled Sent'',''NOT Sent'')
		AND ab.external_bunit not in (20006,20008,20065)
		AND ct.name NOT LIKE ''Metal Rentals%''
		AND ab.trade_date >=DATEADD(m, DATEDIFF(m, 0, GETDATE())-2, 0)
	) x'

	EXEC sp_executesql @sql_stmt


	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME

		SET @email_subject = 'Warning - Missing Invoice Not Sent To JDE Check - FAILED'

		  
		SET @email_query = 'SELECT * from ##MissingInvoiceNotSentToJDECheck'

		IF @debug = 1 PRINT @email_query

		SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##MissingInvoiceNotSentToJDECheck
			RAISERROR ('MissingInvoiceNotSentToJDECheck',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##MissingInvoiceNotSentToJDECheck

END
